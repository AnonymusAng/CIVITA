import importlib.util
import sys
from pathlib import Path
import unittest
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
import admin
from initialize import SetupError

class OfficialDataTest(unittest.TestCase):
    def event(self):
        return dict(id='evento-real',categoryId='eventos',createdBy='uid-real',title='Evento oficial',description='Descripción del evento oficial.',latitude=2.44,longitude=-76.61,startsAt='2026-11-01T08:00:00-05:00',endsAt='2026-11-01T10:00:00-05:00',status='scheduled')
    def test_standard_geohash_vector_matches_android(self):
        self.assertEqual("u4pruydqq",admin.geohash(57.64911,10.40744))
    def test_valid_event_preserves_timezone_and_geopoint(self):
        path,fields=admin.event_document(self.event())
        self.assertEqual('events/evento-real',path)
        self.assertEqual(-76.61,fields['location']['geoPointValue']['longitude'])
        self.assertIn('-05:00',fields['startsAt']['timestampValue'])
    def test_rejects_event_end_before_start(self):
        event=self.event();event['endsAt']='2026-10-31T10:00:00-05:00'
        with self.assertRaises(SetupError):admin.event_document(event)
    def test_rejects_timezone_ambiguity(self):
        event=self.event();event['startsAt']='2026-11-01T08:00:00'
        with self.assertRaises(SetupError):admin.event_document(event)
    def test_rejects_path_injection(self):
        event=self.event();event['id']='../otro'
        with self.assertRaises(SetupError):admin.event_document(event)
    def test_rejects_fake_geometry(self):
        event=self.event();event['latitude']=200
        with self.assertRaises(SetupError):admin.event_document(event)
    def test_feature_maps_only_under_explicit_layer(self):
        path,fields=admin.feature_document(dict(layerId='reportes',id='punto',name='Punto oficial',latitude=2.44,longitude=-76.61))
        self.assertEqual('mapLayers/reportes/features/punto',path)

class AuthorIntegrityTest(unittest.TestCase):
    def test_existing_active_uid_is_required(self):
        class Api:
            def request(self,*args): return {'users':[{'localId':'real-uid'}]}
        self.assertEqual('real-uid',admin.require_auth_user(Api(),'real-uid')['localId'])
    def test_disabled_or_missing_author_is_rejected_before_creating_event(self):
        for response in [{}, {'users':[{'localId':'real-uid','disabled':True}]}, {'users':[{'localId':'different'}]}]:
            class Api:
                def request(self,*args): return response
            with self.assertRaises(SetupError): admin.require_auth_user(Api(),'real-uid')
    def test_feature_properties_must_be_an_object(self):
        with self.assertRaises(SetupError):admin.feature_document(dict(layerId='reportes',id='punto',name='Punto oficial',latitude=2.44,longitude=-76.61,properties=[]))
