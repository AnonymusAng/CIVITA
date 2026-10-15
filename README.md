# CIVITA · Entrega Android

CIVITA.apk: aplicación de prueba para Android 8 o posterior.
CIVITA-Proyecto.zip: fuentes para Android Studio; agregar configuración Firebase propia.
CIVITA-Documentacion.zip: diagramas UML editables y PDF, guías y validación.

Compilación y 20 pruebas unitarias aprobadas. La APK incorpora la configuración Firebase actualizada con cliente OAuth web para Google. Queda pendiente validar desde Android el inicio de sesión y la lectura/publicación con una cuenta real.

## Inicialización de Firestore

La estructura y el inicializador están en [firebase/README.md](firebase/README.md).
Crean la configuración y las cinco categorías, publican reglas y preparan el acceso con correo usando tu cuenta de Cloud Shell. El proyecto ya apunta a civita-5cc42. La salida compartida de Cloud Shell confirma los datos iniciales, las reglas publicadas, el acceso por correo habilitado y la lectura administrativa de Firestore. El archivo Android actualizado incluye el cliente OAuth web. Esta verificación administrativa no sustituye las pruebas desde la app.
