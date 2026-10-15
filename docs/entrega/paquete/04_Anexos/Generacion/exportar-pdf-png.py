from pathlib import Path
from playwright.sync_api import sync_playwright
R=Path(__file__).resolve().parent
with sync_playwright() as p:
 b=p.chromium.launch(executable_path='/usr/bin/chromium',args=['--no-sandbox'])
 for name,w,h,pw,ph in [('CIVITA-Clases',7480,3900,'1189mm','620mm'),('CIVITA-Componentes',3300,1640,'841mm','418mm')]:
  svg=(R/(name+'.svg')).read_text().split('?>')[-1]
  page=b.new_page(viewport={'width':w,'height':h},device_scale_factor=1)
  page.set_content(f'<html><title>CIVITA · Diagramas UML corregidos</title><style>@page{{size:{pw} {ph};margin:0}}html,body{{margin:0;padding:0;width:100%;height:100%}}svg{{display:block;width:100%;height:100%}}</style><body>{svg}</body></html>')
  page.evaluate('document.fonts.ready');page.pdf(path=str(R/(name+'.pdf')),width=pw,height=ph,print_background=True,prefer_css_page_size=True)
  page.set_viewport_size({'width':min(w,2450),'height':round(h*min(w,2450)/w)})
  page.screenshot(path=str(R/(name+'.png')),full_page=True,timeout=120000)
  page.close()
 b.close()
print('PDF vectoriales de una página e imágenes originales exportados')
