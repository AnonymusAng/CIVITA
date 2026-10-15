import base64
import copy
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location("initialize", Path(__file__).resolve().parents[1] / "initialize.py")
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)
SEED = json.loads((m.ROOT / "seed.json").read_text())["documents"]
NAME = f"projects/{m.PROJECT}/databases/(default)/documents/categories/movilidad"

class FakeApi:
    def __init__(self, handler):
        self.handler = handler
        self.calls = []
    def request(self, method, url, body=None, missing_ok=False):
        self.calls.append((method, url, body))
        return self.handler(method, url, body, missing_ok)

class InitializeTest(unittest.TestCase):
    def test_boolean_and_integer_encode_distinct_firestore_types(self):
        self.assertEqual({"booleanValue": True}, m.encode(True))
        self.assertEqual({"integerValue": "3"}, m.encode(3))

    def test_missing_document_uses_create_precondition_and_server_timestamp(self):
        write = m.plan_write(NAME, SEED["categories/movilidad"], None)
        self.assertEqual({"exists": False}, write["currentDocument"])
        self.assertEqual("REQUEST_TIME", write["updateTransforms"][0]["setToServerValue"])
        self.assertNotIn("updateMask", write)

    def test_existing_fields_and_custom_values_are_preserved(self):
        defaults = SEED["categories/movilidad"]
        existing = {"fields": {k: m.encode(v) for k, v in {**defaults, "color": "#E8505B", "custom": "conservar"}.items()}, "updateTime": "version-1"}
        self.assertIsNone(m.plan_write(NAME, defaults, existing))

    def test_partial_document_only_patches_missing_fields_with_version_precondition(self):
        defaults = SEED["categories/movilidad"]
        existing = {"fields": {"name": m.encode("Nombre elegido"), "active": m.encode(False)}, "updateTime": "version-1"}
        write = m.plan_write(NAME, defaults, existing)
        self.assertNotIn("name", write["update"]["fields"])
        self.assertNotIn("active", write["update"]["fields"])
        self.assertEqual({"updateTime": "version-1"}, write["currentDocument"])
        self.assertEqual(set(write["update"]["fields"]), set(write["updateMask"]["fieldPaths"]))

    def test_invalid_existing_schema_is_not_silently_overwritten(self):
        existing = {"fields": {"defaultDurationHours": m.encode(0)}, "updateTime": "v1"}
        with self.assertRaises(m.SetupError):
            m.plan_write(NAME, SEED["categories/movilidad"], existing)

    def test_config_rejects_boolean_integer_and_invalid_radius(self):
        fields = copy.deepcopy(SEED["appConfig/public"])
        fields["maxEvidenceCount"] = True
        with self.assertRaises(m.SetupError): m.validate("appConfig/public", fields)
        fields = copy.deepcopy(SEED["appConfig/public"])
        fields["maxRadiusMeters"] = 1
        with self.assertRaises(m.SetupError): m.validate("appConfig/public", fields)

    def test_plan_mode_performs_no_mutating_requests(self):
        api = FakeApi(lambda *args: None)
        count = m.seed_documents(api, m.PROJECT, SEED, False)
        self.assertEqual(6, count)
        self.assertTrue(all(method == "GET" for method, _, _ in api.calls))

    def test_apply_uses_one_atomic_commit_for_all_missing_documents(self):
        api = FakeApi(lambda *args: None)
        m.seed_documents(api, m.PROJECT, SEED, True)
        mutations = [x for x in api.calls if x[0] != "GET"]
        self.assertEqual(1, len(mutations))
        self.assertTrue(mutations[0][1].endswith("documents:commit"))
        self.assertEqual(6, len(mutations[0][2]["writes"]))

    def test_validation_failure_before_commit_produces_no_writes(self):
        def handler(method, url, body, missing):
            return {"fields": {"defaultDurationHours": m.encode(0)}, "updateTime": "v"} if url.endswith("categories/movilidad") else None
        api = FakeApi(handler)
        with self.assertRaises(m.SetupError): m.seed_documents(api, m.PROJECT, SEED, True)
        self.assertTrue(all(x[0] == "GET" for x in api.calls))

    def test_other_project_is_rejected_without_network(self):
        api = FakeApi(lambda *args: self.fail("No debería acceder a red"))
        with self.assertRaises(m.SetupError): m.main(["--project", "otro-proyecto", "--apply"], api)

    def test_existing_rules_are_backed_up_and_patch_uses_release_wrapper(self):
        name = f"projects/{m.PROJECT}/releases/cloud.firestore"
        def handler(method, url, body, missing):
            if method == "GET" and url.endswith("cloud.firestore"):
                patches = [x for x in api.calls if x[0] == "PATCH"]
                return {"name": name, "rulesetName": f"projects/{m.PROJECT}/rulesets/" + ("new" if patches else "old")}
            if url.endswith("rulesets/old"): return {"source": {"files": [{"name": "firestore.rules", "content": "anterior"}]}}
            if method == "POST": return {"name": f"projects/{m.PROJECT}/rulesets/new"}
            return {}
        api = FakeApi(handler)
        with tempfile.TemporaryDirectory() as tmp:
            m.deploy_rules(api, m.PROJECT, "nuevas", Path(tmp))
            self.assertEqual(1, len(list(Path(tmp).glob("reglas-anteriores-*.json"))))
        patch = next(x[2] for x in api.calls if x[0] == "PATCH")
        self.assertEqual(name, patch["release"]["name"])

    def test_failed_rule_upload_does_not_activate_a_release(self):
        def handler(method, url, body, missing):
            if method == "GET": return None
            raise m.ApiError(400, "Reglas inválidas")
        api = FakeApi(handler)
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaises(m.ApiError): m.deploy_rules(api, m.PROJECT, "invalid", Path(tmp))
        self.assertFalse(any(x[0] == "PATCH" or x[1].endswith("/releases") and x[0] == "POST" for x in api.calls))

    def test_android_configuration_is_project_checked_and_kept_private(self):
        app_name = f"projects/{m.PROJECT}/androidApps/demo"
        config = {"project_info": {"project_id": m.PROJECT}, "client": [{"oauth_client": []}]}
        def handler(method, url, body, missing):
            if url.endswith("/androidApps"): return {"apps": [{"name": app_name, "packageName": m.PACKAGE}]}
            if url.endswith("/sha") and method == "GET": return {"certificates": []}
            if url.endswith("/config"): return {"configFileContents": base64.b64encode(json.dumps(config).encode()).decode()}
            return {}
        api = FakeApi(handler)
        with tempfile.TemporaryDirectory() as tmp:
            self.assertFalse(m.configure_android(api, m.PROJECT, Path(tmp)))
            self.assertEqual(0o600, (Path(tmp) / "google-services.json").stat().st_mode & 0o777)
            self.assertEqual(config, json.loads((Path(tmp) / "google-services.json").read_text()))
        self.assertEqual(2, sum(1 for x in api.calls if x[0] == "POST" and x[1].endswith("/sha")))

if __name__ == "__main__": unittest.main()
