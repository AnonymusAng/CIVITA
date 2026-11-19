# CIVITA · Android 1.3.0

Aplicación nativa Android para compartir y consultar novedades ciudadanas de Popayán.

**Asignatura:** Ingeniería de Software II
**Grupo:** 01 · proyecto individual
**Integrante:** Joan Alberto Erazo Zúñiga · 10742

## Stack y alcance

- Kotlin, Jetpack Compose y Material Design 3, con tema claro y oscuro del sistema.
- MVVM, StateFlow, repositorios e inyección manual en un módulo Android `:app`.
- Firebase Authentication (correo/contraseña y Google) y Cloud Firestore.
- Reportes con categoría, descripción, referencia de lugar, afectación y coordenadas opcionales; confirmación idempotente y actualización en tiempo real.
- Comunidad con detalle, comentarios, validaciones, apoyos, perfil, preferencias, capas, buzón y auditoría para moderadores.
- La versión 1.3.0 incorpora el agente propio de auditoría CIVITA: reglas deterministas ejecutadas en Cloud Functions, sin proveedor externo y sin validar la veracidad de los hechos. El mapa es ilustrativo; GPS automático, push y multimedia no están activos.

## Abrir en Android Studio

1. Extrae el ZIP y abre la carpeta `CIVITA-Android`, donde está `settings.gradle.kts`.
2. Usa JDK 21, Android SDK 37.2 y Build Tools 37.0.0.
3. Sincroniza Gradle con internet para descargar el wrapper y las dependencias si no están en caché.
4. Conserva `app/google-services.json` para el proyecto Firebase configurado o reemplázalo por el archivo cliente de tu aplicación Android.
5. Ejecuta la configuración `app` en Android 8.0/API 26 o superior.

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

La APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.

Para revisar navegación con datos locales, usa `./gradlew assembleDebug -PcivitaSimulation=true`. La variante lleva el sufijo `.simulation` y muestra un aviso; no inicia Firebase.

## Firebase

Habilita Email/Password en Authentication y publica las reglas Firestore incluidas. Google requiere Google Play Services y las huellas SHA de la firma. El JSON incluido contiene configuración cliente; no contiene claves administrativas ni claves privadas de firma. El cliente Android requiere Auth y Firestore. Para la auditoría se necesita desplegar la función, las reglas y los índices incluidos. El agente no necesita claves de proveedores de IA ni envía el texto fuera de Firebase. Consulta firebase/README.md; Cloud Functions en producción puede requerir Blaze y generar costos.

## Validación y límites

El código fuente está en 1.3.0; la APK recuperada 1.1.742 es anterior. La versión 1.3.0 aún necesita compilación con Android Studio y prueba en un teléfono/emulador conectado a Firebase. En el entorno de preparación no estaban disponibles Android SDK ni una distribución Gradle completa; el intento de descarga falló por DNS. No uses la APK 1.1.742 como prueba del diseño ni de la auditoría nuevos.

Consulta [Rúbrica y estado de entrega](docs/entrega/ESTADO-ENTREGA.md), [Validación](docs/entrega/VALIDACION.md), [Guía Android Studio](docs/entrega/ANDROID-STUDIO.md) y [Firebase](firebase/README.md).
