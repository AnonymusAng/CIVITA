# Conectar y crear la base real de CIVITA

La app ya apunta al proyecto **civita-5cc42**. Los scripts de esta carpeta crean su estructura real usando tu sesión de Google en Cloud Shell. Una captura de pantalla y el JSON Android no otorgan permisos administrativos.

## Ejecutar desde tu cuenta

1. Abre https://shell.cloud.google.com/?project=civita-5cc42 e inicia sesión con la cuenta que administra ese proyecto.
2. Pega este bloque en la terminal de Cloud Shell:

```bash
CIVITA_SETUP_DIR=$(mktemp -d)
git clone --depth 1 https://github.com/AnonymusAng/CIVITA.git "$CIVITA_SETUP_DIR" && \
python3 "$CIVITA_SETUP_DIR/firebase/initialize.py" --project civita-5cc42 --apply --enable-email --configure-android
```

No pide tu contraseña ni descarga claves privadas. Usa `gcloud auth print-access-token` en memoria con tu sesión existente. No imprime ni guarda el token. Debes tener permisos de administración de Firestore/reglas, Firebase Authentication y configuración de la app Android en este proyecto. Si tu institución restringe permisos, el script muestra el error del servicio; no intenta sortear la restricción.

## Cambios que realiza

- Habilita las APIs necesarias si tu cuenta tiene permiso.
- Conserva la base (default) existente. Solo si no existe, la crea en southamerica-west1, región de las capturas anteriores, con protección contra borrado.
- Crea appConfig/public y cinco categorías o completa sus campos faltantes. No sobrescribe valores existentes válidos, categorías inactivas, documentos de usuario o reportes.
- Si un valor existente es incompatible con el APK, se detiene antes de escribir documentos para que puedas corregirlo. Los documentos se escriben en un único commit con precondiciones para impedir sobrescrituras concurrentes.
- Guarda las reglas anteriores en civita-resultado y publica las reglas que utiliza esta app. La nueva política requiere autenticación y deniega accesos fuera de las tres rutas documentadas. Los permisos de administración IAM son independientes de las reglas del cliente.
- Habilita correo/contraseña. No crea cuentas o contraseñas de prueba.
- Registra las huellas SHA-1 y SHA-256 del APK entregado. Si después compilas con otra firma, registra también sus huellas desde Android Studio.
- Descarga la configuración actualizada a civita-resultado/google-services.json, con permisos de archivo privados.
- Verifica los seis documentos, la publicación de reglas y la consulta de reportes con tu cuenta administrativa. Guarda resultado.json.

No crea datos falsos. Firestore no mantiene colecciones vacías: reports aparecerá cuando publiques el primer reporte desde la app. Una consulta a esa colección antes del primer reporte devuelve una lista vacía válida.

## Google y prueba desde Android

El inicializador no inventa credenciales OAuth ni habilita una actualización de pago a Identity Platform. Abre Firebase Console → Authentication → Método de acceso y activa Google, seleccionando el correo de soporte. Si Authentication aún no está inicializado, pulsa Comenzar y habilita Correo/Contraseña también. Después repite el comando para descargar la configuración con OAuth web.

En la salida busca `Cliente OAuth web presente: sí`. Si dice no, Google no podrá completar el acceso hasta habilitar el proveedor y descargar la configuración de nuevo.
Desde el menú de Cloud Shell puedes descargar civita-resultado/google-services.json. Envíalo al chat para incorporarlo al proyecto y recompilar el APK, o reemplaza tú mismo app/google-services.json en Android Studio. El JSON no es una clave privada de cuenta de servicio; nunca subas aquí claves privadas ni contraseñas.
El APK ya entregado puede usar correo/contraseña con su configuración actual si el proveedor está habilitado y las reglas/base están preparadas. Para Google necesita recompilar con el JSON que incluya el cliente OAuth.

Valida con una cuenta real: iniciar sesión, ver las cinco categorías, publicar una novedad y comprobar que aparece en reports. Esta prueba desde Android es distinta de la verificación administrativa del script.

## Inspección, repetición y fallos

Sin --apply el script solo inspecciona Firestore y muestra el plan; no crea bases, documentos, reglas ni proveedores. --check-local valida los archivos sin Google ni red.
Puedes repetir --apply: conserva los documentos, completa lo faltante y guarda una nueva copia de las reglas antes de publicarlas. No hay borrados de datos ni cuentas.
Las fases no son una transacción global: si falla Authentication o la descarga del JSON después de crear documentos, los pasos previos permanecen. El mensaje lo advierte; corrige el permiso/configuración y repite el comando.
Si la creación de una base sigue en curso al agotarse la espera, consulta Firestore antes de reintentar; nunca borres y recrees una base existente.

## Verificación disponible

13 pruebas locales del inicializador aprobadas (serialización, preservación de datos, precondiciones, no mutación en inspección, bloqueo de otro proyecto, reglas y descarga de configuración). Se ejecutan con:

```bash
python3 -m unittest discover -s firebase/tests -v
```

Son pruebas con transporte simulado; no se presentan como ejecución en tu Firebase. El entorno de desarrollo no dispone de sesión administrativa y devuelve HTTP 403 a firestore.googleapis.com. La creación remota quedará comprobada cuando ejecutes este comando con tu cuenta y obtengas resultado.json.
