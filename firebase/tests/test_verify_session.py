import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import verify_session as verify
from initialize import SetupError, PROJECT, PACKAGE


class FailingClient:
    def __init__(self, signup_fails=False):
        self.signup_fails = signup_fails
        self.deleted = False

    def request(self, method, url, body=None, token=None):
        if "accounts:signUp?" in url:
            if self.signup_fails:
                raise SetupError("Registro no confirmado")
            return {"localId": "temporary-uid", "idToken": "not-a-real-token"}
        if "accounts:signInWithPassword?" in url:
            raise SetupError("Inicio de sesión denegado")
        if "accounts:delete?" in url:
            self.deleted = True
            return {}
        raise AssertionError("No debe publicar si falla el inicio de sesión")


class Admin:
    def __init__(self, cleanup_fails=False):
        self.calls = []
        self.cleanup_fails = cleanup_fails

    def request(self, method, url, body=None, missing_ok=False):
        self.calls.append((method, url, body))
        if self.cleanup_fails:
            raise SetupError("Limpieza denegada")
        if url.endswith("accounts:lookup"):
            return {"users": [{"localId": "created-before-timeout", "email": body["email"][0]}]}
        return {}


class SessionVerificationTests(unittest.TestCase):
    def test_rejects_config_from_other_project(self):
        with tempfile.TemporaryDirectory() as root:
            p = Path(root)/"google-services.json"
            p.write_text(json.dumps({"project_info": {"project_id": "other"}}))
            with self.assertRaises(SetupError):
                verify.client_key(p)

    def test_selects_only_civita_android_client(self):
        with tempfile.TemporaryDirectory() as root:
            p = Path(root)/"google-services.json"
            p.write_text(json.dumps({"project_info": {"project_id": PROJECT}, "client": [
                {"client_info": {"android_client_info": {"package_name": "other"}}, "api_key": [{"current_key": "wrong"}]},
                {"client_info": {"android_client_info": {"package_name": PACKAGE}}, "api_key": [{"current_key": "client-test-key"}]}]}))
            self.assertEqual(verify.client_key(p), "client-test-key")

    def test_authentication_failure_never_publishes_and_cleans_exact_routes(self):
        client, admin = FailingClient(), Admin()
        run_id = "a"*32
        result = verify.run(client, admin, "client-test-key", run_id)
        self.assertEqual(result["status"], "failed")
        self.assertEqual(result["publish"], "not_run")
        self.assertEqual(result["cleanup"], "passed")
        self.assertTrue(client.deleted)
        deletes = [url.split("/documents/")[1] for method, url, _ in admin.calls if method == "DELETE"]
        self.assertEqual(deletes, [f"reports/e2e_{run_id}/history/created", f"reports/e2e_{run_id}",
                                  "users/temporary-uid/preferences/settings", "users/temporary-uid"])
        self.assertNotIn("not-a-real-token", json.dumps(result))

    def test_cleanup_failure_is_reported_and_never_counts_as_passed(self):
        result = verify.run(FailingClient(), Admin(cleanup_fails=True), "client-test-key", "b"*32)
        self.assertEqual(result["status"], "failed")
        self.assertEqual(result["cleanup"], "failed")
        self.assertEqual(len(result["cleanupPending"]), 4)

    def test_signup_timeout_looks_up_only_this_unique_temporary_email(self):
        admin = Admin()
        result = verify.run(FailingClient(signup_fails=True), admin, "client-test-key", "c"*32)
        self.assertEqual(result["status"], "failed")
        self.assertEqual(result["cleanup"], "passed")
        self.assertEqual(admin.calls[0][2], {"email": ["civita-e2e-" + "c"*32 + "@example.invalid"]})
        self.assertEqual(admin.calls[1][2], {"localId": "created-before-timeout"})
        self.assertFalse(any(method == "DELETE" for method, _, _ in admin.calls))

    def test_run_id_rejects_paths_before_any_request(self):
        with self.assertRaises(SetupError):
            verify.run(FailingClient(), Admin(), "client-test-key", "../reports/real")


if __name__ == "__main__":
    unittest.main()
