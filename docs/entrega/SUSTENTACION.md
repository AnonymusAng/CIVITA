# CIVITA · Guion de sustentación

## Preparación

Abrir los dos PDF de diagramas, el proyecto/código y la consola Firestore. Instalar la APK conectada en un dispositivo con internet y comprobar antes las dos funcionalidades. Cada integrante debe conocer todo el recorrido.

## Recorrido sugerido de ocho a diez minutos

1. Problema y alcance, un minuto. CIVITA permite compartir y consultar novedades ciudadanas en Popayán. Distinguir los reportes reales de cualquier prueba técnica y de la aplicación de simulación.
2. Arquitectura y componentes, dos minutos. Señalar al usuario, las pantallas Compose, los ViewModels, los repositorios, los contratos UML y Firebase. Explicar la diferencia entre la interfaz visual y las interfaces de programación.
3. Primera funcionalidad, dos minutos. Mostrar registro/inicio de sesión por correo, acceso, cierre y reingreso. Las credenciales son responsabilidad de Firebase Authentication; no se guardan contraseñas en Firestore.
4. Segunda funcionalidad, dos minutos. Publicar una novedad, esperar confirmación, abrir Comunidad y su detalle. Localizar el mismo ID en Firestore con su autor, fecha de servidor e historial inicial. Reabrir la app para demostrar persistencia.
5. Clases y decisiones, uno o dos minutos. Localizar en el código una generalización, una realización de interfaz, una referencia con multiplicidad y la publicación transaccional. Explicar cómo requestId evita duplicados al reintentar.
6. Cierre técnico, un minuto. Exponer el alcance comprobado y las capacidades adicionales que todavía requieren prueba. Todos los integrantes deben poder responder preguntas.

## Respuesta sobre arquitectura

«El cliente Android se despliega como un monolito: un módulo :app y una APK. Internamente está organizado con MVVM por capas y repositorios. Firebase Authentication, Firestore y Storage son servicios externos administrados. Los componentes dentro del límite de la APK son responsabilidades lógicas de la misma aplicación».

Esta clasificación está respaldada por settings.gradle.kts, CivitaApp.kt, los ViewModels y los repositorios. MVVM describe organización interna; monolítico describe la unidad de despliegue del cliente.

## Preguntas para todo el equipo

- ¿Qué diferencia hay entre la interfaz visual de usuario y CommunityActions/ReportRepository?
- ¿Qué clases heredan y cuáles implementan interfaces? ¿Por qué el triángulo apunta al tipo general o al contrato?
- ¿Dónde se guardan el UID del autor, las categorías y el historial? ¿Cuándo aparece una colección dinámica en Firestore?
- ¿Cómo distingue Authentication la identidad y cómo autorizan las reglas Firestore las escrituras?
- ¿Por qué la cardinalidad de una lista es 0..* y la de una referencia nullable es 0..1?
- ¿Por qué una subcolección Firestore no implica composición ni eliminación en cascada?
- ¿Qué hacen los timestamps del servidor y el requestId estable?
- ¿Qué ocurre si se pierde la conexión o vence una respuesta? ¿Cómo se identifica el estado en caché?
- ¿Qué requiere Google Sign-In? ¿Por qué Storage es un servicio adicional para archivos?
- ¿Qué se comprobó desde Cloud Shell y qué se comprobó desde la APK?

## Evidencias

Conservar capturas/vídeo de apoyo, el resultado backend sin credenciales y copias locales de APK, proyecto y PDF. La aplicación de simulación permite revisar interfaz; las dos demostraciones de la rúbrica deben realizarse en la APK conectada.
