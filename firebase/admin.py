#!/usr/bin/env python3
"""Carga eventos y puntos oficiales con la cuenta administrativa de Cloud Shell.
Nunca crea datos de ejemplo ni concede roles sin UID explícito.
"""
import argparse
import datetime as dt
import json
import re
from pathlib import Path
from initialize import GoogleApi, PROJECT, SetupError, encode


def checked_text(value, minimum, maximum):
    if not isinstance(value, str) or not minimum <= len(value.strip()) <= maximum:
        raise SetupError("Texto ausente o fuera de límites.")
    return value.strip()


def identifier(value):
    if not isinstance(value, str) or not re.fullmatch(r"[A-Za-z0-9_-]{1,80}", value):
        raise SetupError("Identificador inválido.")
    return value


def point(data):
    lat, lon = data.get('latitude'), data.get('longitude')
    if type(lat) not in (int, float) or type(lon) not in (int, float) or not -90 <= lat <= 90 or not -180 <= lon <= 180:
        raise SetupError("Coordenadas inválidas.")
    return {"geoPointValue": {"latitude": lat, "longitude": lon}}


def geohash(latitude, longitude, precision=9):
    point({'latitude':latitude,'longitude':longitude})
    if type(precision) is not int or not 1 <= precision <= 12:
        raise SetupError('Precisión geohash inválida.')
    alphabet='0123456789bcdefghjkmnpqrstuvwxyz'; lat=[-90.,90.]; lon=[-180.,180.]
    result=''; bits=0; value=0; even=True
    while len(result)<precision:
        bounds=lon if even else lat; coordinate=longitude if even else latitude
        middle=sum(bounds)/2; high=coordinate>=middle
        value=(value<<1)|int(high); bounds[0 if high else 1]=middle
        even=not even;bits+=1
        if bits==5:result+=alphabet[value];bits=0;value=0
    return result


def event_document(data):
    start = dt.datetime.fromisoformat(data['startsAt'].replace('Z','+00:00'))
    end = dt.datetime.fromisoformat(data['endsAt'].replace('Z','+00:00'))
    if not start.tzinfo or not end.tzinfo or end <= start:
        raise SetupError("El evento requiere fechas con zona horaria y fin posterior al inicio.")
    category = identifier(data['categoryId'])
    fields = {k: encode(checked_text(data[k], *limits)) for k,limits in {
        'title': (5,120), 'description': (10,2000), 'createdBy': (1,128)}.items()}
    if data.get('status') not in ('scheduled','cancelled','completed'):
        raise SetupError("Estado de evento inválido.")
    fields.update(categoryId=encode(category),status=encode(data['status']),location=point(data),
                  startsAt={"timestampValue":start.isoformat()},endsAt={"timestampValue":end.isoformat()},
                  geohash=encode(geohash(data["latitude"],data["longitude"])))
    return 'events/'+identifier(data['id']), fields


def require_auth_user(api, uid):
    result=api.request('POST',f'https://identitytoolkit.googleapis.com/v1/projects/{PROJECT}/accounts:lookup',{'localId':[uid]})
    users=result.get('users',[])
    if len(users)!=1 or users[0].get('localId')!=uid or users[0].get('disabled'):
        raise SetupError('El autor debe ser un UID activo de Firebase Authentication.')
    return users[0]


def feature_document(data):
    if not isinstance(data.get('properties',{}),dict):
        raise SetupError('properties debe ser un objeto JSON.')
    return 'mapLayers/'+identifier(data['layerId'])+'/features/'+identifier(data['id']), {
        'name':encode(checked_text(data['name'],2,100)), 'location':point(data),
        'propertiesJson':encode(json.dumps(data.get('properties',{}),ensure_ascii=False))}


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    group=parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--event',type=Path)
    group.add_argument('--feature',type=Path)
    group.add_argument('--moderator-uid',help='UID existente de Firebase Authentication; no es un correo')
    parser.add_argument('--apply',action='store_true')
    args=parser.parse_args(); api=GoogleApi()
    if args.moderator_uid:
        uid=checked_text(args.moderator_uid,1,128)
        if not args.apply:
            print('Plan: asignar claim role=moderator al UID explícito. Requiere --apply.');return
        root=f'https://identitytoolkit.googleapis.com/v1/projects/{PROJECT}/accounts:'
        result=api.request('POST',root+'lookup',{'localId':[uid]})
        users=result.get('users',[])
        if len(users)!=1: raise SetupError('UID no encontrado en Authentication.')
        claims=json.loads(users[0].get('customAttributes','{}'))
        claims['role']='moderator'
        api.request('POST',root+'update',{'localId':uid,'customAttributes':json.dumps(claims)})
        print('Claim de moderador asignado. Cierra sesión y vuelve a entrar para renovar el token.');return
    path, fields = event_document(json.loads(args.event.read_text())) if args.event else feature_document(json.loads(args.feature.read_text()))
    print('Crear documento oficial:',path)
    if not args.apply:
        print('Sólo validación. Añade --apply para crear.');return
    base=f'projects/{PROJECT}/databases/(default)/documents/'
    if args.event:
        require_auth_user(api,fields['createdBy']['stringValue'])
        category=api.request('GET','https://firestore.googleapis.com/v1/'+base+'categories/'+fields['categoryId']['stringValue'])
        if category.get('fields',{}).get('active',{}).get('booleanValue') is not True:
            raise SetupError('La categoría no está activa.')
    else:
        api.request('GET','https://firestore.googleapis.com/v1/'+base+'mapLayers/'+identifier(json.loads(args.feature.read_text())['layerId']))
    # Sólo creación: no sobrescribe un evento/punto oficial existente.
    api.request('POST',f'https://firestore.googleapis.com/v1/projects/{PROJECT}/databases/(default)/documents:commit',{'writes':[{
        'update':{'name':base+path,'fields':fields},'currentDocument':{'exists':False},
        'updateTransforms':[{'fieldPath':'createdAt','setToServerValue':'REQUEST_TIME'},{'fieldPath':'updatedAt','setToServerValue':'REQUEST_TIME'}]}]})
    print('Documento creado y confirmado por Firestore.')

if __name__=='__main__':
    try:main()
    except (SetupError,ValueError,KeyError,FileNotFoundError) as error:
        raise SystemExit('No se completó la operación: '+str(error))
