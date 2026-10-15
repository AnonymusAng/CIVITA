# CIVITA · Diagramas horizontales

CIVITA-Clases.pdf: una página A0 horizontal. CIVITA-Componentes.pdf: una página A1 horizontal con interfaz de usuario explícita, usuario y contratos UML I1/I2. CIVITA-Diagramas.pdf: las dos láminas, exactamente dos páginas horizontales. Abrir los PDF/SVG y ampliar para leer firmas sin pérdida de nitidez.

Arquitectura: cliente Android de despliegue monolítico (una APK / :app), organizado con MVVM + Repository + inyección manual, conectado a Firebase BaaS externo. Leer ARQUITECTURA-Y-RELACIONES.md.

Fuentes editables: PlantUML (.puml), SVG y generadores Python. generar-clases.py usa MODELO-CLASES.json y RELACIONES-PRINCIPALES.json; generar-componentes.py usa Python estándar. exportar-pdf-png.py necesita Playwright y Chromium (ajustar /usr/bin/chromium según equipo). No usa red ni credenciales.

Las clases auxiliares y tipos de detalle se conservan en el catálogo y anexos del proyecto. Las pantallas Compose son funciones agrupadas como componentes lógicos; no clases ficticias.
