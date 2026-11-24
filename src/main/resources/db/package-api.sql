-- NextFit Oracle package API v1. Run as the OWNER of the NextFit tables.
-- Additive installation: no table/data drops. DDL commits; stop the app first.
-- Application procedures never COMMIT; the JDBC caller owns transactions.
SET DEFINE OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE

CREATE OR REPLACE PACKAGE pkg_nf_members AUTHID DEFINER AS
  PROCEDURE measurements_by_member(p_1 IN measurements.member_id%TYPE, p_rows OUT SYS_REFCURSOR);
  PROCEDURE measurements_save(p_member_id IN measurements.member_id%TYPE, p_measured_on IN measurements.measured_on%TYPE, p_weight_kg IN measurements.weight_kg%TYPE, p_waist_cm IN measurements.waist_cm%TYPE, p_chest_cm IN measurements.chest_cm%TYPE, p_hips_cm IN measurements.hips_cm%TYPE, p_id IN measurements.id%TYPE, p_result OUT NUMBER);
  PROCEDURE members_all(p_rows OUT SYS_REFCURSOR);
  PROCEDURE members_by_id(p_1 IN members.id%TYPE, p_rows OUT SYS_REFCURSOR);
  PROCEDURE members_by_document(p_1 IN members.document_number%TYPE, p_rows OUT SYS_REFCURSOR);
  PROCEDURE members_save(p_full_name IN members.full_name%TYPE, p_document_number IN members.document_number%TYPE, p_phone IN members.phone%TYPE, p_email IN members.email%TYPE, p_birth_date IN members.birth_date%TYPE, p_join_date IN members.join_date%TYPE, p_photo_path IN members.photo_path%TYPE, p_emergency_contact IN members.emergency_contact%TYPE, p_active IN members.active%TYPE, p_id IN members.id%TYPE, p_result OUT NUMBER);
  PROCEDURE health_profiles_by_member(p_1 IN health_profiles.member_id%TYPE, p_rows OUT SYS_REFCURSOR);
  PROCEDURE health_profiles_save(p_height_cm IN health_profiles.height_cm%TYPE, p_blood_type IN health_profiles.blood_type%TYPE, p_medical_conditions IN health_profiles.medical_conditions%TYPE, p_allergies IN health_profiles.allergies%TYPE, p_id IN health_profiles.member_id%TYPE, p_result OUT NUMBER);
END pkg_nf_members;
/

CREATE OR REPLACE PACKAGE BODY pkg_nf_members AS
  PROCEDURE measurements_by_member(p_1 IN measurements.member_id%TYPE, p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, member_id, measured_on, weight_kg, waist_cm, chest_cm, hips_cm FROM measurements WHERE member_id = p_1 ORDER BY measured_on, id;
  END measurements_by_member;

  PROCEDURE measurements_save(p_member_id IN measurements.member_id%TYPE, p_measured_on IN measurements.measured_on%TYPE, p_weight_kg IN measurements.weight_kg%TYPE, p_waist_cm IN measurements.waist_cm%TYPE, p_chest_cm IN measurements.chest_cm%TYPE, p_hips_cm IN measurements.hips_cm%TYPE, p_id IN measurements.id%TYPE, p_result OUT NUMBER) IS
  BEGIN
    IF p_id <> 0 THEN
      UPDATE measurements SET member_id = p_member_id, measured_on = p_measured_on, weight_kg = p_weight_kg, waist_cm = p_waist_cm, chest_cm = p_chest_cm, hips_cm = p_hips_cm WHERE id = p_id;
      IF SQL%ROWCOUNT > 0 THEN p_result := p_id; RETURN; END IF;
    END IF;
    IF p_id = 0 THEN
      INSERT INTO measurements (member_id, measured_on, weight_kg, waist_cm, chest_cm, hips_cm) VALUES (p_member_id, p_measured_on, p_weight_kg, p_waist_cm, p_chest_cm, p_hips_cm) RETURNING id INTO p_result;
    ELSE
      INSERT INTO measurements (member_id, measured_on, weight_kg, waist_cm, chest_cm, hips_cm, id) VALUES (p_member_id, p_measured_on, p_weight_kg, p_waist_cm, p_chest_cm, p_hips_cm, p_id);
      p_result := p_id;
    END IF;
  END measurements_save;

  PROCEDURE members_all(p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, full_name, document_number, phone, email, birth_date, join_date, photo_path, emergency_contact, active FROM members ORDER BY id;
  END members_all;

  PROCEDURE members_by_id(p_1 IN members.id%TYPE, p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, full_name, document_number, phone, email, birth_date, join_date, photo_path, emergency_contact, active FROM members WHERE id = p_1;
  END members_by_id;

  PROCEDURE members_by_document(p_1 IN members.document_number%TYPE, p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, full_name, document_number, phone, email, birth_date, join_date, photo_path, emergency_contact, active FROM members WHERE document_number = p_1;
  END members_by_document;

  PROCEDURE members_save(p_full_name IN members.full_name%TYPE, p_document_number IN members.document_number%TYPE, p_phone IN members.phone%TYPE, p_email IN members.email%TYPE, p_birth_date IN members.birth_date%TYPE, p_join_date IN members.join_date%TYPE, p_photo_path IN members.photo_path%TYPE, p_emergency_contact IN members.emergency_contact%TYPE, p_active IN members.active%TYPE, p_id IN members.id%TYPE, p_result OUT NUMBER) IS
  BEGIN
    IF p_id <> 0 THEN
      UPDATE members SET full_name = p_full_name, document_number = p_document_number, phone = p_phone, email = p_email, birth_date = p_birth_date, join_date = p_join_date, photo_path = p_photo_path, emergency_contact = p_emergency_contact, active = p_active WHERE id = p_id;
      IF SQL%ROWCOUNT > 0 THEN p_result := p_id; RETURN; END IF;
    END IF;
    IF p_id = 0 THEN
      INSERT INTO members (full_name, document_number, phone, email, birth_date, join_date, photo_path, emergency_contact, active) VALUES (p_full_name, p_document_number, p_phone, p_email, p_birth_date, p_join_date, p_photo_path, p_emergency_contact, p_active) RETURNING id INTO p_result;
    ELSE
      INSERT INTO members (full_name, document_number, phone, email, birth_date, join_date, photo_path, emergency_contact, active, id) VALUES (p_full_name, p_document_number, p_phone, p_email, p_birth_date, p_join_date, p_photo_path, p_emergency_contact, p_active, p_id);
      p_result := p_id;
    END IF;
  END members_save;

  PROCEDURE health_profiles_by_member(p_1 IN health_profiles.member_id%TYPE, p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT member_id, height_cm, blood_type, medical_conditions, allergies FROM health_profiles WHERE member_id = p_1;
  END health_profiles_by_member;

  PROCEDURE health_profiles_save(p_height_cm IN health_profiles.height_cm%TYPE, p_blood_type IN health_profiles.blood_type%TYPE, p_medical_conditions IN health_profiles.medical_conditions%TYPE, p_allergies IN health_profiles.allergies%TYPE, p_id IN health_profiles.member_id%TYPE, p_result OUT NUMBER) IS
  BEGIN
    UPDATE health_profiles SET height_cm = p_height_cm, blood_type = p_blood_type, medical_conditions = p_medical_conditions, allergies = p_allergies WHERE member_id = p_id;
    IF SQL%ROWCOUNT = 0 THEN
      INSERT INTO health_profiles (height_cm, blood_type, medical_conditions, allergies, member_id) VALUES (p_height_cm, p_blood_type, p_medical_conditions, p_allergies, p_id);
    END IF;
    p_result := p_id;
  END health_profiles_save;

END pkg_nf_members;
/

CREATE OR REPLACE PACKAGE pkg_nf_billing AUTHID DEFINER AS
  PROCEDURE membership_plans_all(p_rows OUT SYS_REFCURSOR);
  PROCEDURE membership_plans_by_id(p_1 IN membership_plans.id%TYPE, p_rows OUT SYS_REFCURSOR);
  PROCEDURE membership_plans_by_name(p_1 IN membership_plans.name%TYPE, p_rows OUT SYS_REFCURSOR);
  PROCEDURE membership_plans_save(p_name IN membership_plans.name%TYPE, p_description IN membership_plans.description%TYPE, p_duration_days IN membership_plans.duration_days%TYPE, p_price IN membership_plans.price%TYPE, p_active IN membership_plans.active%TYPE, p_id IN membership_plans.id%TYPE, p_result OUT NUMBER);
  PROCEDURE payments_all(p_rows OUT SYS_REFCURSOR);
  PROCEDURE payments_by_id(p_1 IN payments.id%TYPE, p_rows OUT SYS_REFCURSOR);
  PROCEDURE payments_by_member(p_1 IN payments.member_id%TYPE, p_rows OUT SYS_REFCURSOR);
  PROCEDURE payments_save(p_subscription_id IN payments.subscription_id%TYPE, p_member_id IN payments.member_id%TYPE, p_paid_at IN payments.paid_at%TYPE, p_method IN payments.method%TYPE, p_subtotal IN payments.subtotal%TYPE, p_discount_type IN payments.discount_type%TYPE, p_discount_value IN payments.discount_value%TYPE, p_registered_by IN payments.registered_by%TYPE, p_id IN payments.id%TYPE, p_result OUT NUMBER);
  PROCEDURE subscriptions_all(p_rows OUT SYS_REFCURSOR);
  PROCEDURE subscriptions_by_id(p_1 IN subscriptions.id%TYPE, p_rows OUT SYS_REFCURSOR);
  PROCEDURE subscriptions_by_member(p_1 IN subscriptions.member_id%TYPE, p_rows OUT SYS_REFCURSOR);
  PROCEDURE subscriptions_save(p_member_id IN subscriptions.member_id%TYPE, p_plan_id IN subscriptions.plan_id%TYPE, p_start_date IN subscriptions.start_date%TYPE, p_end_date IN subscriptions.end_date%TYPE, p_cancelled IN subscriptions.cancelled%TYPE, p_id IN subscriptions.id%TYPE, p_result OUT NUMBER);
END pkg_nf_billing;
/

CREATE OR REPLACE PACKAGE BODY pkg_nf_billing AS
  PROCEDURE membership_plans_all(p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, name, description, duration_days, price, active FROM membership_plans ORDER BY id;
  END membership_plans_all;

  PROCEDURE membership_plans_by_id(p_1 IN membership_plans.id%TYPE, p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, name, description, duration_days, price, active FROM membership_plans WHERE id = p_1;
  END membership_plans_by_id;

  PROCEDURE membership_plans_by_name(p_1 IN membership_plans.name%TYPE, p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, name, description, duration_days, price, active FROM membership_plans WHERE LOWER(name) = LOWER(p_1);
  END membership_plans_by_name;

  PROCEDURE membership_plans_save(p_name IN membership_plans.name%TYPE, p_description IN membership_plans.description%TYPE, p_duration_days IN membership_plans.duration_days%TYPE, p_price IN membership_plans.price%TYPE, p_active IN membership_plans.active%TYPE, p_id IN membership_plans.id%TYPE, p_result OUT NUMBER) IS
  BEGIN
    IF p_id <> 0 THEN
      UPDATE membership_plans SET name = p_name, description = p_description, duration_days = p_duration_days, price = p_price, active = p_active WHERE id = p_id;
      IF SQL%ROWCOUNT > 0 THEN p_result := p_id; RETURN; END IF;
    END IF;
    IF p_id = 0 THEN
      INSERT INTO membership_plans (name, description, duration_days, price, active) VALUES (p_name, p_description, p_duration_days, p_price, p_active) RETURNING id INTO p_result;
    ELSE
      INSERT INTO membership_plans (name, description, duration_days, price, active, id) VALUES (p_name, p_description, p_duration_days, p_price, p_active, p_id);
      p_result := p_id;
    END IF;
  END membership_plans_save;

  PROCEDURE payments_all(p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, subscription_id, member_id, paid_at, method, subtotal, discount_type, discount_value, registered_by FROM payments ORDER BY id;
  END payments_all;

  PROCEDURE payments_by_id(p_1 IN payments.id%TYPE, p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, subscription_id, member_id, paid_at, method, subtotal, discount_type, discount_value, registered_by FROM payments WHERE id = p_1;
  END payments_by_id;

  PROCEDURE payments_by_member(p_1 IN payments.member_id%TYPE, p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, subscription_id, member_id, paid_at, method, subtotal, discount_type, discount_value, registered_by FROM payments WHERE member_id = p_1 ORDER BY id;
  END payments_by_member;

  PROCEDURE payments_save(p_subscription_id IN payments.subscription_id%TYPE, p_member_id IN payments.member_id%TYPE, p_paid_at IN payments.paid_at%TYPE, p_method IN payments.method%TYPE, p_subtotal IN payments.subtotal%TYPE, p_discount_type IN payments.discount_type%TYPE, p_discount_value IN payments.discount_value%TYPE, p_registered_by IN payments.registered_by%TYPE, p_id IN payments.id%TYPE, p_result OUT NUMBER) IS
  BEGIN
    IF p_id <> 0 THEN
      UPDATE payments SET subscription_id = p_subscription_id, member_id = p_member_id, paid_at = p_paid_at, method = p_method, subtotal = p_subtotal, discount_type = p_discount_type, discount_value = p_discount_value, registered_by = p_registered_by WHERE id = p_id;
      IF SQL%ROWCOUNT > 0 THEN p_result := p_id; RETURN; END IF;
    END IF;
    IF p_id = 0 THEN
      INSERT INTO payments (subscription_id, member_id, paid_at, method, subtotal, discount_type, discount_value, registered_by) VALUES (p_subscription_id, p_member_id, p_paid_at, p_method, p_subtotal, p_discount_type, p_discount_value, p_registered_by) RETURNING id INTO p_result;
    ELSE
      INSERT INTO payments (subscription_id, member_id, paid_at, method, subtotal, discount_type, discount_value, registered_by, id) VALUES (p_subscription_id, p_member_id, p_paid_at, p_method, p_subtotal, p_discount_type, p_discount_value, p_registered_by, p_id);
      p_result := p_id;
    END IF;
  END payments_save;

  PROCEDURE subscriptions_all(p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, member_id, plan_id, start_date, end_date, cancelled FROM subscriptions ORDER BY id;
  END subscriptions_all;

  PROCEDURE subscriptions_by_id(p_1 IN subscriptions.id%TYPE, p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, member_id, plan_id, start_date, end_date, cancelled FROM subscriptions WHERE id = p_1;
  END subscriptions_by_id;

  PROCEDURE subscriptions_by_member(p_1 IN subscriptions.member_id%TYPE, p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, member_id, plan_id, start_date, end_date, cancelled FROM subscriptions WHERE member_id = p_1 ORDER BY id;
  END subscriptions_by_member;

  PROCEDURE subscriptions_save(p_member_id IN subscriptions.member_id%TYPE, p_plan_id IN subscriptions.plan_id%TYPE, p_start_date IN subscriptions.start_date%TYPE, p_end_date IN subscriptions.end_date%TYPE, p_cancelled IN subscriptions.cancelled%TYPE, p_id IN subscriptions.id%TYPE, p_result OUT NUMBER) IS
  BEGIN
    IF p_id <> 0 THEN
      UPDATE subscriptions SET member_id = p_member_id, plan_id = p_plan_id, start_date = p_start_date, end_date = p_end_date, cancelled = p_cancelled WHERE id = p_id;
      IF SQL%ROWCOUNT > 0 THEN p_result := p_id; RETURN; END IF;
    END IF;
    IF p_id = 0 THEN
      INSERT INTO subscriptions (member_id, plan_id, start_date, end_date, cancelled) VALUES (p_member_id, p_plan_id, p_start_date, p_end_date, p_cancelled) RETURNING id INTO p_result;
    ELSE
      INSERT INTO subscriptions (member_id, plan_id, start_date, end_date, cancelled, id) VALUES (p_member_id, p_plan_id, p_start_date, p_end_date, p_cancelled, p_id);
      p_result := p_id;
    END IF;
  END subscriptions_save;

END pkg_nf_billing;
/

CREATE OR REPLACE PACKAGE pkg_nf_training AUTHID DEFINER AS
  PROCEDURE assignments_all(p_rows OUT SYS_REFCURSOR);
  PROCEDURE assignments_by_id(p_1 IN assignments.id%TYPE, p_rows OUT SYS_REFCURSOR);
  PROCEDURE assignments_by_member(p_1 IN assignments.member_id%TYPE, p_rows OUT SYS_REFCURSOR);
  PROCEDURE assignments_save(p_member_id IN assignments.member_id%TYPE, p_routine_id IN assignments.routine_id%TYPE, p_assigned_at IN assignments.assigned_at%TYPE, p_status IN assignments.status%TYPE, p_id IN assignments.id%TYPE, p_result OUT NUMBER);
  PROCEDURE exercises_all(p_rows OUT SYS_REFCURSOR);
  PROCEDURE exercises_by_id(p_1 IN exercises.id%TYPE, p_rows OUT SYS_REFCURSOR);
  PROCEDURE exercises_by_name(p_1 IN exercises.name%TYPE, p_rows OUT SYS_REFCURSOR);
  PROCEDURE exercises_save(p_name IN exercises.name%TYPE, p_description IN exercises.description%TYPE, p_muscle_group IN exercises.muscle_group%TYPE, p_difficulty IN exercises.difficulty%TYPE, p_image_path IN exercises.image_path%TYPE, p_id IN exercises.id%TYPE, p_result OUT NUMBER);
  PROCEDURE trainers_all(p_rows OUT SYS_REFCURSOR);
  PROCEDURE trainers_by_id(p_1 IN trainers.id%TYPE, p_rows OUT SYS_REFCURSOR);
  PROCEDURE trainers_by_document(p_1 IN trainers.document_number%TYPE, p_rows OUT SYS_REFCURSOR);
  PROCEDURE trainers_save(p_full_name IN trainers.full_name%TYPE, p_document_number IN trainers.document_number%TYPE, p_phone IN trainers.phone%TYPE, p_email IN trainers.email%TYPE, p_birth_date IN trainers.birth_date%TYPE, p_specialty IN trainers.specialty%TYPE, p_photo_path IN trainers.photo_path%TYPE, p_active IN trainers.active%TYPE, p_id IN trainers.id%TYPE, p_result OUT NUMBER);
  PROCEDURE training_sessions_all(p_rows OUT SYS_REFCURSOR);
  PROCEDURE training_sessions_by_id(p_1 IN training_sessions.id%TYPE, p_rows OUT SYS_REFCURSOR);
  PROCEDURE training_sessions_by_member(p_1 IN training_sessions.member_id%TYPE, p_rows OUT SYS_REFCURSOR);
  PROCEDURE training_sessions_save(p_member_id IN training_sessions.member_id%TYPE, p_assignment_id IN training_sessions.assignment_id%TYPE, p_day_name IN training_sessions.day_name%TYPE, p_session_date IN training_sessions.session_date%TYPE, p_duration_minutes IN training_sessions.duration_minutes%TYPE, p_notes IN training_sessions.notes%TYPE, p_id IN training_sessions.id%TYPE, p_result OUT NUMBER);
  PROCEDURE routines_save(p_name IN routines.name%TYPE, p_objective IN routines.objective%TYPE, p_trainer_id IN routines.trainer_id%TYPE, p_created_at IN routines.created_at%TYPE, p_id IN routines.id%TYPE, p_result OUT NUMBER);
  PROCEDURE routines_all(p_rows OUT SYS_REFCURSOR);
  PROCEDURE lines_all(p_rows OUT SYS_REFCURSOR);
  PROCEDURE routines_by_id(p_1 IN routines.id%TYPE, p_rows OUT SYS_REFCURSOR);
  PROCEDURE lines_by_id(p_1 IN routines.id%TYPE, p_rows OUT SYS_REFCURSOR);
  PROCEDURE routines_by_trainer(p_1 IN routines.trainer_id%TYPE, p_rows OUT SYS_REFCURSOR);
  PROCEDURE lines_by_trainer(p_1 IN routines.trainer_id%TYPE, p_rows OUT SYS_REFCURSOR);
  PROCEDURE lines_delete(p_routine_id IN routine_lines.routine_id%TYPE);
  PROCEDURE lines_insert(p_routine_id IN routine_lines.routine_id%TYPE, p_day_index IN routine_lines.day_index%TYPE, p_line_no IN routine_lines.line_no%TYPE, p_day_name IN routine_lines.day_name%TYPE, p_exercise_id IN routine_lines.exercise_id%TYPE, p_sets IN routine_lines.sets%TYPE, p_repetitions IN routine_lines.repetitions%TYPE, p_rest_seconds IN routine_lines.rest_seconds%TYPE);
END pkg_nf_training;
/

CREATE OR REPLACE PACKAGE BODY pkg_nf_training AS
  PROCEDURE assignments_all(p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, member_id, routine_id, assigned_at, status FROM assignments ORDER BY id;
  END assignments_all;

  PROCEDURE assignments_by_id(p_1 IN assignments.id%TYPE, p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, member_id, routine_id, assigned_at, status FROM assignments WHERE id = p_1;
  END assignments_by_id;

  PROCEDURE assignments_by_member(p_1 IN assignments.member_id%TYPE, p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, member_id, routine_id, assigned_at, status FROM assignments WHERE member_id = p_1 ORDER BY id;
  END assignments_by_member;

  PROCEDURE assignments_save(p_member_id IN assignments.member_id%TYPE, p_routine_id IN assignments.routine_id%TYPE, p_assigned_at IN assignments.assigned_at%TYPE, p_status IN assignments.status%TYPE, p_id IN assignments.id%TYPE, p_result OUT NUMBER) IS
  BEGIN
    IF p_id <> 0 THEN
      UPDATE assignments SET member_id = p_member_id, routine_id = p_routine_id, assigned_at = p_assigned_at, status = p_status WHERE id = p_id;
      IF SQL%ROWCOUNT > 0 THEN p_result := p_id; RETURN; END IF;
    END IF;
    IF p_id = 0 THEN
      INSERT INTO assignments (member_id, routine_id, assigned_at, status) VALUES (p_member_id, p_routine_id, p_assigned_at, p_status) RETURNING id INTO p_result;
    ELSE
      INSERT INTO assignments (member_id, routine_id, assigned_at, status, id) VALUES (p_member_id, p_routine_id, p_assigned_at, p_status, p_id);
      p_result := p_id;
    END IF;
  END assignments_save;

  PROCEDURE exercises_all(p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, name, description, muscle_group, difficulty, image_path FROM exercises ORDER BY id;
  END exercises_all;

  PROCEDURE exercises_by_id(p_1 IN exercises.id%TYPE, p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, name, description, muscle_group, difficulty, image_path FROM exercises WHERE id = p_1;
  END exercises_by_id;

  PROCEDURE exercises_by_name(p_1 IN exercises.name%TYPE, p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, name, description, muscle_group, difficulty, image_path FROM exercises WHERE LOWER(name) = LOWER(p_1);
  END exercises_by_name;

  PROCEDURE exercises_save(p_name IN exercises.name%TYPE, p_description IN exercises.description%TYPE, p_muscle_group IN exercises.muscle_group%TYPE, p_difficulty IN exercises.difficulty%TYPE, p_image_path IN exercises.image_path%TYPE, p_id IN exercises.id%TYPE, p_result OUT NUMBER) IS
  BEGIN
    IF p_id <> 0 THEN
      UPDATE exercises SET name = p_name, description = p_description, muscle_group = p_muscle_group, difficulty = p_difficulty, image_path = p_image_path WHERE id = p_id;
      IF SQL%ROWCOUNT > 0 THEN p_result := p_id; RETURN; END IF;
    END IF;
    IF p_id = 0 THEN
      INSERT INTO exercises (name, description, muscle_group, difficulty, image_path) VALUES (p_name, p_description, p_muscle_group, p_difficulty, p_image_path) RETURNING id INTO p_result;
    ELSE
      INSERT INTO exercises (name, description, muscle_group, difficulty, image_path, id) VALUES (p_name, p_description, p_muscle_group, p_difficulty, p_image_path, p_id);
      p_result := p_id;
    END IF;
  END exercises_save;

  PROCEDURE trainers_all(p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, full_name, document_number, phone, email, birth_date, specialty, photo_path, active FROM trainers ORDER BY id;
  END trainers_all;

  PROCEDURE trainers_by_id(p_1 IN trainers.id%TYPE, p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, full_name, document_number, phone, email, birth_date, specialty, photo_path, active FROM trainers WHERE id = p_1;
  END trainers_by_id;

  PROCEDURE trainers_by_document(p_1 IN trainers.document_number%TYPE, p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, full_name, document_number, phone, email, birth_date, specialty, photo_path, active FROM trainers WHERE document_number = p_1;
  END trainers_by_document;

  PROCEDURE trainers_save(p_full_name IN trainers.full_name%TYPE, p_document_number IN trainers.document_number%TYPE, p_phone IN trainers.phone%TYPE, p_email IN trainers.email%TYPE, p_birth_date IN trainers.birth_date%TYPE, p_specialty IN trainers.specialty%TYPE, p_photo_path IN trainers.photo_path%TYPE, p_active IN trainers.active%TYPE, p_id IN trainers.id%TYPE, p_result OUT NUMBER) IS
  BEGIN
    IF p_id <> 0 THEN
      UPDATE trainers SET full_name = p_full_name, document_number = p_document_number, phone = p_phone, email = p_email, birth_date = p_birth_date, specialty = p_specialty, photo_path = p_photo_path, active = p_active WHERE id = p_id;
      IF SQL%ROWCOUNT > 0 THEN p_result := p_id; RETURN; END IF;
    END IF;
    IF p_id = 0 THEN
      INSERT INTO trainers (full_name, document_number, phone, email, birth_date, specialty, photo_path, active) VALUES (p_full_name, p_document_number, p_phone, p_email, p_birth_date, p_specialty, p_photo_path, p_active) RETURNING id INTO p_result;
    ELSE
      INSERT INTO trainers (full_name, document_number, phone, email, birth_date, specialty, photo_path, active, id) VALUES (p_full_name, p_document_number, p_phone, p_email, p_birth_date, p_specialty, p_photo_path, p_active, p_id);
      p_result := p_id;
    END IF;
  END trainers_save;

  PROCEDURE training_sessions_all(p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, member_id, assignment_id, day_name, session_date, duration_minutes, notes FROM training_sessions ORDER BY id;
  END training_sessions_all;

  PROCEDURE training_sessions_by_id(p_1 IN training_sessions.id%TYPE, p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, member_id, assignment_id, day_name, session_date, duration_minutes, notes FROM training_sessions WHERE id = p_1;
  END training_sessions_by_id;

  PROCEDURE training_sessions_by_member(p_1 IN training_sessions.member_id%TYPE, p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, member_id, assignment_id, day_name, session_date, duration_minutes, notes FROM training_sessions WHERE member_id = p_1 ORDER BY id;
  END training_sessions_by_member;

  PROCEDURE training_sessions_save(p_member_id IN training_sessions.member_id%TYPE, p_assignment_id IN training_sessions.assignment_id%TYPE, p_day_name IN training_sessions.day_name%TYPE, p_session_date IN training_sessions.session_date%TYPE, p_duration_minutes IN training_sessions.duration_minutes%TYPE, p_notes IN training_sessions.notes%TYPE, p_id IN training_sessions.id%TYPE, p_result OUT NUMBER) IS
  BEGIN
    IF p_id <> 0 THEN
      UPDATE training_sessions SET member_id = p_member_id, assignment_id = p_assignment_id, day_name = p_day_name, session_date = p_session_date, duration_minutes = p_duration_minutes, notes = p_notes WHERE id = p_id;
      IF SQL%ROWCOUNT > 0 THEN p_result := p_id; RETURN; END IF;
    END IF;
    IF p_id = 0 THEN
      INSERT INTO training_sessions (member_id, assignment_id, day_name, session_date, duration_minutes, notes) VALUES (p_member_id, p_assignment_id, p_day_name, p_session_date, p_duration_minutes, p_notes) RETURNING id INTO p_result;
    ELSE
      INSERT INTO training_sessions (member_id, assignment_id, day_name, session_date, duration_minutes, notes, id) VALUES (p_member_id, p_assignment_id, p_day_name, p_session_date, p_duration_minutes, p_notes, p_id);
      p_result := p_id;
    END IF;
  END training_sessions_save;

  PROCEDURE routines_save(p_name IN routines.name%TYPE, p_objective IN routines.objective%TYPE, p_trainer_id IN routines.trainer_id%TYPE, p_created_at IN routines.created_at%TYPE, p_id IN routines.id%TYPE, p_result OUT NUMBER) IS
  BEGIN
    IF p_id <> 0 THEN
      UPDATE routines SET name = p_name, objective = p_objective, trainer_id = p_trainer_id, created_at = p_created_at WHERE id = p_id;
      IF SQL%ROWCOUNT > 0 THEN p_result := p_id; RETURN; END IF;
    END IF;
    IF p_id = 0 THEN
      INSERT INTO routines (name, objective, trainer_id, created_at) VALUES (p_name, p_objective, p_trainer_id, p_created_at) RETURNING id INTO p_result;
    ELSE
      INSERT INTO routines (name, objective, trainer_id, created_at, id) VALUES (p_name, p_objective, p_trainer_id, p_created_at, p_id);
      p_result := p_id;
    END IF;
  END routines_save;

  PROCEDURE routines_all(p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, name, objective, trainer_id, created_at FROM routines ORDER BY id;
  END routines_all;

  PROCEDURE lines_all(p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT routine_id, day_index, day_name, exercise_id, sets, repetitions, rest_seconds FROM routine_lines WHERE routine_id IN (SELECT id FROM routines) ORDER BY routine_id, day_index, line_no;
  END lines_all;

  PROCEDURE routines_by_id(p_1 IN routines.id%TYPE, p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, name, objective, trainer_id, created_at FROM routines WHERE id = p_1 ORDER BY id;
  END routines_by_id;

  PROCEDURE lines_by_id(p_1 IN routines.id%TYPE, p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT routine_id, day_index, day_name, exercise_id, sets, repetitions, rest_seconds FROM routine_lines WHERE routine_id IN (SELECT id FROM routines WHERE id = p_1) ORDER BY routine_id, day_index, line_no;
  END lines_by_id;

  PROCEDURE routines_by_trainer(p_1 IN routines.trainer_id%TYPE, p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, name, objective, trainer_id, created_at FROM routines WHERE trainer_id = p_1 ORDER BY id;
  END routines_by_trainer;

  PROCEDURE lines_by_trainer(p_1 IN routines.trainer_id%TYPE, p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT routine_id, day_index, day_name, exercise_id, sets, repetitions, rest_seconds FROM routine_lines WHERE routine_id IN (SELECT id FROM routines WHERE trainer_id = p_1) ORDER BY routine_id, day_index, line_no;
  END lines_by_trainer;

  PROCEDURE lines_delete(p_routine_id IN routine_lines.routine_id%TYPE) IS
  BEGIN
    DELETE FROM routine_lines WHERE routine_id = p_routine_id;
  END lines_delete;

  PROCEDURE lines_insert(p_routine_id IN routine_lines.routine_id%TYPE, p_day_index IN routine_lines.day_index%TYPE, p_line_no IN routine_lines.line_no%TYPE, p_day_name IN routine_lines.day_name%TYPE, p_exercise_id IN routine_lines.exercise_id%TYPE, p_sets IN routine_lines.sets%TYPE, p_repetitions IN routine_lines.repetitions%TYPE, p_rest_seconds IN routine_lines.rest_seconds%TYPE) IS
  BEGIN
    INSERT INTO routine_lines (routine_id, day_index, line_no, day_name, exercise_id, sets, repetitions, rest_seconds) VALUES (p_routine_id, p_day_index, p_line_no, p_day_name, p_exercise_id, p_sets, p_repetitions, p_rest_seconds);
  END lines_insert;

END pkg_nf_training;
/

CREATE OR REPLACE PACKAGE pkg_nf_access AUTHID DEFINER AS
  PROCEDURE attendance_all(p_rows OUT SYS_REFCURSOR);
  PROCEDURE attendance_open(p_rows OUT SYS_REFCURSOR);
  PROCEDURE attendance_count_open(p_rows OUT SYS_REFCURSOR);
  PROCEDURE attendance_by_member(p_1 IN attendance.member_id%TYPE, p_rows OUT SYS_REFCURSOR);
  PROCEDURE attendance_open_by_member(p_1 IN attendance.member_id%TYPE, p_rows OUT SYS_REFCURSOR);
  PROCEDURE attendance_save(p_member_id IN attendance.member_id%TYPE, p_check_in IN attendance.check_in%TYPE, p_check_out IN attendance.check_out%TYPE, p_id IN attendance.id%TYPE, p_result OUT NUMBER);
  PROCEDURE member_credentials_by_member(p_1 IN member_credentials.member_id%TYPE, p_rows OUT SYS_REFCURSOR);
  PROCEDURE member_credentials_save(p_password_hash IN member_credentials.password_hash%TYPE, p_id IN member_credentials.member_id%TYPE, p_result OUT NUMBER);
  PROCEDURE member_credentials_ids(p_rows OUT SYS_REFCURSOR);
  PROCEDURE member_credentials_delete(p_member_id IN member_credentials.member_id%TYPE);
  PROCEDURE faces_all(p_rows OUT SYS_REFCURSOR);
  PROCEDURE faces_count(p_1 IN face_templates.member_id%TYPE, p_rows OUT SYS_REFCURSOR);
  PROCEDURE faces_ids(p_rows OUT SYS_REFCURSOR);
  PROCEDURE faces_delete(p_member_id IN face_templates.member_id%TYPE);
  PROCEDURE faces_insert(p_member_id IN face_templates.member_id%TYPE, p_embedding IN face_templates.embedding%TYPE);
  PROCEDURE key_read(p_key OUT BLOB);
  PROCEDURE key_initialize(p_candidate IN BLOB, p_key OUT BLOB);
END pkg_nf_access;
/

CREATE OR REPLACE PACKAGE BODY pkg_nf_access AS
  PROCEDURE attendance_all(p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, member_id, check_in, check_out FROM attendance ORDER BY id;
  END attendance_all;

  PROCEDURE attendance_open(p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, member_id, check_in, check_out FROM attendance WHERE check_out IS NULL ORDER BY check_in DESC;
  END attendance_open;

  PROCEDURE attendance_count_open(p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT COUNT(*) FROM attendance WHERE check_out IS NULL;
  END attendance_count_open;

  PROCEDURE attendance_by_member(p_1 IN attendance.member_id%TYPE, p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, member_id, check_in, check_out FROM attendance WHERE member_id = p_1 ORDER BY id;
  END attendance_by_member;

  PROCEDURE attendance_open_by_member(p_1 IN attendance.member_id%TYPE, p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, member_id, check_in, check_out FROM attendance WHERE member_id = p_1 AND check_out IS NULL ORDER BY check_in DESC FETCH FIRST 1 ROWS ONLY;
  END attendance_open_by_member;

  PROCEDURE attendance_save(p_member_id IN attendance.member_id%TYPE, p_check_in IN attendance.check_in%TYPE, p_check_out IN attendance.check_out%TYPE, p_id IN attendance.id%TYPE, p_result OUT NUMBER) IS
  BEGIN
    IF p_id <> 0 THEN
      UPDATE attendance SET member_id = p_member_id, check_in = p_check_in, check_out = p_check_out WHERE id = p_id;
      IF SQL%ROWCOUNT > 0 THEN p_result := p_id; RETURN; END IF;
    END IF;
    IF p_id = 0 THEN
      INSERT INTO attendance (member_id, check_in, check_out) VALUES (p_member_id, p_check_in, p_check_out) RETURNING id INTO p_result;
    ELSE
      INSERT INTO attendance (member_id, check_in, check_out, id) VALUES (p_member_id, p_check_in, p_check_out, p_id);
      p_result := p_id;
    END IF;
  END attendance_save;

  PROCEDURE member_credentials_by_member(p_1 IN member_credentials.member_id%TYPE, p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT member_id, password_hash FROM member_credentials WHERE member_id = p_1;
  END member_credentials_by_member;

  PROCEDURE member_credentials_save(p_password_hash IN member_credentials.password_hash%TYPE, p_id IN member_credentials.member_id%TYPE, p_result OUT NUMBER) IS
  BEGIN
    UPDATE member_credentials SET password_hash = p_password_hash WHERE member_id = p_id;
    IF SQL%ROWCOUNT = 0 THEN
      INSERT INTO member_credentials (password_hash, member_id) VALUES (p_password_hash, p_id);
    END IF;
    p_result := p_id;
  END member_credentials_save;

  PROCEDURE member_credentials_ids(p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT member_id FROM member_credentials;
  END member_credentials_ids;

  PROCEDURE member_credentials_delete(p_member_id IN member_credentials.member_id%TYPE) IS
  BEGIN
    DELETE FROM member_credentials WHERE member_id = p_member_id;
  END member_credentials_delete;

  PROCEDURE faces_all(p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT member_id, embedding FROM face_templates ORDER BY id;
  END faces_all;

  PROCEDURE faces_count(p_1 IN face_templates.member_id%TYPE, p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT COUNT(*) FROM face_templates WHERE member_id = p_1;
  END faces_count;

  PROCEDURE faces_ids(p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT DISTINCT member_id FROM face_templates;
  END faces_ids;

  PROCEDURE faces_delete(p_member_id IN face_templates.member_id%TYPE) IS
  BEGIN
    DELETE FROM face_templates WHERE member_id = p_member_id;
  END faces_delete;

  PROCEDURE faces_insert(p_member_id IN face_templates.member_id%TYPE, p_embedding IN face_templates.embedding%TYPE) IS
  BEGIN
    INSERT INTO face_templates (member_id, embedding) VALUES (p_member_id, p_embedding);
  END faces_insert;

  PROCEDURE key_read(p_key OUT BLOB) IS
  BEGIN
    BEGIN
      SELECT access_key INTO p_key FROM access_settings WHERE id = 1;
    EXCEPTION WHEN NO_DATA_FOUND THEN p_key := NULL;
    END;
  END key_read;

  PROCEDURE key_initialize(p_candidate IN BLOB, p_key OUT BLOB) IS
  BEGIN
    BEGIN
      INSERT INTO access_settings (id, access_key) VALUES (1, p_candidate);
      p_key := p_candidate;
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN
      SELECT access_key INTO p_key FROM access_settings WHERE id = 1;
    END;
  END key_initialize;

END pkg_nf_access;
/

CREATE OR REPLACE PACKAGE pkg_nf_system AUTHID DEFINER AS
  PROCEDURE audit_log_all(p_rows OUT SYS_REFCURSOR);
  PROCEDURE audit_log_save(p_event_time IN audit_log.event_time%TYPE, p_username IN audit_log.username%TYPE, p_action IN audit_log.action%TYPE, p_detail IN audit_log.detail%TYPE, p_id IN audit_log.id%TYPE, p_result OUT NUMBER);
  PROCEDURE users_all(p_rows OUT SYS_REFCURSOR);
  PROCEDURE users_by_id(p_1 IN users.id%TYPE, p_rows OUT SYS_REFCURSOR);
  PROCEDURE users_by_username(p_1 IN users.username%TYPE, p_rows OUT SYS_REFCURSOR);
  PROCEDURE users_save(p_username IN users.username%TYPE, p_full_name IN users.full_name%TYPE, p_password_hash IN users.password_hash%TYPE, p_role IN users.role%TYPE, p_active IN users.active%TYPE, p_id IN users.id%TYPE, p_result OUT NUMBER);
  PROCEDURE ping(p_version OUT NUMBER);
  PROCEDURE reset_counter(p_table IN VARCHAR2);
END pkg_nf_system;
/

CREATE OR REPLACE PACKAGE BODY pkg_nf_system AS
  PROCEDURE audit_log_all(p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, event_time, username, action, detail FROM audit_log ORDER BY id;
  END audit_log_all;

  PROCEDURE audit_log_save(p_event_time IN audit_log.event_time%TYPE, p_username IN audit_log.username%TYPE, p_action IN audit_log.action%TYPE, p_detail IN audit_log.detail%TYPE, p_id IN audit_log.id%TYPE, p_result OUT NUMBER) IS
  BEGIN
    IF p_id = 0 THEN
      INSERT INTO audit_log (event_time, username, action, detail) VALUES (p_event_time, p_username, p_action, p_detail) RETURNING id INTO p_result;
    ELSE
      INSERT INTO audit_log (event_time, username, action, detail, id) VALUES (p_event_time, p_username, p_action, p_detail, p_id);
      p_result := p_id;
    END IF;
  END audit_log_save;

  PROCEDURE users_all(p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, username, full_name, password_hash, role, active FROM users ORDER BY id;
  END users_all;

  PROCEDURE users_by_id(p_1 IN users.id%TYPE, p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, username, full_name, password_hash, role, active FROM users WHERE id = p_1;
  END users_by_id;

  PROCEDURE users_by_username(p_1 IN users.username%TYPE, p_rows OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_rows FOR SELECT id, username, full_name, password_hash, role, active FROM users WHERE LOWER(username) = LOWER(p_1);
  END users_by_username;

  PROCEDURE users_save(p_username IN users.username%TYPE, p_full_name IN users.full_name%TYPE, p_password_hash IN users.password_hash%TYPE, p_role IN users.role%TYPE, p_active IN users.active%TYPE, p_id IN users.id%TYPE, p_result OUT NUMBER) IS
  BEGIN
    IF p_id <> 0 THEN
      UPDATE users SET username = p_username, full_name = p_full_name, password_hash = p_password_hash, role = p_role, active = p_active WHERE id = p_id;
      IF SQL%ROWCOUNT > 0 THEN p_result := p_id; RETURN; END IF;
    END IF;
    IF p_id = 0 THEN
      INSERT INTO users (username, full_name, password_hash, role, active) VALUES (p_username, p_full_name, p_password_hash, p_role, p_active) RETURNING id INTO p_result;
    ELSE
      INSERT INTO users (username, full_name, password_hash, role, active, id) VALUES (p_username, p_full_name, p_password_hash, p_role, p_active, p_id);
      p_result := p_id;
    END IF;
  END users_save;

  PROCEDURE ping(p_version OUT NUMBER) IS
  BEGIN
    SELECT COUNT(*) INTO p_version FROM members;
    p_version := 1;
  END ping;

  PROCEDURE reset_counter(p_table IN VARCHAR2) IS
  BEGIN
    IF p_table NOT IN ('users', 'members', 'membership_plans', 'subscriptions', 'payments', 'attendance', 'measurements', 'exercises', 'trainers', 'routines', 'assignments', 'training_sessions', 'audit_log', 'face_templates') OR p_table IS NULL THEN
      RAISE_APPLICATION_ERROR(-20030, 'Contador no permitido');
    END IF;
    EXECUTE IMMEDIATE 'ALTER TABLE ' || p_table || ' MODIFY (id GENERATED BY DEFAULT AS IDENTITY (START WITH LIMIT VALUE))';
  END reset_counter;

END pkg_nf_system;
/

DECLARE
  v_errors PLS_INTEGER;
BEGIN
  SELECT COUNT(*) INTO v_errors FROM user_objects WHERE object_name IN ('PKG_NF_MEMBERS', 'PKG_NF_BILLING', 'PKG_NF_TRAINING', 'PKG_NF_ACCESS', 'PKG_NF_SYSTEM') AND status <> 'VALID';
  IF v_errors > 0 THEN RAISE_APPLICATION_ERROR(-20031, 'API invalida: consultar USER_ERRORS'); END IF;
END;
/
