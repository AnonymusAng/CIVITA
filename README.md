# CIVITA · Android y documentación final

Kotlin y Jetpack Compose con acceso Google/correo, reportes, perfil/preferencias, comentarios, validaciones, apoyos, historial y buzón de moderación. Versión **1.1.742**.

## Descargar

- [Proyecto completo para Android Studio](https://github.com/AnonymusAng/CIVITA/raw/refs/heads/main/entrega/CIVITA-Proyecto.zip): extraer y abrir **CIVITA-Android**. Incluye configuración cliente Firebase actualizada.
- [APK conectada](https://github.com/AnonymusAng/CIVITA/raw/refs/heads/main/entrega/CIVITA.apk).
- [Diagramas y documentación](https://github.com/AnonymusAng/CIVITA/raw/refs/heads/main/entrega/CIVITA-Documentacion.zip).
- [Diagramas PDF](https://github.com/AnonymusAng/CIVITA/raw/refs/heads/main/entrega/CIVITA-Diagramas.pdf).

El repositorio también contiene las fuentes visibles. app/google-services.json no se rastrea en Git; el ZIP incluye la configuración cliente que proporcionó el usuario, sin claves privadas de servidor.

## Abrir y verificar

Consulta [la guía de Android Studio](docs/entrega/ANDROID-STUDIO.md). Requisitos probados: JDK 21, SDK 37.2, Build Tools 37.0.0, AGP 9.4.0 y Gradle Wrapper 9.6.0 con checksum. Android mínimo 8.0/API 26.

[Validación y limitaciones](docs/entrega/VALIDACION.md) distingue compilación, pruebas unitarias, simulación y operaciones remotas pendientes. Las capturas de simulación no representan datos reales.

## Firestore y MER

El esquema ampliado está en [firebase/SCHEMA.md](firebase/SCHEMA.md), con 18 patrones documentales. Los MER originales permanecen en docs/database y los modelos actualizados en docs/uml.

**Es necesario publicar las nuevas reglas antes de usar las nuevas operaciones contra producción.** Sigue [firebase/README.md](firebase/README.md); el script Cloud Shell prueba las reglas en emuladores antes de publicarlas y conserva los datos existentes. Una configuración JSON Android no otorga permisos administrativos.

Firestore crea las colecciones dinámicas con la primera acción real. No se insertan incidentes o usuarios ficticios. Cloud Storage necesita bucket/reglas operativos para evidencias; no se cambia facturación. El mapa es ilustrativo y las suscripciones no implican envío push desplegado.

By: Joan Erazo v.10742
