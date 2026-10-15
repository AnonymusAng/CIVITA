from pathlib import Path
import xml.etree.ElementTree as E
import textwrap,html,json
O=Path(__file__).resolve().parent
B=json.loads((O/'MODELO-CLASES.json').read_text());N='http://www.w3.org/2000/svg';E.register_namespace('',N)
r=E.Element('{'+N+'}svg',{'width':'4900','height':'6930','viewBox':'0 0 4900 6930'})
def el(n,a={},p=r):return E.SubElement(p,'{'+N+'}'+n,{k:str(v) for k,v in a.items()})
def text(x,y,s,size=23,bold=False,color='#213e54'):
 t=el('text',{'x':x,'y':y,'font-family':'DejaVu Sans','font-size':size,'font-weight':'bold' if bold else 'normal','fill':color});t.text=s
el('rect',{'width':4900,'height':6930,'fill':'white'})
d=el('defs')
for name,path,fill in [('ref','M 1 1 L 13 7 L 1 13','none'),('triangle','M 1 1 L 13 7 L 1 13 Z','white')]:
 m=el('marker',{'id':name,'viewBox':'0 0 15 15','refX':13,'refY':7,'markerWidth':12,'markerHeight':12,'orient':'auto'},d);el('path',{'d':path,'fill':fill,'stroke':'#365770','stroke-width':1.4},m)
box={};lines_manifest=[]
def card(name,x,y,w=1280,kind=None,lines=None):
 b=B.get(name,{'kind':'class','header':name,'lines':[]});lines=lines if lines is not None else b['lines']
 lines=list(dict.fromkeys(lines));attrs=[];ops=[]
 for s in lines:
  if s.startswith('- required') or s.startswith('- parseColor') or s.startswith('- observe<') or s in ['- uid(): String','- report(id: String): DocumentReference','- user(): DocumentReference']:continue
  s=s
  (ops if '(' in s else attrs).append(s)
 wrap=lambda seq:[ln for s in seq for ln in textwrap.wrap(s,width=91,subsequent_indent='    ',break_long_words=False,break_on_hyphens=False)]
 attrs,ops=wrap(attrs),wrap(ops)
 tag=kind or ('interface' if b['kind']=='interface' else 'enumeration' if b['kind']=='enum' else 'data class' if 'data class' in b['header'] else 'object' if 'object' in b.get('header','') else 'class')
 fill='#efe7fb' if name.endswith('UiState') else '#ddf1e9' if tag in ['data class','enumeration'] else '#deebf7'
 head=95;h=head+len(attrs)*29+len(ops)*29+(25 if attrs and ops else 15)
 el('rect',{'x':x,'y':y,'width':w,'height':h,'rx':5,'fill':'#fbfcfd','stroke':'#54718a','stroke-width':2})
 el('path',{'d':f'M{x} {y+head} V{y} H{x+w} V{y+head} Z','fill':fill})
 text(x+w/2-100,y+30,'«'+tag+'»',21)
 text(x+20,y+67,{'RepositorySnapshot':'RepositorySnapshot<T>'}.get(name,name),29,True)
 if name=='ViewModel':r[-1].set('font-style','italic')
 el('line',{'x1':x,'x2':x+w,'y1':y+head,'y2':y+head,'stroke':'#54718a','stroke-width':2})
 yy=y+head+31
 for s in attrs:text(x+20,yy,s,22);yy+=29
 if attrs and ops:
  el('line',{'x1':x,'x2':x+w,'y1':yy-13,'y2':yy-13,'stroke':'#8fa7b8','stroke-width':1});yy+=10
 for s in ops:text(x+20,yy,s,22);yy+=29
 box[name]=(x,y,w,h);return h
# Clases principales completas en su interfaz pública; operaciones de CommunityActions
# se heredan por realización, sin repetirlas en CommunityViewModel.
actions=set(B['CommunityActions']['lines']);B['CommunityViewModel']['lines']=[s for s in B['CommunityViewModel']['lines'] if s not in actions]
text(80,75,'CIVITA · DIAGRAMA DE CLASES',52,True)
text(80,120,'MVVM por capas · Repositorios · Inyección manual · UML 2.5.1',29)
text(80,170,'Clases principales de autenticación, publicación y comunidad · Código Kotlin 1.1.742',25)
card('ViewModel',2060,240,780,kind='external AndroidX',lines=[])
text(80,420,'AUTENTICACIÓN',32,True);text(1770,420,'REPORTES',32,True);text(3460,420,'COMUNIDAD',32,True)
for name,x,y in [('AuthViewModel',80,490),('CivitaViewModel',1770,490),('CommunityViewModel',3460,490),('AuthRepository',80,1150),('ReportRepository',1770,1150),('CommunityActions',3460,1350),('FirebaseReportRepository',1770,1740),('CommunityRepository',3460,2130),('AuthUiState',80,1850),('AuthValidation',80,2450),('MainActivity',80,3500),('ComponentActivity',80,3800),('AppContainer',80,2920),('ReportDraft',1770,2350),('CivitaUiState',1770,2980),('CommunityUiState',3460,3800),('IncidentCategory',80,4050),('UrbanReport',1770,4050),('UserProfile',80,4650),('UserPreferences',3460,4700),('ReportStatus',1770,4920),('AppConfig',80,5350),('ReportComment',3460,5350),('GeoHash',1770,5500)]:card(name,x,y)
def at(n,side='bottom',offset=0):
 x,y,w,h=box[n];return {'top':(x+w/2+offset,y),'bottom':(x+w/2+offset,y+h),'left':(x,y+h/2+offset),'right':(x+w,y+h/2+offset)}[side]
def edge(a,b,kind,points,role='',mult=None,label=None):
 color='#227f70' if kind=='id' else '#365770'
 el('polyline',{'points':' '.join(f'{x},{y}' for x,y in points),'fill':'none','stroke':color,'stroke-width':2.6,'stroke-linejoin':'round',**({'stroke-dasharray':'13 10'} if kind in ['use','realize'] else {}),**({'marker-end':'url(#triangle)'} if kind in ['inherit','realize'] else {'marker-end':'url(#ref)'} if kind in ['ref','use'] else {})})
 if role and label:text(*label,role,23,False,color)
 if mult:
  sx,sy=points[0];ex,ey=points[-1];px,py=points[-2];nx,ny=points[1]
  text(sx-70 if nx<sx else sx+14,sy+30 if nx!=sx else sy-18,mult[0],22,False,color)
  text(ex-65 if px<ex else ex+14,ey-18 if py<=ey else ey+32,mult[1],22,False,color)
 lines_manifest.append({'from':a,'to':b,'type':kind,'role':role,'multiplicity':mult})
# Generalización compartida: todos los extremos parten de clases y acaban en ViewModel.
for n in ['AuthViewModel','CivitaViewModel','CommunityViewModel']:
 p=at(n,'top');edge(n,'ViewModel','inherit',[p,(p[0],380),(2450,380),at('ViewModel','bottom')])
edge('AuthViewModel','AuthRepository','ref',[at('AuthViewModel'),at('AuthRepository','top')],'repository',('0..*','1'),(735,1040))
edge('CivitaViewModel','ReportRepository','ref',[at('CivitaViewModel'),at('ReportRepository','top')],'repository',('0..*','1'),(2430,1070))
edge('FirebaseReportRepository','ReportRepository','realize',[at('FirebaseReportRepository','top'),at('ReportRepository')],'realiza',None,(2440,1630))
edge('CommunityViewModel','CommunityActions','realize',[at('CommunityViewModel'),at('CommunityActions','top')],'realiza sus 12 operaciones',None,(4120,1270))
p=at('CommunityViewModel','right',-100);q=at('CommunityRepository','right',-300);edge('CommunityViewModel','CommunityRepository','ref',[p,(4800,p[1]),(4800,q[1]),q],'repository',('0..*','1'),(4550,2015))
edge('MainActivity','ComponentActivity','inherit',[at('MainActivity'),at('ComponentActivity','top')])
# Estados: referencias derivadas de StateFlow.value, sin propiedad exclusiva.
for a,b,x,side in [('AuthViewModel','AuthUiState',35,'left'),('CivitaViewModel','CivitaUiState',3230,'right'),('CommunityViewModel','CommunityUiState',4860,'right')]:
 p=at(a,side,80);q=at(b,side);edge(a,b,'ref',[p,(x,p[1]),(x,q[1]),q],'state.value',('0..*','1'),None)
p=at('AuthViewModel','left',-100);q=at('AuthValidation','left');edge('AuthViewModel','AuthValidation','use',[p,(10,p[1]),(10,q[1]),q])
# Inyección manual: tres referencias opcionales realmente declaradas en AppContainer.
p=at('AppContainer','left');q=at('AuthRepository','left',100);edge('AppContainer','AuthRepository','ref',[p,(60,p[1]),(60,q[1]),q],mult=('0..*','0..1'))
p=at('AppContainer','right',-80);q=at('ReportRepository','left');edge('AppContainer','ReportRepository','ref',[p,(1530,p[1]),(1530,q[1]),q],'repository',('0..*','0..1'),(1545,2200))
p=at('AppContainer','right',100);q=at('CommunityRepository','left');edge('AppContainer','CommunityRepository','ref',[p,(1570,p[1]),(1570,3690),(3310,3690),(3310,q[1]),q],'communityRepository',('0..*','0..1'),(2060,3660))
edge('FirebaseReportRepository','ReportDraft','use',[at('FirebaseReportRepository'),at('ReportDraft','top')],'publishReport(draft, requestId)',None,(2380,2260))
# Objetos de dominio y valores del estado; cardinalidades en el extremo del tipo.
edge('CivitaUiState','UrbanReport','ref',[at('CivitaUiState'),at('UrbanReport','top')],'reports / visibleReports',('0..*','0..*'),(2460,3900))
p=at('CivitaUiState','left',-50);q=at('IncidentCategory','right');edge('CivitaUiState','IncidentCategory','ref',[p,(1640,p[1]),(1640,q[1]),q],'categories',('0..*','0..*'),(1450,3980))
p=at('CivitaUiState','left',100);q=at('AppConfig','right');edge('CivitaUiState','AppConfig','ref',[p,(1610,p[1]),(1610,q[1]),q],'config',('0..*','0..1'),(1440,5280))
edge('IncidentCategory','UrbanReport','id',[at('IncidentCategory','right',-50),at('UrbanReport','left',-120)],'categoryId {ID}',('1','0..*'),(1390,4140))
p=at('UserProfile','right');q=at('UrbanReport','left',100);edge('UserProfile','UrbanReport','id',[p,(1500,p[1]),(1500,q[1]),q],'creatorId {ID}',('1','0..*'),(1395,4570))
edge('UrbanReport','ReportStatus','ref',[at('UrbanReport'),at('ReportStatus','top')],'status',('0..*','1'),(2460,4800))
p=at('CommunityUiState','left',-80);q=at('UserProfile','right',90);edge('CommunityUiState','UserProfile','ref',[p,(3370,p[1]),(3370,4590),(1440,4590),(1440,q[1]),q],'profile',('0..*','0..1'),(2640,4560))
edge('CommunityUiState','UserPreferences','ref',[at('CommunityUiState'),at('UserPreferences','top')],'preferences',('0..*','1'),(4110,4640))
p=at('CommunityUiState','right',100);q=at('ReportComment','right');edge('CommunityUiState','ReportComment','ref',[p,(4805,p[1]),(4805,q[1]),q],'comments',('0..*','0..*'),(4560,5260))
p=at('FirebaseReportRepository','right',100);q=at('GeoHash','right');edge('FirebaseReportRepository','GeoHash','use',[p,(3270,p[1]),(3270,q[1]),q],'encode()',None,(3090,5420))
p=at('UrbanReport','right',80);q=at('ReportComment','left');edge('UrbanReport','ReportComment','id',[p,(3350,p[1]),(3350,q[1]),q],'comments/{id} {ruta}',('1','0..*'),(3360,5150))
p=at('UserProfile','right',70);q=at('ReportComment','bottom');edge('UserProfile','ReportComment','id',[p,(1460,p[1]),(1460,5850),(4100,5850),q],'authorId {ID}',('1','0..*'),(2380,5820))
p=at('UserProfile','top');q=at('UserPreferences','top');edge('UserProfile','UserPreferences','id',[p,(p[0],4570),(q[0],4570),q],'preferences/settings {ruta}',('1','0..1'),(2200,4540))
# Validación de disposición: ninguna caja se superpone.
for a,(x,y,w,h) in box.items():
 for b,(xx,yy,ww,hh) in box.items():
  if a<b:assert x+w<=xx or xx+ww<=x or y+h<=yy or yy+hh<=y,(a,b)
text(80,6040,'NOTACIÓN Y ALCANCE',32,True)
for i,s in enumerate(['Triángulo hueco + línea continua: generalización. Los tres ViewModels heredan de AndroidX ViewModel.',
'Triángulo hueco + línea discontinua: realización. FirebaseReportRepository implementa ReportRepository;',
'CommunityViewModel implementa CommunityActions. Las operaciones del contrato no se repiten en su implementación.',
'Flecha abierta continua: asociación navegable. state.value es el objeto expuesto por StateFlow, no un miembro adicional.',
'Flecha abierta discontinua: dependencia de uso/parámetro. Línea verde: asociación lógica del dominio por ID de Firestore.',
'1 = exactamente uno; 0..1 = opcional; 0..* = varios. + público; - privado; # protegido; ? nullable; {readOnly} solo lectura; {suspend} coroutine.',
'No hay herencia entre las entidades Kotlin. No se afirma propiedad exclusiva ni borrado en cascada: no corresponden rombos.',
'Esta lámina muestra las clases principales. Tipos de detalle, fábricas y extensiones privadas están en CATALOGO-CODIGO.md.',
'Las pantallas Compose son funciones de nivel superior; no se convierten en clases ficticias. Los SDK se identifican por sus tipos.',
'Las data classes generan constructor, copy, componentN, equals, hashCode y toString. Fuentes: app/src/main/java/.../civita.']):text(80,6100+i*46,s,25)
text(80,6765,'Fuente de la arquitectura: CivitaApp.kt → ViewModels → repositorios → SDK Firebase. Diseño MVVM con backend administrado (BaaS).',26,True)
text(80,6820,'Una página A0 vertical · PDF/SVG vectoriales: ampliar para revisar atributos, firmas y multiplicidades.',25)
E.ElementTree(r).write(O/'CIVITA-Clases.svg',encoding='utf-8',xml_declaration=True)
(O/'RELACIONES-PRINCIPALES.json').write_text(json.dumps(lines_manifest,ensure_ascii=False,indent=2)+'\n')
# Fuente PlantUML del MISMO diagrama principal, además del SVG editorial y su generador.
parts=['@startuml','skinparam classAttributeIconSize 0','hide empty members','title CIVITA · Clases principales']
for n in box:
 b=B.get(n,{'kind':'class','lines':[]});parts.append(b['kind']+' '+n+' {\n'+'\n'.join(b['lines'])+'\n}')
for z in lines_manifest:
 arrow={'inherit':'--|>','realize':'..|>','ref':'-->','use':'..>','id':'--'}[z['type']];m=z['multiplicity'];parts.append(z['from']+(' "'+m[0]+'"' if m else '')+' '+arrow+(' "'+m[1]+'"' if m else '')+' '+z['to']+(' : '+z['role'] if z['role'] else ''))
parts.append('@enduml');(O/'CIVITA-Clases.puml').write_text('\n'.join(parts)+'\n')
(O/'CAJAS.json').write_text(json.dumps(box,indent=2)+'\n')
print('Disposición editorial fija:',len(box),'clasificadores,',len(lines_manifest),'relaciones')
