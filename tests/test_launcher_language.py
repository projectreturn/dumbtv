import json
from pathlib import Path
import re
import unittest


SOURCE = Path(__file__).resolve().parents[1] / "launcher/src/tv/dumbtv/home"


class LauncherLanguageTests(unittest.TestCase):
    def setUp(self):
        self.source = (SOURCE / "Language.java").read_text(encoding="utf-8")
        string = r'"(?:[^"\\]|\\.)*"'
        pairs = re.findall(r'\{\s*(' + string + r')\s*,\s*(' + string + r')\s*\}', self.source)
        self.pairs = [(json.loads(a), json.loads(b)) for a, b in pairs]
        self.english = dict(self.pairs)

    def test_only_requested_languages(self):
        self.assertIn('CODES = {"system", "de", "en"}', self.source)
        self.assertNotIn("Locale.setDefault", self.source)

    def test_translation_keys_unique_and_nonempty(self):
        self.assertGreater(len(self.pairs), 50)
        self.assertEqual(len(self.pairs), len(self.english))
        for german, english in self.pairs:
            self.assertTrue(german.strip())
            self.assertTrue(english.strip())

    def test_format_placeholders_preserved(self):
        for german, english in self.pairs:
            self.assertEqual(re.findall(r"%[ds]", german), re.findall(r"%[ds]", english))

    def test_settings_labels_and_values_covered(self):
        config = (SOURCE / "Config.java").read_text(encoding="utf-8")
        choices = config.split("static final Choice ACCENT", 1)[1].split("static final Choice[] CHOICES", 1)[0]
        for text in re.findall(r'"([^"\n]+)"', choices):
            if text[0].isupper() and text not in {"System", "Pink"}:
                self.assertIn(text, self.english, text)


if __name__ == "__main__":
    unittest.main()
