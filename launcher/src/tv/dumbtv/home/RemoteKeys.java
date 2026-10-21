package tv.dumbtv.home;

import android.content.Context;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.provider.Settings;
import android.view.KeyEvent;
import android.widget.Toast;

/** Shared shortcut actions for launcher keys and the optional system key filter. */
final class RemoteKeys {
    private RemoteKeys() {}

    static boolean shortcut(int code) {
        // Some Google TV remotes label NOTIFICATION with a gear rather than a bell.
        return code == KeyEvent.KEYCODE_SETTINGS || code == KeyEvent.KEYCODE_NOTIFICATION
                || code == KeyEvent.KEYCODE_TV || code == KeyEvent.KEYCODE_GUIDE;
    }

    static boolean dispatch(Context activity, KeyEvent event) {
        if (!shortcut(event.getKeyCode())) return false;
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_TV
                    || event.getKeyCode() == KeyEvent.KEYCODE_GUIDE) tvApp(activity);
            else settings(activity);
        }
        return true;
    }

    static void settings(Context activity) {
        Intent tv = new Intent(Intent.ACTION_MAIN)
                .addCategory("android.intent.category.LEANBACK_SETTINGS");
        open(activity, tv.resolveActivity(activity.getPackageManager()) != null
                ? tv : new Intent(Settings.ACTION_SETTINGS), null);
    }

    private static void tvApp(Context activity) {
        String pkg = Config.get(activity).get(Config.TV_APP, "");
        if (pkg.isEmpty()) {
            open(activity, new Intent(activity, Options.class)
                    .putExtra("choose_tv_app", true), null);
            return;
        }
        Intent intent = activity.getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
        if (intent == null) intent = activity.getPackageManager().getLaunchIntentForPackage(pkg);
        if (intent == null) {
            Toast.makeText(activity, Language.text(activity, "TV-App nicht verfügbar. Unter Gestaltung neu auswählen."),
                    Toast.LENGTH_LONG).show();
            return;
        }
        open(activity, intent, pkg);
    }

    private static void open(Context activity, Intent intent, String pkg) {
        if (Guard.AMAZON.equals(pkg)) Guard.pause();
        try {
            activity.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(activity, Language.text(activity, "Konnte nicht geöffnet werden"), Toast.LENGTH_SHORT).show();
        }
    }
}
