# CIVITA · Base de datos y configuración final

Proyecto: **civita-5cc42**. Usa la cuenta institucional que administra el proyecto. La app usa el JSON Android actualizado con cliente OAuth web. Ese JSON es configuración cliente y no concede acceso administrativo.

La ejecución anterior compartida de Cloud Shell confirmó Firestore, categorías, reglas antiguas y acceso por correo. **Las reglas y rutas ampliadas de esta entrega todavía deben publicarse.** No se han presentado como desplegadas desde este entorno.

## Actualización recomendada en Cloud Shell

```bash
CIVITA_SETUP_DIR=$(mktemp -d)
git clone --depth 1 https://github.com/AnonymusAng/CIVITA.git "$CIVITA_SETUP_DIR" &&
bash "$CIVITA_SETUP_DIR/firebase/actualizar-cloud-shell.sh"
```

El script usa la sesión activa de gcloud; comprueba que sea la cuenta institucional. Ejecuta primero las pruebas Python y las reglas con emuladores oficiales, y se detiene si fallan. Requiere Node.js 22 o superior, Java 21 y Python 3 (disponibles o instalables en Cloud Shell). Nunca imprime tokens, crea claves privadas ni cambia facturación. Si npm/firebase requieren otra versión de Node, usa la compatible con package.json antes de repetir.

Después conserva la base existente, completa únicamente valores ausentes, crea la capa de reportes y publica firestore.rules tras guardar las reglas anteriores. También confirma correo y descarga configuración Android. No borra reportes ni usuarios, y preserva valores existentes válidos. Los permisos IAM son distintos de las reglas de cliente.

## Colecciones y MER

El diccionario completo está en [SCHEMA.md](SCHEMA.md) y [schema.json](schema.json). Se incluyen usuarios, preferencias, suscripciones, buzón, reportes, comentarios, validaciones, apoyos, evidencias, historial, denuncias, moderación, categorías, eventos, capas y puntos. deviceTokens tiene esquema y reglas preparados; la entrega no registra tokens ni envía push.

Firestore no mantiene colecciones vacías. Los únicos datos de configuración iniciales son appConfig/public, cinco categorías y mapLayers/reportes. El resto aparece al registrar/iniciar una cuenta y ejecutar acciones reales. No se crean usuarios, incidentes ni eventos ficticios para llenar la consola.

La app crea perfil y preferencias al iniciar sesión. Publicar crea reporte e historial atómicos. Participar crea comentarios, validaciones o apoyos. Moderar crea acción, historial y notificación privada en el mismo commit. Los privilegios de moderador proceden de un custom claim verificado, nunca de editar el documento users.

## Evidencias: servicio separado

Cloud Storage debe estar habilitado y tener un bucket operativo. Puede requerir un plan con facturación según las condiciones de Firebase; estos scripts **no habilitan ni cambian el plan**. Sin Storage, el acceso, los reportes de texto y la participación siguen usando Firestore; adjuntar archivos mostrará un error explicativo.

Cuando el bucket ya exista, obtén su nombre del google-services.json y publica las reglas del archivo:

```bash
CIVITA_BUCKET=$(python3 -c 'import json; print(json.load(open("civita-resultado/google-services.json"))["project_info"]["storage_bucket"])')
python3 "$CIVITA_SETUP_DIR/firebase/initialize.py" --apply --storage-bucket "$CIVITA_BUCKET"
```

Las reglas Storage consultan Firestore para comprobar autor y límite. Firebase requiere que su agente Storage tenga `roles/firebaserules.firestoreServiceAgent`; la consola de Storage o el despliegue interactivo oficial de Firebase solicita habilitar este acceso entre servicios. El script no cambia IAM silenciosamente. Verifica una subida real antes de presentarla como funcional.

## Moderación y datos oficiales

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
