# Evidencia de validación · CIVITA 1.1.742

## Verificado localmente

- APK conectada: compilación assembleDebug correcta, versión 1.1.742, código 3, Android mínimo API 26.
- 26 pruebas unitarias Android aprobadas, sin fallos. Incluyen validación de borradores, geohash estándar y recuperación/idempotencia de estado de publicación.
- 34 pruebas Python aprobadas, sin fallos. Incluyen preservación de datos, precondiciones, diagnóstico sin escrituras, configuración Android y validación de eventos/puntos oficiales.
- APK de simulación compilada por separado, applicationId co.edu.uniautonoma.civita.simulation; no inicializa Firebase.
- ZIP extraído: Gradle Wrapper ejecutó help --offline correctamente desde la carpeta distribuida. Se verifican CRC y coincidencia de cada archivo antes de publicar.
- Firma de la APK conectada verificada con apksigner. SHA-1 y SHA-256 coinciden con las registradas por el inicializador.
- 19 vistas de diagramas generadas, con fuentes PlantUML, SVG y PNG. Se verificó sintaxis y se inspeccionaron visualmente componentes y MER.

## Verificado por el usuario en Cloud Shell (versión anterior)

La salida compartida confirma acceso al proyecto con su cuenta institucional, base Firestore nativa, configuración y categorías, reglas anteriores publicadas, correo habilitado, consulta administrativa de reportes y JSON Android actualizado con cliente OAuth web. No equivale a una prueba del SDK Android.

## Pendiente de verificación o publicación

- El usuario aportó la salida de Cloud Shell que confirma publicación de las reglas ampliadas y la prueba real de sesión, publicación, consulta y limpieza. No fue un despliegue desde este entorno.
- Las pruebas de reglas Firestore/Storage se intentaron ejecutar, pero el proxy deniega descargar el emulador oficial desde storage.googleapis.com. Se conserva el registro del fallo y los tests reproducibles. **No se cuentan como aprobados.** El script Cloud Shell los ejecuta antes del despliegue y se detiene si fallan.
- Primer intento de Android Lint: el proceso quedó atascado en KotlinUFile.getAllCommentsInFile / BidirectionalTextDetector sin terminar. No se presenta el informe de una versión anterior como validación de esta versión. El reintento con fuentes congeladas tampoco produjo informe final antes de detenerlo; esta revisión no certifica Android Lint. Se conserva el diagnóstico.
- Google real requiere un dispositivo con Google Play Services. El emulador usado no lo tiene; no se afirma autenticación Google verificada.
- La prueba de cliente REST por correo, publicación y consulta contra producción fue confirmada por la salida Cloud Shell del usuario; sus datos temporales se limpiaron. El recorrido de interfaz Android, Google, moderación y evidencias mantiene su validación independiente pendiente.
- Cloud Storage requiere bucket habilitado, reglas publicadas y permiso del agente de reglas para leer Firestore. No se verificó una subida contra producción ni se habilitó facturación.

## Ejecución visual y límites del emulador

La APK de simulación definitiva se instaló y abrió en API 26 por software, sin KVM. El sistema Android del AVD mostró ANR de System UI durante el arranque/captura. Se cerró ese proceso y se ajustó temporalmente el fixture para revisar el contenido de la app; esto no modifica el APK. No se certifica estabilidad del sistema ni rendimiento en un teléfono real con estas capturas.

La automatización UI Automator no completó su recorrido: falló esperando el estado de reposo del sistema. Se conserva el resultado incomplete y no se cuenta como prueba aprobada. Se entregan únicamente las vistas inspeccionadas manualmente; se descartaron capturas en blanco o tomadas durante transiciones. System UI volvió a habilitarse al terminar.

## Interpretación de las capturas

Las capturas rotuladas SIMULACIÓN LOCAL permiten revisar la interfaz y la navegación. Usan ejemplos en memoria y no acreditan lectura/escritura en Firestore. Las capturas del acceso corresponden a la pantalla de la APK conectada; mostrarla tampoco demuestra un inicio de sesión exitoso.

No se garantizan una calificación ni ausencia absoluta de errores en todos los dispositivos. La guía de sustentación distingue claramente lo demostrado, lo implementado y lo pendiente de despliegue.

## Sesión autenticada y dos funcionalidades

Se añadió firebase/verify_session.py y su guía PRUEBA-SESION.md. Se comprobó de nuevo que Authentication, Firestore, Secure Token y Rules responden con denegación 403 del túnel del proxy. No hay sesión administrativa Google ni gcloud en este entorno. La prueba real no pudo ejecutarse desde este entorno; posteriormente el usuario aportó su salida Cloud Shell con sesión, publicación, consulta y limpieza correctas. Esto verifica el backend con ID token de cliente, no la interfaz Android. Los seis nuevos tests unitarios verifican selección de proyecto, detención ante fallo y limpieza limitada a la ejecución; no sustituyen Firebase real.

## Diagramas principales corregidos

Clases: una página horizontal panorámica (1189 × 620 mm); componentes: una página horizontal panorámica (841 × 418 mm). La recopilación principal CIVITA-Diagramas.pdf tiene exactamente dos páginas. Se validó sintaxis PlantUML y ausencia de conectores atravesando cajas en la lámina de clases. El catálogo y las 19 vistas de detalle se conservan como anexos.

La revisión horizontal mantiene 25 clasificadores y 30 relaciones; componentes identifica siete funciones de pantalla en un bloque explícito de interfaz visual y separa los contratos I1/I2. Se verifica el módulo único :app en settings.gradle.kts para la clasificación monolítica del cliente.

La entrega organizada separa las láminas UML de las explicaciones, el estado de la rúbrica y el guion. Los PDF de diagramas no contienen los bloques de explicación.
