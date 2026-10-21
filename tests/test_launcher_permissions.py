from pathlib import Path
import unittest
import xml.etree.ElementTree as ET


LAUNCHER = Path(__file__).resolve().parents[1] / "launcher"
ANDROID = "{http://schemas.android.com/apk/res/android}"


class LauncherPermissionTests(unittest.TestCase):
    def test_only_read_access_and_legacy_keeper_permissions(self):
        manifest = ET.parse(LAUNCHER / "AndroidManifest.xml").getroot()
        permissions = {item.get(ANDROID + "name"): item for item in manifest.findall("uses-permission")}
        keeper = {
            "android.permission.PACKAGE_USAGE_STATS",
            "android.permission.RECEIVE_BOOT_COMPLETED",
            "android.permission.REORDER_TASKS",
        }
        self.assertEqual(set(permissions), keeper | {"android.permission.READ_EXTERNAL_STORAGE"})
        for name in keeper:
            self.assertEqual(permissions[name].get(ANDROID + "maxSdkVersion"), "27")

    def test_optional_remote_service_cannot_read_screen_contents(self):
        remote = ET.parse(LAUNCHER / "res/xml/remote.xml").getroot()
        self.assertEqual(remote.get(ANDROID + "canRetrieveWindowContent"), "false")
        self.assertEqual(remote.get(ANDROID + "canRequestFilterKeyEvents"), "true")
        manifest = ET.parse(LAUNCHER / "AndroidManifest.xml").getroot()
        service = next(s for s in manifest.findall("application/service")
                       if s.get(ANDROID + "name") == ".RemoteService")
        self.assertEqual(service.get(ANDROID + "permission"),
                         "android.permission.BIND_ACCESSIBILITY_SERVICE")


if __name__ == "__main__":
    unittest.main()
