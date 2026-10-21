package tv.dumbtv.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.StateListDrawable;
import android.media.tv.TvInputManager;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Every setting is a row: left/right or OK cycles it, and the change is saved at once. */
public final class Options extends Activity {
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private final List<Runnable> refreshers = new ArrayList<>();
    private Config cfg;
    private FolderPicker folders;
    private String language;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        cfg = Config.get(this);
        language = Language.signature(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setFocusable(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setBackgroundColor(0xFF0C0C0E);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(56), dp(40), dp(56), dp(40));
        scroll.addView(list);

        TextView title = Ui.text(this, 30, Ui.TEXT, Ui.LIGHT);
        title.setText(tr("Gestaltung"));
        title.setPadding(dp(16), 0, 0, dp(20));
        list.addView(title);
        list.addView(action("Sprache", this::pickLanguage));

        for (Config.Choice c : Config.CHOICES) list.addView(choice(c));
        list.addView(action("Bilderordner auswählen", this::pickWallpaperFolder));
        TextView folder = label("");
        folder.setTextSize(13);
        folder.setTextColor(Ui.MUTED);
        folder.setPadding(dp(16), 0, dp(16), dp(8));
        list.addView(folder);
        refreshers.add(() -> folder.setText(tr("Bilderordner: ") + cfg.wallpaperFolder().getPath()));
        list.addView(action("Nächstes Hintergrundbild", () -> {
            cfg.nextWallpaper();
            refresh();
        }));
        list.addView(gap());
        list.addView(action("TV-Taste: App auswählen", this::pickTvApp));
        list.addView(action("Fernbedienungsdienst aktivieren / deaktivieren", () -> {
            new AlertDialog.Builder(this, Ui.DIALOG)
                    .setTitle(tr("dumbtv Fernbedienung"))
                    .setMessage(tr("Damit Zahnrad und TV auch außerhalb des Launchers funktionieren, kann der Dienst Tastenereignisse empfangen. Er reagiert nur auf diese Tasten, liest keine Bildschirminhalte und hat keinen Internetzugriff. In den Bedienungshilfen lässt er sich jederzeit abschalten."))
                    .setPositiveButton(tr("Bedienungshilfen öffnen"), (dialog, which) -> {
                        try {
                            startActivity(new android.content.Intent(
                                    android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));
                        } catch (android.content.ActivityNotFoundException | SecurityException e) {
                            android.widget.Toast.makeText(this, tr("Bedienungshilfen bitte über die Systemeinstellungen öffnen."),
                                    android.widget.Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton(tr("Abbrechen"), null)
                    .show();
        }));
        list.addView(action("Apps auf dem Startbildschirm", () -> pick(Catalog.APPS)));
        list.addView(action("Eingänge auf dem Startbildschirm", () -> pick(Catalog.INPUTS)));
        list.addView(action("Reihenfolge, Namen und ausgeblendete Kacheln zurücksetzen", this::reset));
        list.addView(action("Amazon-Startbildschirm öffnen", () -> {
            Guard.amazon(this);
            finish();
        }));

        TextView about = Ui.text(this, 13, Ui.FAINT, Ui.MEDIUM);
        about.setLineSpacing(0, 1.4f);
        about.setPadding(dp(16), dp(28), 0, 0);
        about.setText(tr("Einstellungsdatei: ") + cfg.dir + "/" + Config.FILE
                + "\n" + tr("Bilderordner auswählen: JPG, PNG und WebP direkt im gewählten Ordner")
                + "\n" + tr("Hintergrundwechsel bestimmt, wann das nächste Bild angezeigt wird.")
                + "\n" + tr("Kein Internetzugriff. Kein Tracking, keine Werbung, keine Konten."));
        list.addView(about);

        setContentView(scroll);
        refresh();
        if (getIntent().getBooleanExtra("choose_tv_app", false)) pickTvApp();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cfg.refresh();
        if (!Language.signature(this).equals(language)) { recreate(); return; }
        refresh();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        return RemoteKeys.dispatch(this, event) || super.dispatchKeyEvent(event);
    }

    @Override protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        if (intent.getBooleanExtra("choose_tv_app", false)) pickTvApp();
    }

    private void refresh() {
        for (Runnable r : refreshers) r.run();
    }

    private void pickWallpaperFolder() {
        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {android.Manifest.permission.READ_EXTERNAL_STORAGE}, 209);
            return;
        }
        if (folders != null) folders.close();
        folders = new FolderPicker(this, cfg, this::refresh);
        folders.show();
    }

    @Override protected void onDestroy() {
        if (folders != null) folders.close();
        super.onDestroy();
    }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request != 209) return;
        if (grants.length > 0 && grants[0] == android.content.pm.PackageManager.PERMISSION_GRANTED)
            pickWallpaperFolder();
        else android.widget.Toast.makeText(this,
                tr("Ohne Lesezugriff kann kein eigener Bilderordner ausgewählt werden."),
                android.widget.Toast.LENGTH_LONG).show();
    }

    private View choice(Config.Choice c) {
        LinearLayout row = row();
        TextView name = label(c.title);
        TextView value = Ui.text(this, 16, Ui.MUTED, Ui.MEDIUM);
        value.setGravity(Gravity.END);
        row.addView(name, new LinearLayout.LayoutParams(0, WRAP, 1f));
        row.addView(value, new LinearLayout.LayoutParams(WRAP, WRAP));
        row.setOnClickListener(v -> step(c, 1));
        row.setOnKeyListener((v, code, e) -> {
            if (e.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (code == KeyEvent.KEYCODE_DPAD_LEFT) return step(c, -1);
            if (code == KeyEvent.KEYCODE_DPAD_RIGHT) return step(c, 1);
            return false;
        });
        Runnable show = () -> {
            boolean focused = row.isFocused();
            String text = tr(c.name(cfg.get(c)));
            if (c == Config.ACCENT) {
                SpannableString s = new SpannableString("●  " + text);
                s.setSpan(new ForegroundColorSpan(cfg.accent()), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                value.setText(focused ? "‹  " : "");
                value.append(s);
                if (focused) value.append("  ›");
            } else {
                value.setText(focused ? "‹  " + text + "  ›" : text);
            }
            value.setTextColor(focused ? Ui.TEXT : Ui.MUTED);
            boolean off = (c == Config.DIM || c == Config.ROTATE)
                    && !"image".equals(cfg.get(Config.BACKGROUND));
            row.setAlpha(off ? 0.4f : 1f);
        };
        row.setOnFocusChangeListener((v, has) -> show.run());
        refreshers.add(show);
        refreshers.add(() -> style(row));
        return row;
    }

    private boolean step(Config.Choice c, int by) {
        int i = Arrays.asList(c.values).indexOf(cfg.get(c));
        int n = c.values.length;
        cfg.set(c.key, c.values[((i + by) % n + n) % n]);
        refresh();
        return true;
    }

    private View action(String text, Runnable run) {
        LinearLayout row = row();
        row.addView(label(text), new LinearLayout.LayoutParams(0, WRAP, 1f));
        TextView chevron = Ui.text(this, 16, Ui.MUTED, Ui.MEDIUM);
        chevron.setText("›");
        row.addView(chevron);
        row.setOnClickListener(v -> run.run());
        refreshers.add(() -> style(row));
        return row;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setFocusable(true);
        row.setClickable(true);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(600), WRAP);
        lp.bottomMargin = dp(4);
        row.setLayoutParams(lp);
        return row;
    }

    private void style(View row) {
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[] {android.R.attr.state_focused},
                Ui.round((cfg.accent() & 0x00FFFFFF) | 0x33000000, dp(10)));
        bg.addState(new int[0], Ui.round(0x00000000, dp(10)));
        row.setBackground(bg);
    }

    private TextView label(String text) {
        TextView v = Ui.text(this, 16, Ui.TEXT, Ui.MEDIUM);
        v.setText(tr(text));
        return v;
    }

    private View gap() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(18)));
        return v;
    }

    private void pick(String kind) {
        List<Item> items = Catalog.APPS.equals(kind)
                ? Catalog.apps(this)
                : Catalog.inputs(this, (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE));
        CharSequence[] names = new CharSequence[items.size()];
        boolean[] on = new boolean[items.size()];
        for (int i = 0; i < items.size(); i++) {
            names[i] = Catalog.label(cfg, items.get(i));
            on[i] = Catalog.shown(cfg, kind, items.get(i));
        }
        new AlertDialog.Builder(this, Ui.DIALOG)
                .setTitle(tr(Catalog.APPS.equals(kind) ? "Apps auf dem Startbildschirm" : "Eingänge auf dem Startbildschirm"))
                .setMultiChoiceItems(names, on, (d, which, checked) ->
                        Catalog.setShown(cfg, kind, items.get(which), checked))
                .setPositiveButton(tr("Fertig"), null)
                .show();
    }

    private void reset() {
        new AlertDialog.Builder(this, Ui.DIALOG)
                .setTitle(tr("Startbildschirm zurücksetzen?"))
                .setMessage(tr("Reihenfolge, Namen und ausgeblendete Kacheln werden zurückgesetzt. Farben und Größen bleiben erhalten."))
                .setPositiveButton(tr("Zurücksetzen"), (d, w) -> cfg.removePrefix("name.", "apps.", "inputs."))
                .setNegativeButton(tr("Abbrechen"), null)
                .show();
    }

    private void pickTvApp() {
        List<Item> items = Catalog.apps(this);
        CharSequence[] names = new CharSequence[items.size() + 1];
        names[0] = tr("Keine App / bei Tastendruck auswählen");
        String current = cfg.get(Config.TV_APP, "");
        int selected = 0;
        for (int i = 0; i < items.size(); i++) {
            names[i + 1] = Catalog.label(cfg, items.get(i));
            if (items.get(i).id.equals(current)) selected = i + 1;
        }
        new AlertDialog.Builder(this, Ui.DIALOG)
                .setTitle(tr("App für die TV-Taste"))
                .setSingleChoiceItems(names, selected, (dialog, which) -> {
                    cfg.set(Config.TV_APP, which == 0 ? null : items.get(which - 1).id);
                    dialog.dismiss();
                })
                .setNegativeButton(tr("Abbrechen"), null)
                .show();
    }

    private int dp(float v) {
        return Ui.dp(this, v);
    }

    private String tr(String text) { return Language.text(this, text); }

    private void pickLanguage() {
        String current = Language.code(this);
        int selected = Arrays.asList(Language.CODES).indexOf(current);
        new AlertDialog.Builder(this, Ui.DIALOG)
                .setTitle(tr("Sprache"))
                .setSingleChoiceItems(new String[] {tr("Systemsprache"), "Deutsch", "English"},
                        selected, (dialog, which) -> {
                            dialog.dismiss();
                            if (!current.equals(Language.CODES[which])) {
                                cfg.set(Language.KEY, Language.CODES[which]);
                                recreate();
                            }
                        })
                .setNegativeButton(tr("Abbrechen"), null).show();
    }
}
