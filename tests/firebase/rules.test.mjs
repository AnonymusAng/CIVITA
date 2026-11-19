import { readFile } from 'node:fs/promises';
import { before, after, beforeEach, test } from 'node:test';
import assert from 'node:assert/strict';
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing';
import { doc, setDoc, getDoc, updateDoc, deleteDoc, serverTimestamp, collection, query, where, orderBy, getDocs, runTransaction } from 'firebase/firestore';

let env;
const uid = 'ciudadano-a';
const valid = () => ({creatorId: uid, categoryId: 'movilidad', categoryName: 'Movilidad', categoryColor: '#1976D2',
  title: 'Semáforo averiado', description: 'El semáforo no cambia en la intersección.',
  locationReference: 'Carrera 6 con calle 8', severity: 2, status: 'review', confidence: 0,
  auditState: 'pending',
  createdAt: serverTimestamp(), updatedAt: serverTimestamp()});
const db = () => env.authenticatedContext(uid).firestore();
const visible = () => ({...valid(), auditState: 'visible', auditSummary: 'Contenido claro para mostrar; hecho no verificado.'});
async function seedReport(id, data=visible()) {
  await env.withSecurityRulesDisabled(async c => setDoc(doc(c.firestore(), `reports/${id}`), {
    ...data, createdAt: new Date(), updatedAt: new Date(),
  }));
}

before(async () => {
  env = await initializeTestEnvironment({projectId: 'demo-civita', firestore: {
    host: '127.0.0.1', port: 8080, rules: await readFile(new URL('../../firestore.rules', import.meta.url), 'utf8'),
  }});
});
after(async () => { await env?.cleanup(); });
beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async c => {
    await setDoc(doc(c.firestore(), 'categories/movilidad'), {active: true, name: 'Movilidad', color: '#1976D2', defaultDurationHours: 4});
    await setDoc(doc(c.firestore(), 'categories/inactiva'), {active: false, name: 'Inactiva', color: '#000000', defaultDurationHours: 4});
    await setDoc(doc(c.firestore(), 'appConfig/public'), {allowedMediaTypes: ['image/jpeg','image/png','video/mp4'], maxEvidenceBytes: 10485760, maxEvidenceCount: 3, minRadiusMeters: 250, maxRadiusMeters: 15000});
  });
});

test('permite leer configuración y categorías a una sesión autenticada', async () => {
  await assertSucceeds(getDoc(doc(db(), 'appConfig/public')));
  await assertSucceeds(getDoc(doc(db(), 'categories/movilidad')));
});
test('rechaza lectura y escritura sin autenticación', async () => {
  const guest = env.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(guest, 'appConfig/public')));
  await assertFails(setDoc(doc(guest, 'reports/r1'), valid()));
});
test('crea reporte y recupera contenido persistido y timestamps', async () => {
  await assertSucceeds(setDoc(doc(db(), 'reports/r1'), valid()));
  const snapshot = await assertSucceeds(getDoc(doc(db(), 'reports/r1')));
  assert.equal(snapshot.data().creatorId, uid);
  assert.equal(snapshot.data().severity, 2);
  assert.equal(typeof snapshot.data().createdAt.toMillis(), 'number');
});
test('consulta ordenada por createdAt devuelve los reportes', async () => {
  await seedReport('visible');
  const result = await assertSucceeds(getDocs(query(collection(db(), 'reports'), where('auditState','==','visible'), orderBy('createdAt', 'desc'))));
  assert.equal(result.size, 1);
});
test('transacción idempotente no duplica ni actualiza el reporte al reintentar', async () => {
  const firestore = db(); const ref = doc(firestore, 'reports/r1');
  const publish = () => runTransaction(firestore, async tx => {
    const existing = await tx.get(ref);
    if (!existing.exists()) tx.set(ref, valid());
    return ref.id;
  });
  assert.equal(await assertSucceeds(publish()), 'r1');
  const first = (await getDoc(ref)).data().createdAt.toMillis();
  assert.equal(await assertSucceeds(publish()), 'r1');
  assert.equal((await getDoc(ref)).data().createdAt.toMillis(), first);
  assert.equal((await getDoc(ref)).exists(), true);
});
test('rechaza suplantación del autor', async () => {
  await assertFails(setDoc(doc(db(), 'reports/r1'), {...valid(), creatorId: 'otra-persona'}));
});
test('rechaza categorías ausentes, inactivas y nombres falsificados', async () => {
  await assertFails(setDoc(doc(db(), 'reports/r1'), {...valid(), categoryId: 'ausente'}));
  await assertFails(setDoc(doc(db(), 'reports/r2'), {...valid(), categoryId: 'inactiva', categoryName: 'Inactiva', categoryColor: '#000000'}));
  await assertFails(setDoc(doc(db(), 'reports/r3'), {...valid(), categoryName: 'Inventada'}));
});
test('rechaza gravedad inválida y números no enteros', async () => {
  for (const severity of [0, 4, 1.5]) await assertFails(setDoc(doc(db(), 'reports/r'+severity), {...valid(), severity}));
});
test('rechaza textos inválidos, campos extra y timestamps del cliente', async () => {
  await assertFails(setDoc(doc(db(), 'reports/r1'), {...valid(), title: ''}));
  await assertFails(setDoc(doc(db(), 'reports/blank'), {...valid(), title: '     '}));
  await assertFails(setDoc(doc(db(), 'reports/r2'), {...valid(), description: 'a'.repeat(2001)}));
  await assertFails(setDoc(doc(db(), 'reports/r3'), {...valid(), administrator: true}));
  await assertFails(setDoc(doc(db(), 'reports/r4'), {...valid(), createdAt: new Date(0)}));
});
test('no permite inventar confianza ni verificar un reporte al crearlo', async () => {
  await assertFails(setDoc(doc(db(), 'reports/r1'), {...valid(), confidence: 100}));
  await assertFails(setDoc(doc(db(), 'reports/r2'), {...valid(), status: 'verified'}));
  await assertFails(setDoc(doc(db(), 'reports/r3'), {...valid(), auditState: 'visible'}));
});
test('un reporte pendiente sólo lo puede leer su autor o moderación', async () => {
  await assertSucceeds(setDoc(doc(db(), 'reports/pendiente'), valid()));
  await assertSucceeds(getDoc(doc(db(), 'reports/pendiente')));
  await assertFails(getDoc(doc(userDb('otro'), 'reports/pendiente')));
});
test('rechaza actualizaciones y borrado desde el cliente', async () => {
  await assertSucceeds(setDoc(doc(db(), 'reports/r1'), valid()));
  await assertFails(updateDoc(doc(db(), 'reports/r1'), {confidence: 100}));
  await assertFails(deleteDoc(doc(db(), 'reports/r1')));
});
test('protege configuración, categorías y colecciones no habilitadas', async () => {
  await assertFails(setDoc(doc(db(), 'appConfig/public'), {maxEvidenceCount: 1000}));
  await assertFails(setDoc(doc(db(), 'categories/nueva'), {active: true}));
  await assertFails(setDoc(doc(db(), 'users/admin'), {role: 'admin'}));
});

const userDb = (id=uid, role='citizen') => env.authenticatedContext(id,{email:`${id}@example.invalid`,role,firebase:{sign_in_provider:'password'}}).firestore();
const profile = (id=uid) => ({fullName:'Ciudadano de prueba',email:`${id}@example.invalid`,role:'citizen',status:'active',trustLevel:1,createdAt:serverTimestamp()});

test('crea perfil privado y prohíbe escalar rol o confianza', async()=>{
  const d=userDb();await assertSucceeds(setDoc(doc(d,`users/${uid}`),profile()));
  await assertSucceeds(updateDoc(doc(d,`users/${uid}`),{fullName:'Nombre editado'}));
  await assertFails(updateDoc(doc(d,`users/${uid}`),{role:'admin'}));
  await assertFails(updateDoc(doc(d,`users/${uid}`),{trustLevel:100}));
  await assertFails(getDoc(doc(userDb('otro'),`users/${uid}`)));
  await assertFails(setDoc(doc(userDb('otro'),'users/otro'),{...profile('otro'),role:'moderator'}));
});
test('preferencias privadas validan radio y categorías', async()=>{
  const d=userDb();const r=doc(d,`users/${uid}/preferences/settings`);
  const p={alertRadiusMeters:1500,categoryIds:['movilidad'],pushEnabled:false,preciseLocationAllowed:false};
  await assertSucceeds(setDoc(r,p));await assertFails(setDoc(r,{...p,alertRadiusMeters:0}));
  await assertFails(setDoc(r,{...p,categoryIds:['inventada']}));
  await assertFails(getDoc(doc(userDb('otro'),`users/${uid}/preferences/settings`)));
});
test('suscripciones requieren propietario, categoría activa y GeoPoint', async()=>{
  const {GeoPoint}=await import('firebase/firestore');const d=userDb();const path=`users/${uid}/alertSubscriptions/movilidad`;
  const s={categoryId:'movilidad',center:new GeoPoint(2.44,-76.61),radiusMeters:1500,active:true};
  await assertSucceeds(setDoc(doc(d,path),s));await assertFails(setDoc(doc(userDb('otro'),path),s));
  await assertFails(setDoc(doc(d,path),{...s,radiusMeters:15001}));
});
test('comentarios requieren autor real y no permiten suplantar nombre',async()=>{
  const d=userDb();await setDoc(doc(d,`users/${uid}`),profile());await seedReport('r1');
  const c={authorId:uid,authorName:'Ciudadano de prueba',body:'Información adicional útil.',hidden:false,createdAt:serverTimestamp()};
  await assertSucceeds(setDoc(doc(d,'reports/r1/comments/c1'),c));
  await assertFails(setDoc(doc(d,'reports/r1/comments/c2'),{...c,authorName:'Otra persona'}));
  await assertFails(updateDoc(doc(d,'reports/r1/comments/c1'),{hidden:true}));
  await assertSucceeds(updateDoc(doc(userDb('moderador','moderator'),'reports/r1/comments/c1'),{hidden:true}));
});
test('validación única por UID sin autovalidación ni pesos arbitrarios',async()=>{
  const d=userDb();await seedReport('r1');const other=userDb('otro');
  const v={value:'confirm',weight:1,createdAt:serverTimestamp()};
  await assertFails(setDoc(doc(d,`reports/r1/validations/${uid}`),v));
  await assertSucceeds(setDoc(doc(other,'reports/r1/validations/otro'),v));
  await assertSucceeds(setDoc(doc(other,'reports/r1/validations/otro'),{...v,value:'deny'}));
  assert.equal((await getDocs(collection(other,'reports/r1/validations'))).size,1);
  await assertFails(setDoc(doc(other,'reports/r1/validations/otro'),{...v,weight:100}));
  await assertFails(setDoc(doc(other,'reports/r1/validations/tercero'),v));
});
test('apoyo por UID evita duplicados y permite retirarlo',async()=>{
  const d=userDb();await seedReport('r1');const r=doc(d,`reports/r1/supports/${uid}`);
  await assertSucceeds(setDoc(r,{createdAt:serverTimestamp()}));await assertFails(setDoc(r,{createdAt:serverTimestamp()}));
  await assertSucceeds(deleteDoc(r));await assertFails(setDoc(doc(d,'reports/ausente/supports/'+uid),{createdAt:serverTimestamp()}));
});
test('denuncias son privadas para autor y moderadores',async()=>{
  const d=userDb();await seedReport('r1');const path=`reports/r1/abuseReports/${uid}`;
  await assertSucceeds(setDoc(doc(d,path),{reason:'inappropriate',details:'Contenido que debe revisarse.',createdAt:serverTimestamp()}));
  await assertFails(getDoc(doc(userDb('otro'),path)));await assertSucceeds(getDoc(doc(userDb('mod','moderator'),path)));
});
test('historial inicial se crea junto al reporte; no se falsifica después',async()=>{
  const {writeBatch}=await import('firebase/firestore');const d=userDb();const b=writeBatch(d);
  b.set(doc(d,'reports/r1'),valid());b.set(doc(d,'reports/r1/history/created'),{changedBy:uid,fromStatus:'none',toStatus:'review',reason:'Publicación inicial del reporte',createdAt:serverTimestamp()});
  await assertSucceeds(b.commit());await assertFails(setDoc(doc(d,'reports/r1/history/falso'),{changedBy:uid,fromStatus:'review',toStatus:'verified',reason:'Cambio inventado por el autor',createdAt:serverTimestamp()}));
});
test('moderación exige claim e historial y acción atómicos; notificación privada',async()=>{
  const {writeBatch}=await import('firebase/firestore');const d=userDb();await seedReport('r1');
  const mod=userDb('moderador','moderator');
  await assertFails(updateDoc(doc(mod,'reports/r1'),{status:'verified',lastActionId:'a1',updatedAt:serverTimestamp()}));
  const b=writeBatch(mod);const reason='La información ha sido comprobada.';
  b.update(doc(mod,'reports/r1'),{status:'verified',lastActionId:'a1',updatedAt:serverTimestamp()});
  b.set(doc(mod,'reports/r1/history/a1'),{changedBy:'moderador',fromStatus:'review',toStatus:'verified',reason,createdAt:serverTimestamp()});
  b.set(doc(mod,'moderationActions/a1'),{moderatorId:'moderador',reportId:'r1',action:'change_status',reason,createdAt:serverTimestamp()});
  b.set(doc(mod,`users/${uid}/notifications/a1`),{type:'report_status',title:'Tu reporte cambió de estado',body:reason,sourceId:'r1',readAt:null,createdAt:serverTimestamp(),actionId:'a1'});
  await assertSucceeds(b.commit());const n=doc(d,`users/${uid}/notifications/a1`);
  await assertSucceeds(updateDoc(n,{readAt:serverTimestamp()}));await assertFails(updateDoc(n,{body:'Inventado'}));
  await assertFails(getDoc(doc(userDb('otro'),`users/${uid}/notifications/a1`)));
});
test('moderación puede liberar a comunidad y registrar revisión humana en una transacción', async()=>{
  const {writeBatch}=await import('firebase/firestore');
  await assertSucceeds(setDoc(doc(db(),'reports/r1'),valid()));
  const mod=userDb('moderador','moderator');const batch=writeBatch(mod);const reviewId='review-a1';
  batch.update(doc(mod,'reports/r1'),{auditState:'visible',auditSummary:'Revisión humana: información contextual revisada.',auditReasonCode:'human_release',auditUpdatedAt:serverTimestamp(),auditReviewerId:'moderador',auditReviewId:reviewId});
  batch.set(doc(mod,`reportAuditReviews/${reviewId}`),{reportId:'r1',reviewerId:'moderador',action:'release',reason:'Se revisó el contexto y puede mostrarse.',createdAt:serverTimestamp()});
  await assertSucceeds(batch.commit());
  await assertSucceeds(getDoc(doc(db(),'reports/r1')));
});
test('eventos y capas se leen pero sólo administración escribe por API',async()=>{
  const d=userDb();await assertSucceeds(getDocs(collection(d,'events')));await assertSucceeds(getDocs(collection(d,'mapLayers')));
  await assertFails(setDoc(doc(d,'events/falso'),{title:'Evento inventado'}));await assertFails(setDoc(doc(d,'mapLayers/falsa'),{active:true}));
});
