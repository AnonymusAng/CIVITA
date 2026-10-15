#!/usr/bin/env python3
"""Inicialización Firestore con la cuenta del usuario de Google Cloud Shell.
Solo biblioteca estándar. No almacena ni imprime tokens de Google.
"""
import argparse
import base64
import copy
import datetime as dt
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

PROJECT = "civita-5cc42"
PACKAGE = "co.edu.uniautonoma.civita"
ROOT = Path(__file__).resolve().parent
SHA = {
    "SHA_1": "CFB01B9059579B7B91F12189137AC04E078873EC",
    "SHA_256": "9D15DACB34A16615AF6973F7675CA9D3DD45D66BEE5AFDC38B3D067B7D75DCAD",
}

class SetupError(Exception):
    pass

class ApiError(SetupError):
    def __init__(self, code, message):
        self.code = code
        super().__init__(f"HTTP {code}: {message}")

class GoogleApi:
    def request(self, method, url, body=None, missing_ok=False):
        # Capturado en memoria; nunca se escribe el token en archivos o registros.
        result = subprocess.run(["gcloud", "auth", "print-access-token", "--quiet"],
                                capture_output=True, text=True, check=False)
        token = result.stdout.strip()
        if result.returncode or not token:
            raise SetupError("No hay una sesión de Google Cloud activa. En Cloud Shell inicia sesión con la cuenta que administra CIVITA.")
        data = json.dumps(body).encode() if body is not None else None
        headers = {"Authorization": "Bearer " + token, "Content-Type": "application/json", "x-goog-user-project": PROJECT}
        request = urllib.request.Request(url, data=data, headers=headers, method=method)
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                content = response.read()
                return json.loads(content) if content else {}
        except urllib.error.HTTPError as error:
            if missing_ok and error.code == 404:
                return None
            try:
                message = json.loads(error.read()).get("error", {}).get("message", "Solicitud rechazada.")
            except (ValueError, AttributeError):
                message = "Solicitud rechazada por el servicio o el proxy."
            raise ApiError(error.code, message) from None
        except (urllib.error.URLError, TimeoutError):
            raise SetupError("No se pudo contactar con Google Cloud. Comprueba conexión y permisos de red.") from None

    def wait(self, url, operation):
        for _ in range(60):
            if operation.get("done"):
                if operation.get("error"):
                    raise SetupError("Google no pudo completar la creación de la base de datos.")
                return operation.get("response", {})
            time.sleep(2)
            operation = self.request("GET", url)
        raise SetupError("La creación sigue en curso. Vuelve a ejecutar el inicializador para consultar su estado.")


def encode(value):
    if type(value) is bool:
        return {"booleanValue": value}
    if type(value) is int:
        return {"integerValue": str(value)}
    if isinstance(value, str):
        return {"stringValue": value}
    if isinstance(value, list):
        return {"arrayValue": {"values": [encode(x) for x in value]}}
    raise SetupError("Tipo de dato no admitido en los valores iniciales.")


def decode(value):
    for key in ("booleanValue", "stringValue", "timestampValue"):
        if key in value:
            return value[key]
    if "integerValue" in value:
        return int(value["integerValue"])
    if "doubleValue" in value:
        return float(value["doubleValue"])
    if "arrayValue" in value:
        return [decode(x) for x in value["arrayValue"].get("values", [])]
    return None


def validate(path, fields):
    if path == "appConfig/public":
        media = fields.get("allowedMediaTypes")
        ok = isinstance(media, list) and all(isinstance(x, str) and x.strip() for x in media)
        ok = ok and all(type(fields.get(x)) is int for x in (
            "maxEvidenceBytes", "maxEvidenceCount", "minRadiusMeters", "maxRadiusMeters"))
        if ok:
            ok = (fields["maxEvidenceBytes"] > 0 and 0 <= fields["maxEvidenceCount"] <= 100
                  and fields["minRadiusMeters"] > 0 and fields["maxRadiusMeters"] >= fields["minRadiusMeters"])
    elif re.fullmatch(r"categories/[A-Za-z0-9_-]{1,80}", path):
        ok = (type(fields.get("active")) is bool
              and isinstance(fields.get("name"), str) and bool(fields["name"].strip())
              and isinstance(fields.get("color"), str) and bool(re.fullmatch(r"#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?", fields["color"]))
              and type(fields.get("defaultDurationHours")) is int and 1 <= fields["defaultDurationHours"] <= 168)
    else:
        raise SetupError("Ruta ajena al esquema CIVITA: " + path)
    if not ok:
        raise SetupError(f"Datos incompatibles en {path}. Revisa los tipos y límites en SCHEMA.md. No se han sobrescrito valores existentes.")


def plan_write(name, defaults, existing):
    old = {k: decode(v) for k, v in (existing or {}).get("fields", {}).items()}
    merged = {**defaults, **old}
    validate("/".join(name.split("/documents/")[1:]), merged)
    missing = {k: v for k, v in defaults.items() if k not in old}
    if not missing:
        return None
    write = {"update": {"name": name, "fields": {k: encode(v) for k, v in missing.items()}},
             "updateTransforms": [{"fieldPath": "updatedAt", "setToServerValue": "REQUEST_TIME"}]}
    if existing:
        if not existing.get("updateTime"):
            raise SetupError("El documento no tiene versión de actualización; no se modificará.")
        write["updateMask"] = {"fieldPaths": list(missing)}
        write["currentDocument"] = {"updateTime": existing["updateTime"]}
    else:
        write["currentDocument"] = {"exists": False}
    return write


def seed_documents(api, project, documents, apply):
    db = f"projects/{project}/databases/(default)"
    writes = []
    for path, defaults in documents.items():
        name = db + "/documents/" + path
        current = api.request("GET", "https://firestore.googleapis.com/v1/" + name, missing_ok=True)
        write = plan_write(name, defaults, current)
        print(("COMPLETAR " if current else "CREAR ") + path if write else "CONSERVAR " + path)
        if write:
            writes.append(write)
    if apply and writes:
        api.request("POST", "https://firestore.googleapis.com/v1/" + db + "/documents:commit", {"writes": writes})
    return len(writes)


def deploy_rules(api, project, content, output):
    root = "https://firebaserules.googleapis.com/v1/"
    release_name = f"projects/{project}/releases/cloud.firestore"
    previous = api.request("GET", root + release_name, missing_ok=True)
    if previous:
        backup = api.request("GET", root + previous["rulesetName"])
        target = output / ("reglas-anteriores-" + dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%S%fZ") + ".json")
        target.write_text(json.dumps({"release": previous, "ruleset": backup}, ensure_ascii=False, indent=2))
        target.chmod(0o600)
        print("Copia de reglas anteriores:", target)
    ruleset = api.request("POST", root + f"projects/{project}/rulesets",
                         {"source": {"files": [{"name": "firestore.rules", "content": content}]}})
    release = {"name": release_name, "rulesetName": ruleset["name"]}
    if previous:
        api.request("PATCH", root + release_name, {"release": release})
    else:
        api.request("POST", root + f"projects/{project}/releases", release)
    installed = api.request("GET", root + release_name)
    if installed.get("rulesetName") != ruleset["name"]:
        raise SetupError("No se confirmó la publicación de las reglas.")
    print("Reglas Firestore publicadas y confirmadas.")


def configure_android(api, project, output):
    root = "https://firebase.googleapis.com/v1beta1/"
    apps = api.request("GET", root + f"projects/{project}/androidApps").get("apps", [])
    apps = [app for app in apps if app.get("packageName") == PACKAGE and app.get("state", "ACTIVE") == "ACTIVE"]
    if len(apps) != 1:
        raise SetupError("Debe existir una aplicación Android activa con paquete " + PACKAGE)
    name = apps[0]["name"]
    known = api.request("GET", root + name + "/sha").get("certificates", [])
    known = {(x.get("certType"), x.get("shaHash", "").replace(":", "").upper()) for x in known}
    for kind, value in SHA.items():
        if (kind, value) not in known:
            api.request("POST", root + name + "/sha", {"certType": kind, "shaHash": value})
    data = api.request("GET", root + name + "/config")
    config = json.loads(base64.b64decode(data["configFileContents"]))
    if config.get("project_info", {}).get("project_id") != project:
        raise SetupError("Firebase devolvió configuración de otro proyecto; no se guardó.")
    target = output / "google-services.json"
    target.write_text(json.dumps(config, indent=2))
    target.chmod(0o600)
    has_web = any(x.get("client_type") == 3 for c in config.get("client", []) for x in c.get("oauth_client", []))
    print("Configuración Android descargada:", target)
    print("Cliente OAuth web presente:", "sí" if has_web else "no; habilita Google en Authentication y vuelve a ejecutar")
    return has_web


def main(argv=None, api=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", default=PROJECT)
    parser.add_argument("--apply", action="store_true", help="Crea o completa documentos y publica reglas")
    parser.add_argument("--enable-apis", action="store_true", help="Opcional: habilita APIs; requiere serviceusage.services.enable")
    parser.add_argument("--enable-email", action="store_true", help="Habilita acceso con correo/contraseña")
    parser.add_argument("--configure-android", action="store_true", help="Registra huellas del APK entregado y descarga JSON")
    parser.add_argument("--check-local", action="store_true", help="Valida archivos sin Google ni red")
    parser.add_argument("--output", type=Path, default=Path.cwd() / "civita-resultado")
    args = parser.parse_args(argv)
    if args.project != PROJECT:
        raise SetupError("Este inicializador está limitado al proyecto " + PROJECT)
    spec = json.loads((ROOT / "seed.json").read_text())
    if spec["project"] != args.project or spec["database"] != "(default)":
        raise SetupError("El proyecto o la base no coinciden con el esquema revisado.")
    for path, fields in spec["documents"].items():
        validate(path, fields)
    rules = (ROOT / "firestore.rules").read_text()
    if not rules.strip():
        raise SetupError("No se encontró el archivo de reglas.")
    if args.check_local:
        print("Esquema válido: configuración pública y cinco categorías. Sin acceso a red ni cambios remotos.")
        return
    if not args.apply and (args.enable_email or args.configure_android or args.enable_apis):
        raise SetupError("Las opciones de configuración requieren --apply.")
    api = api or GoogleApi()
    if args.enable_apis:
        result = subprocess.run(["gcloud", "services", "enable", "firestore.googleapis.com",
                                 "firebaserules.googleapis.com", "firebase.googleapis.com", "identitytoolkit.googleapis.com",
                                 "--project=" + args.project, "--quiet"], check=False)
        if result.returncode:
            raise SetupError("No se pudieron habilitar las APIs. Si ya están habilitadas, repite sin --enable-apis; de lo contrario, el administrador necesita habilitarlas.")
    db_name = f"projects/{args.project}/databases/(default)"
    db_url = "https://firestore.googleapis.com/v1/" + db_name
    database = api.request("GET", db_url, missing_ok=True)
    if database is None:
        if not args.apply:
            raise SetupError("La base (default) aún no existe. --apply permite crearla en southamerica-west1.")
        op = api.request("POST", f"https://firestore.googleapis.com/v1/projects/{args.project}/databases?databaseId=%28default%29",
                         {"locationId": "southamerica-west1", "type": "FIRESTORE_NATIVE", "deleteProtectionState": "DELETE_PROTECTION_ENABLED"})
        api.wait("https://firestore.googleapis.com/v1/" + op["name"], op)
    elif database.get("type") != "FIRESTORE_NATIVE":
        raise SetupError("La base existente no es Firestore Native. No se modificará ni se recreará.")
    print("Proyecto:", args.project, "· Base: (default)")
    count = seed_documents(api, args.project, spec["documents"], args.apply)
    if not args.apply:
        print("Solo inspección. Documentos a crear o completar:", count)
        return
    args.output.mkdir(parents=True, exist_ok=True)
    deploy_rules(api, args.project, rules, args.output)
    if args.enable_email:
        api.request("PATCH", f"https://identitytoolkit.googleapis.com/admin/v2/projects/{args.project}/config?updateMask=signIn.email.enabled,signIn.email.passwordRequired",
                    {"signIn": {"email": {"enabled": True, "passwordRequired": True}}})
        config = api.request("GET", f"https://identitytoolkit.googleapis.com/admin/v2/projects/{args.project}/config")
        if not config.get("signIn", {}).get("email", {}).get("enabled"):
            raise SetupError("No se confirmó el acceso con correo. Habilítalo en Firebase Authentication.")
        print("Acceso con correo/contraseña habilitado.")
    google_ready = configure_android(api, args.project, args.output) if args.configure_android else None
    for path, defaults in spec["documents"].items():
        doc = api.request("GET", db_url + "/documents/" + path)
        validate(path, {k: decode(v) for k, v in doc.get("fields", {}).items()})
    api.request("POST", db_url + "/documents:runQuery", {"structuredQuery": {
        "from": [{"collectionId": "reports"}], "orderBy": [{"field": {"fieldPath": "createdAt"}, "direction": "DESCENDING"}], "limit": 1}})
    summary = {"project": args.project, "database": "(default)", "documentsConfirmed": list(spec["documents"]),
               "rulesConfirmed": True, "googleClientInConfig": google_ready, "androidClientLoginAndPublishVerified": False}
    (args.output / "resultado.json").write_text(json.dumps(summary, indent=2, ensure_ascii=False))
    print("Base y consulta de reportes verificadas con tu cuenta administrativa.")
    print("reports se crea con el primer reporte real de la app; no se insertaron incidentes ficticios.")
    print("Falta probar inicio de sesión y publicación desde Android. Resultado:", args.output / "resultado.json")

if __name__ == "__main__":
    try:
        main()
    except (SetupError, FileNotFoundError, ValueError) as error:
        print("No se completó la configuración:", str(error), file=sys.stderr)
        print("Si alguna etapa anterior terminó, sus cambios se conservan. Puedes repetir el comando; no borra documentos.", file=sys.stderr)
        sys.exit(1)
