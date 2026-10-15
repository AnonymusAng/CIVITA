# Verificar sesión y dos funcionalidades contra Firebase real

Esta prueba usa **Firebase Authentication de cliente** y las reglas Firestore publicadas. Las escrituras del reporte y las consultas usan un ID token del usuario temporal, no IAM. La cuenta administrativa se usa para el diagnóstico previo y la limpieza final. No se considera una prueba de interfaz Android ni de Google Sign-In.

En Google Cloud Shell, con la cuenta institucional que administra CIVITA:

```bash
CIVITA_TEST_DIR=$(mktemp -d)
git clone --depth 1 https://github.com/AnonymusAng/CIVITA.git "$CIVITA_TEST_DIR" &&
bash "$CIVITA_TEST_DIR/firebase/actualizar-cloud-shell.sh" &&
python3 "$CIVITA_TEST_DIR/firebase/verify_session.py" --run
```

El primer script verifica las reglas con emuladores antes de publicarlas y actualiza el JSON en `~/civita-resultado`. Se detiene si una etapa falla. No cambies permisos ni desactives reglas para hacer pasar la prueba.

La prueba real crea una cuenta de correo temporal y un reporte marcado **PRUEBA AUTOMATIZADA**, que puede ser visible brevemente para usuarios conectados. No representa un incidente real. Al terminar, intenta borrar solamente la cuenta, su perfil/preferencias y el reporte/historial de esa ejecución, incluso si una comprobación falla. No modifica categorías, configuración ni reportes de otras personas. Necesita permisos administrativos de Auth y Firestore para esa limpieza.

Comprueba:

1. Registro temporal e inicio de sesión por correo con el mismo UID.
2. Lectura de configuración y categoría y creación del perfil bajo las reglas del cliente.
3. **Funcionalidad 1:** publicar el reporte y su historial inicial de manera atómica, con timestamps del servidor, y leer los valores persistidos.
4. **Funcionalidad 2:** volver a iniciar sesión, ejecutar la consulta de Comunidad ordenada por fecha y recuperar el mismo reporte y detalle desde el servidor.
5. Limpieza de los datos temporales. Un error de limpieza impide un resultado global aprobado y enumera las rutas pendientes.

Resultado: `~/civita-resultado/prueba-sesion.json`, sin contraseñas, claves API ni tokens. Sólo `status: "passed"` con las cuatro etapas `authentication`, `publish`, `query` y `cleanup` aprobadas acredita esta prueba del backend. Una ejecución no realizada, un fallo o una simulación no la sustituyen.

## Verificación adicional obligatoria en Android

Con las reglas actualizadas, instalar la APK conectada, crear/iniciar una cuenta de prueba propia y publicar un reporte identificado como prueba técnica. Comprobar que aparece en Comunidad, abrir su detalle, cerrar sesión, volver a entrar y comprobar que sigue disponible. Revisar que no aparezcan mensajes de permiso denegado o datos sólo en caché. La aplicación no tiene borrado ciudadano de reportes: la limpieza de una prueba manual requiere al administrador y debe limitarse a su ID concreto.

Google Sign-In necesita un teléfono o emulador con Google Play Services y las huellas de la firma correcta registradas en Firebase. La prueba REST anterior verifica correo, no Google.

## Estado de esta revisión

La ejecución real desde el entorno de Codex está bloqueada: el proxy devuelve `403 Forbidden` al contactar Authentication y Firestore, no hay sesión administrativa Google y la descarga oficial del emulador Firestore también está bloqueada. La configuración guardada ya incluye esos dominios; guardar una configuración no la activa en una máquina existente. Revisar y publicar el entorno es una acción de la plataforma del usuario, no un despliegue Firebase.

El flujo de fallo y limpieza del verificador tiene seis pruebas unitarias; el conjunto Python suma 34 aprobadas. Esas pruebas no realizan llamadas a Firebase real. La compilación Android y sus 26 pruebas anteriores permanecen separadas de esta verificación pendiente.
