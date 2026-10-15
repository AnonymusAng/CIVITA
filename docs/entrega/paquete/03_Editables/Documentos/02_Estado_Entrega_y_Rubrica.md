# CIVITA · Estado de la entrega y rúbrica

## Qué está preparado y qué falta

La salida Cloud Shell aportada por el usuario confirmó reglas ampliadas publicadas, registro e inicio de sesión Firebase por correo, publicación de un reporte con autor y fecha del servidor, consulta después de volver a iniciar sesión y limpieza de los datos temporales. Esta evidencia corresponde al backend mediante cliente REST. El recorrido autenticado dentro de la APK Android sigue pendiente de comprobación.

| Criterio de la rúbrica | Peso | Estado y acción pendiente |
|---|---|---|
| Diagrama de componentes | 20 % | Preparado: interfaz de usuario con siete funciones de pantalla, coordinación, datos, contratos UML y servicios Firebase. Explicación separada. Sustentar las responsabilidades y conexiones. |
| Diagrama de clases | 20 % | Preparado: 25 clasificadores principales, atributos, firmas, visibilidad, generalizaciones, realizaciones y 30 relaciones. El catálogo y las vistas de detalle complementan el alcance. Sustentar su correspondencia con el código. |
| Base de datos implementada y conexión funcional | 20 % | Backend confirmado en Cloud Shell. Falta demostrar que la APK inicia sesión, lee configuración/categorías y realiza escrituras/consultas contra ese mismo proyecto Firebase. |
| Primera funcionalidad | 15 % | Propuesta: registro, inicio y cierre de sesión por correo. Debe ejecutarse en vivo desde la APK; comprobar acceso, salida y reingreso. |
| Segunda funcionalidad | 15 % | Propuesta: publicar una novedad y consultarla en Comunidad/detalle, con persistencia al cerrar y volver a abrir la app. Debe ejecutarse en vivo desde la APK. |
| Sustentación y dominio técnico | 10 % | Preparar a todos los integrantes: arquitectura, relaciones UML, conexión Firebase, permisos y ejecución de ambas funcionalidades. |

## Lista esencial antes de presentar

1. Instalar la APK conectada CIVITA 1.1.742 en un teléfono Android 8 o superior, o emulador compatible. Usar conexión a internet y la APK conectada, identificada sin el rótulo SIMULACIÓN LOCAL.
2. Ejecutar la primera funcionalidad con una cuenta del equipo: crear cuenta si es necesario, iniciar sesión, comprobar acceso, cerrar sesión y volver a entrar. No incluir contraseñas ni tokens en capturas o vídeos.
3. Ejecutar la segunda funcionalidad: seleccionar categoría, diligenciar el formulario y publicar un reporte auténtico o inequívocamente marcado como prueba técnica. Esperar confirmación de servidor.
4. Localizarlo en Comunidad y abrir el detalle. Mostrar en Firestore el mismo documento/ID, creatorId y timestamps; comprobar también history/created. Esta es la evidencia de integración real desde Android.
5. Cerrar completamente la aplicación, abrirla y volver a consultar el reporte con internet. Comprobar que no queda un error de permisos o una indicación de datos pendientes de sincronizar.
6. Ensayar la sustentación con todos los integrantes usando el código y los dos diagramas. Mantener copias locales de APK, proyecto y documentación.

El borrado ciudadano de reportes está deshabilitado por reglas. Si se crea un reporte de prueba manual, su limpieza debe limitarse a su ID y subcolecciones mediante una cuenta administrativa. La prueba automática de Cloud Shell ya limpió sus propios datos; no se reutiliza como prueba de interacción Android.

## Evidencia recomendada de respaldo

Guardar capturas o un vídeo corto del acceso autenticado, publicación confirmada, reporte visible, documento Firestore y persistencia después de reabrir. La rúbrica exige ejecutar y demostrar las dos funcionalidades; estas evidencias ayudan si falla la conectividad durante la presentación, pero no sustituyen la demostración solicitada.

## Pendientes según lo que se vaya a presentar

- Google: el código está integrado y la configuración contiene el cliente OAuth web. Falta probar desde Android con Google Play Services, proveedor habilitado y huella de la APK correcta. El emulador básico de esta nube no tiene Play Services.
- Evidencias multimedia: falta confirmar bucket Cloud Storage operativo, reglas y permiso del agente para consultar Firestore, y probar subida/consulta. Es independiente del funcionamiento de reportes textuales. No se activó facturación desde este entorno.
- Comentarios, validaciones, apoyos, moderación, buzón, preferencias y suscripciones: implementados en código; su recorrido Android no se ha certificado. Si se incluyen en la demostración, probarlos antes con las cuentas/roles correspondientes.
- Mapa: la vista actual es ilustrativa; no acredita GPS ni seguimiento en tiempo real. Las suscripciones guardadas no equivalen a entrega push desplegada.
- Calidad técnica adicional: la compilación y las 26 pruebas unitarias Android pasaron. Hay 34 pruebas Python aprobadas. Android Lint no terminó en este entorno; no se declara aprobado. Las pruebas locales de reglas estuvieron bloqueadas por la descarga del emulador; la configuración Cloud Shell sí llegó a la publicación y a la prueba real confirmadas por el usuario.

Estos pendientes adicionales no son seis requisitos nuevos de la rúbrica. Afectan las capacidades que se describan como funcionales en la presentación y los compromisos de alcance del proyecto.

## Cierre de la entrega

La entrega podrá considerarse comprobada cuando el equipo complete el recorrido de las dos funcionalidades desde la APK conectada, muestre la integración con Firestore y sustente las decisiones de diseño. Los diagramas y la evidencia backend ya están preparados; no se presenta una simulación visual como prueba de integración Android.
