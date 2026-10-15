# Guion de sustentación · CIVITA

## Preparación

Instalar la APK conectada, publicar las nuevas reglas con firebase/actualizar-cloud-shell.sh, comprobar internet y crear dos cuentas reales del equipo. Mantener listas la consola Firestore, la app y el PDF de diagramas. Si se demuestra Google, usar teléfono con Play Services y la SHA de esa APK registrada. No compartir contraseñas en las evidencias.

## Recorrido sugerido (8–10 minutos)

1. Problema y arquitectura (1 minuto): explicar participación ciudadana, Compose, ViewModels, repositorios y servicios Firebase usando componentes.
2. Acceso (2 minutos): registro/correo, logout/login; mostrar perfil y preferencias guardadas. Si corresponde, demostrar Google. El docente debe poder distinguir la cuenta real de la app de simulación.
3. Reporte (2 minutos): elegir categoría, publicar novedad auténtica, abrir Comunidad y mostrar el documento Firestore y history/created. Reabrir la app y comprobar persistencia.
4. Participación (1 minuto): con otra cuenta, comentar, validar y apoyar. Mostrar que UID como ID impide votos duplicados.
5. Moderación (1 minuto): si hay claim configurado, cambiar estado con motivo, mostrar historial, acción y buzón del autor.
6. Diagramas y decisiones (1–2 minutos): relacionar clases con archivos reales, explicar multiplicidades y señalar dónde la transacción protege integridad.

## Preguntas que todos deben poder responder

- ¿Por qué Firestore y Authentication cumplen responsabilidades distintas?
- ¿Dónde se crean users y preferences? ¿Por qué no se guardan contraseñas allí?
- ¿Cómo se evita suplantar al autor o asignarse un rol privilegiado?
- ¿Qué diferencia hay entre validación ciudadana y verificación oficial?
- ¿Cómo evita duplicados el requestId de publicación?
- ¿Qué pasa si se pierde internet o una respuesta vence?
- ¿Por qué una colección vacía no aparece en la consola?
- ¿Por qué evidencias requieren Cloud Storage además de Firestore?
- ¿Qué partes son funcionales, cuáles están simuladas y cuáles requieren un servicio adicional?

## Evidencia para la rúbrica

Guardar capturas propias de acceso confirmado, reporte visible, documento Firestore, comentario/validación y reglas publicadas. Conservar una copia local de APK, proyecto y PDFs. La aplicación de simulación sirve para revisar interfaz, no reemplaza las demostraciones funcionales que exige la rúbrica.
