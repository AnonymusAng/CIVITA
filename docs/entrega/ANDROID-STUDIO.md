# Abrir CIVITA en Android Studio

1. Descarga CIVITA-Proyecto.zip desde GitHub y extrae todo. Abre la carpeta **CIVITA-Android**, la que contiene settings.gradle.kts; no abras sólo app.
2. Usa Android Studio compatible con Android Gradle Plugin 9.4.0. En Gradle JDK selecciona **JDK 21**. Conserva Gradle Wrapper 9.6.0 y su SHA-256.
3. Desde SDK Manager instala Android SDK **37.2**, Build Tools **37.0.0** y acepta sus licencias. Android Studio necesita conexión para descargar dependencias la primera vez.
4. El ZIP incluye app/google-services.json del proyecto civita-5cc42, ya actualizado con cliente OAuth web. No contiene claves privadas de servidor. El repositorio Git no lo rastrea; conserva el archivo del ZIP o descarga el tuyo desde Firebase.
5. Sincroniza Gradle, selecciona app y ejecuta en Android 8.0/API 26 o superior. Para Google usa dispositivo con Play Services.
6. Desde la terminal del proyecto ejecuta `./gradlew signingReport` (Windows: `gradlew.bat signingReport`). Registra en Firebase las SHA-1/SHA-256 de tu firma debug local si difieren de las de la APK entregada. Luego actualiza el JSON si Firebase lo requiere.
7. Publica las reglas y configuración ampliadas mediante firebase/actualizar-cloud-shell.sh en Cloud Shell antes de probar las nuevas operaciones en producción. La sesión administrativa de Cloud Shell no se incorpora al APK.

## Comandos

Linux/macOS: `./gradlew testDebugUnitTest assembleDebug lintDebug`.
Windows: `gradlew.bat testDebugUnitTest assembleDebug lintDebug`.
APK de desarrollo: app/build/outputs/apk/debug/app-debug.apk.

Simulación visual independiente, sin Firebase: `./gradlew assembleDebug -PcivitaSimulation=true`. Tiene applicationId con sufijo .simulation y aviso visible. Para volver al APK conectado, recompila sin esa propiedad.

Firebase Emulator Suite: inicia los tres servicios locales auth/firestore/storage con el mismo ID civita-5cc42 del JSON y compila con `-PcivitaEmulators=true`. Todos esos servicios se emulan y las pruebas de reglas usan un proyecto distinto demo-civita. Es otra configuración local; usa 10.0.2.2 y permite HTTP sólo en ese build debug. No mezcles cuentas o datos locales con producción. Una compilación release fuerza ambos modos desactivados.

No se distribuyen local.properties, cachés, node_modules ni keystores privados. Android Studio creará los archivos locales que corresponden a tu equipo.
