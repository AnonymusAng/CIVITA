# Evidencia de validación · CIVITA 1.1.742

## Verificado localmente

- APK conectada: compilación assembleDebug correcta, versión 1.1.742, código 3, Android mínimo API 26.
- 26 pruebas unitarias Android aprobadas, sin fallos. Incluyen validación de borradores, geohash estándar y recuperación/idempotencia de estado de publicación.
- 28 pruebas Python aprobadas, sin fallos. Incluyen preservación de datos, precondiciones, diagnóstico sin escrituras, configuración Android y validación de eventos/puntos oficiales.
- APK de simulación compilada por separado, applicationId co.edu.uniautonoma.civita.simulation; no inicializa Firebase.
- ZIP extraído: Gradle Wrapper ejecutó help --offline correctamente desde la carpeta distribuida. Se verifican CRC y coincidencia de cada archivo antes de publicar.
- Firma de la APK conectada verificada con apksigner. SHA-1 y SHA-256 coinciden con las registradas por el inicializador.
- 19 vistas de diagramas generadas, con fuentes PlantUML, SVG y PNG. Se verificó sintaxis y se inspeccionaron visualmente componentes y MER.

## Verificado por el usuario en Cloud Shell (versión anterior)

La salida compartida confirma acceso al proyecto con su cuenta institucional, base Firestore nativa, configuración y categorías, reglas anteriores publicadas, correo habilitado, consulta administrativa de reportes y JSON Android actualizado con cliente OAuth web. No equivale a una prueba del SDK Android.

## Pendiente de verificación o publicación

- Las reglas ampliadas de esta versión no se han publicado desde este entorno. Debe ejecutarse firebase/actualizar-cloud-shell.sh usando la sesión administrativa del usuario. La ampliación necesita estas nuevas reglas.
- Las pruebas de reglas Firestore/Storage se intentaron ejecutar, pero el proxy deniega descargar el emulador oficial desde storage.googleapis.com. Se conserva el registro del fallo y los tests reproducibles. **No se cuentan como aprobados.** El script Cloud Shell los ejecuta antes del despliegue y se detiene si fallan.
- Primer intento de Android Lint: el proceso quedó atascado en KotlinUFile.getAllCommentsInFile / BidirectionalTextDetector sin terminar. No se presenta el informe de una versión anterior como validación de esta versión. El reintento con fuentes congeladas tampoco produjo informe final antes de detenerlo; esta revisión no certifica Android Lint. Se conserva el diagnóstico.
- Google real requiere un dispositivo con Google Play Services. El emulador usado no lo tiene; no se afirma autenticación Google verificada.
- Inicio de sesión por correo, lectura, publicación y moderación contra producción necesitan prueba desde un teléfono con las reglas nuevas. No se crearon usuarios, reportes ni eventos ficticios en la base real.
- Cloud Storage requiere bucket habilitado, reglas publicadas y permiso del agente de reglas para leer Firestore. No se verificó una subida contra producción ni se habilitó facturación.

## Interpretación de las capturas

Las capturas rotuladas SIMULACIÓN LOCAL permiten revisar la interfaz y la navegación. Usan ejemplos en memoria y no acreditan lectura/escritura en Firestore. Las capturas del acceso corresponden a la pantalla de la APK conectada; mostrarla tampoco demuestra un inicio de sesión exitoso.

No se garantizan una calificación ni ausencia absoluta de errores en todos los dispositivos. La guía de sustentación distingue claramente lo demostrado, lo implementado y lo pendiente de despliegue.
