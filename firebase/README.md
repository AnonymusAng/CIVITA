# CIVITA · Base de datos y configuración final

Proyecto: **civita-5cc42**. Usa la cuenta institucional que administra el proyecto. La app usa el JSON Android actualizado con cliente OAuth web. Ese JSON es configuración cliente y no concede acceso administrativo.

La ejecución anterior compartida de Cloud Shell confirmó Firestore, categorías, reglas antiguas y acceso por correo. **Las reglas y rutas ampliadas de esta entrega todavía deben publicarse.** No se han presentado como desplegadas desde este entorno.

## Actualización recomendada en Cloud Shell

```bash
CIVITA_SETUP_DIR=$(mktemp -d)
git clone --depth 1 https://github.com/AnonymusAng/CIVITA.git "$CIVITA_SETUP_DIR" &&
bash "$CIVITA_SETUP_DIR/firebase/actualizar-cloud-shell.sh"
```

El script usa la sesión activa de gcloud; comprueba que sea la cuenta institucional. Ejecuta primero pruebas de los scripts auxiliares Python y las reglas con emuladores oficiales, y se detiene si fallan. Las pruebas Python no ejecutan la app Android ni llaman a Firebase real. Requiere Node.js 22 o superior, Java 21 y Python 3 (disponibles o instalables en Cloud Shell). Nunca imprime tokens, crea claves privadas ni cambia facturación. Si npm/firebase requieren otra versión de Node, usa la compatible con package.json antes de repetir.

Después conserva la base existente, completa únicamente valores ausentes, crea la capa de reportes y publica firestore.rules tras guardar las reglas anteriores. También confirma correo y descarga la configuración Android a $HOME/civita-resultado. No borra reportes ni usuarios, y preserva valores existentes válidos. Los permisos IAM son distintos de las reglas de cliente.

## Colecciones y MER

El diccionario completo está en [SCHEMA.md](SCHEMA.md) y [schema.json](schema.json). Se incluyen usuarios, preferencias, suscripciones, buzón, reportes, comentarios, validaciones, apoyos, evidencias, historial, denuncias, moderación, categorías, eventos, capas y puntos. deviceTokens tiene esquema y reglas preparados; la entrega no registra tokens ni envía push.

Firestore no mantiene colecciones vacías. Los únicos datos de configuración iniciales son appConfig/public, cinco categorías y mapLayers/reportes. El resto aparece al registrar/iniciar una cuenta y ejecutar acciones reales. No se crean usuarios, incidentes ni eventos ficticios para llenar la consola.

La app crea perfil y preferencias al iniciar sesión. Publicar crea reporte e historial atómicos. Participar crea comentarios, validaciones o apoyos. Moderar crea acción, historial y notificación privada en el mismo commit. Los privilegios de moderador proceden de un custom claim verificado, nunca de editar el documento users.

## Evidencias: fuera del MVP activo

El esquema conserva rutas y campos de evidencia como extensión futura, pero CIVITA 1.3.0 no carga ni muestra archivos, no usa Firebase Storage y no requiere un bucket. No ejecutes inicialización o reglas de Storage para esta versión. Los flujos documentados y las pruebas de entrega cubren reportes de texto, Firestore y participación comunitaria.

## Moderación y datos oficiales

## Agente propio de auditoría CIVITA

Cada reporte nuevo comienza con `auditState: pending` y no se muestra en Comunidad. La Cloud Function `auditNewReport` ejecuta `auditReport()` desde `functions/auditPolicy.js`: un motor experto determinista propio del proyecto que puntúa claridad, detalle, referencia de lugar, variedad del texto y señales de pertinencia cívica. Detecta posibles datos de contacto, contenido dañino y patrones de spam. Sólo asigna `visible` o `manual_review`; no verifica hechos ni cambia el estado oficial `verified`. El texto se procesa dentro de la función de CIVITA y no se envía a un proveedor/modelo externo. Las reglas, puntuación y mensajes son código fuente mantenido en este repositorio, no un modelo entrenado con datos.

No hay clave de proveedor de IA que configurar. Desde la raíz del proyecto, instala las dependencias y ejecuta las pruebas del agente:

```bash
cd functions
npm install
npm test
cd ..
firebase deploy --only functions,firestore:rules,firestore:indexes
```

Cloud Functions de producción puede requerir el plan Blaze y generar cargos por cómputo o red según el uso y las cuotas vigentes. Configura alertas de presupuesto y límites de instancias antes de habilitar el flujo con usuarios reales. La pestaña **Auditoría IA** aparece sólo para cuentas con custom claim `moderator` o `admin`; el moderador puede autorizar un caso pendiente con un motivo que queda registrado en `reportAuditReviews`.

El agente del proyecto no es machine learning ni un modelo entrenado: es un sistema experto determinista escrito para CIVITA. Su resultado ayuda a priorizar publicación/revisión; no certifica la realidad de un incidente. Ante datos personales, señales dañinas, falta de contexto o error de ejecución, deriva a revisión humana. El archivo `functions/test/auditPolicy.test.js` contiene cinco pruebas automatizadas de sus reglas. La prueba local del motor no equivale a desplegar la Cloud Function.

### Moderación y datos oficiales

Tras crear una cuenta real, copia su UID desde Authentication. Un administrador autorizado puede asignarle el claim explícitamente:

```bash
python3 firebase/admin.py --moderator-uid UID_REAL --apply
```

Cierra y vuelve a iniciar sesión en Android para renovar el token. No se asigna moderación automáticamente por conocer un correo.

Eventos oficiales: crea un JSON propio con id, categoryId, createdBy (UID), title, description, latitude, longitude, startsAt y endsAt (ISO 8601 con zona), y status (scheduled/cancelled/completed). Luego:

```bash
python3 firebase/admin.py --event evento-oficial.json
python3 firebase/admin.py --event evento-oficial.json --apply
```

Puntos oficiales: JSON con layerId existente, id, name, latitude, longitude y properties opcional; `python3 firebase/admin.py --feature punto-oficial.json --apply`. Sólo crean documentos nuevos, nunca sobrescriben un ID existente. No distribuyen datos ficticios como si fueran reales.

## Comprobaciones y límites

- Python: `python3 -m unittest discover -s firebase/tests -v`.
- Reglas: `cd tests/firebase && npm ci && npm test`; usa demo-civita, sin tocar producción.
- Android: `./gradlew testDebugUnitTest assembleDebug lintDebug` con JDK/SDK indicados en el README.
- Google real requiere un teléfono/emulador con Play Services y la SHA de la firma instalada.
- Compilar desde otro equipo genera otra firma debug: agrega sus SHA desde `./gradlew signingReport` a Firebase.
- Capturas de simulación y pruebas unitarias no certifican conexión real; comprueba inicio de sesión y publicación en tu dispositivo.

## Sesión autenticada y prueba funcional real

Consulta [PRUEBA-SESION.md](PRUEBA-SESION.md). Después de actualizar las reglas, `python3 firebase/verify_session.py --run` verifica correo, publicación y consulta usando un ID token de cliente, y limpia sus datos temporales. No equivale a una prueba Android ni de Google.
