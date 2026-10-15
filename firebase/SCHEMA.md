# CIVITA · Esquema documental completo (versión 2)

Adaptación de ambos MER originales del ZIP a Cloud Firestore. Los identificadores son String y los timestamps los genera el servidor salvo fechas de eventos/vigencia. Las rutas dinámicas se crean con actividad real: Firestore no conserva colecciones vacías.

## Reglas de integridad

- Perfil: propietario; correo privado. Rol y confianza no son editables. Un custom claim `role=moderator/admin` autoriza moderación.
- Reporte: autor autenticado y categoría activa; texto validado; estado inicial review. El alta crea historial en la misma transacción. Coordenadas opcionales, sin GPS automático.
- Comentarios: autor autenticado, nombre tomado del perfil, longitud limitada. Moderadores pueden ocultar; es ocultamiento de presentación, no borrado de los datos.
- Validaciones y apoyos: UID como clave por reporte. No hay voto duplicado ni autovalidación. Peso fijo 1.
- Estado: sólo moderador, con historial y acción atómicos. El mismo commit crea un mensaje privado al autor.
- Evidencia: sólo autor, JPEG/PNG/MP4 hasta 10 MiB; contador transaccional máximo 3 y metadatos en Firestore. Archivo físico en Cloud Storage. No se guarda una URL pública en el documento; se resuelve al abrir.
- Suscripciones: privadas, categoría válida, GeoPoint y radio acotado. Se guardan; no hay servicio de envío automático de alertas por distancia.
- Eventos y capas: lectura autenticada; altas oficiales con admin.py y una cuenta administrativa. No se siembran eventos ficticios.
- deviceTokens: esquema y reglas disponibles; esta app no registra tokens ni envía notificaciones push.
- confidence/trustLevel: campos reservados. Esta versión muestra recuentos ciudadanos y estado oficial, sin afirmar cálculo automático de confianza.
- Seguridad: sin escrituras públicas, denegación por defecto. Las nuevas reglas deben publicarse antes de usar las nuevas pantallas contra producción.

## `users/{uid}`

| Campo | Tipo y restricción |
|---|---|
| `fullName` | String |
| `email` | String (privado) |
| `role` | String = citizen (claim controla privilegios) |
| `status` | String = active |
| `trustLevel` | Int = 1 |
| `createdAt` | Timestamp servidor |

## `users/{uid}/preferences/settings`

| Campo | Tipo y restricción |
|---|---|
| `alertRadiusMeters` | Int 250..15000 |
| `categoryIds` | List<String> |
| `pushEnabled` | Boolean |
| `preciseLocationAllowed` | Boolean |

## `users/{uid}/alertSubscriptions/{categoryId}`

| Campo | Tipo y restricción |
|---|---|
| `categoryId` | String → categories |
| `center` | GeoPoint |
| `radiusMeters` | Int 250..15000 |
| `active` | Boolean |

## `users/{uid}/deviceTokens/{tokenId}`

| Campo | Tipo y restricción |
|---|---|
| `token` | String |
| `platform` | String = android |
| `updatedAt` | Timestamp servidor |

## `users/{uid}/notifications/{id}`

| Campo | Tipo y restricción |
|---|---|
| `type` | String |
| `title` | String |
| `body` | String |
| `sourceId` | String → reports/events según tipo |
| `readAt` | Timestamp? |
| `createdAt` | Timestamp servidor |
| `actionId` | String → moderationActions (cambio de estado) |

## `reports/{id}`

| Campo | Tipo y restricción |
|---|---|
| `creatorId` | String → users/Auth UID |
| `categoryId` | String → categories |
| `categoryName` | String desnormalizado |
| `categoryColor` | String #RRGGBB |
| `title` | String 5..120 |
| `description` | String 10..2000 |
| `locationReference` | String 3..200 |
| `location` | GeoPoint? |
| `geohash` | String longitud 9 o vacío si no hay coordenadas |
| `severity` | Int 1..3 |
| `status` | review  /  verified  /  resolved |
| `confidence` | Int 0..100 (reservado al servidor) |
| `createdAt` | Timestamp servidor |
| `updatedAt` | Timestamp servidor |
| `expiresAt` | Timestamp |
| `evidenceCount` | Int 0..3 |
| `lastActionId` | String? → history/moderationActions |
| `lastEvidenceId` | String? → evidence |

## `reports/{id}/comments/{commentId}`

| Campo | Tipo y restricción |
|---|---|
| `authorId` | String → users/Auth UID |
| `authorName` | String instantánea |
| `body` | String 1..1000 |
| `hidden` | Boolean |
| `createdAt` | Timestamp servidor |

## `reports/{id}/validations/{uid}`

| Campo | Tipo y restricción |
|---|---|
| `value` | confirm  /  deny |
| `weight` | Int = 1 |
| `createdAt` | Timestamp servidor |

## `reports/{id}/supports/{uid}`

| Campo | Tipo y restricción |
|---|---|
| `createdAt` | Timestamp servidor |

## `reports/{id}/evidence/{evidenceId}`

| Campo | Tipo y restricción |
|---|---|
| `storagePath` | String → Cloud Storage |
| `mediaType` | image/jpeg  /  image/png  /  video/mp4 |
| `sizeBytes` | Int 1..10485760 |
| `uploadedBy` | String → users/Auth UID |
| `createdAt` | Timestamp servidor |

## `reports/{id}/history/{historyId}`

| Campo | Tipo y restricción |
|---|---|
| `changedBy` | String → users/Auth UID |
| `fromStatus` | none  /  review  /  verified  /  resolved |
| `toStatus` | review  /  verified  /  resolved |
| `reason` | String 10..500 |
| `createdAt` | Timestamp servidor |

## `reports/{id}/abuseReports/{uid}`

| Campo | Tipo y restricción |
|---|---|
| `reason` | String = inappropriate |
| `details` | String 10..1000 |
| `createdAt` | Timestamp servidor |

## `moderationActions/{id}`

| Campo | Tipo y restricción |
|---|---|
| `moderatorId` | String → Auth UID con claim role |
| `reportId` | String → reports |
| `action` | String = change_status |
| `reason` | String 10..500 |
| `createdAt` | Timestamp servidor |

## `events/{id}`

| Campo | Tipo y restricción |
|---|---|
| `categoryId` | String → categories |
| `createdBy` | String → Auth UID |
| `title` | String |
| `description` | String |
| `location` | GeoPoint |
| `geohash` | String estándar de longitud 9 |
| `startsAt` | Timestamp |
| `endsAt` | Timestamp |
| `status` | scheduled  /  cancelled  /  completed |
| `createdAt` | Timestamp servidor |
| `updatedAt` | Timestamp servidor |

## `mapLayers/{id}`

| Campo | Tipo y restricción |
|---|---|
| `name` | String |
| `layerType` | reports  /  points |
| `styleJson` | String JSON |
| `active` | Boolean |
| `version` | Int |
| `updatedAt` | Timestamp servidor |

## `mapLayers/{id}/features/{featureId}`

| Campo | Tipo y restricción |
|---|---|
| `name` | String |
| `location` | GeoPoint |
| `propertiesJson` | String JSON |
| `createdAt` | Timestamp servidor |
| `updatedAt` | Timestamp servidor |

## `categories/{id}`

| Campo | Tipo y restricción |
|---|---|
| `name` | String |
| `icon` | String |
| `color` | String #RRGGBB |
| `defaultDurationHours` | Int 1..168 |
| `active` | Boolean |
| `version` | Int |
| `updatedAt` | Timestamp servidor |

## `appConfig/public`

| Campo | Tipo y restricción |
|---|---|
| `allowedMediaTypes` | List<String> |
| `maxEvidenceBytes` | Int |
| `maxEvidenceCount` | Int |
| `minRadiusMeters` | Int |
| `maxRadiusMeters` | Int |
| `version` | Int |
| `updatedAt` | Timestamp servidor |

La configuración pública incluye también supportedCity (String), allowedReportStatuses (List<String>) y validationWeight (Int = 1), como parámetros descriptivos del esquema. Las reglas conservan los estados y el peso fijo permitidos; editar estos parámetros no amplía permisos de cliente.

## Correspondencia con el MER original

`REPORT_SUPPORTS` pasa a `reports/{id}/supports/{uid}`. `USER_PREFERENCES`, `ALERT_SUBSCRIPTIONS` y `NOTIFICATIONS` son hijos de `users`. `MAP_LAYERS` y `LAYER_FEATURES` se conservan como `mapLayers` y `features`. Los UUID SQL se sustituyen por UID de Authentication o IDs Firestore; geometry(Point) por GeoPoint y jsonb por String JSON en configuración de capas.

No existe borrado en cascada automático en Firestore. Las reglas de esta entrega deniegan borrar reportes y auditoría. La creación de una subcolección no elimina esa restricción. La ubicación ausente se guarda como null, sin inventar posiciones. El acceso anónimo del diseño conceptual no se habilita: la implementación usa Google/correo, según lo solicitado.
