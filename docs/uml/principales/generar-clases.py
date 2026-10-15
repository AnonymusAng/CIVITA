from pathlib import Path
import xml.etree.ElementTree as E
import textwrap,html,json
O=Path(__file__).resolve().parent
B=json.loads((O/'MODELO-CLASES.json').read_text());N='http://www.w3.org/2000/svg';E.register_namespace('',N)
r=E.Element('{'+N+'}svg',{'width':'7480','height':'4700','viewBox':'0 0 7480 4700'})
def el(n,a={},p=r):return E.SubElement(p,'{'+N+'}'+n,{k:str(v) for k,v in a.items()})
def text(x,y,s,size=23,bold=False,color='#213e54'):
 t=el('text',{'x':x,'y':y,'font-family':'DejaVu Sans','font-size':size,'font-weight':'bold' if bold else 'normal','fill':color});t.text=s
el('rect',{'width':7480,'height':4700,'fill':'white'})
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
import heapq,bisect,math
relations=json.loads((O/'RELACIONES-PRINCIPALES.json').read_text())
actions=set(B['CommunityActions']['lines']);B['CommunityViewModel']['lines']=[s for s in B['CommunityViewModel']['lines'] if s not in actions]
text(100,75,'CIVITA · DIAGRAMA DE CLASES · VISTA HORIZONTAL',46,True)
text(100,125,'Cliente Android de despliegue monolítico (:app / una APK) · Diseño interno MVVM por capas + Repository + inyección manual',28)
card('ViewModel',1830,240,780,kind='external AndroidX',lines=[])
for x,t in [(100,'01 · AUTENTICACIÓN'),(1580,'02 · REPORTES'),(3060,'03 · COMUNIDAD'),(4540,'04 · ESTADOS Y CUENTA'),(6020,'05 · DOMINIO / ANDROID')]:text(x,405,t,30,True)
positions=[('AuthViewModel',100,540),('AuthRepository',100,1200),('AuthUiState',100,1900),('AuthValidation',100,2450),('AppContainer',100,2850),('CivitaViewModel',1580,540),('ReportRepository',1580,1200),('FirebaseReportRepository',1580,1720),('ReportDraft',1580,2400),('GeoHash',1580,3100),('CommunityViewModel',3060,540),('CommunityActions',3060,1400),('CommunityRepository',3060,2130),('CivitaUiState',4540,540),('CommunityUiState',4540,1300),('AppConfig',4540,2300),('UserPreferences',4540,2850),('ReportComment',4540,3300),('MainActivity',6020,540),('ComponentActivity',6020,850),('IncidentCategory',6020,1250),('UserProfile',6020,1800),('UrbanReport',6020,2300),('ReportStatus',6020,3000)]
for n,x,y in positions:card(n,x,y)
def at(n,side):
 x,y,w,h=box[n];return {'top':(x+w/2,y),'bottom':(x+w/2,y+h),'left':(x,y+h/2),'right':(x+w,y+h/2)}[side]
def escape(p,side):
 x,y=p;return {'top':(x,y-45),'bottom':(x,y+45),'left':(x-45,y),'right':(x+45,y)}[side]
ports={
 ('AuthViewModel','AuthRepository'):('bottom','top'),('CivitaViewModel','ReportRepository'):('bottom','top'),('FirebaseReportRepository','ReportRepository'):('top','bottom'),('CommunityViewModel','CommunityActions'):('bottom','top'),('CommunityViewModel','CommunityRepository'):('right','right'),('MainActivity','ComponentActivity'):('bottom','top'),
 ('AuthViewModel','AuthUiState'):('left','left'),('CivitaViewModel','CivitaUiState'):('right','left'),('CommunityViewModel','CommunityUiState'):('right','left'),('AuthViewModel','AuthValidation'):('left','left'),
 ('AppContainer','AuthRepository'):('left','left'),('AppContainer','ReportRepository'):('right','left'),('AppContainer','CommunityRepository'):('right','left'),('FirebaseReportRepository','ReportDraft'):('bottom','top'),('CivitaUiState','UrbanReport'):('right','left'),('CivitaUiState','IncidentCategory'):('right','left'),('CivitaUiState','AppConfig'):('left','left'),
 ('IncidentCategory','UrbanReport'):('right','right'),('UserProfile','UrbanReport'):('bottom','top'),('UrbanReport','ReportStatus'):('bottom','top'),('CommunityUiState','UserProfile'):('right','left'),('CommunityUiState','UserPreferences'):('left','left'),('CommunityUiState','ReportComment'):('right','right'),('FirebaseReportRepository','GeoHash'):('left','left'),('UrbanReport','ReportComment'):('left','right'),('UserProfile','ReportComment'):('left','right'),('UserProfile','UserPreferences'):('left','right')}
# Malla de visibilidad ortogonal: sólo corredores que no cortan interiores de clases.
xv={25.,7455.};yv={205.,3650.,3720.};rects=list(box.values())
for x,y,w,h in rects:
 for gap in [25,45,70]:xv|={float(x-gap),float(x+w+gap)};yv|={float(y-gap),float(y+h+gap)}
for rel in relations:
 if rel['to']=='ViewModel':continue
 ss,ts=ports[(rel['from'],rel['to'])]
 for n,side in [(rel['from'],ss),(rel['to'],ts)]:
  p=escape(at(n,side),side);xv.add(p[0]);yv.add(p[1])
xs=sorted(x for x in xv if 15<=x<=7465);ys=sorted(y for y in yv if 195<=y<=3730)
valid={}
for ix,x in enumerate(xs):
 for iy,y in enumerate(ys):valid[ix,iy]=not any(xx<x<xx+w and yy<y<yy+h for xx,yy,w,h in rects)
def clear(a,b):
 ax,ay=a;bx,by=b
 if ay==by:return not any(y<ay<y+h and max(ax,bx)>x and min(ax,bx)<x+w for x,y,w,h in rects)
 return not any(x<ax<x+w and max(ay,by)>y and min(ay,by)<y+h for x,y,w,h in rects)
adj={}
for ix in range(len(xs)):
 for iy in range(len(ys)):
  if not valid[ix,iy]:continue
  a=(xs[ix],ys[iy]);v=[]
  for jx,jy,direction in [(ix-1,iy,1),(ix+1,iy,1),(ix,iy-1,2),(ix,iy+1,2)]:
   if (jx,jy) in valid and valid[jx,jy] and clear(a,(xs[jx],ys[jy])):v.append(((jx,jy),direction,abs(xs[jx]-a[0])+abs(ys[jy]-a[1])))
  adj[ix,iy]=v
used={}
def route(start,end):
 s=(xs.index(start[0]),ys.index(start[1]));t=(xs.index(end[0]),ys.index(end[1]));initial=(s,0);g={initial:0};parent={};queue=[(0,0,s,0)]
 while queue:
  _,cost,node,d=heapq.heappop(queue);state=(node,d)
  if cost!=g.get(state):continue
  if node==t:
   path=[node]
   while state in parent:state=parent[state];path.append(state[0])
   path.reverse()
   for a,b in zip(path,path[1:]):k=tuple(sorted((a,b)));used[k]=used.get(k,0)+1
   points=[(xs[ix],ys[iy]) for ix,iy in path];simple=[]
   for p in points:
    if len(simple)>1 and ((simple[-2][0]==simple[-1][0]==p[0]) or (simple[-2][1]==simple[-1][1]==p[1])):simple[-1]=p
    else:simple.append(p)
   return simple
  for nxt,nd,dist in adj.get(node,[]):
   key=tuple(sorted((node,nxt)));newcost=cost+dist+(80 if d and d!=nd else 0)+used.get(key,0)*360;ns=(nxt,nd)
   if newcost<g.get(ns,float('inf')):
    g[ns]=newcost;parent[ns]=((node,d));heur=abs(xs[nxt[0]]-end[0])+abs(ys[nxt[1]]-end[1]);heapq.heappush(queue,(newcost+heur,newcost,nxt,nd))
 raise RuntimeError('No existe corredor libre')
layout=[]
def draw(rel,points,ss=None,ts=None):
 kind=rel['type'];color='#227f70' if kind=='id' else '#365770'
 el('polyline',{'points':' '.join(f'{x},{y}' for x,y in points),'fill':'none','stroke':color,'stroke-width':2.6,'stroke-linejoin':'round',**({'stroke-dasharray':'13 10'} if kind in ['use','realize'] else {}),**({'marker-end':'url(#triangle)'} if kind in ['inherit','realize'] else {'marker-end':'url(#ref)'} if kind in ['ref','use'] else {})})
 if rel['multiplicity']:
  for p,side,m in [(points[0],ss,rel['multiplicity'][0]),(points[-1],ts,rel['multiplicity'][1])]:
   x,y=p;text(x-80 if side=='left' else x+15,y-18 if side!='bottom' else y+30,m,21,False,color)
 role=rel['role']
 if role and role!='state.value':
  # Etiqueta sobre un tramo suficientemente largo, con rotación sólo del rótulo.
  need=len(role)*11+40
  candidates=[(abs(b[0]-a[0])+abs(b[1]-a[1]),a,b) for a,b in zip(points,points[1:]) if abs(b[0]-a[0])+abs(b[1]-a[1])>need]
  if candidates:
   _,a,b=max(candidates);x=(a[0]+b[0])/2;y=(a[1]+b[1])/2
   if a[1]==b[1]:text(x-len(role)*5.4,y-14,role,21,False,color)
   else:
    t=el('text',{'transform':f'translate({x-12},{y+len(role)*5.4}) rotate(-90)','font-family':'DejaVu Sans','font-size':21,'fill':color});t.text=role
 layout.append({**rel,'points':points})
# Primero se trazan las cadenas directas; las relaciones largas usan corredores libres.
for rel in sorted(relations,key=lambda r:0 if r['type'] in ['inherit','realize'] else 1):
 if rel['to']=='ViewModel':
  p=at(rel['from'],'top');q=at('ViewModel','bottom');draw(rel,[p,(p[0],445),(q[0],445),q]);continue
 ss,ts=ports[(rel['from'],rel['to'])];p=at(rel['from'],ss);q=at(rel['to'],ts);mid=route(escape(p,ss),escape(q,ts));draw(rel,[p]+mid+[q],ss,ts)
for a,(x,y,w,h) in box.items():
 for b,(xx,yy,ww,hh) in box.items():
  if a<b:assert x+w<=xx or xx+ww<=x or y+h<=yy or yy+hh<=y,(a,b)
text(100,3900,'RELACIONES Y ARQUITECTURA',32,True)
notes=['Triángulo hueco continuo: generalización (ViewModels → ViewModel y MainActivity → ComponentActivity).',
'Triángulo hueco discontinuo: realización (FirebaseReportRepository → ReportRepository; CommunityViewModel → CommunityActions).',
'Flecha abierta continua: referencia navegable; state.value es la referencia derivada de StateFlow. Flecha abierta discontinua: dependencia.',
'Línea verde: asociación lógica por ID o ruta Firestore. 1 = uno; 0..1 = opcional; 0..* = varios. Las dependencias no llevan cardinalidad.',
'+ público; - privado; # protegido; ? nullable; {readOnly} propiedad val; {suspend} operación coroutine. Entidades Kotlin sin herencia entre sí.',
'Una APK / módulo :app: monolito en despliegue del cliente. MVVM y Repository describen su organización interna; Firebase es un backend externo BaaS.',
'Clases principales: los tipos de detalle y fábricas están en el catálogo. No se afirman composición exclusiva ni eliminación automática en cascada.']
for i,n in enumerate(notes):text(100,3960+i*53,n,27)
text(100,4480,'Fuente: implementación Kotlin CIVITA 1.1.742 · UML 2.5.1 · Una sola página A0 horizontal · PDF y SVG vectoriales ampliables',27,True)
E.ElementTree(r).write(O/'CIVITA-Clases.svg',encoding='utf-8',xml_declaration=True)
(O/'CAJAS.json').write_text(json.dumps(box,indent=2)+'\n');(O/'TRAZADO-CLASES.json').write_text(json.dumps(layout,ensure_ascii=False,indent=2)+'\n')
print('Clases horizontales:',len(box),'clasificadores;',len(layout),'relaciones')
