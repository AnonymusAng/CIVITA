# Esquema Firestore de CIVITA

Proyecto: civita-5cc42. Base: (default), Firestore Native.
No es SQL: las colecciones se materializan al guardar documentos. La aplicación utiliza tres rutas; las cuentas y contraseñas se gestionan en Firebase Authentication, no en una colección de contraseñas.

## appConfig/public

| Campo | Tipo Firestore | Valor inicial |
|---|---|---|
| allowedMediaTypes | array de string | image/jpeg, image/png, video/mp4 |
| maxEvidenceBytes | integer | 10485760 |
| maxEvidenceCount | integer | 3 |
| minRadiusMeters | integer | 250 |
| maxRadiusMeters | integer | 15000 |
| version | integer | 1 |
| updatedAt | timestamp | servidor |

Se conserva un valor existente válido; se completan solo campos faltantes. Los límites de evidencias y radios se documentan como configuración prevista: el APK actual publica texto y no incorpora GPS, multimedia o filtros de distancia.

## categories/{categoryId}

Documentos iniciales: movilidad, obras, seguridad, eventos y ambiente. Conservan la organización del prototipo original. Movilidad usa #1976D2 según la base mostrada; cualquier color existente válido se conserva.

| Campo | Tipo | Restricción |
|---|---|---|
| name | string | nombre visible no vacío |
| color | string | #RRGGBB o #AARRGGBB |
| active | boolean | la app consulta las activas |
| defaultDurationHours | integer | 1–168; inicial 4 |
| icon | string | nombre documental del icono |
| version | integer | inicial 1 |
| updatedAt | timestamp | servidor al crear o completar |

## reports/{requestId}

La app crea estos documentos con transacciones remotas. No se insertan reportes ficticios ni documentos vacíos para simular la colección: ambos contaminarían la consulta o romperían el mapeo estricto del APK.

| Campo | Tipo | Origen / restricción |
|---|---|---|
| creatorId | string | UID de Firebase Authentication |
| categoryId | string | ID de categoría existente y activa |
| categoryName | string | nombre de la categoría |
| categoryColor | string | color de la categoría |
| title | string | 5–120 caracteres, sin texto solo en blanco |
| description | string | 10–2000 caracteres, sin texto solo en blanco |
| locationReference | string | 3–200 caracteres; referencia manual |
| severity | integer | 1–3 |
| status | string | review al crear |
| confidence | integer | 0 al crear |
| createdAt | timestamp | serverTimestamp |
| updatedAt | timestamp | serverTimestamp |

El ID estable evita duplicar reintentos del mismo borrador. Las reglas rechazan campos adicionales, autor falsificado, categorías inactivas y timestamps manuales. El cliente no puede modificar o borrar reportes.

## Relaciones y consultas

categoryId referencia el ID de una categoría; creatorId referencia un usuario de Authentication. Firestore no aplica claves foráneas SQL: la transacción y las reglas validan la categoría y el UID.
Las categorías se consultan por active == true. Los reportes se consultan con createdAt descendente y límite 100. Son consultas con índices de campo único automáticos; no requieren un índice compuesto personalizado. El inicializador no borra índices existentes.
La vista Alertas deriva de los reportes; no necesita otra colección. El perfil usa FirebaseUser; no necesita una colección users para el alcance actual.
