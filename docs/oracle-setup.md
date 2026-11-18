# Conectar NextFit a Oracle Cloud

## Primera instalación a cargo de Ronald

Desde la raíz del proyecto, con NextFit cerrado y `nextfit.properties` configurado para
la base del equipo, instala las tablas con el usuario de la aplicación.

```powershell
mvn compile exec:java "-Dexec.mainClass=com.nextfit.persistence.jdbc.OracleSchemaInstaller"
```

Confirma que la última línea diga **0 con error**. Si Oracle ya tiene los datos del equipo,
continúa con el arranque. Para cargar una base nueva, usa los CSV acordados por el equipo:

```powershell
mvn compile exec:java "-Dexec.mainClass=com.nextfit.persistence.jdbc.CsvToOracleMigration"
```

Comprueba el mensaje de migración completa y el resultado de los ajustes de los contadores.
La herramienta actualiza filas existentes, así que la carga debe partir de la copia de datos
que el equipo haya elegido.

Abre NextFit **primero en el computador que tiene el `data/access.key` de los carnets que
quieren conservar**. Si es el primer arranque con la tabla nueva, la aplicación adopta esa
clave en Oracle. Abre los otros equipos después. Cada uno guarda la clave compartida para
usarla también al arrancar en modo CSV. Los carnets que se hayan firmado con otra clave se
deben reemitir. La dirección del portal que está impresa en un QR también debe ser accesible
desde el celular.

## Tres pasos para Deiner y Santiago

1. En la rama `develop`, ejecuten `git pull --ff-only origin develop`.
2. Si todavía no tienen `nextfit.properties`, copien `nextfit.properties.example` con ese
   nombre. Completen la URL y las credenciales del usuario de la aplicación con los datos
   que les comparta Ronald por privado. Si usan wallet, guárdenlo fuera del repositorio,
   ajusten `TNS_ADMIN` a la ruta local y pongan `db.enabled=true`.
3. Abran NextFit desde la raíz con `mvn compile exec:java`. En la consola debe aparecer
   `Almacenamiento: base de datos Oracle`. Inicien sesión y comprueben que los tres equipos
   muestran los mismos socios.

## Prueba para la entrega

- Con Oracle activo, registren un socio de prueba y actualicen la lista en los otros equipos.
  Comprueben que un código de carnet emitido con la clave compartida funciona en el portal
  servido por otro equipo con ese mismo socio.
- Como administrador, creen un respaldo y comprueben que el ZIP `nextfit-respaldo-oracle-*.zip`
  contiene el registro de prueba en `members.csv`. El ZIP contiene datos privados y la clave de los carnets.
- Cierren NextFit, desactiven `db.enabled` o arranquen sin conexión a Oracle. Verifiquen en
  consola `Almacenamiento: archivos CSV` y entren con las credenciales del almacenamiento
  local. Los CSV locales se usan desde el arranque y pueden tener datos de otra fecha.
  La [guía de respaldo](backup-restore.md) explica cómo cargar el último ZIP en modo CSV.
- Vuelvan a activar Oracle y confirmen que el socio de prueba sigue estando en la base.

Las fotos de socios permanecen en `data/photos/` y las imágenes de ejercicios en
`data/images/`. Oracle guarda la ruta de cada imagen. Para que la foto se vea en otro
equipo, copien esos archivos por un medio privado manteniendo la ruta relativa. El
respaldo incluye las fotos presentes en el computador que lo genera.
