import {readFile} from 'node:fs/promises';
import {before,after,test} from 'node:test';
import {initializeTestEnvironment,assertSucceeds,assertFails} from '@firebase/rules-unit-testing';
import {doc,setDoc} from 'firebase/firestore';
import {ref,uploadBytes} from 'firebase/storage';
let env;
before(async()=>{
 env=await initializeTestEnvironment({projectId:'demo-civita',firestore:{host:'127.0.0.1',port:8080,rules:await readFile(new URL('../../firestore.rules',import.meta.url),'utf8')},storage:{host:'127.0.0.1',port:9199,rules:await readFile(new URL('../../storage.rules',import.meta.url),'utf8')}});
 await env.withSecurityRulesDisabled(async c=>{await setDoc(doc(c.firestore(),'reports/storage-test'),{creatorId:'author',status:'review',evidenceCount:0});});
});
after(async()=>{await env?.cleanup();});
const metadata={contentType:'image/png',customMetadata:{uploadedBy:'author'}};
test('Storage acepta imagen del autor y rechaza terceros y visitantes',async()=>{
 const a=env.authenticatedContext('author',{firebase:{sign_in_provider:'password'}}).storage();
 await assertSucceeds(uploadBytes(ref(a,'reports/storage-test/evidence/one'),new Uint8Array([137,80,78,71]),metadata));
 await assertFails(uploadBytes(ref(env.authenticatedContext('other').storage(),'reports/storage-test/evidence/two'),new Uint8Array([1]),{...metadata,customMetadata:{uploadedBy:'other'}}));
 await assertFails(uploadBytes(ref(env.unauthenticatedContext().storage(),'reports/storage-test/evidence/three'),new Uint8Array([1]),metadata));
});
test('Storage rechaza MIME no admitido y archivos vacíos',async()=>{
 const a=env.authenticatedContext('author',{firebase:{sign_in_provider:'password'}}).storage();
 await assertFails(uploadBytes(ref(a,'reports/storage-test/evidence/html'),new Uint8Array([1]),{...metadata,contentType:'text/html'}));
 await assertFails(uploadBytes(ref(a,'reports/storage-test/evidence/empty'),new Uint8Array([]),metadata));
});
