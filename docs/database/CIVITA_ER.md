# Modelo entidad–relación de CIVITA

Este modelo corresponde al MVP móvil de Popayán. La fuente editable principal
es `CIVITA_ER.drawio`; este archivo conserva una representación Mermaid útil en
GitHub, GitLab, IntelliJ y Mermaid Live.

```mermaid
erDiagram
    USERS ||--o| USER_PREFERENCES : configura
    USERS ||--o{ REPORTS : publica
    USERS ||--o{ VALIDATIONS : emite
    USERS ||--o{ COMMENTS : escribe
    USERS ||--o{ REPORT_SUPPORTS : apoya
    USERS ||--o{ ALERT_SUBSCRIPTIONS : configura
    USERS ||--o{ NOTIFICATIONS : recibe
    USERS ||--o{ MODERATION_ACTIONS : ejecuta

    CATEGORIES ||--o{ REPORTS : clasifica
    CATEGORIES ||--o{ EVENTS : clasifica
    CATEGORIES ||--o{ ALERT_SUBSCRIPTIONS : filtra

    REPORTS ||--o{ EVIDENCES : contiene
    REPORTS ||--o{ VALIDATIONS : recibe
    REPORTS ||--o{ COMMENTS : contiene
    REPORTS ||--o{ REPORT_SUPPORTS : recibe
    REPORTS ||--o{ REPORT_STATUS_HISTORY : registra
    REPORTS ||--o{ MODERATION_ACTIONS : controla
    REPORTS ||--o{ NOTIFICATIONS : origina

    COMMENTS ||--o{ COMMENTS : responde
    EVENTS ||--o{ NOTIFICATIONS : origina
    MAP_LAYERS ||--o{ LAYER_FEATURES : contiene
```

## Decisiones clave

- `VALIDATIONS` impone una validación por usuario y reporte.
- `REPORT_SUPPORTS` representa el apoyo comunitario sin duplicados.
- `REPORT_STATUS_HISTORY` no se actualiza: cada transición crea una fila.
- `MODERATION_ACTIONS` registra quién tomó la decisión y por qué.
- `geometry` usa PostGIS para reportes, eventos y elementos de capas.
- El puntaje de confianza se guarda como valor calculado y auditable.
