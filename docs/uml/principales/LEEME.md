# Diagramas corregidos CIVITA

Abrir CIVITA-Clases.pdf (una página A0 vertical) y CIVITA-Componentes.pdf (una página A3 horizontal). CIVITA-Diagramas.pdf reúne ambas láminas, exactamente dos páginas. Los PDF y SVG son vectoriales: ampliar sin pérdida de nitidez. Para revisar firmas no reducir la lámina de clases a A4.

Arquitectura: MVVM por capas, Repository, inyección manual y Firebase BaaS. Leer ARQUITECTURA-Y-RELACIONES.md para la explicación y correspondencia con el código. CATALOGO-CODIGO.md contiene el inventario adicional.

Los .puml son editables con PlantUML. La disposición editorial fija se regenera con generar-clases.py, generar-componentes.py y exportar-pdf-png.py. Los dos primeros usan Python estándar y MODELO-CLASES.json. El exportador usa Python Playwright y Chromium en /usr/bin/chromium (ajustar ejecutable según instalación); mantiene TLS y no descarga recursos de red. No requiere Firebase ni claves.

La recopilación anterior de 19 vistas queda como anexo de detalle en el proyecto; estos dos son los diagramas principales corregidos.
