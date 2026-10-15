from pathlib import Path
import xml.etree.ElementTree as E
O=Path(__file__).resolve().parent;N='http://www.w3.org/2000/svg';E.register_namespace('',N)
r=E.Element('{'+N+'}svg',{'width':'3300','height':'2320','viewBox':'0 0 3300 2320'})
def el(n,a={},p=r):return E.SubElement(p,'{'+N+'}'+n,{k:str(v) for k,v in a.items()})
def text(x,y,s,size=25,bold=False,color='#173a55'):
 t=el('text',{'x':x,'y':y,'font-family':'DejaVu Sans','font-size':size,'font-weight':'bold' if bold else 'normal','fill':color});t.text=s
el('rect',{'width':3300,'height':2320,'fill':'white'})
d=el('defs');m=el('marker',{'id':'use','viewBox':'0 0 12 12','refX':'11','refY':'6','markerWidth':'10','markerHeight':'10','orient':'auto'},d);el('path',{'d':'M 1 1 L 11 6 L 1 11','fill':'none','stroke':'#365d7c','stroke-width':'1.5'},m)
boxes={};routes=[]
def edge(points,dashed=True,arrow=True):
 el('polyline',{'points':' '.join(f'{x},{y}' for x,y in points),'fill':'none','stroke':'#365d7c','stroke-width':'3','stroke-dasharray':'12 9' if dashed else 'none',**({'marker-end':'url(#use)'} if arrow else {})});routes.append(points)
def frame(x,y,w,h,title,fill,size=28):
 el('rect',{'x':x,'y':y,'width':w,'height':h,'rx':16,'fill':fill,'stroke':'#b7cbd7','stroke-width':2});text(x+24,y+43,title,size,True)
def glyph(x,y):
 for dx,dy,w,h in [(0,0,28,36),(-8,6,17,9),(-8,23,17,9)]:el('rect',{'x':x+dx,'y':y+dy,'width':w,'height':h,'fill':'white','stroke':'#54748b','stroke-width':2})
def component(key,x,y,w,h,title,lines,fill='#fff'):
 el('rect',{'x':x,'y':y,'width':w,'height':h,'rx':6,'fill':fill,'stroke':'#54748b','stroke-width':2});glyph(x+w-48,y+20)
 text(x+20,y+40,title,27 if w>400 else 25,True)
 for i,s in enumerate(lines):text(x+20,y+86+i*35,s,24)
 boxes[key]=(x,y,w,h)
text(60,65,'CIVITA · COMPONENTES · INTERFAZ DE USUARIO Y BACKEND',42,True)
text(60,120,'Diseño interno: MVVM + Repository · Despliegue: cliente monolítico Android (una APK) + Firebase BaaS externo',28)
frame(250,220,2300,1350,'CLIENTE ANDROID · CIVITA.apk · UN MÓDULO GRADLE :app','#f5f9fe')
frame(2700,220,550,1350,'FIREBASE · SERVICIOS EXTERNOS','#f0faf5',26)
# Componente compuesto de presentación: las pantallas son funciones Compose, no clases ficticias.
frame(300,350,800,1020,'INTERFAZ DE USUARIO / VIEW','#e9f3ff',30);glyph(1038,375)
text(325,445,'Jetpack Compose · CivitaApp() y navegación',25)
component('AuthScreen',330,500,350,190,'Acceso',['AuthScreen()','Correo / Google','Registro y recuperación'])
component('MapScreen',710,500,350,190,'Mapa',['MapScreen()','Vista ilustrativa','Categorías y reportes'])
component('ReportScreen',330,745,350,190,'Publicación',['ReportScreen()','Formulario del reporte','Envío y confirmación'])
component('CommunityScreen',710,745,350,190,'Comunidad',['CommunityScreen()','Reportes / eventos','Buzón y capas'])
component('ReportDetailScreen',330,990,350,230,'Detalle',['ReportDetailScreen()','Comentarios / validación','Apoyos / evidencias','Historial y moderación'])
component('ProfileScreen',710,990,350,230,'Cuenta',['ProfileScreen()','PreferencesScreen()','Perfil y preferencias','Cierre de sesión'])
text(325,1310,'Funciones de UI agrupadas como componentes lógicos.',23)
# Actor humano fuera del artefacto Android.
el('circle',{'cx':110,'cy':765,'r':22,'fill':'white','stroke':'#365d7c','stroke-width':3})
for x1,y1,x2,y2 in [(110,787,110,870),(65,820,155,820),(110,870,75,920),(110,870,145,920)]:el('line',{'x1':x1,'y1':y1,'x2':x2,'y2':y2,'stroke':'#365d7c','stroke-width':3})
text(45,970,'Usuario',27,True);edge([(158,840),(300,840)],False,False);text(165,815,'usa',23)
component('ViewModels',1260,400,450,280,'Coordinación MVVM',['AuthViewModel','CivitaViewModel','CommunityViewModel','Estado observable StateFlow','Provee CommunityActions (I1)'])
component('Repositories',1940,400,500,280,'Repositorios',['AuthRepository','FirebaseReportRepository','CommunityRepository','Acceso a datos y transacciones','Provee ReportRepository (I2)'])
component('Composition',1260,1240,450,170,'Composición / DI',['CivitaApp + AppContainer','Repositorios y fábricas'])
component('CredentialManager',1260,940,450,180,'Credential Manager',['Cuenta Google / Google ID','Credencial para Firebase Auth'])
component('FirebaseSdk',1940,1000,500,240,'SDK Firebase Android',['FirebaseAuth','FirebaseFirestore','FirebaseStorage'])
component('Authentication',2740,390,470,180,'Authentication',['Correo / contraseña / Google','Identidad y sesión'])
component('Firestore',2740,780,470,180,'Cloud Firestore',['Documentos y listeners','Consultas y transacciones'])
component('Storage',2740,1120,470,180,'Cloud Storage',['Archivos de evidencia','Imágenes y vídeos'])
edge([(1100,570),(1260,570)]);text(1110,548,'observa / actúa',20)
edge([(1710,550),(1940,550)]);text(1745,527,'Auth / comunidad',21)
# Interfaces UML provistas: contratos, diferenciados de la interfaz visual.
for x,name in [(1170,'CommunityActions'),(1850,'ReportRepository')]:
 el('circle',{'cx':x,'cy':800,'r':12,'fill':'white','stroke':'#365d7c','stroke-width':3});text(x-20,852,'I1' if name=='CommunityActions' else 'I2',24,True)
edge([(1100,930),(1135,930),(1135,800),(1158,800)])
edge([(1430,680),(1430,800),(1182,800)],False,False)
edge([(1630,680),(1630,800),(1838,800)])
edge([(2180,680),(2180,800),(1862,800)],False,False)
edge([(1260,1330),(1210,1330),(1210,325),(1480,325),(1480,400)]);text(1300,310,'«create» / suministra',22)
edge([(1710,1360),(1880,1360),(1880,325),(2180,325),(2180,400)]);text(1950,310,'«create»',22)
edge([(2380,680),(2380,1000)]);text(2395,935,'SDK',22)
edge([(1940,600),(1910,600),(1910,1030),(1710,1030)]);text(1740,1005,'Google ID',21)
edge([(2440,1040),(2560,1040),(2560,480),(2740,480)])
edge([(2440,1120),(2600,1120),(2600,870),(2740,870)])
edge([(2440,1190),(2640,1190),(2640,1210),(2740,1210)])
text(2460,1320,'Red autenticada',21)
# Artefactos de seguridad publicados por administración, no por la UI.
for x,title in [(2740,'firestore.rules'),(2990,'storage.rules')]:
 el('path',{'d':f'M{x} 1400 H{x+185} L{x+210} 1425 V1500 H{x} Z','fill':'#fff6e4','stroke':'#b38b40','stroke-width':2});el('path',{'d':f'M{x+185} 1400 V1425 H{x+210}','fill':'none','stroke':'#b38b40','stroke-width':2});text(x+15,1460,title,24,True)
edge([(2740,1450),(2720,1450),(2720,1020),(2960,1020),(2960,960)])
edge([(3095,1400),(3095,1300)])
edge([(3200,1450),(3230,1450),(3230,1000),(3050,1000),(3050,960)])
frame(250,1700,3000,530,'CÓMO EXPLICAR LA ARQUITECTURA EN LA SUSTENTACIÓN','#fbfcfe',30)
notes=['Despliegue del cliente: monolítico. Todo el código Android se compila en :app y se distribuye en una sola APK.',
'Diseño interno: MVVM por capas. View = pantallas Compose; ViewModel = coordinación/StateFlow; Model = tipos y repositorios.',
'Interfaz de usuario: bloque View de la izquierda. Contratos UML: I1 = CommunityActions; I2 = ReportRepository (círculos).',
'Los componentes dentro de la APK son responsabilidades lógicas; Firebase es un backend administrado externo, accedido mediante sus SDK.',
'Flecha discontinua abierta: dependencia de uso. Círculo: interfaz proporcionada. Línea continua al actor: interacción con la UI.',
'Cloud Shell publica reglas/configuración por separado. Storage requiere bucket y permisos; sus reglas consultan autor/contador en Firestore.',
'Fuente: settings.gradle.kts, CivitaApp.kt, ui/screens/*.kt, ViewModels y repositorios · UML 2.5.1 · Una sola página horizontal.']
for i,n in enumerate(notes):text(280,1795+i*57,n,27 if i<4 else 25)
E.ElementTree(r).write(O/'CIVITA-Componentes.svg',encoding='utf-8',xml_declaration=True)
(O/'CAJAS-COMPONENTES.json').write_text(__import__('json').dumps(boxes,indent=2)+'\n')
print('Componentes: UI explícita con siete pantallas, contratos UML y despliegue Android/Firebase')
