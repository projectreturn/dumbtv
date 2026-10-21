package tv.dumbtv.home;

import android.content.Context;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Offline launcher translations; does not change the Android system language. */
final class Language {
    static final String KEY = "ui.language";
    static final String[] CODES = {"system", "de", "en"};
    private static final String[][] WORDS = {
        {"Sprache", "Language"}, {"Systemsprache", "System language"},
        {"Gestaltung", "Appearance"}, {"Einstellungen", "Settings"}, {"Eingänge", "Inputs"},
        {"Akzentfarbe", "Accent color"}, {"Weiß", "White"}, {"Blau", "Blue"}, {"Türkis", "Cyan"},
        {"Grün", "Green"}, {"Bernstein", "Amber"}, {"Rot", "Red"}, {"Violett", "Violet"},
        {"Kachelgröße", "Tile size"}, {"Klein", "Small"}, {"Mittel", "Medium"}, {"Groß", "Large"},
        {"App-Namen", "App names"}, {"Bei Auswahl", "On focus"}, {"Immer", "Always"}, {"Nie", "Never"},
        {"Uhrzeit", "Clock"}, {"24 Stunden", "24-hour"}, {"12 Stunden", "12-hour"},
        {"Eingänge anzeigen", "Show inputs"}, {"An", "On"}, {"Aus", "Off"},
        {"Hintergrund", "Background"}, {"Schwarz", "Black"}, {"Graphit", "Graphite"},
        {"Mitternacht", "Midnight"}, {"Bilddatei", "Image file"}, {"Bild abdunkeln", "Image dim"},
        {"Hintergrundwechsel", "Wallpaper rotation"}, {"Bei Rückkehr", "Every return home"},
        {"Stündlich", "Hourly"}, {"Täglich", "Daily"}, {"Nach Tageszeit", "By time of day"},
        {"Bilderordner auswählen", "Choose picture folder"}, {"Bilderordner: ", "Picture folder: "},
        {"Nächstes Hintergrundbild", "Next wallpaper"}, {"TV-Taste: App auswählen", "TV button: choose app"},
        {"Fernbedienungsdienst aktivieren / deaktivieren", "Enable / disable remote service"},
        {"dumbtv Fernbedienung", "dumbtv remote control"},
        {"Damit Zahnrad und TV auch außerhalb des Launchers funktionieren, kann der Dienst Tastenereignisse empfangen. Er reagiert nur auf diese Tasten, liest keine Bildschirminhalte und hat keinen Internetzugriff. In den Bedienungshilfen lässt er sich jederzeit abschalten.", "To use Settings and TV outside the launcher, the service can receive key events. It only handles these shortcuts, cannot read screen contents and has no internet access. You can disable it in Accessibility at any time."},
        {"Bedienungshilfen öffnen", "Open Accessibility"},
        {"Bedienungshilfen bitte über die Systemeinstellungen öffnen.", "Please open Accessibility from system settings."},
        {"Abbrechen", "Cancel"}, {"Fertig", "Done"}, {"Speichern", "Save"},
        {"Apps auf dem Startbildschirm", "Apps on home screen"},
        {"Eingänge auf dem Startbildschirm", "Inputs on home screen"},
        {"Reihenfolge, Namen und ausgeblendete Kacheln zurücksetzen", "Reset order, names and hidden tiles"},
        {"Amazon-Startbildschirm öffnen", "Open Amazon home screen"},
        {"Einstellungsdatei: ", "Settings file: "},
        {"Bilderordner auswählen: JPG, PNG und WebP direkt im gewählten Ordner", "Choose picture folder: JPG, PNG and WebP files directly in the selected folder"},
        {"Hintergrundwechsel bestimmt, wann das nächste Bild angezeigt wird.", "Wallpaper rotation determines when the next image is displayed."},
        {"Kein Internetzugriff. Kein Tracking, keine Werbung, keine Konten.", "No internet access. No tracking, ads or accounts."},
        {"Ohne Lesezugriff kann kein eigener Bilderordner ausgewählt werden.", "Read access is required to choose a picture folder."},
        {"Startbildschirm zurücksetzen?", "Reset home screen?"},
        {"Reihenfolge, Namen und ausgeblendete Kacheln werden zurückgesetzt. Farben und Größen bleiben erhalten.", "Order, names and hidden tiles will be reset. Colors and sizes stay unchanged."},
        {"Zurücksetzen", "Reset"}, {"Keine App / bei Tastendruck auswählen", "No app / choose when pressed"},
        {"App für die TV-Taste", "App for the TV button"},
        {"Interner Speicher", "Internal storage"}, {"dumbtv-Bilderordner", "dumbtv picture folder"},
        {"USB / Speicher: ", "USB / storage: "}, {"Speicherort auswählen", "Choose storage"},
        {"Standardordner", "Default folder"},
        {"Dieser Ordner ist nicht lesbar oder der Speicher wurde entfernt.", "This folder is unreadable or the storage has been removed."},
        {"↑ Übergeordneter Ordner", "↑ Parent folder"}, {"Bilderordner", "Picture folder"},
        {"%d Bilder, ohne Unterordner", "%d pictures, excluding subfolders"},
        {"Auswählen", "Select"}, {"Speicherorte", "Storage"},
        {"TV-App nicht verfügbar. Unter Gestaltung neu auswählen.", "TV app unavailable. Choose another app in Appearance."},
        {"Konnte nicht geöffnet werden", "Could not open that"},
        {"Verschieben", "Move"}, {"Umbenennen", "Rename"}, {"Ausblenden", "Hide"}, {"App-Info", "App info"},
        {"Ausgeblendet. Unter Gestaltung wieder einblendbar.", "Hidden. Restore it from Appearance."},
    };
    private static final Map<String, String> ENGLISH = new HashMap<>();
    static { for (String[] pair : WORDS) ENGLISH.put(pair[0], pair[1]); }
    private Language() {}

    static String code(Context context) {
        String value = Config.get(context).get(KEY, "system");
        return "de".equals(value) || "en".equals(value) ? value : "system";
    }

    static Locale locale(Context context) {
        String value = code(context);
        if ("system".equals(value)) value = Locale.getDefault().getLanguage();
        return "de".equals(value) ? Locale.GERMAN : Locale.ENGLISH;
    }

    static String signature(Context context) {
        return code(context) + ":" + locale(context).getLanguage();
    }

    static String text(Context context, String german) {
        if (Locale.GERMAN.equals(locale(context))) return german;
        String translated = ENGLISH.get(german);
        return translated == null ? german : translated;
    }
}
