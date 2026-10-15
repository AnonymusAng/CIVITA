#!/usr/bin/env python3
"""Prueba real del backend con Auth de cliente; nunca sustituye una prueba Android.

Ejecutar en Cloud Shell. Crea una cuenta temporal y un reporte explícitamente de
prueba; usa IAM sólo para diagnóstico/limpieza, nunca para aprobar escrituras de
cliente. No imprime contraseñas, claves API ni tokens.
"""
import argparse
import datetime as dt
import json
import secrets
import shutil
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid
from pathlib import Path

from initialize import GoogleApi, SetupError, PROJECT, PACKAGE, encode, decode

DATABASE = f"projects/{PROJECT}/databases/(default)"
DOCUMENTS = DATABASE + "/documents"
FIRESTORE = "https://firestore.googleapis.com/v1/"
AUTH = "https://identitytoolkit.googleapis.com/v1/accounts:"


class ClientApi:
    def request(self, method, url, body=None, token=None):
        headers = {"Content-Type": "application/json"}
        if token:
            headers["Authorization"] = "Bearer " + token
        request = urllib.request.Request(url, method=method, headers=headers,
            data=json.dumps(body).encode() if body is not None else None)
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                raw = response.read()
                return json.loads(raw) if raw else {}
        except urllib.error.HTTPError as error:
            # Nunca incluir URL (contiene API key), cuerpo ni tokens en el error.
            try:
                status = json.loads(error.read()).get("error", {}).get("status", "")
            except (ValueError, AttributeError):
                status = ""
            raise SetupError(f"Cliente {urllib.parse.urlsplit(url).hostname}: HTTP {error.code} {status}") from None
        except (urllib.error.URLError, TimeoutError):
            raise SetupError("El cliente no pudo contactar Firebase; revisar red/TLS.") from None


def client_key(path):
    config = json.loads(Path(path).read_text())
    if config.get("project_info", {}).get("project_id") != PROJECT:
        raise SetupError("El JSON no corresponde a CIVITA; no se realizará la prueba.")
    for client in config.get("client", []):
        if client.get("client_info", {}).get("android_client_info", {}).get("package_name") == PACKAGE:
            keys = client.get("api_key", [])
            if keys and keys[0].get("current_key"):
                return keys[0]["current_key"]
    raise SetupError("Falta la configuración cliente Android de CIVITA.")


def write(path, fields, timestamps=()):
    result = {"update": {"name": DOCUMENTS + "/" + path,
                         "fields": {key: encode(value) for key, value in fields.items()}},
              "currentDocument": {"exists": False}}
    if timestamps:
        result["updateTransforms"] = [{"fieldPath": field, "setToServerValue": "REQUEST_TIME"}
                                      for field in timestamps]
    return result


def checked(condition, message):
    if not condition:
        raise SetupError(message)


def run(client, admin, key, run_id):
    checked(len(run_id) == 32 and all(c in "0123456789abcdef" for c in run_id), "Identificador de prueba inválido.")
    email = f"civita-e2e-{run_id}@example.invalid"
    password = secrets.token_urlsafe(30)
    auth_url = lambda action: AUTH + action + "?key=" + urllib.parse.quote(key, safe="")
    report_id = "e2e_" + run_id
    title = "PRUEBA AUTOMATIZADA CIVITA " + run_id[:8]
    uid = None
    token = None
    result = {"project": PROJECT, "runId": run_id, "scope": "real Firebase client REST; Android UI not tested",
              "authentication": "not_run", "publish": "not_run", "query": "not_run", "cleanup": "not_run"}
    try:
        result["authentication"] = "running"
        account = client.request("POST", auth_url("signUp"),
                                 {"email": email, "password": password, "returnSecureToken": True})
        uid = account.get("localId")
        token = account.get("idToken")
        checked(bool(uid and token), "Firebase no devolvió una sesión de prueba.")
        login = client.request("POST", auth_url("signInWithPassword"),
                               {"email": email, "password": password, "returnSecureToken": True})
        checked(login.get("localId") == uid and bool(login.get("idToken")), "El inicio de sesión no conserva el UID.")
        token = login["idToken"]
        result["authentication"] = "passed"
        print("OK: registro temporal e inicio de sesión con Firebase Authentication.")
        get = lambda path: client.request("GET", FIRESTORE + DOCUMENTS + "/" + path, token=token)
        config = get("appConfig/public")
        checked("maxEvidenceCount" in config.get("fields", {}), "Falta configuración pública compatible.")
        category_doc = get("categories/movilidad")
        category = {k: decode(v) for k, v in category_doc.get("fields", {}).items()}
        checked(category.get("active") is True, "Movilidad está inactiva; no se alterará para probar.")
        profile = write(f"users/{uid}", {"fullName": "Prueba automática CIVITA", "email": email,
            "role": "citizen", "status": "active", "trustLevel": 1}, ["createdAt"])
        prefs = write(f"users/{uid}/preferences/settings", {"alertRadiusMeters": 1500,
            "categoryIds": [], "pushEnabled": False, "preciseLocationAllowed": False})
        client.request("POST", FIRESTORE + DATABASE + "/documents:commit", {"writes": [profile, prefs]}, token)
        checked(decode(get(f"users/{uid}")["fields"]["email"]) == email, "Perfil autenticado no recuperado.")
        # Misma estructura de escritura de FirebaseReportRepository; sin bypass IAM.
        report = write(f"reports/{report_id}", {"creatorId": uid, "categoryId": "movilidad",
            "categoryName": category["name"], "categoryColor": category["color"], "title": title,
            "description": "PRUEBA TÉCNICA TEMPORAL. No representa un incidente real. Se elimina al terminar.",
            "locationReference": "PRUEBA AUTOMATIZADA, sin ubicación real", "severity": 1,
            "status": "review", "confidence": 0, "geohash": "", "evidenceCount": 0}, ["createdAt", "updatedAt"])
        report["update"]["fields"]["location"] = {"nullValue": None}
        expires = dt.datetime.now(dt.timezone.utc) + dt.timedelta(hours=category["defaultDurationHours"])
        report["update"]["fields"]["expiresAt"] = {"timestampValue": expires.isoformat().replace("+00:00", "Z")}
        history = write(f"reports/{report_id}/history/created", {"changedBy": uid, "fromStatus": "none",
            "toStatus": "review", "reason": "Publicación inicial del reporte"}, ["createdAt"])
        client.request("POST", FIRESTORE + DATABASE + "/documents:commit", {"writes": [report, history]}, token)
        saved = get("reports/" + report_id)
        fields = saved.get("fields", {})
        checked(decode(fields.get("creatorId", {})) == uid and decode(fields.get("title", {})) == title
                and bool(fields.get("createdAt", {}).get("timestampValue")), "El reporte persistido no coincide.")
        checked(decode(get(f"reports/{report_id}/history/created")["fields"]["changedBy"]) == uid,
                "Falta el historial atómico de publicación.")
        result["publish"] = "passed"
        print("OK: funcionalidad 1, publicación persistida con autor y timestamp de servidor.")
        # Nuevo inicio de sesión y consulta usada por la app; no lectura desde caché.
        login = client.request("POST", auth_url("signInWithPassword"),
                               {"email": email, "password": password, "returnSecureToken": True})
        checked(login.get("localId") == uid and bool(login.get("idToken")), "No se pudo reiniciar sesión.")
        token = login["idToken"]
        rows = client.request("POST", FIRESTORE + DOCUMENTS + ":runQuery", {"structuredQuery": {
            "from": [{"collectionId": "reports"}], "orderBy": [{"field": {"fieldPath": "createdAt"},
            "direction": "DESCENDING"}], "limit": 100}}, token)
        checked(any(row.get("document", {}).get("name") == DOCUMENTS + "/reports/" + report_id for row in rows),
                "El reporte no aparece en la consulta de Comunidad.")
        checked(get("reports/" + report_id)["fields"] == fields, "Los datos cambiaron al consultar desde otra sesión.")
        result["query"] = "passed"
        print("OK: funcionalidad 2, consulta de Comunidad y detalle después de volver a iniciar sesión.")
    except (SetupError, KeyError, TypeError, ValueError) as error:
        if result["authentication"] == "running":
            result["authentication"] = "failed"
        result["error"] = str(error) if isinstance(error, SetupError) else "Respuesta de Firebase incompatible con el esquema esperado."
        print("FALLO:", result["error"])
    finally:
        cleanup_errors = []
        if uid:
            # Sólo se borran rutas de esta ejecución; no consultas globales ni borrado recursivo.
            for path in [f"reports/{report_id}/history/created", f"reports/{report_id}",
                         f"users/{uid}/preferences/settings", f"users/{uid}"]:
                try:
                    admin.request("DELETE", FIRESTORE + DOCUMENTS + "/" + path, missing_ok=True)
                except SetupError:
                    cleanup_errors.append(path)
            try:
                client.request("POST", auth_url("delete"), {"idToken": token})
            except SetupError:
                try:
                    admin.request("POST", f"https://identitytoolkit.googleapis.com/v1/projects/{PROJECT}/accounts:delete", {"localId": uid})
                except SetupError:
                    cleanup_errors.append("cuenta temporal")
        else:
            # Un timeout al registrar podría haber creado la cuenta sin devolver respuesta.
            try:
                found = admin.request("POST", f"https://identitytoolkit.googleapis.com/v1/projects/{PROJECT}/accounts:lookup", {"email": [email]})
                for user in found.get("users", []):
                    if user.get("email") == email:
                        admin.request("POST", f"https://identitytoolkit.googleapis.com/v1/projects/{PROJECT}/accounts:delete", {"localId": user["localId"]})
            except SetupError:
                cleanup_errors.append("comprobación de cuenta temporal")
        result["cleanup"] = "failed" if cleanup_errors else "passed"
        if cleanup_errors:
            result["cleanupPending"] = cleanup_errors
            result["temporaryEmail"] = email
            print("ATENCIÓN: limpieza pendiente; revisar únicamente las rutas de esta ejecución en el resultado.")
        else:
            print("OK: limpieza de los datos temporales de esta ejecución.")
    result["status"] = "passed" if all(result[k] == "passed" for k in ["authentication", "publish", "query", "cleanup"]) else "failed"
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--config", type=Path, default=Path.home()/"civita-resultado/google-services.json")
    parser.add_argument("--output", type=Path, default=Path.home()/"civita-resultado/prueba-sesion.json")
    parser.add_argument("--run", action="store_true", help="Autoriza cuenta/reporte temporales y su limpieza.")
    args = parser.parse_args()
    if not args.run:
        print("Sin escrituras. Añade --run para ejecutar la prueba real y limpiar sus datos temporales.")
        return 0
    try:
        if not shutil.which("gcloud"):
            raise SetupError("Ejecuta esta prueba en Google Cloud Shell con la cuenta que administra CIVITA; aquí falta gcloud.")
        key = client_key(args.config)
        admin = GoogleApi()
        admin.request("GET", f"https://cloudresourcemanager.googleapis.com/v1/projects/{PROJECT}")
        result = run(ClientApi(), admin, key, uuid.uuid4().hex)
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
        args.output.chmod(0o600)
        print("Resultado sin credenciales:", args.output)
        return 0 if result["status"] == "passed" else 1
    except (SetupError, OSError, ValueError) as error:
        print("No se ejecutó la prueba:", str(error) if isinstance(error, SetupError) else "Revisa el archivo JSON y la ruta de salida.")
        return 1


if __name__ == "__main__":
    sys.exit(main())
