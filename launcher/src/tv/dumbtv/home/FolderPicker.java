package tv.dumbtv.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Environment;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Read-only, remote-friendly folder browser for TVs without a real DocumentsUI picker. */
final class FolderPicker {
    private final Activity activity;
    private final Config cfg;
    private final Runnable changed;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private AlertDialog dialog;
    private int generation;
    private boolean closed;

    FolderPicker(Activity activity, Config cfg, Runnable changed) {
        this.activity = activity;
        this.cfg = cfg;
        this.changed = changed;
    }

    void show() {
        int token = ++generation;
        io.execute(() -> {
            List<File> roots = new ArrayList<>();
            roots.add(Environment.getExternalStorageDirectory());
            File[] volumes = new File("/storage").listFiles();
            if (volumes != null) for (File volume : volumes) {
                String name = volume.getName();
                if (!"emulated".equals(name) && !"self".equals(name) && volume.isDirectory())
                    roots.add(volume);
            }
            // Some TV firmware hides /storage itself but exposes volume-specific app dirs.
            for (File external : activity.getExternalFilesDirs(null)) {
                if (external == null) continue;
                String path = external.getAbsolutePath();
                int marker = path.indexOf("/Android/data/");
                if (marker < 0) continue;
                File volume = new File(path.substring(0, marker));
                if (!roots.contains(volume)) roots.add(volume);
            }
            File own = new File(cfg.dir, Config.WALLPAPERS);
            if (own.isDirectory() || own.mkdirs()) roots.add(own);
            activity.runOnUiThread(() -> {
                if (!valid(token)) return;
                CharSequence[] names = new CharSequence[roots.size()];
                for (int i = 0; i < roots.size(); i++) names[i] = i == 0
                        ? tr("Interner Speicher") : roots.get(i).equals(own)
                        ? tr("dumbtv-Bilderordner") : tr("USB / Speicher: ") + roots.get(i).getName();
                replace(new AlertDialog.Builder(activity, Ui.DIALOG)
                        .setTitle(tr("Speicherort auswählen"))
                        .setItems(names, (d, which) -> browse(roots.get(which), roots.get(which)))
                        .setNeutralButton(tr("Standardordner"), (d, which) -> {
                            cfg.set(Config.WALLPAPER_DIR, null);
                            changed.run();
                            close();
                        })
                        .setNegativeButton(tr("Abbrechen"), (d, which) -> close()).create());
            });
        });
    }

    private void browse(File root, File folder) {
        int token = ++generation;
        io.execute(() -> {
            List<File> dirs = new ArrayList<>();
            int images = 0;
            File[] children = folder.listFiles();
            if (children != null) for (File child : children) {
                if (child.isDirectory() && !child.getName().startsWith(".") && inside(root, child))
                    dirs.add(child);
                else if (child.isFile() && image(child.getName())) images++;
            }
            Collections.sort(dirs, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            boolean parent = !folder.equals(root);
            boolean readable = children != null;
            int count = images;
            activity.runOnUiThread(() -> {
                if (!valid(token)) return;
                if (!readable) {
                    Toast.makeText(activity, tr("Dieser Ordner ist nicht lesbar oder der Speicher wurde entfernt."),
                            Toast.LENGTH_LONG).show();
                    show();
                    return;
                }
                CharSequence[] names = new CharSequence[dirs.size() + (parent ? 1 : 0)];
                if (parent) names[0] = tr("↑ Übergeordneter Ordner");
                for (int i = 0; i < dirs.size(); i++) names[i + (parent ? 1 : 0)] = "▸ " + dirs.get(i).getName();
                TextView title = Ui.text(activity, 17, Ui.TEXT, Ui.MEDIUM);
                title.setPadding(Ui.dp(activity, 24), Ui.dp(activity, 20), Ui.dp(activity, 24), Ui.dp(activity, 12));
                title.setText(tr("Bilderordner") + "\n" + folder.getPath() + "\n"
                        + String.format(Language.locale(activity), tr("%d Bilder, ohne Unterordner"), count));
                AlertDialog next = new AlertDialog.Builder(activity, Ui.DIALOG)
                        .setCustomTitle(title)
                        .setItems(names, (d, which) -> browse(root,
                                parent && which == 0 ? folder.getParentFile() : dirs.get(which - (parent ? 1 : 0))))
                        .setPositiveButton(tr("Auswählen"), (d, which) -> {
                            cfg.set(Config.WALLPAPER_DIR, folder.getAbsolutePath());
                            cfg.set(Config.BACKGROUND.key, "image");
                            cfg.set("wallpaper.offset", "0");
                            changed.run();
                            close();
                        })
                        .setNeutralButton(tr("Speicherorte"), (d, which) -> show())
                        .setNegativeButton(tr("Abbrechen"), (d, which) -> close()).create();
                replace(next);
                next.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(count > 0);
            });
        });
    }

    private void replace(AlertDialog next) {
        if (dialog != null) dialog.dismiss();
        dialog = next;
        next.setOnCancelListener(d -> close());
        next.show();
    }

    private boolean valid(int token) {
        if (activity.isFinishing() || activity.isDestroyed()) { close(); return false; }
        return !closed && token == generation;
    }

    void close() {
        closed = true;
        if (dialog != null) dialog.dismiss();
        io.shutdownNow();
    }

    private static boolean inside(File root, File folder) {
        try {
            return folder.getCanonicalPath().startsWith(root.getCanonicalPath() + File.separator);
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean image(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
                || lower.endsWith(".webp");
    }

    private String tr(String text) { return Language.text(activity, text); }
}
