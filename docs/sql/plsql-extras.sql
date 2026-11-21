-- =====================================================================
-- NextFit - PL/SQL extras (Base de Datos)
-- Ejecutar como ADMIN en Database Actions (Oracle Autonomous).
-- Estos objetos NO cambian el comportamiento de la aplicación Java:
-- la app no los llama. Sirven para mostrar PL/SQL sobre las tablas reales.
-- Se puede ejecutar de nuevo sin problema (CREATE OR REPLACE / IF NOT EXISTS).
-- =====================================================================

ALTER SESSION SET CURRENT_SCHEMA = NEXTFIT_APP;

-- ---------------------------------------------------------------------
-- 1. Tabla de apoyo para el disparador de pagos.
--    OJO: no se escribe en audit_log, porque la app lee su columna "action"
--    con AuditAction.valueOf y un valor desconocido la rompería.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payment_db_log (
    id         NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payment_id NUMBER(19)    NOT NULL,
    member_id  NUMBER(19)    NOT NULL,
    subtotal   NUMBER(12,2)  NOT NULL,
    logged_at  TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
    db_user    VARCHAR2(128) NOT NULL
);

-- ---------------------------------------------------------------------
-- 2. VISTA: socios activos con una membresía vigente hoy.
--    Una vista es una consulta guardada con nombre.
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW v_active_members AS
SELECT m.id,
       m.full_name,
       m.document_number,
       MAX(s.end_date)                          AS valid_until,
       MAX(s.end_date) - TRUNC(SYSDATE)         AS days_left
FROM   members m
JOIN   subscriptions s ON s.member_id = m.id
WHERE  m.active = 1
AND    s.cancelled = 0
AND    TRUNC(SYSDATE) BETWEEN s.start_date AND s.end_date
GROUP  BY m.id, m.full_name, m.document_number;

-- ---------------------------------------------------------------------
-- 3. FUNCIÓN: días que le quedan a un socio.
--    Misma regla que Subscription.daysLeftOn en Java: si no tiene
--    membresía vigente devuelve 0, nunca un número negativo.
--    Lanza un error propio (-20002) si el socio no existe.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_days_left (p_member_id IN NUMBER)
RETURN NUMBER
IS
    v_exists NUMBER;
    v_last   DATE;
BEGIN
    SELECT COUNT(*) INTO v_exists FROM members WHERE id = p_member_id;
    IF v_exists = 0 THEN
        RAISE_APPLICATION_ERROR(-20002, 'No existe el socio ' || p_member_id);
    END IF;

    -- MAX nunca lanza NO_DATA_FOUND: si no hay filas devuelve NULL.
    SELECT MAX(end_date)
    INTO   v_last
    FROM   subscriptions
    WHERE  member_id = p_member_id
    AND    cancelled = 0
    AND    TRUNC(SYSDATE) BETWEEN start_date AND end_date;

    IF v_last IS NULL THEN
        RETURN 0;
    END IF;
    RETURN TRUNC(v_last) - TRUNC(SYSDATE);
END fn_days_left;
/

-- ---------------------------------------------------------------------
-- 4. PROCEDIMIENTO: renovar la membresía de un socio y registrar el pago.
--    Hace dos INSERT que deben ir juntos (suscripción + pago).
--    No hace COMMIT: quien lo llama decide si confirma o hace ROLLBACK.
--    Reglas iguales a las de Java:
--      * empieza hoy, o el día siguiente al vencimiento si aún tiene vigente
--      * termina en inicio + duración - 1 (MembershipPlan.endDateFor)
--    p_method debe ser CASH, CARD o TRANSFER (valores del enum PaymentMethod).
-- ---------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_renew_subscription (
    p_member_id IN NUMBER,
    p_plan_id   IN NUMBER,
    p_method    IN VARCHAR2,
    p_user      IN VARCHAR2 DEFAULT 'DB'
)
IS
    v_duration NUMBER;
    v_price    NUMBER;
    v_active   NUMBER;
    v_last_end DATE;
    v_start    DATE;
    v_sub_id   NUMBER;
BEGIN
    IF p_method NOT IN ('CASH', 'CARD', 'TRANSFER') THEN
        RAISE_APPLICATION_ERROR(-20004, 'Método de pago inválido: ' || p_method);
    END IF;

    BEGIN
        SELECT duration_days, price, active
        INTO   v_duration, v_price, v_active
        FROM   membership_plans
        WHERE  id = p_plan_id;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20003, 'No existe el plan ' || p_plan_id);
    END;

    IF v_active = 0 THEN
        RAISE_APPLICATION_ERROR(-20005, 'El plan ' || p_plan_id || ' está inactivo');
    END IF;

    SELECT MAX(end_date)
    INTO   v_last_end
    FROM   subscriptions
    WHERE  member_id = p_member_id
    AND    cancelled = 0
    AND    end_date >= TRUNC(SYSDATE);

    IF v_last_end IS NULL THEN
        v_start := TRUNC(SYSDATE);
    ELSE
        v_start := TRUNC(v_last_end) + 1;
    END IF;

    INSERT INTO subscriptions (member_id, plan_id, start_date, end_date, cancelled)
    VALUES (p_member_id, p_plan_id, v_start, v_start + v_duration - 1, 0)
    RETURNING id INTO v_sub_id;

    -- discount_type y discount_value se llenan con NONE y 0 porque JdbcPaymentRepository
    -- lee el tipo con un switch y falla si viene NULL (pago sin descuento = NONE).
    INSERT INTO payments (subscription_id, member_id, paid_at, method, subtotal,
                          discount_type, discount_value, registered_by)
    VALUES (v_sub_id, p_member_id, SYSTIMESTAMP, p_method, v_price,
            'NONE', 0, p_user);
END sp_renew_subscription;
/

-- ---------------------------------------------------------------------
-- 5. DISPARADOR 1 (AFTER INSERT): cada pago nuevo deja una fila en
--    payment_db_log, con el usuario de base de datos que lo insertó.
--    Se dispara por cada fila (FOR EACH ROW), sin importar quién inserte:
--    la app, SQL Developer o el procedimiento anterior.
-- ---------------------------------------------------------------------
CREATE OR REPLACE TRIGGER trg_payments_log
AFTER INSERT ON payments
FOR EACH ROW
BEGIN
    INSERT INTO payment_db_log (payment_id, member_id, subtotal, db_user)
    VALUES (:NEW.id, :NEW.member_id, :NEW.subtotal,
            SYS_CONTEXT('USERENV', 'SESSION_USER'));
END trg_payments_log;
/

-- ---------------------------------------------------------------------
-- 6. DISPARADOR 2 (BEFORE INSERT/UPDATE): impide una suscripción cuya
--    fecha final sea anterior a la inicial. Es la misma regla del
--    constructor de Subscription en Java, ahora también en la base.
-- ---------------------------------------------------------------------
CREATE OR REPLACE TRIGGER trg_subscriptions_dates
BEFORE INSERT OR UPDATE OF start_date, end_date ON subscriptions
FOR EACH ROW
WHEN (NEW.end_date < NEW.start_date)
BEGIN
    RAISE_APPLICATION_ERROR(-20010,
        'La fecha final no puede ser anterior a la inicial');
END trg_subscriptions_dates;
/

-- ---------------------------------------------------------------------
-- 7. PAQUETE: agrupa consultas sobre membresías por vencer.
--    La especificación (spec) es lo público; el cuerpo (body) es la lógica.
-- ---------------------------------------------------------------------
CREATE OR REPLACE PACKAGE pkg_member_stats AS
    FUNCTION  expiring_count (p_days IN NUMBER) RETURN NUMBER;
    PROCEDURE print_expiring (p_days IN NUMBER);
END pkg_member_stats;
/

CREATE OR REPLACE PACKAGE BODY pkg_member_stats AS

    FUNCTION expiring_count (p_days IN NUMBER) RETURN NUMBER
    IS
        v_total NUMBER;
    BEGIN
        SELECT COUNT(DISTINCT s.member_id)
        INTO   v_total
        FROM   subscriptions s
        JOIN   members m ON m.id = s.member_id
        WHERE  m.active = 1
        AND    s.cancelled = 0
        AND    s.end_date BETWEEN TRUNC(SYSDATE) AND TRUNC(SYSDATE) + p_days;
        RETURN v_total;
    END expiring_count;

    PROCEDURE print_expiring (p_days IN NUMBER)
    IS
        -- Cursor explícito: recorre las filas una por una.
        CURSOR c_expiring IS
            SELECT m.full_name, MAX(s.end_date) AS end_date
            FROM   subscriptions s
            JOIN   members m ON m.id = s.member_id
            WHERE  m.active = 1
            AND    s.cancelled = 0
            AND    s.end_date BETWEEN TRUNC(SYSDATE) AND TRUNC(SYSDATE) + p_days
            GROUP  BY m.full_name
            ORDER  BY MAX(s.end_date);
    BEGIN
        FOR r IN c_expiring LOOP
            DBMS_OUTPUT.PUT_LINE(r.full_name || ' vence el '
                                 || TO_CHAR(r.end_date, 'DD/MM/YYYY'));
        END LOOP;
    END print_expiring;

END pkg_member_stats;
/

-- =====================================================================
-- PRUEBAS (para mostrar en la sustentación)
-- =====================================================================

-- Qué objetos quedaron creados:
SELECT object_type, object_name, status
FROM   all_objects
WHERE  owner = 'NEXTFIT_APP'
AND    object_type IN ('VIEW','FUNCTION','PROCEDURE','TRIGGER','PACKAGE','PACKAGE BODY')
ORDER  BY object_type, object_name;

-- Vista y función:
SELECT * FROM v_active_members FETCH FIRST 5 ROWS ONLY;
SELECT id, full_name, fn_days_left(id) AS days_left FROM members FETCH FIRST 5 ROWS ONLY;

-- Paquete (activar DBMS_OUTPUT en SQL Developer / Database Actions):
SET SERVEROUTPUT ON
SELECT pkg_member_stats.expiring_count(30) AS expiring_in_30_days FROM dual;
BEGIN
    pkg_member_stats.print_expiring(30);
END;
/

-- Procedimiento + disparador 1, sin guardar nada (termina en ROLLBACK):
DECLARE
    v_member NUMBER;
    v_plan   NUMBER;
BEGIN
    SELECT MIN(id) INTO v_member FROM members WHERE active = 1;
    SELECT MIN(id) INTO v_plan   FROM membership_plans WHERE active = 1;
    sp_renew_subscription(v_member, v_plan, 'CASH', 'demo');
    DBMS_OUTPUT.PUT_LINE('Renovado el socio ' || v_member);
END;
/
SELECT * FROM payment_db_log ORDER BY id DESC FETCH FIRST 3 ROWS ONLY;
ROLLBACK;

-- Disparador 2: este INSERT debe fallar con ORA-20010 (y no deja nada).
-- Descomentar para mostrarlo:
-- INSERT INTO subscriptions (member_id, plan_id, start_date, end_date, cancelled)
-- VALUES ((SELECT MIN(id) FROM members), (SELECT MIN(id) FROM membership_plans),
--         DATE '2026-12-10', DATE '2026-12-01', 0);
