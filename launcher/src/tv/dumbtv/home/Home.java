package tv.dumbtv.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.animation.ValueAnimator;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class Home extends Activity {
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final List<TextView> chips = new ArrayList<>();
    private Config cfg;
    private TvInputManager tv;
    private boolean inputWatching, timeWatching;
    private int applied = -1, appsGeneration;
    private boolean resumed, landed;
    private FrameLayout root;
    private ImageView wall;
    private View shade;
    private View headerFade;
    private View header;
    private ScrollView page;
    private TextView clock, date;
    private Row apps, inputs;
    private Tile moving;
    private String wallKey;
    private int headerInset;
    private String lastKind, lastId;
    private boolean appsLoaded, inputsLoaded;
    private String language;
    private ValueAnimator scrollMotion;
    private final RectF focusBox = new RectF();
    private final Runnable followFocus = () -> followFocus(true);
    private final Runnable settleFrame = () -> {
        refreshFrame(root);
        redrawSurface();
    };

    private final BroadcastReceiver packages = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            Uri data = i.getData();
            loadApps(data == null ? null : data.getSchemeSpecificPart());
        }
    };

    private final BroadcastReceiver time = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            tick();
        }
    };

    private final TvInputManager.TvInputCallback inputWatch = new TvInputManager.TvInputCallback() {
        @Override
        public void onInputStateChanged(String id, int state) {
            loadInputs();
        }

        @Override
        public void onInputAdded(String id) {
            loadInputs();
        }

        @Override
        public void onInputRemoved(String id) {
            loadInputs();
        }

        @Override
        public void onInputUpdated(String id) {
            loadInputs();
        }
    };

    private final class Row {
        final String kind;
        final TextView title;
        final HorizontalScrollView scroll;
        final View body;
        final ViewGroup strip;
        final boolean grid;
        List<Item> all = new ArrayList<>();

        Row(String kind, String name, LinearLayout parent) {
            this.kind = kind;
            grid = Catalog.APPS.equals(kind);
            int edge = dp(56), bleed = dp(14);
            title = Ui.text(Home.this, 12, Ui.FAINT, Ui.MEDIUM);
            title.setText(tr(name).toUpperCase(Language.locale(Home.this)));
            title.setLetterSpacing(0.14f);
            title.setPadding(edge, dp(20), edge, dp(2));
            parent.addView(title, new LinearLayout.LayoutParams(MATCH, WRAP));
            if (grid) {
                GridLayout tiles = new GridLayout(Home.this);
                tiles.setColumnCount(appColumns());
                tiles.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
                tiles.setUseDefaultMargins(false);
                tiles.setClipChildren(false);
                tiles.setClipToPadding(false);
                tiles.setPadding(edge, dp(12), edge, dp(8));
                strip = tiles;
                body = tiles;
                scroll = null;
                parent.addView(body, new LinearLayout.LayoutParams(MATCH, WRAP));
            } else {
                LinearLayout tiles = new LinearLayout(Home.this);
                tiles.setClipChildren(false);
                tiles.setClipToPadding(false);
                tiles.setPadding(bleed, dp(12), bleed, dp(8));
                strip = tiles;
                scroll = new HorizontalScrollView(Home.this) {
                    /** Keeps the focused tile centered, except near either end of the row. */
                    @Override
                    protected int computeScrollDeltaToGetChildRectOnScreen(Rect r) {
                        if (getChildCount() == 0) return 0;
                        int range = Math.max(0, getChildAt(0).getWidth()
                                - (getWidth() - getPaddingLeft() - getPaddingRight()));
                        int target = Math.max(0, Math.min(range, r.centerX() - getWidth() / 2));
                        return target - getScrollX();
                    }
                };
                scroll.setFocusable(false);
                scroll.setHorizontalScrollBarEnabled(false);
                scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
                scroll.setClipChildren(false);
                scroll.setClipToPadding(false);
                scroll.setPadding(edge - bleed, 0, edge - bleed, 0);
                scroll.addView(strip, new FrameLayout.LayoutParams(WRAP, WRAP));
                body = scroll;
                parent.addView(body, new LinearLayout.LayoutParams(MATCH, WRAP));
            }
        }

        void setVisible(boolean on) {
            int v = on ? View.VISIBLE : View.GONE;
            title.setVisibility(v);
            body.setVisibility(v);
        }
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Future<List<Item>> catalog = io.submit(() -> Catalog.apps(this));
        cfg = Config.get(this);
        language = Language.signature(this);
        android.content.SharedPreferences selection = getSharedPreferences("selection", MODE_PRIVATE);
        lastKind = selection.getString("kind", Catalog.APPS);
        lastId = selection.getString("id", "");
        tv = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        setContentView(build());

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addDataScheme("package");
        registerReceiver(packages, f);
        Keeper.start(this);
        firstApps(catalog);
    }

    /**
     * Waits for the app list and loads the art of the tiles that fit on screen, so the first
     * frame is complete. After a cold start that frame is what people see first: loading in the
     * background drew the Inputs row alone, focused HDMI 1 for ~0.3 s, then popped the apps and
     * their banners in.
     */
    private void firstApps(Future<List<Item>> catalog) {
        io.execute(() -> {
            List<Item> list;
            try {
                list = catalog.get();
            } catch (ExecutionException e) {
                loadApps(null);
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            int fit = appColumns();
            for (Item it : Catalog.visible(cfg, Catalog.APPS, list)) {
                if (fit-- <= 0) break;
                Catalog.art(getPackageManager(), it);
                it.artRequested = true;
            }
            ui.post(() -> {
                if (isDestroyed()) return;
                apps.all = list;
                appsLoaded = true;
                render(apps);
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cfg.refresh();
        if (!Language.signature(this).equals(language)) { recreate(); return; }
        resumed = true;
        Guard.arrived(this);
        cfg.refresh();
        cfg.visited();
        if (cfg.version() != applied) apply();
        else background();
        IntentFilter f = new IntentFilter(Intent.ACTION_TIME_TICK);
        f.addAction(Intent.ACTION_TIME_CHANGED);
        f.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(time, f);
        timeWatching = true;
        tick();
        root.post(() -> {
            restoreSelection();
            followFocus(false);
        });
    }

    @Override
    public void onEnterAnimationComplete() {
        Guard.shown();
    }

    @Override
    protected void onPause() {
        rememberSelection();
        super.onPause();
        resumed = false;
        ui.removeCallbacks(followFocus);
        ui.removeCallbacks(settleFrame);
        if (scrollMotion != null) scrollMotion.cancel();
        if (timeWatching) {
            unregisterReceiver(time);
            timeWatching = false;
        }
        if (moving != null) stopMoving();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(packages);
        if (tv != null && inputWatching) tv.unregisterCallback(inputWatch);
        io.shutdownNow();
        super.onDestroy();
    }

    /**
     * Android 9 keeps a hidden window's GPU context (about 35 MB on this TV) unless memory is
     * critical. Dropping it while an app plays costs one context rebuild on the way back.
     */
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level < TRIM_MEMORY_UI_HIDDEN || level >= TRIM_MEMORY_COMPLETE) return;
        try {
            Class<?> wmg = Class.forName("android.view.WindowManagerGlobal");
            Object global = wmg.getMethod("getInstance").invoke(null);
            wmg.getMethod("trimMemory", int.class).invoke(global, TRIM_MEMORY_COMPLETE);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // not reachable on this build: the context simply stays
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (moving != null) stopMoving();
        if (resumed) restoreSelection();
    }

    @Override
    public void onBackPressed() {
        focusFirst();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (RemoteKeys.shortcut(e.getKeyCode()) && moving != null) stopMoving();
        if (RemoteKeys.dispatch(this, e)) return true;
        if (moving == null) {
            View focused = getCurrentFocus();
            int code = e.getKeyCode();
            if (focused instanceof Tile && focused.getParent() == apps.strip
                    && (code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT)) {
                if (e.getAction() == KeyEvent.ACTION_DOWN) {
                    // Linear app order continues across row boundaries, in either direction.
                    int next = apps.strip.indexOfChild(focused)
                            + (code == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : -1);
                    if (next >= 0 && next < apps.strip.getChildCount())
                        apps.strip.getChildAt(next).requestFocus();
                }
                // At the ends of the entire grid, stay put rather than jump to the header.
                return true;
            }
            return super.dispatchKeyEvent(e);
        }
        if (e.getAction() == KeyEvent.ACTION_DOWN) {
            switch (e.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    shift(-1);
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    shift(1);
                    break;
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (moving.kind.equals(Catalog.APPS)) shift(-appColumns());
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (moving.kind.equals(Catalog.APPS)) shift(appColumns());
                    break;
                default:
                    stopMoving();
            }
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int code, KeyEvent e) {
        if (code == KeyEvent.KEYCODE_MENU) {
            View f = getCurrentFocus();
            if (f instanceof Tile) menu((Tile) f);
            else startActivity(new Intent(this, Options.class));
            return true;
        }
        return super.onKeyDown(code, e);
    }

    private View build() {
        root = new FrameLayout(this) {
            @Override protected boolean drawChild(Canvas canvas, View child, long time) {
                return child == header || super.drawChild(canvas, child, time);
            }

            @Override protected void dispatchDraw(Canvas canvas) {
                super.dispatchDraw(canvas);
                if (header != null) header.draw(canvas);
            }

            @Override
            public void requestChildFocus(View child, View focused) {
                super.requestChildFocus(child, focused);
                if (focused instanceof Tile) {
                    lastKind = ((Tile) focused).kind;
                    lastId = ((Tile) focused).item.id;
                }
                ui.removeCallbacks(followFocus);
                ui.post(followFocus);
                ui.removeCallbacks(settleFrame);
                ui.postDelayed(settleFrame, 280);
            }

            @Override
            public View focusSearch(View focused, int direction) {
                if (direction == View.FOCUS_DOWN && chips.contains(focused)) {
                    View below = nearest(apps.strip, focused);
                    if (below != null) return below;
                }
                return super.focusSearch(focused, direction);
            }
        };
        headerInset = dp(184);
        wall = new ImageView(this);
        wall.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(wall, new FrameLayout.LayoutParams(MATCH, MATCH));
        shade = new View(this);
        root.addView(shade, new FrameLayout.LayoutParams(MATCH, MATCH));

        ScrollView scroll = new ScrollView(this) {
            /** Keep the focused card below the fixed header, including its fade and focus zoom. */
            @Override
            protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
                // Focus-driven scrolling is animated by followFocus(), including key repeats.
                return 0;
            }

            @Override public boolean requestChildRectangleOnScreen(View child, Rect rect,
                    boolean immediate) {
                return false;
            }

            @Override
            protected void onScrollChanged(int x, int y, int oldX, int oldY) {
                super.onScrollChanged(x, y, oldX, oldY);
                refreshFrame(root);
                redrawSurface();
            }

            /**
             * Up and down only move the focus; scrolling follows it. Left to ScrollView, a press
             * with nothing focusable that way scrolls half a screen and leaves rows cut.
             */
            @Override
            public boolean executeKeyEvent(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.KEYCODE_DPAD_UP:
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                    case KeyEvent.KEYCODE_PAGE_UP:
                    case KeyEvent.KEYCODE_PAGE_DOWN:
                    case KeyEvent.KEYCODE_SPACE:
                        return false;
                    default:
                        return super.executeKeyEvent(e);
                }
            }
        };
        scroll.setFocusable(false);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, headerInset, 0, 0);
        page = scroll;
        root.addView(scroll, new FrameLayout.LayoutParams(MATCH, MATCH));

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setClipChildren(false);
        column.setClipToPadding(false);
        column.setPadding(0, 0, 0, dp(32));
        scroll.addView(column, new FrameLayout.LayoutParams(MATCH, WRAP));

        LinearLayout head = new LinearLayout(this);
        header = head;
        // The focused icon grows beyond its layout slot, including into the side padding.
        head.setClipChildren(false);
        head.setClipToPadding(false);
        head.setGravity(Gravity.TOP);
        head.setPadding(dp(56), dp(40), dp(56), dp(16));
        LinearLayout when = new LinearLayout(this);
        when.setOrientation(LinearLayout.VERTICAL);
        clock = Ui.text(this, 52, Ui.TEXT, Ui.LIGHT);
        date = Ui.text(this, 15, Ui.MUTED, Ui.MEDIUM);
        date.setPadding(dp(2), dp(8), 0, 0);
        when.addView(clock);
        when.addView(date);
        head.addView(when, new LinearLayout.LayoutParams(0, WRAP, 1f));
        head.addView(chip(false, v -> startActivity(new Intent(this, Options.class))));
        head.addView(chip(true, v -> openSettings()));

        apps = new Row(Catalog.APPS, "Apps", column);
        column.removeView(apps.title);
        inputs = new Row(Catalog.INPUTS, "Eingänge", column);
        headerFade = new View(this);
        headerFade.setFocusable(false);
        root.addView(headerFade, new FrameLayout.LayoutParams(MATCH, dp(196)));
        root.addView(head, new FrameLayout.LayoutParams(MATCH, WRAP));
        return root;
    }

    /** The child of {@code strip} horizontally closest to {@code from}, or null when empty. */
    private static View nearest(ViewGroup strip, View from) {
        int[] at = new int[2];
        from.getLocationOnScreen(at);
        int x = at[0] + from.getWidth() / 2;
        View best = null;
        int bestGap = Integer.MAX_VALUE;
        int count = strip instanceof GridLayout
                ? Math.min(((GridLayout) strip).getColumnCount(), strip.getChildCount())
                : strip.getChildCount();
        for (int i = 0; i < count; i++) {
            View c = strip.getChildAt(i);
            c.getLocationOnScreen(at);
            int gap = Math.abs(at[0] + c.getWidth() / 2 - x);
            if (gap < bestGap) {
                bestGap = gap;
                best = c;
            }
        }
        return best;
    }

    private TextView chip(boolean settings, View.OnClickListener click) {
        TextView v = new HeaderButton(this, settings);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(18), dp(9), dp(18), dp(9));
        v.setFocusable(true);
        v.setClickable(true);
        v.setOnClickListener(click);
        v.setOnFocusChangeListener((view, has) ->
                view.animate().scaleX(has ? 1.06f : 1f).scaleY(has ? 1.06f : 1f).setDuration(110).start());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(48), dp(42));
        lp.leftMargin = dp(10);
        lp.topMargin = dp(6);
        v.setLayoutParams(lp);
        chips.add(v);
        return v;
    }

    /** Retarget from the current position, never queue animations behind rapid remote presses. */
    private void followFocus(boolean animate) {
        View focused = getCurrentFocus();
        if (!(focused instanceof Tile) || !resumed) {
            if (scrollMotion != null) scrollMotion.cancel();
            return;
        }
        Tile tile = (Tile) focused;
        tile.focusBounds(root, focusBox, page.getScrollY());
        int current = page.getScrollY();
        int target = current;
        if (focusBox.top - current < dp(196)) target = Math.round(focusBox.top) - dp(196);
        else if (focusBox.bottom - current > page.getHeight() - dp(36))
            target = Math.round(focusBox.bottom) - page.getHeight() + dp(36);
        int range = Math.max(0, page.getChildAt(0).getBottom()
                + page.getPaddingBottom() - page.getHeight());
        target = Math.max(0, Math.min(range, target));
        if (scrollMotion != null) scrollMotion.cancel();
        if (!animate) { page.scrollTo(0, target); return; }
        if (target == current) return;
        scrollMotion = ValueAnimator.ofInt(current, target);
        scrollMotion.setDuration(240);
        scrollMotion.setInterpolator(new DecelerateInterpolator());
        scrollMotion.addUpdateListener(a -> page.scrollTo(0, (int) a.getAnimatedValue()));
        scrollMotion.start();
    }

    private static void refreshFrame(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) refreshFrame(group.getChildAt(i));
        }
        view.invalidate();
    }

    /** Refresh the window during motion only; there is no idle redraw loop. */
    private void redrawSurface() {
        root.invalidate();
        getWindow().getDecorView().invalidate();
    }

    private void apply() {
        applied = cfg.version();
        int accent = cfg.accent();
        for (TextView c : chips) Ui.focusStyle(c, accent, 0x14FFFFFF, dp(18));
        render(apps);
        updateInputWatch();
        loadInputs();
        background();
        tick();
    }

    /** Rebuilds the app list, keeping loaded art for every package except {@code changed}. */
    private void loadApps(String changed) {
        int generation = ++appsGeneration;
        io.execute(() -> {
            List<Item> list = Catalog.apps(this);
            ui.post(() -> {
                if (generation != appsGeneration || isDestroyed()) return;
                Map<String, Item> previous = new HashMap<>();
                for (Item old : apps.all) previous.put(old.id, old);
                for (Item fresh : list) {
                    Item old = previous.get(fresh.id);
                    if (old != null && !old.id.equals(changed) && old.artRequested) {
                        fresh.banner = old.banner;
                        fresh.icon = old.icon;
                        fresh.artRequested = true;
                    }
                }
                apps.all = list;
                appsLoaded = true;
                render(apps);
            });
        });
    }

    private void loadInputs() {
        inputsLoaded = true;
        if (!"on".equals(cfg.get(Config.INPUTS))) {
            if (!inputs.all.isEmpty()) inputs.all = new ArrayList<>();
            render(inputs);
            return;
        }
        inputs.all = Catalog.inputs(this, tv);
        render(inputs);
    }

    /** Watches HDMI state only while the inputs row is actually in use. */
    private void updateInputWatch() {
        boolean want = tv != null && "on".equals(cfg.get(Config.INPUTS));
        if (want == inputWatching) return;
        if (want) tv.registerCallback(inputWatch, ui);
        else tv.unregisterCallback(inputWatch);
        inputWatching = want;
    }

    private void render(Row row) {
        if (moving != null && moving.kind.equals(row.kind)) stopMoving();
        View focused = getCurrentFocus();
        String keep = focused instanceof Tile && ((Tile) focused).kind.equals(row.kind)
                ? ((Tile) focused).item.id : null;
        row.strip.removeAllViews();

        boolean enabled = !Catalog.INPUTS.equals(row.kind) || "on".equals(cfg.get(Config.INPUTS));
        List<Item> shown = enabled ? Catalog.visible(cfg, row.kind, row.all) : new ArrayList<>();
        int columns = row.grid ? appColumns() : 0;
        if (row.grid) ((GridLayout) row.strip).setColumnCount(columns);
        int[] size = row.grid ? appCardSize() : cardSize();
        int w = size[0], h = Catalog.APPS.equals(row.kind) ? size[1] : Math.round(size[1] * 0.56f);
        String labels = cfg.get(Config.LABELS);
        int accent = cfg.accent();
        int index = 0, restore = -1;
        for (Item it : shown) {
            Tile t = new Tile(this, it, row.kind, Catalog.label(cfg, it), w, h, labels, accent);
            it.tile = t;
            t.setOnClickListener(v -> open((Tile) v));
            t.setOnLongClickListener(v -> {
                menu((Tile) v);
                return true;
            });
            if (row.grid) {
                GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                lp.width = WRAP;
                lp.height = WRAP;
                lp.rightMargin = index % columns == columns - 1 ? 0 : dp(18);
                lp.bottomMargin = dp(16);
                row.strip.addView(t, lp);
            } else {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WRAP, WRAP);
                lp.rightMargin = dp(18);
                row.strip.addView(t, lp);
            }
            if (it.id.equals(keep)) restore = index;
            index++;
            if (Catalog.APPS.equals(row.kind) && !it.artRequested) loadArt(it);
        }
        row.setVisible(!shown.isEmpty());
        if (restore >= 0) {
            row.strip.getChildAt(restore).requestFocus();
        } else if (keep != null && row.strip.getChildCount() > 0) {
            row.strip.getChildAt(0).requestFocus();
        } else if (getCurrentFocus() == null || !landed) {
            restoreSelection();
        }
    }

    private void loadArt(Item it) {
        it.artRequested = true;
        io.execute(() -> {
            Catalog.art(getPackageManager(), it);
            ui.post(() -> {
                Tile tile = it.tile;
                if (tile != null && tile.getParent() != null) tile.invalidate();
            });
        });
    }

    private int[] cardSize() {
        switch (cfg.get(Config.TILES)) {
            case "small":
                return new int[] {dp(144), dp(81)};
            case "large":
                return new int[] {dp(224), dp(126)};
            default:
                return new int[] {dp(180), dp(101)};
        }
    }

    /** Grid density follows the existing tile-size preference. */
    private int appColumns() {
        switch (cfg.get(Config.TILES)) {
            case "small":
                return 6;
            case "large":
                return 4;
            default:
                return 5;
        }
    }

    /** Fits a complete 16:9 app grid between the same side margins used by the header. */
    private int[] appCardSize() {
        int columns = appColumns();
        int available = getResources().getDisplayMetrics().widthPixels - dp(112)
                - dp(18) * (columns - 1);
        int width = Math.max(dp(96), available / columns);
        return new int[] {width, Math.round(width * 9f / 16f)};
    }

    private void focusFirst() {
        if (scrollMotion != null) scrollMotion.cancel();
        if (page != null) page.scrollTo(0, 0);
        if (apps.scroll != null) apps.scroll.scrollTo(0, 0);
        if (inputs.scroll != null) inputs.scroll.scrollTo(0, 0);
        View first = apps.strip.getChildAt(0);
        if (first == null) first = inputs.strip.getChildAt(0);
        landed = apps.strip.getChildCount() > 0;
        if (first == null) first = chips.get(0);
        first.requestFocus();
    }

    private void rememberSelection() {
        if (lastId == null || lastId.isEmpty()) return;
        getSharedPreferences("selection", MODE_PRIVATE).edit()
                .putString("kind", lastKind).putString("id", lastId).apply();
    }

    private void restoreSelection() {
        Row row = Catalog.INPUTS.equals(lastKind) ? inputs : apps;
        for (int i = 0; i < row.strip.getChildCount(); i++) {
            Tile tile = (Tile) row.strip.getChildAt(i);
            if (tile.item.id.equals(lastId)) {
                landed = true;
                tile.requestFocus();
                root.post(() -> followFocus(false));
                return;
            }
        }
        // Wait for the asynchronous app list rather than overwrite the saved selection.
        if (!landed && (row == apps && !appsLoaded || row == inputs && !inputsLoaded)) return;
        focusFirst();
    }

    private void tick() {
        String mode = cfg.get(Config.CLOCK);
        boolean h24 = "24h".equals(mode) || "auto".equals(mode) && DateFormat.is24HourFormat(this);
        Date now = new Date();
        if (h24) {
            clock.setText(DateFormat.format("HH:mm", now));
        } else {
            SpannableString half = new SpannableString(" " + DateFormat.format("a", now));
            half.setSpan(new RelativeSizeSpan(0.36f), 0, half.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            clock.setText(DateFormat.format("h:mm", now));
            clock.append(half);
        }
        Locale locale = Language.locale(this);
        date.setText(new java.text.SimpleDateFormat(
                DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"), locale).format(now));
        refreshFrame(root);
        // hourly and time-of-day rotations change while the screen stays open; a no-op otherwise
        background();
    }

    private void background() {
        boolean image = "image".equals(cfg.get(Config.BACKGROUND));
        File file = image ? cfg.wallpaper() : null;
        int base = cfg.background();
        root.setBackgroundColor(base);
        int dim = Integer.parseInt(cfg.get(Config.DIM));
        shade.setBackgroundColor(file == null ? Color.TRANSPARENT : Color.argb(dim * 255 / 100, 0, 0, 0));
        int[] fade = file == null
                ? new int[] {base, base, base, base & 0x00FFFFFF}
                : new int[] {0xFF000000, 0xFF000000, 0xFF000000, 0x00000000};
        headerFade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, fade));
        String key = file == null ? null : file.getPath() + ":" + file.lastModified();
        if (key == null ? wallKey == null : key.equals(wallKey)) return;
        wallKey = key;
        if (file == null) {
            wall.setImageDrawable(null);
            return;
        }
        io.execute(() -> {
            Bitmap b = decode(file, getResources().getDisplayMetrics().widthPixels,
                    getResources().getDisplayMetrics().heightPixels);
            ui.post(() -> {
                if (key.equals(wallKey)) wall.setImageBitmap(b);
            });
        });
    }

    private static Bitmap decode(File f, int w, int h) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getPath(), o);
        o.inSampleSize = 1;
        while (o.outWidth / (o.inSampleSize * 2) >= w && o.outHeight / (o.inSampleSize * 2) >= h) {
            o.inSampleSize *= 2;
        }
        o.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(f.getPath(), o);
    }

    /**
     * Fire OS answers the generic settings action with a stub that reopens Amazon's home, which
     * Guard then replaces with ours. The TV settings entry point opens the real settings screen.
     */
    private void openSettings() {
        RemoteKeys.settings(this);
    }

    private void open(Tile t) {
        launch(new Intent(t.item.intent), Catalog.APPS.equals(t.kind) ? t.item.id : null);
    }

    private void launch(Intent i, String pkg) {
        if (Guard.AMAZON.equals(pkg)) Guard.pause();
        try {
            startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, tr("Konnte nicht geöffnet werden"), Toast.LENGTH_SHORT).show();
        }
    }

    private void menu(Tile t) {
        boolean app = Catalog.APPS.equals(t.kind);
        String[] actions = app
                ? new String[] {"Verschieben", "Umbenennen", "Ausblenden", "App-Info"}
                : new String[] {"Verschieben", "Umbenennen", "Ausblenden"};
        for (int i = 0; i < actions.length; i++) actions[i] = tr(actions[i]);
        new AlertDialog.Builder(this, Ui.DIALOG)
                .setTitle(Catalog.label(cfg, t.item))
                .setItems(actions, (d, which) -> {
                    switch (which) {
                        case 0:
                            if (t.getParent() == null) break;
                            moving = t;
                            t.setMoving(true);
                            break;
                        case 1:
                            rename(t);
                            break;
                        case 2:
                            Catalog.setShown(cfg, t.kind, t.item, false);
                            render(app ? apps : inputs);
                            Toast.makeText(this, tr("Ausgeblendet. Unter Gestaltung wieder einblendbar."), Toast.LENGTH_SHORT).show();
                            break;
                        default:
                            launch(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", t.item.id, null)), null);
                    }
                })
                .show();
    }

    private void rename(Tile t) {
        EditText field = new EditText(this);
        field.setSingleLine();
        field.setText(Catalog.label(cfg, t.item));
        field.setSelectAllOnFocus(true);
        FrameLayout box = new FrameLayout(this);
        box.setPadding(dp(24), dp(8), dp(24), 0);
        box.addView(field);
        String key = "name." + t.item.id;
        AlertDialog d = new AlertDialog.Builder(this, Ui.DIALOG)
                .setTitle(tr("Umbenennen"))
                .setView(box)
                .setPositiveButton(tr("Speichern"), (dialog, w) -> {
                    String name = field.getText().toString().trim();
                    cfg.set(key, name.equals(t.item.label.toString()) ? null : name);
                    apply();
                })
                .setNeutralButton(tr("Original"), (dialog, w) -> {
                    cfg.set(key, null);
                    apply();
                })
                .setNegativeButton(tr("Abbrechen"), null)
                .create();
        d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        d.show();
    }

    private void shift(int by) {
        ViewGroup strip = (ViewGroup) moving.getParent();
        int from = strip.indexOfChild(moving), to = from + by;
        if (to < 0 || to >= strip.getChildCount()) return;
        ViewGroup.LayoutParams lp = moving.getLayoutParams();
        strip.removeViewAt(from);
        strip.addView(moving, to, lp);
        moving.requestFocus();
    }

    private void stopMoving() {
        Tile t = moving;
        moving = null;
        t.setMoving(false);
        ViewGroup strip = (ViewGroup) t.getParent();
        if (strip == null) return;
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < strip.getChildCount(); i++) ids.add(((Tile) strip.getChildAt(i)).item.id);
        Catalog.saveOrder(cfg, t.kind, ids);
        applied = cfg.version();
        root.post(() -> followFocus(false));
    }

    private int dp(float v) {
        return Ui.dp(this, v);
    }

    private String tr(String text) { return Language.text(this, text); }
}
