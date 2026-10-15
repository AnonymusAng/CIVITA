# Conciliación del Kanban de CIVITA

El modelo de datos se construyó con el alcance consolidado del proyecto. Antes de implementar persistencia, se debe contrastar con las historias reales de Jira.

## Exportar desde Jira

1. Abra **Filtros > Ver todos los issues** en `civitapopayan.atlassian.net`.
2. Seleccione el proyecto CIVITA y quite filtros de estado para incluir todo el backlog.
3. Use **Exportar > CSV (todos los campos)**.
4. Conserve, como mínimo: clave, resumen, tipo, estado, épica/padre, prioridad, descripción, criterios de aceptación y etiquetas.
5. Guarde el CSV en esta carpeta como `civita_jira_export.csv`.

## Qué revisar contra el modelo

- Cada historia que crea o modifica información debe corresponder a una entidad o relación.
- Los estados del Kanban deben alinearse con `report_status`; los estados del tablero no siempre son estados del dominio.
- Las reglas de anonimato, moderación, validación, cierre y reapertura deben aparecer en los criterios de aceptación.
- Definir si eventos y capas del mapa pertenecen al MVP o a una iteración posterior.
- No implementar tablas solo porque aparecen en el modelo: primero confirmar que soportan una historia priorizada.

## Decisiones pendientes

- Proveedor de mapas y restricciones de licencia.
- Backend y autenticación.
- Política de conservación de fotografías y ubicación.
- Radio y canales de notificación.
- Quién puede verificar, moderar y cerrar un reporte.
