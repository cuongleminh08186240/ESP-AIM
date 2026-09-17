package FakePingZzz.Plus;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import FakePingZzz.Plus.MainActivity.NvAuth;
import FakePingZzz.Plus.MainActivity.NvVerify;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MediaRenderService extends Service {

    private WindowManager wm;

    private View fxPanel;
    private View vxPanel;
    private View gxPanel;
    private View txPanel;

    private TextView fxBtn;
    private TextView vxBtn;
    private TextView gxBtn;
    private TextView txBtn;

    private WindowManager.LayoutParams fxLp;
    private WindowManager.LayoutParams vxLp;
    private WindowManager.LayoutParams gxLp;
    private WindowManager.LayoutParams txLp;

    private MediaPlayer mpOn;
    private MediaPlayer mpOff;

    private boolean fxOn = false;
    private Handler freezeTimerHandler = new Handler(Looper.getMainLooper());
    private Runnable freezeTimerRunnable;
    private boolean fxTmr = false;
    private int freezeTimerGeneration = 0;

    private boolean vxOn = false;
    private boolean gxOn = false;
    private boolean txOn = false;

    // Chặn trạng thái cũ từ service/animation ghi đè UI ngay sau khi người dùng vừa bấm.
    private static final long TOGGLE_SYNC_GRACE_MS = 900L;
    private long freezePendingUntil = 0L;
    private long ghostPendingUntil = 0L;
    private boolean fxWant = false;
    private boolean gxWant = false;

    private View tmView;
    private WindowManager.LayoutParams tmLp;
    private boolean treMaTranAdded = false;

    // ==================== MENU NỔI ====================
    private View mxBtn;
    private WindowManager.LayoutParams mxLp;
    private View mxPanel;
    private WindowManager.LayoutParams mpLp;
    private boolean menuPanelAdded = false;
    private boolean menuOpen = false;

    // ==================== TÂM ẢO ====================
    private AimView aimView;
    private WindowManager.LayoutParams aimLp;
    private boolean aimAdded = false;
    private boolean aimVisible = false;

    // Tuỳ chỉnh tâm ảo
    private int aimColor = 0xFFFFFFFF;
    // Styles: 0=Cross, 1=Dot, 2=Circle+, 3=Sniper, 4=Diamond, 5=Star, 6=Triangle, 7=Reticle,
    //         8=Spin360, 9=SpinDNA, 10=SpinGalaxy, 11=SpinPulse, 12=SpinFlower, 13=SpinPortal
    //         14=SpinBracket, 15=SpinDoubleX, 16=SpinParallel, 17=SpinArrowCross,
    //         18=SpinTriArc, 19=SpinRainbow, 20=SpinStar4, 21=SpinComet,
    //         22=SpinNova, 23=SpinHelix, 24=SpinClock, 25=SpinCrystal,
    //         26=SpinVortex, 27=SpinWeb, 28=SpinOmega, 29=SpinAurora
    private int aimStyle = 0;
    private int aimSize = 60;
    private int aimThickness = 4;

    // Spin animation — 2 góc xoay ngược nhau cho các tâm phức tạp
    private Handler spinHandler = new Handler(Looper.getMainLooper());
    private Runnable spinRunnable;
    private float spinAngle  = 0f;   // xoay thuận
    private float spinAngle2 = 0f;   // xoay ngược (slower)
    private float spinAngle3 = 0f;   // xoay nhanh (pulse)
    private boolean isSpinning = false;

    private SharedPreferences prefs;

    // FIX: track destroy state to prevent post-destroy crashes
    private volatile boolean isServiceDestroyed = false;

    public static final int SIZE_DEFAULT = 130;
    public static final int VPN_SIZE_DEFAULT = SIZE_DEFAULT / 2;

    private static final int UI_BLACK = 0xFF0A1018;
    private static final int UI_BLACK_SOFT = 0xFF162131;
    private static final int UI_GREEN = 0xFF00E676;
    private static final int UI_GRAY_SOFT = 0x336AA8FF;
    private static final int UI_WHITE = 0xFFF7FAFF;
    private static final int UI_WHITE_SOFT = 0xCCE1EBF8;
    private static final int UI_PANEL = 0xF2141D2B;
    private static final int UI_PANEL_2 = 0xF01B2A3D;
    private static final int UI_ACCENT = 0xFF7AAEFF;
    private static final int UI_ACCENT_2 = 0xFFB388FF;
    private static final int UI_DANGER = 0xFFFF6B8A;

    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if (intent == null) return;

            // SECURITY FIX: Chỉ chấp nhận broadcast từ chính package của app.
            // Kẻ crack có thể gửi broadcast giả từ app khác để bypass key check.
            // Android không cho biết sender package trong BroadcastReceiver thông thường,
            // nên ta dùng permission-based protection: chỉ VPN service mới có thể
            // set extra "_t" khớp với secret lưu trong prefs.
            String token = intent.getStringExtra("_t");
            String expectedToken = prefs != null ? prefs.getString("_xt", "") : "";
            if (expectedToken.isEmpty() || !expectedToken.equals(token)) {
                // Broadcast không có token hoặc token sai → bỏ qua
                return;
            }

            vxOn = intent.getBooleanExtra("_r", false);
            m_uvb();

            // Sync real Freeze state from VPN service.
            // This prevents the overlay showing OFF while the VPN service is still ON.
            boolean serviceFreeze = intent.getBooleanExtra("freeze", fxOn);
            if (!m_isfb(serviceFreeze) && fxOn != serviceFreeze) {
                fxOn = serviceFreeze;
                fxWant = serviceFreeze;
                m_afb();
                if (fxOn) {
                    m_sfat();
                } else {
                    m_cfat();
                }
            }

            boolean serviceGhost = intent.getBooleanExtra("ghost", gxOn);
            if (!m_isgb(serviceGhost) && gxOn != serviceGhost) {
                gxOn = serviceGhost;
                gxWant = serviceGhost;
                m_agb();
            }
        }
    };

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter filter = new IntentFilter(GameBoosterVpnService.BC_SYNC);
        registerReceiverCompat(statusReceiver, filter);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        try { mpOn = MediaPlayer.create(this, R.raw.on); } catch (Exception e) { mpOn = null; }
        try { mpOff = MediaPlayer.create(this, R.raw.off); } catch (Exception e) { mpOff = null; }

        prefs = getSharedPreferences("cfg", 0);
        prefs.edit().putBoolean("sw_v", false).apply();
        if (!SecurityManager.performSecurityCheck(this)) {
            stopSelf();
            return;
        }

        // FIX: Only add overlay views if permission is granted
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        // Không yêu cầu login/key; khởi tạo overlay trực tiếp.
        m_iov();
    }

    /** Khởi tạo overlay views — chỉ gọi sau khi key đã xác nhận hợp lệ */
    private void m_iov() {
        aimColor = prefs.getInt("ac_c", 0xFFFF3333);
        aimStyle = prefs.getInt("ac_s", 0);
        aimSize  = prefs.getInt("ac_z", 60);
        aimThickness = prefs.getInt("ac_t", 4);

        m_sob();
        m_ctm();
        m_cmb();
        createAimView();
        syncAimWithPrefs();
    }

    /**
     * Kiểm tra key async — nếu hợp lệ thì mới init overlay.
     * Nếu không hợp lệ → stopSelf().
     */
    

    private void registerReceiverCompat(BroadcastReceiver receiver, IntentFilter filter) {
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                Context.class
                        .getMethod("registerReceiver", BroadcastReceiver.class, IntentFilter.class, int.class)
                        .invoke(this, receiver, filter, 4);
                return;
            } catch (Exception ignored) {}
        }
        registerReceiver(receiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Cho phép qua nếu là lệnh UPDATE (không phải start mới)
        boolean isUpdateAction = intent != null && intent.getAction() != null
                && (intent.getAction().startsWith("UPDATE_") || "rk_sync".equals(intent.getAction()));
        if (!isUpdateAction) {
            if (!LibLoader.isLoaded()) {
                android.util.Log.e("MediaRenderService", "Dual-lib missing — stop");
                stopSelf();
                return START_NOT_STICKY;
            }
            if (!LibLoader.safeIsFloatAllowed()) {
                android.util.Log.e("MediaRenderService", "LibLoader FloatAllowed=false — stop");
                stopSelf();
                return START_NOT_STICKY;
            }
        }
        // ─────────────────────────────────────────────────────────
        if (intent != null && intent.getAction() != null) {

            if ("UPDATE_SIZE_FREEZE".equals(intent.getAction())) {
                int sz = intent.getIntExtra("size", getFreezeSize());
                prefs.edit().putInt("sf_z", sz).apply();
                updateViewSize(fxPanel, fxLp, sz);
            }

            if ("UPDATE_SIZE_GHOST".equals(intent.getAction())) {
                int sz = intent.getIntExtra("size", getGhostSize());
                prefs.edit().putInt("sg_z", sz).apply();
                updateViewSize(gxPanel, gxLp, sz);
            }

            if ("UPDATE_SIZE_TELE".equals(intent.getAction())) {
                int sz = intent.getIntExtra("size", getTeleSize());
                prefs.edit().putInt("st_z", sz).apply();
                updateViewSize(txPanel, txLp, sz);
            }

            if ("UPDATE_SIZE_VPN".equals(intent.getAction())) {
                int sz = intent.getIntExtra("size", getVpnSize());
                prefs.edit().putInt("sv_z", sz).apply();
                updateViewSize(vxPanel, vxLp, sz);
            }

            if ("UPDATE_ALPHA_FREEZE".equals(intent.getAction())) {
                int a = intent.getIntExtra("alpha", 100);
                prefs.edit().putInt("af_a", a).apply();
                if (fxPanel != null) {
                    try { fxPanel.setAlpha(a / 100f); } catch (Exception ignored) {}
                }
            }

            if ("UPDATE_ALPHA_GHOST".equals(intent.getAction())) {
                int a = intent.getIntExtra("alpha", 100);
                prefs.edit().putInt("ag_a", a).apply();
                if (gxPanel != null) {
                    try { gxPanel.setAlpha(a / 100f); } catch (Exception ignored) {}
                }
            }

            if ("UPDATE_ALPHA_TELE".equals(intent.getAction())) {
                int a = intent.getIntExtra("alpha", 100);
                prefs.edit().putInt("at_a", a).apply();
                if (txPanel != null) {
                    try { txPanel.setAlpha(a / 100f); } catch (Exception ignored) {}
                }
            }

            if ("UPDATE_ALPHA_VPN".equals(intent.getAction())) {
                int a = intent.getIntExtra("alpha", 100);
                prefs.edit().putInt("av_a", a).apply();
                if (vxPanel != null) {
                    try { vxPanel.setAlpha(a / 100f); } catch (Exception ignored) {}
                }
            }

            if ("UPDATE_TOGGLE_PEN".equals(intent.getAction())) {
                boolean enabled = intent.getBooleanExtra("enabled", false);
                prefs.edit().putBoolean("sw_p", enabled).apply();
                syncAimWithPrefs();
            }

            if ("UPDATE_TOGGLE_FREEZE".equals(intent.getAction())) {
                boolean enabled = intent.getBooleanExtra("enabled", true);
                prefs.edit().putBoolean("sw_f", enabled).apply();
                if (!enabled && fxOn) {
                    // If Freeze button is disabled in Settings, force VPN Freeze OFF too.
                    setFreezeState(false);
                }
                m_sob();
            }

            if ("UPDATE_FREEZE_TIME".equals(intent.getAction())) {
                int offMs = intent.hasExtra("off_ms")
                        ? sanitizeFreezeTimerMs(intent.getIntExtra("off_ms", getFreezeAutoOffMs()))
                        : sanitizeFreezeTimerMs(intent.getIntExtra("off_sec", Math.max(1, Math.round(getFreezeAutoOffMs() / 1000f))) * 1000);
                prefs.edit()
                        .putInt("fat_ms", offMs)
                        .putInt("fat_s", Math.max(1, Math.round(offMs / 1000f)))
                        .putInt("fan_s", 0)
                        .apply();
                if (fxTmr) m_sfat();
            }

            if ("UPDATE_TOGGLE_GHOST".equals(intent.getAction())) {
                boolean enabled = intent.getBooleanExtra("enabled", true);
                prefs.edit().putBoolean("sw_g", enabled).apply();
                if (!enabled && gxOn) {
                    setGhostState(false);
                }
                m_sob();
            }

            if ("UPDATE_TOGGLE_TELE".equals(intent.getAction())) {
                boolean enabled = intent.getBooleanExtra("enabled", true);
                prefs.edit().putBoolean("sw_t", enabled).apply();
                m_sob();
            }

            if ("UPDATE_TOGGLE_VPN".equals(intent.getAction())) {
                // VPN không có nút nổi riêng: khi nhấn khởi chạy ở MainActivity, VPN tự ON.
                // Giữ action này để tương thích bản cũ nhưng luôn ẩn nút VPN.
                prefs.edit().putBoolean("sw_v", false).apply();
                m_hvb();
            }

            if ("UPDATE_SOUND2".equals(intent.getAction())) {
                boolean on = intent.getBooleanExtra("sound2", false);
                setTreMaTranVisible(on);
            }

            if ("UPDATE_TELE_SPEED".equals(intent.getAction())) {
                int speed = intent.getIntExtra("ts_v", 3);
                prefs.edit().putInt("ts_v", speed).apply();
                Intent i = new Intent(this, GameBoosterVpnService.class);
                i.setAction("UPDATE_TELE_SPEED");
                i.putExtra("ts_v", speed);
                startService(i);
            }

            if ("UPDATE_REDUCE_FPS_DROP".equals(intent.getAction())) {
                boolean enabled = intent.getBooleanExtra("enabled", true);
                prefs.edit().putBoolean("sw_rf", enabled).apply();
                if (isSpinning) {
                    stopSpin();
                    startSpin();
                }
                refreshAim();
            }
        }
        return START_STICKY;
    }

    private void updateViewSize(View v, WindowManager.LayoutParams p, int sz) {
        if (v == null || p == null) return;
        p.width = sz;
        p.height = sz;
        if (v instanceof TextView) {
            applyFloatingTextStyle((TextView) v, ((TextView) v).getText().toString(), sz);
        }
        clampParamsToScreen(p, v);
        if (!v.isAttachedToWindow()) return;
        try { wm.updateViewLayout(v, p); } catch (Exception ignored) {}
    }

    private String getAndroidId() {
        return Settings.Secure.getString(
            getContentResolver(),
            Settings.Secure.ANDROID_ID
        );
    }

    private void runOnUiThread(Runnable action) {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            action.run();
        } else {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(action);
        }
    }

    private boolean m_rvk() {
        return SecurityManager.performSecurityCheck(this);
    }

    private boolean isViewAttached(View view) {
        return view != null && view.isAttachedToWindow();
    }

    private void addFloatingViewWithAppear(final View view, WindowManager.LayoutParams params, final float finalAlpha) {
        if (view == null || params == null) return;
        try {
            view.animate().cancel();
            view.clearAnimation();
            view.setAlpha(0f);
            view.setScaleX(0.18f);
            view.setScaleY(0.18f);
            view.setPivotX(params.width > 0 ? params.width / 2f : 0f);
            view.setPivotY(params.height > 0 ? params.height / 2f : 0f);
            view.setLayerType(View.LAYER_TYPE_HARDWARE, null);

            wm.addView(view, params);

            view.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        view.setPivotX(view.getWidth() / 2f);
                        view.setPivotY(view.getHeight() / 2f);
                        view.animate()
                                .alpha(finalAlpha)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(230)
                                .setInterpolator(new android.view.animation.OvershootInterpolator(1.15f))
                                .withEndAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            view.setAlpha(finalAlpha);
                                            view.setScaleX(1f);
                                            view.setScaleY(1f);
                                            view.setLayerType(View.LAYER_TYPE_NONE, null);
                                        } catch (Exception ignored) {}
                                    }
                                })
                                .start();
                    } catch (Exception e) {
                        try {
                            view.setAlpha(finalAlpha);
                            view.setScaleX(1f);
                            view.setScaleY(1f);
                            view.setLayerType(View.LAYER_TYPE_NONE, null);
                        } catch (Exception ignored) {}
                    }
                }
            });
        } catch (Exception ignored) {
            try {
                view.setAlpha(finalAlpha);
                view.setScaleX(1f);
                view.setScaleY(1f);
                view.setLayerType(View.LAYER_TYPE_NONE, null);
            } catch (Exception e) {}
        }
    }

    private void applyToggleButtonState(final TextView btn, final boolean enabled, boolean animate) {
        if (btn == null) return;

        Object oldState = btn.getTag();
        boolean hasOldState = oldState instanceof Boolean;
        boolean stateUnchanged = hasOldState && ((Boolean) oldState).booleanValue() == enabled;
        if (!hasOldState) animate = false;
        btn.setTag(Boolean.valueOf(enabled));

        final int finalColor = enabled ? UI_GREEN : UI_BLACK;

        try {
            // Hủy mọi animation cũ trước khi set trạng thái mới để tránh nháy ON rồi mới OFF.
            btn.animate().cancel();
            btn.clearAnimation();
            setSafeCircleBackground(btn, finalColor);
            btn.setTextColor(UI_WHITE);
            btn.setTranslationY(0f);

            if (stateUnchanged || !animate || !isViewAttached(btn)) {
                btn.setScaleX(1f);
                btn.setScaleY(1f);
                btn.setLayerType(View.LAYER_TYPE_NONE, null);
                return;
            }

            btn.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            btn.setPivotX(btn.getWidth() > 0 ? btn.getWidth() / 2f : 0f);
            btn.setPivotY(btn.getHeight() > 0 ? btn.getHeight() / 2f : 0f);

            // Màu được set ngay lập tức, chỉ animate scale nhẹ nên UI không bị delay trạng thái.
            // Không phóng lớn hơn khung overlay, vì WindowManager sẽ cắt mất 4 góc.
            // Chỉ thu nhỏ nhẹ rồi trả về 1f để vẫn mượt mà không bị lỏm góc.
            btn.setScaleX(0.94f);
            btn.setScaleY(0.94f);
            btn.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationY(0f)
                    .setDuration(170)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(0.85f))
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                // Chỉ chốt lại nếu trạng thái hiện tại vẫn là trạng thái của lần bấm này.
                                Object nowState = btn.getTag();
                                if (nowState instanceof Boolean
                                        && ((Boolean) nowState).booleanValue() == enabled) {
                                    setSafeCircleBackground(btn, finalColor);
                                    btn.setScaleX(1f);
                                    btn.setScaleY(1f);
                                    btn.setTranslationY(0f);
                                    btn.setLayerType(View.LAYER_TYPE_NONE, null);
                                }
                            } catch (Exception ignored) {}
                        }
                    })
                    .start();
        } catch (Exception e) {
            try {
                setSafeCircleBackground(btn, finalColor);
                btn.setScaleX(1f);
                btn.setScaleY(1f);
                btn.setTranslationY(0f);
                btn.setTextColor(UI_WHITE);
                btn.setLayerType(View.LAYER_TYPE_NONE, null);
            } catch (Exception ignored) {}
        }
    }

    private boolean m_isfb(boolean serviceFreeze) {
        long now = System.currentTimeMillis();
        if (now < freezePendingUntil && serviceFreeze != fxWant) {
            return true;
        }
        if (serviceFreeze == fxWant) {
            freezePendingUntil = 0L;
        }
        return false;
    }

    private boolean m_isgb(boolean serviceGhost) {
        long now = System.currentTimeMillis();
        if (now < ghostPendingUntil && serviceGhost != gxWant) {
            return true;
        }
        if (serviceGhost == gxWant) {
            ghostPendingUntil = 0L;
        }
        return false;
    }

    private boolean isPenEnabled() {
        return prefs().getBoolean("sw_p", false);
    }

    private boolean m_ife() {
        return prefs().getBoolean("sw_f", true);
    }

    private boolean m_ige() {
        return prefs().getBoolean("sw_g", true);
    }

    private boolean m_ite() {
        return prefs().getBoolean("sw_t", true);
    }

    private boolean isVpnEnabled() {
        return prefs().getBoolean("sw_v", true);
    }

    private void syncAimWithPrefs() {
        if (isPenEnabled()) showAim();
        else hideAim();
    }

    private void m_sob() {
        if (m_ife()) m_sfb();
        else m_hfb();

        if (m_ige()) m_sgb();
        else m_hgb();

        if (m_ite()) m_stb();
        else m_htb();

        // VPN tự chạy khi bấm Khởi chạy, không hiển thị thành nút nổi riêng.
        m_hvb();
    }

    private void m_sfb() {
        if (fxPanel == null || fxLp == null) {
            m_cfb();
            return;
        }
        if (!isViewAttached(fxPanel)) {
            try {
                addFloatingViewWithAppear(fxPanel, fxLp, m_gfa() / 100f);
            } catch (Exception ignored) {}
        }
    }

    private void m_hfb() {
        m_cfat();
        if (isViewAttached(fxPanel)) {
            try { wm.removeView(fxPanel); } catch (Exception ignored) {}
        }
    }

    private void m_sgb() {
        if (gxPanel == null || gxLp == null) {
            m_cgb();
            return;
        }
        if (!isViewAttached(gxPanel)) {
            try {
                addFloatingViewWithAppear(gxPanel, gxLp, m_gga() / 100f);
            } catch (Exception ignored) {}
        }
    }

    private void m_hgb() {
        if (isViewAttached(gxPanel)) {
            try { wm.removeView(gxPanel); } catch (Exception ignored) {}
        }
    }

    private void m_stb() {
        if (txPanel == null || txLp == null) {
            m_ctb();
            return;
        }
        if (!isViewAttached(txPanel)) {
            try {
                addFloatingViewWithAppear(txPanel, txLp, m_gta() / 100f);
            } catch (Exception ignored) {}
        }
    }

    private void m_htb() {
        if (isViewAttached(txPanel)) {
            try { wm.removeView(txPanel); } catch (Exception ignored) {}
        }
    }

    private void m_svb() {
        if (vxPanel == null || vxLp == null) {
            m_cvb();
            return;
        }
        if (!isViewAttached(vxPanel)) {
            try {
                addFloatingViewWithAppear(vxPanel, vxLp, m_gva() / 100f);
            } catch (Exception ignored) {}
        }
    }

    private void m_hvb() {
        if (isViewAttached(vxPanel)) {
            try { wm.removeView(vxPanel); } catch (Exception ignored) {}
        }
    }

    // ================= FREEZE =================
    private void m_cfb() {
        fxBtn = new TextView(this);
        fxBtn.setText("Freeze");
        fxBtn.setGravity(Gravity.CENTER);
        fxBtn.setTextColor(UI_WHITE);
        setSafeCircleBackground(fxBtn, UI_BLACK);
        applyFloatingTextStyle(fxBtn, "Freeze", getFreezeSize());
        fxBtn.setTag(Boolean.valueOf(fxOn));
        applyPressFeedback(fxBtn, 0.94f);
        fxPanel = fxBtn;

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        fxLp = new WindowManager.LayoutParams(
                getFreezeSize(), getFreezeSize(), type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        fxLp.gravity = Gravity.TOP | Gravity.LEFT;
        fxLp.x = prefs.getInt("fx_x", 100);
        fxLp.y = prefs.getInt("fx_y", 550);
        clampParamsToScreen(fxLp, fxPanel);

        if (fxPanel != null) fxPanel.setAlpha(m_gfa() / 100f);
        try {
            if (!isViewAttached(fxPanel)) addFloatingViewWithAppear(fxPanel, fxLp, m_gfa() / 100f);
        } catch (Exception ignored) {}

        fxBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (m_rvk()) toggleFreeze();
            }
        });

        fxPanel.setOnTouchListener(new DragTouch(fxLp, "fx_x", "fx_y"));
    }

    private void toggleFreeze() {
        setFreezeState(!fxOn);
    }

    private void setFreezeState(boolean enabled) {
        // Cancel old timer first so an old callback cannot toggle Freeze later.
        m_cfat();

        fxWant = enabled;
        freezePendingUntil = System.currentTimeMillis() + TOGGLE_SYNC_GRACE_MS;

        if (fxOn == enabled) {
            m_afb();
            if (enabled) m_sfat();
            return;
        }

        // Cập nhật UI ngay trước, service xử lý sau để không có cảm giác delay/nháy trạng thái cũ.
        fxOn = enabled;
        m_afb();
        play(fxOn ? mpOn : mpOff);
        if (fxOn) m_sfat();

        Intent i = new Intent(this, GameBoosterVpnService.class);
        i.setAction(GameBoosterVpnService.A_F1);
        i.putExtra("enabled", enabled); // explicit state, not blind toggle
        startService(i);
    }

    private void m_afb() {
        applyToggleButtonState(fxBtn, fxOn, true);
    }

    private int sanitizeFreezeTimerMs(int value) {
        if (value < 1000) return 1000;
        if (value > 10000) return 10000;
        int step = 500;
        int rounded = Math.round(value / (float) step) * step;
        if (rounded < 1000) return 1000;
        if (rounded > 10000) return 10000;
        return rounded;
    }

    private int getFreezeAutoOffMs() {
        int fallbackMs = sanitizeFreezeTimerMs(prefs().getInt("fat_s", 3) * 1000);
        return sanitizeFreezeTimerMs(prefs().getInt("fat_ms", fallbackMs));
    }

    private void removeFreezeAutoCallback() {
        freezeTimerGeneration++;
        if (freezeTimerRunnable != null) {
            try { freezeTimerHandler.removeCallbacks(freezeTimerRunnable); } catch (Exception ignored) {}
            freezeTimerRunnable = null;
        }
    }

    private void m_cfat() {
        removeFreezeAutoCallback();
        fxTmr = false;
    }

    private void m_sfat() {
        removeFreezeAutoCallback();

        // Only schedule while Freeze is ON. When time is up, it turns OFF only.
        if (!fxOn) {
            fxTmr = false;
            return;
        }

        int delayMs = getFreezeAutoOffMs();
        final int timerToken = freezeTimerGeneration;

        fxTmr = true;
        freezeTimerRunnable = new Runnable() {
            @Override
            public void run() {
                if (timerToken != freezeTimerGeneration) return;
                if (isServiceDestroyed || !m_ife()) {
                    m_cfat();
                    return;
                }
                if (fxOn) setFreezeState(false);
            }
        };
        freezeTimerHandler.postDelayed(freezeTimerRunnable, delayMs);
    }

    // ================= VPN =================
    private void m_cvb() {
        vxBtn = new TextView(this);
        vxBtn.setText("VPN");
        vxBtn.setGravity(Gravity.CENTER);
        vxBtn.setTextColor(UI_WHITE);
        setSafeCircleBackground(vxBtn, UI_BLACK);
        applyFloatingTextStyle(vxBtn, "VPN", getVpnSize());
        vxBtn.setTag(Boolean.valueOf(vxOn));
        vxPanel = vxBtn;

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        vxLp = new WindowManager.LayoutParams(
                getVpnSize(), getVpnSize(), type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        vxLp.gravity = Gravity.TOP | Gravity.LEFT;
        vxLp.x = prefs.getInt("vx_x", 260);
        vxLp.y = prefs.getInt("vx_y", 600);
        clampParamsToScreen(vxLp, vxPanel);

        if (vxPanel != null) vxPanel.setAlpha(m_gva() / 100f);
        clampParamsToScreen(vxLp, vxPanel);
        addFloatingViewWithAppear(vxPanel, vxLp, m_gva() / 100f);

        vxBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (m_rvk()) m_tvp();
            }
        });

        vxPanel.setOnTouchListener(new DragTouch(vxLp, "vx_x", "vx_y"));
    }

    private void m_tvp() {
        if (!vxOn) {
            Intent prepare = android.net.VpnService.prepare(this);
            if (prepare != null) {
                Intent broadcast = new Intent("FakePingZzz.Plus.md.PERM");
                sendBroadcast(broadcast);
            } else {
                m_svd();
            }
        } else {
            Intent i = new Intent(this, GameBoosterVpnService.class);
            i.setAction(GameBoosterVpnService.A_SX);
            startService(i);
            play(mpOff);
        }
    }

    private void m_svd() {
        if (!LibLoader.isLoaded()) {
            android.util.Log.e("MediaRenderService", "m_svd blocked: dual-lib missing");
            return;
        }
        if (!LibLoader.safeIsVpnAllowed()) {
            android.util.Log.e("MediaRenderService", "m_svd blocked: LibLoader VpnAllowed=false");
            return;
        }
        Intent i = new Intent(this, GameBoosterVpnService.class);
        i.setAction(GameBoosterVpnService.A_S1);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
        else startService(i);
        play(mpOn);
    }

    private void m_uvb() {
        applyToggleButtonState(vxBtn, vxOn, true);
    }

    // ================= GHOST =================
    private void m_cgb() {
        gxBtn = new TextView(this);
        gxBtn.setText("Ghost");
        gxBtn.setGravity(Gravity.CENTER);
        gxBtn.setTextColor(UI_WHITE);
        setSafeCircleBackground(gxBtn, UI_BLACK);
        applyFloatingTextStyle(gxBtn, "Ghost", getGhostSize());
        gxBtn.setTag(Boolean.valueOf(gxOn));
        applyPressFeedback(gxBtn, 0.94f);
        gxPanel = gxBtn;

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        gxLp = new WindowManager.LayoutParams(
                getGhostSize(), getGhostSize(), type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        gxLp.gravity = Gravity.TOP | Gravity.LEFT;
        gxLp.x = prefs.getInt("gx_x", 100);
        gxLp.y = prefs.getInt("gx_y", 700);
        clampParamsToScreen(gxLp, gxPanel);

        if (gxPanel != null) gxPanel.setAlpha(m_gga() / 100f);
        try {
            if (!isViewAttached(gxPanel)) addFloatingViewWithAppear(gxPanel, gxLp, m_gga() / 100f);
        } catch (Exception ignored) {}

        gxBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (m_rvk()) toggleGhost();
            }
        });

        gxPanel.setOnTouchListener(new DragTouch(gxLp, "gx_x", "gx_y"));
    }

    private void toggleGhost() {
        setGhostState(!gxOn);
    }

    private void setGhostState(boolean enabled) {
        gxWant = enabled;
        ghostPendingUntil = System.currentTimeMillis() + TOGGLE_SYNC_GRACE_MS;

        if (gxOn == enabled) {
            m_agb();
            return;
        }

        // Cập nhật UI ngay trước, tránh broadcast cũ làm hiện ON lại trong nửa giây.
        gxOn = enabled;
        m_agb();
        play(gxOn ? mpOn : mpOff);

        Intent i = new Intent(this, GameBoosterVpnService.class);
        i.setAction(GameBoosterVpnService.A_G1);
        i.putExtra("enabled", enabled); // explicit state, not blind toggle
        startService(i);
    }

    private void m_agb() {
        applyToggleButtonState(gxBtn, gxOn, true);
    }

    // ================= TELE =================
    private void m_ctb() {
        txBtn = new TextView(this);
        txBtn.setText("TeleKill");
        txBtn.setGravity(Gravity.CENTER);
        txBtn.setTextColor(UI_WHITE);
        setSafeCircleBackground(txBtn, UI_BLACK);
        applyFloatingTextStyle(txBtn, "TeleKill", getTeleSize());
        txBtn.setTag(Boolean.valueOf(txOn));
        applyPressFeedback(txBtn, 0.94f);
        txPanel = txBtn;

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        txLp = new WindowManager.LayoutParams(
                getTeleSize(), getTeleSize(), type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        txLp.gravity = Gravity.TOP | Gravity.LEFT;
        txLp.x = prefs.getInt("tx_x", 100);
        txLp.y = prefs.getInt("tx_y", 850);
        clampParamsToScreen(txLp, txPanel);

        if (txPanel != null) txPanel.setAlpha(m_gta() / 100f);
        try {
            if (!isViewAttached(txPanel)) addFloatingViewWithAppear(txPanel, txLp, m_gta() / 100f);
        } catch (Exception ignored) {}

        txBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (m_rvk()) toggleTele();
            }
        });

        txPanel.setOnTouchListener(new DragTouch(txLp, "tx_x", "tx_y"));
    }

    private void toggleTele() {
        Intent i = new Intent(this, GameBoosterVpnService.class);
        i.setAction(GameBoosterVpnService.A_TK);
        startService(i);

        txOn = !txOn;
        applyToggleButtonState(txBtn, txOn, true);
        play(txOn ? mpOn : mpOff);
    }

    // ================= TRE MÃ TRẬN BAR =================
    private void m_ctm() {
        tmView = new View(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setColor(0x883B4F68);
        bg.setCornerRadius(12);
        bg.setStroke(2, 0x66D5E0ED);
        tmView.setBackgroundDrawable(bg);

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        tmLp = new WindowManager.LayoutParams(
                300, 12, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        tmLp.gravity = Gravity.TOP | Gravity.LEFT;
        tmLp.x = prefs.getInt("tm_x", 20);
        tmLp.y = prefs.getInt("tm_y", 0);

        tmView.setClickable(true);
        final DragTouch dt = new DragTouch(tmLp, "tm_x", "tm_y");
        tmView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                dt.onTouch(v, e);
                return true;
            }
        });

        if (prefs.getBoolean("sound2", true)) {
            wm.addView(tmView, tmLp);
            treMaTranAdded = true;
        }
    }

    private void setTreMaTranVisible(boolean show) {
        if (show && !treMaTranAdded && tmView != null) {
            try { wm.addView(tmView, tmLp); treMaTranAdded = true; } catch (Exception ignored) {}
        } else if (!show && treMaTranAdded && tmView != null) {
            try { wm.removeView(tmView); treMaTranAdded = false; } catch (Exception ignored) {}
        }
    }

    // ==================== MENU NỔI (BÁNH RĂNG) ====================

    private void m_cmb() {
        TextView menuBtn = new TextView(this);
        menuBtn.setText("⚙");
        menuBtn.setGravity(Gravity.CENTER);
        menuBtn.setTextColor(UI_WHITE);
        menuBtn.setTextSize(20);

        GradientDrawable menuBg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF203A61, 0xFF12233D});
        menuBg.setShape(GradientDrawable.OVAL);
        menuBg.setStroke(2, UI_WHITE_SOFT);
        menuBtn.setBackgroundDrawable(menuBg);
        applyPressFeedback(menuBtn, 0.94f);

        mxBtn = menuBtn;

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        mxLp = new WindowManager.LayoutParams(
                74, 74, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        mxLp.gravity = Gravity.TOP | Gravity.LEFT;
        mxLp.x = prefs.getInt("mx_x", 10);
        mxLp.y = prefs.getInt("mx_y", 200);
        clampParamsToScreen(mxLp, mxBtn);

        addFloatingViewWithAppear(mxBtn, mxLp, 1f);

        mxBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (menuOpen) closeMenuPanel();
                else openMenuPanel();
            }
        });

        mxBtn.setOnTouchListener(new DragTouch(mxLp, "mx_x", "mx_y"));
    }

    // ==================== MỞ MENU PANEL ====================
    private void openMenuPanel() {
        if (menuPanelAdded) return;
        menuOpen = true;

        // Panel ngoài với nền tối, bo góc
        FrameLayout panel = new FrameLayout(this) {
            @Override
            protected void dispatchDraw(Canvas canvas) {
                // Nền tối gradient
                Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                bgPaint.setColor(0xF0121822);
                RectF rect = new RectF(0, 0, getWidth(), getHeight());
                canvas.drawRoundRect(rect, 28, 28, bgPaint);

                // Viền gradient tím-xanh
                Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                borderPaint.setStyle(Paint.Style.STROKE);
                borderPaint.setStrokeWidth(2.5f);
                borderPaint.setColor(0xFF4A6A9A);
                RectF borderRect = new RectF(1.25f, 1.25f, getWidth() - 1.25f, getHeight() - 1.25f);
                canvas.drawRoundRect(borderRect, 27, 27, borderPaint);

                super.dispatchDraw(canvas);
            }
        };

        // ScrollView để cuộn nội dung
        ScrollView scrollView = new ScrollView(this);
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER_HORIZONTAL);
        inner.setPadding(16, 12, 16, 12);

        // ---- TIÊU ĐỀ MENU ----
                TextView title = makeMenuTitle("⚙  BẢNG ĐIỀU KHIỂN");
        inner.addView(title);
        inner.addView(makeDivider(UI_WHITE_SOFT));

        // ---- ĐIỀU KHIỂN NÚT NỔI ----
        inner.addView(makeSectionLabel("NÚT NỔI"));

        final TextView lockToggle = makeWideToggleButton(
                isLocked() ? "🔒  KHOÁ NÚT" : "🔓  MỞ NÚT",
                isLocked());
        styleLockButton(lockToggle, isLocked());
        lockToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean locked = !isLocked();
                prefs.edit().putBoolean("lock", locked).apply();
                lockToggle.setText(locked ? "🔒  KHOÁ NÚT" : "🔓  MỞ NÚT");
                styleLockButton(lockToggle, locked);
                play(locked ? mpOn : mpOff);
            }
        });
        inner.addView(lockToggle);

        LinearLayout toggleRow1 = makeMenuRow();
        toggleRow1.addView(makeFeatureToggle("Freeze", "sw_f", m_ife()));
        toggleRow1.addView(makeFeatureToggle("Ghost", "sw_g", m_ige()));
        inner.addView(toggleRow1);

        LinearLayout toggleRow2 = makeMenuRow();
        toggleRow2.addView(makeFeatureToggle("Tele", "sw_t", m_ite()));
        inner.addView(toggleRow2);

        inner.addView(makeSliderRow("KÍCH THƯỚC NÚT", 70, 190, getOverlayButtonSize(), "px",
                new SliderChangeListener() {
                    @Override
                    public void onChanged(int value) {
                        setOverlayButtonSize(value);
                    }
                }));

        inner.addView(makeSliderRow("ĐỘ MỜ NÚT", 25, 100, getOverlayButtonAlpha(), "%",
                new SliderChangeListener() {
                    @Override
                    public void onChanged(int value) {
                        setOverlayButtonAlpha(value);
                    }
                }));

        inner.addView(makeSpace(4));
        inner.addView(makeDivider(0x33FFFFFF));

        // ---- TIÊU ĐỀ TÂM ẢO ----
        TextView aimTitle = makeMenuTitle("TÂM ẢO");
        inner.addView(aimTitle);
        inner.addView(makeDivider(UI_WHITE_SOFT));

        // ---- BẬT/TẮT TÂM ẢO ----
        final TextView aimToggle = makeMenuLabel(
                aimVisible ? "● TÂM ẢO:  BẬT" : "○ TÂM ẢO:  TẮT",
                aimVisible ? UI_WHITE : UI_WHITE_SOFT, 13);
        aimToggle.setPadding(12, 8, 12, 8);
        styleRoundButton(aimToggle, aimVisible ? UI_GREEN : UI_BLACK_SOFT,
                aimVisible ? UI_WHITE : 0xFF425268);
        aimToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aimVisible = !aimVisible;
                prefs.edit().putBoolean("sw_p", aimVisible).apply();
                aimToggle.setText(aimVisible ? "● TÂM ẢO:  BẬT" : "○ TÂM ẢO:  TẮT");
                aimToggle.setTextColor(aimVisible ? UI_WHITE : UI_WHITE_SOFT);
                styleRoundButton(aimToggle, aimVisible ? UI_GREEN : UI_BLACK_SOFT,
                        aimVisible ? UI_WHITE : 0xFF425268);
                if (aimVisible) showAim();
                else hideAim();
            }
        });
        inner.addView(aimToggle);

        // Margin
        inner.addView(makeSpace(6));

        // ---- KIỂU TÂM ẢO ----
        inner.addView(makeSectionLabel("KIỂU TÂM"));

        // Row 1: Cross, Dot, Circle+, Sniper
        // Row 2: Diamond, Star, Triangle, Reticle
        // Row 3: Spin360, SpinDNA, SpinGalaxy, SpinPulse
        // Row 4: SpinFlower, SpinPortal
        String[] styleNames  = {"✛",  "●",  "◎",  "⊕",  "◇",  "★",  "▲",  "⌖",
                                 "↻",  "⚛",  "🌀", "◉",  "✿",  "⊛",
                                 "{·}", "XX", "||",  "=>",  "△↻", "Rbow", "✴",  "☄",
                                 "✸",  "⟡",  "⏱",  "♦",  "〜", "❋",  "Ω",  "∿"};
        String[] styleTips   = {"Cross","Dot","Ring","Sniper","Diamond","Star","Triangle","Reticle",
                                 "Spin360","SpinDNA","Galaxy","Pulse","Flower","Portal",
                                 "Bracket","DoubleX","Parallel","ArrowX","TriArc","Rainbow","Star4","Comet",
                                 "Nova","Helix","Clock","Crystal","Vortex","Web","Omega","Aurora"};
        int cols = 4;
        int rows = (int) Math.ceil(styleNames.length / (double) cols);

        // Tạo grid
        for (int row = 0; row < rows; row++) {
            LinearLayout styleRow = new LinearLayout(this);
            styleRow.setOrientation(LinearLayout.HORIZONTAL);
            styleRow.setGravity(Gravity.CENTER);
            for (int col = 0; col < cols; col++) {
                int s = row * cols + col;
                if (s >= styleNames.length) break;
                final int styleIdx = s;
                final TextView styleBtn = makeStyleButton(styleNames[s], styleTips[s], aimStyle == s);
                styleBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        aimStyle = styleIdx;
                        prefs.edit().putInt("ac_s", aimStyle).apply();
                        // Dừng spin nếu đổi sang style tĩnh
                        if (aimStyle < 8) stopSpin();
                        else startSpin();
                        refreshAim();
                        // Cập nhật highlight tất cả style button
                        closeMenuPanel();
                        openMenuPanel();
                    }
                });
                styleRow.addView(styleBtn);
            }
            inner.addView(styleRow);
        }

        inner.addView(makeSpace(4));
        inner.addView(makeDivider(0x33FFFFFF));

        // ---- MÀU TÂM ẢO ----
        inner.addView(makeSectionLabel("MÀU SẮC"));

        final LinearLayout colorRow = new LinearLayout(this);
        colorRow.setOrientation(LinearLayout.HORIZONTAL);
        colorRow.setGravity(Gravity.CENTER);

        final int[] colors    = {0xFFFF3333, 0xFF00E676, 0xFF2196F3, 0xFFFFEB3B,
                                  0xFFFF6D00, 0xFFFFFFFF, 0xFFE040FB, 0xFF00E5FF};
        final String[] cNames = {"Đỏ","Xanh lá","Xanh biển","Vàng",
                                  "Cam","Trắng","Tím","Cyan"};

        for (int c = 0; c < colors.length; c++) {
            final int col = colors[c];
            View dot = new View(this);
            GradientDrawable dotBg = new GradientDrawable();
            dotBg.setShape(GradientDrawable.OVAL);
            dotBg.setColor(col);
            dotBg.setStroke(aimColor == col ? 3 : 0, 0xFFFFFFFF);
            dot.setBackgroundDrawable(dotBg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(32, 32);
            lp.setMargins(5, 2, 5, 2);
            dot.setLayoutParams(lp);
            dot.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    aimColor = col;
                    prefs.edit().putInt("ac_c", aimColor).apply();
                    refreshAim();
                    for (int k = 0; k < colorRow.getChildCount(); k++) {
                        GradientDrawable d = new GradientDrawable();
                        d.setShape(GradientDrawable.OVAL);
                        d.setColor(colors[k]);
                        d.setStroke(aimColor == colors[k] ? 3 : 0, 0xFFFFFFFF);
                        colorRow.getChildAt(k).setBackgroundDrawable(d);
                    }
                }
            });
            colorRow.addView(dot);
        }
        inner.addView(colorRow);

        inner.addView(makeSpace(4));
        inner.addView(makeDivider(0x33FFFFFF));

        // ---- KÍCH THƯỚC ----
        inner.addView(makeSectionLabel("KÍCH THƯỚC"));
        final TextView sizeVal = makeMenuLabel("" + aimSize, UI_WHITE, 13);

        LinearLayout sizeCtrl = new LinearLayout(this);
        sizeCtrl.setOrientation(LinearLayout.HORIZONTAL);
        sizeCtrl.setGravity(Gravity.CENTER);

        TextView sizeDec = makeCtrlButton("−", UI_BLACK_SOFT);
        sizeDec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (aimSize > 20) {
                    aimSize -= 5;
                    prefs.edit().putInt("ac_z", aimSize).apply();
                    sizeVal.setText("" + aimSize);
                    refreshAim();
                }
            }
        });

        TextView sizeInc = makeCtrlButton("+", UI_BLACK_SOFT);
        sizeInc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (aimSize < 200) {
                    aimSize += 5;
                    prefs.edit().putInt("ac_z", aimSize).apply();
                    sizeVal.setText("" + aimSize);
                    refreshAim();
                }
            }
        });

        sizeCtrl.addView(sizeDec);
        LinearLayout.LayoutParams valLp = new LinearLayout.LayoutParams(60,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        valLp.gravity = Gravity.CENTER_VERTICAL;
        sizeVal.setLayoutParams(valLp);
        sizeCtrl.addView(sizeVal);
        sizeCtrl.addView(sizeInc);
        inner.addView(sizeCtrl);

        // ---- ĐỘ DÀY ----
        inner.addView(makeSectionLabel("ĐỘ DÀY NÉT"));
        final TextView thickVal = makeMenuLabel("" + aimThickness, UI_WHITE, 13);

        LinearLayout thickCtrl = new LinearLayout(this);
        thickCtrl.setOrientation(LinearLayout.HORIZONTAL);
        thickCtrl.setGravity(Gravity.CENTER);

        TextView thickDec = makeCtrlButton("−", UI_BLACK_SOFT);
        thickDec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (aimThickness > 1) {
                    aimThickness--;
                    prefs.edit().putInt("ac_t", aimThickness).apply();
                    thickVal.setText("" + aimThickness);
                    refreshAim();
                }
            }
        });

        TextView thickInc = makeCtrlButton("+", UI_BLACK_SOFT);
        thickInc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (aimThickness < 12) {
                    aimThickness++;
                    prefs.edit().putInt("ac_t", aimThickness).apply();
                    thickVal.setText("" + aimThickness);
                    refreshAim();
                }
            }
        });

        thickCtrl.addView(thickDec);
        thickVal.setLayoutParams(valLp);
        thickCtrl.addView(thickVal);
        thickCtrl.addView(thickInc);
        inner.addView(thickCtrl);

        inner.addView(makeSpace(6));
        inner.addView(makeDivider(0x33FFFFFF));
        inner.addView(makeSpace(4));

        // ---- NÚT ĐÓNG ----
        TextView closeBtn = new TextView(this);
        closeBtn.setText("✕   ĐÓNG");
        closeBtn.setTextColor(UI_WHITE_SOFT);
        closeBtn.setTextSize(12);
        closeBtn.setGravity(Gravity.CENTER);
        closeBtn.setPadding(20, 10, 20, 10);
        closeBtn.setLetterSpacing(0.12f);
        styleRoundButton(closeBtn, 0xFF1A2235, 0xFF3A5070);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeMenuPanel();
            }
        });
        inner.addView(closeBtn);

        scrollView.addView(inner);

        FrameLayout.LayoutParams scrollLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        panel.addView(scrollView, scrollLp);

        mxPanel = panel;

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        mpLp = new WindowManager.LayoutParams(
                360, 620, type,
                // FIX: Remove FLAG_NOT_FOCUSABLE so ScrollView and buttons receive touch/focus properly
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        mpLp.gravity = Gravity.TOP | Gravity.LEFT;
        mpLp.x = mxLp.x + 86;
        mpLp.y = mxLp.y - 70;
        clampParamsToScreen(mpLp, mxPanel);

        try {
            addFloatingViewWithAppear(mxPanel, mpLp, 1f);
            menuPanelAdded = true;
        } catch (Exception e) {
            // FIX: ensure menuPanelAdded stays false if addView fails
            menuPanelAdded = false;
            mxPanel = null;
        }}

    private void closeMenuPanel() {
        menuOpen = false;
        if (menuPanelAdded && mxPanel != null) {
            final View oldPanel = mxPanel;
            menuPanelAdded = false;
            mxPanel = null;
            try {
                // Đóng ngay lập tức để tránh panel nháy/chớp do fade/scale animation còn chạy.
                oldPanel.animate().cancel();
                oldPanel.clearAnimation();
                oldPanel.setAlpha(1f);
                oldPanel.setScaleX(1f);
                oldPanel.setScaleY(1f);
                oldPanel.setLayerType(View.LAYER_TYPE_NONE, null);
                wm.removeView(oldPanel);
            } catch (Exception e) {
                try { wm.removeView(oldPanel); } catch (Exception ignored) {}
            }
        }
    }

    // ==================== SPIN 360° ====================
    private void startSpin() {
        if (isSpinning) return;
        isSpinning = true;
        spinRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isSpinning) return;
                spinAngle  = (spinAngle  + 3.0f) % 360f;   // thuận ~60fps
                spinAngle2 = (spinAngle2 - 1.8f + 360f) % 360f; // ngược chậm
                spinAngle3 = (spinAngle3 + 5.5f) % 360f;   // nhanh (pulse/petal)
                refreshAim();
                spinHandler.postDelayed(this, getSpinFrameDelayMs()); // adaptive fps
            }
        };
        spinHandler.post(spinRunnable);
    }

    private int getSpinFrameDelayMs() {
        return prefs() != null && prefs().getBoolean("sw_rf", true) ? 24 : 16;
    }

    private void stopSpin() {
        isSpinning = false;
        if (spinRunnable != null) {
            spinHandler.removeCallbacks(spinRunnable);
            spinRunnable = null;
        }
        spinAngle  = 0f;
        spinAngle2 = 0f;
        spinAngle3 = 0f;
    }

    // ==================== UI HELPERS ====================

    private void applyPressFeedback(final View view, final float pressedScale) {
        if (view == null) return;
        view.setClickable(true);
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(pressedScale).scaleY(pressedScale).alpha(0.94f).translationY(1f).setDuration(80).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1f).scaleY(1f).alpha(1f).translationY(0f).setDuration(150).start();
                        break;
                }
                return false;
            }
        });
    }

    private interface SliderChangeListener {
        void onChanged(int value);
    }

    private LinearLayout makeMenuRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, 2, 0, 2);
        return row;
    }

    private TextView makeWideToggleButton(String text, boolean enabled) {
        TextView chip = makeMenuLabel(text, UI_WHITE, 12);
        chip.setGravity(Gravity.CENTER);
        chip.setSingleLine(true);
        chip.setEllipsize(TextUtils.TruncateAt.END);
        chip.setIncludeFontPadding(false);
        chip.setPadding(10, 9, 10, 9);
        styleToggleChip(chip, enabled);
        applyPressFeedback(chip, 0.96f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(12, 4, 12, 5);
        chip.setLayoutParams(lp);
        return chip;
    }

    private TextView makeFeatureToggle(final String title, final String prefKey, boolean enabled) {
        final TextView chip = makeMenuLabel((enabled ? "●  " : "○  ") + title, UI_WHITE, 12);
        chip.setGravity(Gravity.CENTER);
        chip.setSingleLine(true);
        chip.setEllipsize(TextUtils.TruncateAt.END);
        chip.setIncludeFontPadding(false);
        chip.setPadding(8, 9, 8, 9);
        styleToggleChip(chip, enabled);
        applyPressFeedback(chip, 0.96f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(138,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(5, 4, 5, 4);
        chip.setLayoutParams(lp);
        chip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean next = !prefs().getBoolean(prefKey, true);
                prefs.edit().putBoolean(prefKey, next).apply();
                chip.setText((next ? "●  " : "○  ") + title);
                styleToggleChip(chip, next);
                if ("sw_f".equals(prefKey) && !next && fxOn) setFreezeState(false);
                if ("sw_g".equals(prefKey) && !next && gxOn) setGhostState(false);
                m_sob();
                play(next ? mpOn : mpOff);
            }
        });
        return chip;
    }

    private void styleToggleChip(TextView tv, boolean enabled) {
        if (tv == null) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(22);
        bg.setColor(enabled ? UI_GREEN : UI_BLACK_SOFT);
        bg.setStroke(2, enabled ? UI_WHITE_SOFT : 0xFF425268);
        tv.setTextColor(enabled ? UI_WHITE : UI_WHITE_SOFT);
        tv.setBackgroundDrawable(bg);
    }

    /** Style nút khoá/mở — đỏ khi khoá, xanh khi mở */
    private void styleLockButton(TextView tv, boolean locked) {
        if (tv == null) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(22);
        if (locked) {
            bg.setColor(0xFF2A1525);
            bg.setStroke(2, UI_DANGER);
            tv.setTextColor(UI_DANGER);
        } else {
            bg.setColor(UI_GREEN);
            bg.setStroke(2, UI_WHITE_SOFT);
            tv.setTextColor(UI_WHITE);
        }
        tv.setBackgroundDrawable(bg);
    }

    private LinearLayout makeSliderRow(String label, int min, int max, int value,
                                       final String suffix, final SliderChangeListener listener) {
        final int safeMin = min;
        final int safeMax = Math.max(max, min + 1);
        int safeValue = clampInt(value, safeMin, safeMax);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(12, 6, 12, 7);
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x661D2B40, 0x44101824});
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(20);
        bg.setStroke(1, 0x334D6585);
        box.setBackgroundDrawable(bg);
        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        boxLp.setMargins(12, 5, 12, 5);
        box.setLayoutParams(boxLp);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView name = makeMenuLabel(label, UI_WHITE_SOFT, 10);
        name.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        name.setSingleLine(true);
        name.setIncludeFontPadding(false);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        top.addView(name, nameLp);

        final TextView val = makeMenuLabel(safeValue + suffix, UI_WHITE, 12);
        val.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        val.setSingleLine(true);
        val.setIncludeFontPadding(false);
        top.addView(val, new LinearLayout.LayoutParams(78,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        box.addView(top);

        SeekBar seek = new SeekBar(this);
        seek.setMax(safeMax - safeMin);
        seek.setProgress(safeValue - safeMin);
        seek.setPadding(0, 2, 0, 0);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int real = safeMin + progress;
                val.setText(real + suffix);
                if (fromUser && listener != null) listener.onChanged(real);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (listener != null) listener.onChanged(safeMin + seekBar.getProgress());
            }
        });
        box.addView(seek, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        return box;
    }

    private int getOverlayButtonSize() {
        return clampInt(prefs().getInt("sf_z", SIZE_DEFAULT), 70, 190);
    }

    private void setOverlayButtonSize(int value) {
        int sz = clampInt(value, 70, 190);
        prefs.edit()
                .putInt("sf_z", sz)
                .putInt("sg_z", sz)
                .putInt("st_z", sz)
                .apply();
        updateViewSize(fxPanel, fxLp, sz);
        updateViewSize(gxPanel, gxLp, sz);
        updateViewSize(txPanel, txLp, sz);
    }

    private int getOverlayButtonAlpha() {
        return clampInt(prefs().getInt("af_a", 100), 25, 100);
    }

    private void setOverlayButtonAlpha(int value) {
        int a = clampInt(value, 25, 100);
        prefs.edit()
                .putInt("af_a", a)
                .putInt("ag_a", a)
                .putInt("at_a", a)
                .apply();
        applyOverlayAlpha(fxPanel, a);
        applyOverlayAlpha(gxPanel, a);
        applyOverlayAlpha(txPanel, a);
    }

    private void applyOverlayAlpha(View view, int alpha) {
        if (view == null) return;
        try { view.setAlpha(clampInt(alpha, 25, 100) / 100f); } catch (Exception ignored) {}
    }

    private void applyFloatingTextStyle(TextView tv, String label, int buttonSize) {
        if (tv == null) return;
        tv.setText(label);
        tv.setGravity(Gravity.CENTER);
        tv.setSingleLine(true);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        tv.setIncludeFontPadding(false);
        tv.setPadding(4, 0, 4, 0);
        float textSize;
        if (buttonSize <= 60) textSize = 9f;
        else if (buttonSize <= 85) textSize = 10f;
        else if (buttonSize <= 110) textSize = label.length() > 5 ? 11f : 12f;
        else if (buttonSize <= 145) textSize = label.length() > 5 ? 13f : 15f;
        else textSize = label.length() > 5 ? 15f : 17f;
        tv.setTextSize(textSize);
        try { tv.setShadowLayer(5f, 0f, 1.2f, 0xAA000000); } catch (Exception ignored) {}
    }

    private int clampInt(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    private void clampParamsToScreen(WindowManager.LayoutParams p, View view) {
        if (p == null || wm == null) return;
        try {
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(dm);
            int w = p.width > 0 ? p.width : (view != null && view.getWidth() > 0 ? view.getWidth() : 1);
            int h = p.height > 0 ? p.height : (view != null && view.getHeight() > 0 ? view.getHeight() : 1);
            int maxX = Math.max(0, dm.widthPixels - w);
            int maxY = Math.max(0, dm.heightPixels - h);
            p.x = clampInt(p.x, 0, maxX);
            p.y = clampInt(p.y, 0, maxY);
        } catch (Exception ignored) {}
    }

    /** Tiêu đề menu lớn */
    private TextView makeMenuTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(UI_WHITE);
        tv.setTextSize(15);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, 8, 0, 8);
        tv.setLetterSpacing(0.12f);
        try { tv.setShadowLayer(6f, 0f, 1f, 0x88000000); } catch (Exception ignored) {}
        return tv;
    }

    /** Label section nhỏ */
    private TextView makeSectionLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(UI_ACCENT);
        tv.setTextSize(10);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, 8, 0, 4);
        tv.setLetterSpacing(0.2f);
        try { tv.setShadowLayer(4f, 0f, 1f, 0x44000000); } catch (Exception ignored) {}
        return tv;
    }

    /** Label text thông thường */
    private TextView makeMenuLabel(String text, int color, float size) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(size);
        tv.setGravity(Gravity.CENTER);
        tv.setClickable(true);
        return tv;
    }

    /** Style Button bo góc */
    private void styleRoundButton(TextView tv, int bgColor, int borderColor) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(20);
        bg.setColor(bgColor);
        bg.setStroke(2, borderColor);
        tv.setBackgroundDrawable(bg);
    }

    /** Nút chọn kiểu tâm ảo */
    private TextView makeStyleButton(String icon, String tip, boolean selected) {
        TextView btn = new TextView(this);
        btn.setText(icon);
        btn.setTextSize(selected ? 20 : 17);
        btn.setGravity(Gravity.CENTER);
        btn.setTextColor(selected ? UI_WHITE : UI_WHITE_SOFT);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(14);
        bg.setColor(selected ? UI_GREEN : UI_BLACK_SOFT);
        bg.setStroke(2, selected ? UI_WHITE_SOFT : 0xFF415064);
        btn.setBackgroundDrawable(bg);
        applyPressFeedback(btn, 0.96f);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(62, 54);
        lp.setMargins(4, 3, 4, 3);
        btn.setLayoutParams(lp);
        return btn;
    }

    /** Nút +/- điều chỉnh */
    private TextView makeCtrlButton(String text, int color) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextColor(UI_WHITE);
        btn.setTextSize(20);
        btn.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(16);
        bg.setColor(color);
        bg.setStroke(2, UI_WHITE_SOFT);
        btn.setBackgroundDrawable(bg);
        applyPressFeedback(btn, 0.94f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(44, 44);
        lp.setMargins(8, 4, 8, 4);
        btn.setLayoutParams(lp);
        return btn;
    }

    /** Đường kẻ ngang */
    private View makeDivider(int color) {
        View div = new View(this);
        div.setBackgroundColor(color);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        lp.setMargins(16, 4, 16, 4);
        div.setLayoutParams(lp);
        return div;
    }

    /** Khoảng cách */
    private View makeSpace(int dp) {
        View space = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp);
        space.setLayoutParams(lp);
        return space;
    }

    // ==================== TÂM ẢO (AimView) ====================

    /**
     * Custom view vẽ tâm ảo đẹp.
     * Styles: 0=Cross, 1=Dot, 2=Circle+, 3=Sniper,
     *         4=Diamond, 5=Star, 6=Triangle, 7=Reticle, 8=Spin360°
     */
    private class AimView extends View {
        Paint paint     = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint paintGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint paintFill = new Paint(Paint.ANTI_ALIAS_FLAG);

        AimView(Context ctx) { super(ctx); }

        @Override
        protected void onDraw(Canvas canvas) {
            int cx   = getWidth() / 2;
            int cy   = getHeight() / 2;
            int half = aimSize / 2;
            float thick = aimThickness;
            int gap = aimSize / 8;

            // --- paint chính ---
            paint.setColor(aimColor);
            paint.setStrokeWidth(thick);
            paint.setStyle(Paint.Style.STROKE);

            // --- paint glow ---
            int glowColor = (aimColor & 0x00FFFFFF) | 0x40000000;
            paintGlow.setColor(glowColor);
            paintGlow.setStrokeWidth(thick + 8);
            paintGlow.setStyle(Paint.Style.STROKE);
            paintGlow.setAlpha(70);

            // --- paint fill ---
            paintFill.setColor(aimColor);
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setAntiAlias(true);

            switch (aimStyle) {

                // ── 0: CROSS (dấu + với khoảng trống giữa) ──────────────────
                case 0:
                    canvas.drawLine(cx - half, cy, cx - gap, cy, paintGlow);
                    canvas.drawLine(cx + gap,  cy, cx + half, cy, paintGlow);
                    canvas.drawLine(cx, cy - half, cx, cy - gap, paintGlow);
                    canvas.drawLine(cx, cy + gap,  cx, cy + half, paintGlow);

                    canvas.drawLine(cx - half, cy, cx - gap, cy, paint);
                    canvas.drawLine(cx + gap,  cy, cx + half, cy, paint);
                    canvas.drawLine(cx, cy - half, cx, cy - gap, paint);
                    canvas.drawLine(cx, cy + gap,  cx, cy + half, paint);

                    canvas.drawCircle(cx, cy, thick * 0.8f, paintFill);
                    break;

                // ── 1: DOT (chấm tròn) ────────────────────────────────────────
                case 1:
                    paintFill.setAlpha(255);
                    canvas.drawCircle(cx, cy, half / 2.5f, paintFill);
                    paint.setAlpha(120);
                    canvas.drawCircle(cx, cy, half * 0.85f, paint);
                    paint.setAlpha(255);
                    break;

                // ── 2: CIRCLE+ (vòng + 4 chấm) ───────────────────────────────
                case 2:
                    canvas.drawCircle(cx, cy, half * 0.7f, paintGlow);
                    canvas.drawCircle(cx, cy, half * 0.7f, paint);
                    canvas.drawCircle(cx,            cy - (int)(half * 0.7f), thick, paintFill);
                    canvas.drawCircle(cx,            cy + (int)(half * 0.7f), thick, paintFill);
                    canvas.drawCircle(cx - (int)(half * 0.7f), cy,            thick, paintFill);
                    canvas.drawCircle(cx + (int)(half * 0.7f), cy,            thick, paintFill);
                    break;

                // ── 3: SNIPER (vòng ngoài + cross dài + chấm) ────────────────
                case 3:
                    canvas.drawCircle(cx, cy, half,        paintGlow);
                    canvas.drawCircle(cx, cy, half,        paint);
                    canvas.drawCircle(cx, cy, half * 0.35f, paint);
                    canvas.drawLine(cx - half, cy, cx + half, cy, paint);
                    canvas.drawLine(cx, cy - half, cx, cy + half, paint);
                    canvas.drawCircle(cx, cy, thick * 0.6f, paintFill);
                    break;

                // ── 4: DIAMOND (hình thoi) ────────────────────────────────────
                case 4:
                    Path diamond = new Path();
                    diamond.moveTo(cx,        cy - half);
                    diamond.lineTo(cx + half, cy);
                    diamond.lineTo(cx,        cy + half);
                    diamond.lineTo(cx - half, cy);
                    diamond.close();
                    canvas.drawPath(diamond, paintGlow);
                    canvas.drawPath(diamond, paint);
                    // chấm tâm
                    canvas.drawCircle(cx, cy, thick, paintFill);
                    // 4 vạch từ góc vào giữa (gap)
                    canvas.drawLine(cx, cy - half, cx, cy - gap, paint);
                    canvas.drawLine(cx, cy + half, cx, cy + gap, paint);
                    canvas.drawLine(cx - half, cy, cx - gap, cy, paint);
                    canvas.drawLine(cx + half, cy, cx + gap, cy, paint);
                    break;

                // ── 5: STAR (ngôi sao 6 cánh) ────────────────────────────────
                case 5:
                    Path star = new Path();
                    int pts6 = 6;
                    float outerR = half;
                    float innerR = half * 0.45f;
                    for (int i = 0; i < pts6 * 2; i++) {
                        float r   = (i % 2 == 0) ? outerR : innerR;
                        float ang = (float)(Math.PI / pts6 * i - Math.PI / 2);
                        float sx  = cx + r * (float) Math.cos(ang);
                        float sy  = cy + r * (float) Math.sin(ang);
                        if (i == 0) star.moveTo(sx, sy);
                        else        star.lineTo(sx, sy);
                    }
                    star.close();
                    paintFill.setAlpha(60);
                    canvas.drawPath(star, paintFill);
                    paintFill.setAlpha(255);
                    canvas.drawPath(star, paint);
                    canvas.drawCircle(cx, cy, thick, paintFill);
                    break;

                // ── 6: TRIANGLE (tam giác hướng lên) ─────────────────────────
                case 6:
                    Path tri = new Path();
                    tri.moveTo(cx,             cy - half);
                    tri.lineTo(cx + half * 0.87f, cy + half * 0.5f);
                    tri.lineTo(cx - half * 0.87f, cy + half * 0.5f);
                    tri.close();
                    paintFill.setAlpha(50);
                    canvas.drawPath(tri, paintFill);
                    paintFill.setAlpha(255);
                    canvas.drawPath(tri, paint);
                    // cross nhỏ bên trong
                    canvas.drawLine(cx - gap * 0.8f, cy + gap, cx + gap * 0.8f, cy + gap, paint);
                    canvas.drawCircle(cx, cy + gap, thick * 0.7f, paintFill);
                    break;

                // ── 7: RETICLE (ô ngắm chuyên nghiệp) ───────────────────────
                case 7:
                    // Vòng ngoài
                    canvas.drawCircle(cx, cy, half,        paintGlow);
                    canvas.drawCircle(cx, cy, half,        paint);
                    // Vòng giữa
                    canvas.drawCircle(cx, cy, half * 0.5f, paint);
                    // 4 nét từ vòng ngoài vào vòng giữa
                    canvas.drawLine(cx - half,         cy, cx - (int)(half * 0.55f), cy, paint);
                    canvas.drawLine(cx + (int)(half * 0.55f), cy, cx + half,         cy, paint);
                    canvas.drawLine(cx, cy - half,         cx, cy - (int)(half * 0.55f), paint);
                    canvas.drawLine(cx, cy + (int)(half * 0.55f), cx, cy + half,         paint);
                    // 4 vạch ngắn chéo 45° trên vòng giữa
                    float d45 = (float)(half * 0.5f / Math.sqrt(2));
                    float dd  = thick * 2;
                    canvas.drawLine(cx - d45 - dd, cy - d45 - dd, cx - d45 + dd, cy - d45 + dd, paint);
                    canvas.drawLine(cx + d45 - dd, cy - d45 - dd, cx + d45 + dd, cy - d45 + dd, paint);
                    canvas.drawLine(cx - d45 - dd, cy + d45 - dd, cx - d45 + dd, cy + d45 + dd, paint);
                    canvas.drawLine(cx + d45 - dd, cy + d45 - dd, cx + d45 + dd, cy + d45 + dd, paint);
                    // chấm tâm
                    canvas.drawCircle(cx, cy, thick * 0.7f, paintFill);
                    break;

                // ── 8: SPIN 360° (xoay hình tròn) ────────────────────────────
                case 8:
                    canvas.save();
                    canvas.rotate(spinAngle, cx, cy);

                    // Vòng ngoài + đứt đoạn xoay
                    Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    arcPaint.setColor(aimColor);
                    arcPaint.setStrokeWidth(thick);
                    arcPaint.setStyle(Paint.Style.STROKE);

                    RectF oval = new RectF(cx - half, cy - half, cx + half, cy + half);
                    // Vẽ 4 cung hở (mỗi cung 80°, cách nhau 10°)
                    for (int i = 0; i < 4; i++) {
                        canvas.drawArc(oval, i * 90f + 5f, 80f, false, arcPaint);
                    }

                    // Vòng trong nhỏ (xoay ngược)
                    canvas.restore();
                    canvas.save();
                    canvas.rotate(-spinAngle * 1.5f, cx, cy);

                    RectF innerOval = new RectF(cx - half * 0.5f, cy - half * 0.5f,
                                                 cx + half * 0.5f, cy + half * 0.5f);
                    arcPaint.setStrokeWidth(thick * 0.7f);
                    for (int i = 0; i < 3; i++) {
                        canvas.drawArc(innerOval, i * 120f + 10f, 100f, false, arcPaint);
                    }

                    canvas.restore();

                    // Cross cố định (không xoay)
                    canvas.drawLine(cx - gap * 2, cy, cx - gap, cy, paint);
                    canvas.drawLine(cx + gap,     cy, cx + gap * 2, cy, paint);
                    canvas.drawLine(cx, cy - gap * 2, cx, cy - gap, paint);
                    canvas.drawLine(cx, cy + gap,     cx, cy + gap * 2, paint);

                    // Chấm tâm
                    canvas.drawCircle(cx, cy, thick * 0.9f, paintFill);
                    break;

                // ── 9: SPIN DNA (2 vòng xoay ngược, đan chéo nhau) ──────────
                case 9: {
                    Paint dna = new Paint(Paint.ANTI_ALIAS_FLAG);
                    dna.setStyle(Paint.Style.STROKE);
                    dna.setStrokeCap(Paint.Cap.ROUND);

                    // Vòng ngoài thuận – 3 cung 100°
                    dna.setColor(aimColor);
                    dna.setStrokeWidth(thick);
                    dna.setAlpha(230);
                    canvas.save();
                    canvas.rotate(spinAngle, cx, cy);
                    RectF r1 = new RectF(cx - half, cy - half, cx + half, cy + half);
                    for (int i = 0; i < 3; i++) {
                        canvas.drawArc(r1, i * 120f, 90f, false, dna);
                    }
                    canvas.restore();

                    // Vòng giữa ngược – 3 cung 80°
                    dna.setAlpha(180);
                    dna.setStrokeWidth(thick * 0.8f);
                    int a2 = (aimColor & 0x00FFFFFF) | 0xFF000000;
                    // màu bù (invert nhẹ)
                    dna.setColor(((~aimColor) & 0x00FFFFFF) | 0xFF000000);
                    canvas.save();
                    canvas.rotate(spinAngle2, cx, cy);
                    RectF r2 = new RectF(cx - half * 0.7f, cy - half * 0.7f,
                                          cx + half * 0.7f, cy + half * 0.7f);
                    for (int i = 0; i < 3; i++) {
                        canvas.drawArc(r2, i * 120f + 60f, 80f, false, dna);
                    }
                    canvas.restore();

                    // Vòng trong – 4 chấm xoay
                    canvas.save();
                    canvas.rotate(spinAngle3, cx, cy);
                    paintFill.setColor(aimColor);
                    for (int i = 0; i < 4; i++) {
                        float ang = (float)(Math.toRadians(i * 90));
                        float ox = (float)(Math.cos(ang) * half * 0.35f);
                        float oy = (float)(Math.sin(ang) * half * 0.35f);
                        canvas.drawCircle(cx + ox, cy + oy, thick * 0.9f, paintFill);
                    }
                    canvas.restore();

                    // Chấm trung tâm cố định
                    paintFill.setColor(0xFFFFFFFF);
                    canvas.drawCircle(cx, cy, thick * 0.8f, paintFill);
                    break;
                }

                // ── 10: SPIN GALAXY (3 lớp vòng xoay tốc độ khác nhau) ───────
                case 10: {
                    Paint gx = new Paint(Paint.ANTI_ALIAS_FLAG);
                    gx.setStyle(Paint.Style.STROKE);
                    gx.setStrokeCap(Paint.Cap.ROUND);

                    int baseColor = aimColor;
                    // Alpha layers
                    int c1 = (baseColor & 0x00FFFFFF) | 0xFF000000;
                    int c2 = (baseColor & 0x00FFFFFF) | 0xBB000000;
                    int c3 = (baseColor & 0x00FFFFFF) | 0x77000000;

                    // Lớp 1 — vòng ngoài lớn, 6 cung nhỏ xoay thuận
                    gx.setColor(c1);
                    gx.setStrokeWidth(thick);
                    canvas.save();
                    canvas.rotate(spinAngle, cx, cy);
                    RectF rOuter = new RectF(cx - half, cy - half, cx + half, cy + half);
                    for (int i = 0; i < 6; i++) {
                        canvas.drawArc(rOuter, i * 60f + 5f, 50f, false, gx);
                    }
                    canvas.restore();

                    // Lớp 2 — vòng giữa, 4 cung xoay ngược
                    gx.setColor(c2);
                    gx.setStrokeWidth(thick * 0.85f);
                    canvas.save();
                    canvas.rotate(spinAngle2, cx, cy);
                    RectF rMid = new RectF(cx - half * 0.65f, cy - half * 0.65f,
                                            cx + half * 0.65f, cy + half * 0.65f);
                    for (int i = 0; i < 4; i++) {
                        canvas.drawArc(rMid, i * 90f + 10f, 70f, false, gx);
                    }
                    canvas.restore();

                    // Lớp 3 — vòng trong, 3 cung xoay nhanh
                    gx.setColor(c3);
                    gx.setStrokeWidth(thick * 0.65f);
                    canvas.save();
                    canvas.rotate(spinAngle3, cx, cy);
                    RectF rInner = new RectF(cx - half * 0.35f, cy - half * 0.35f,
                                              cx + half * 0.35f, cy + half * 0.35f);
                    for (int i = 0; i < 3; i++) {
                        canvas.drawArc(rInner, i * 120f + 20f, 90f, false, gx);
                    }
                    canvas.restore();

                    // 4 vạch cross cố định
                    paint.setAlpha(160);
                    canvas.drawLine(cx - gap * 2.5f, cy, cx - gap, cy, paint);
                    canvas.drawLine(cx + gap, cy, cx + gap * 2.5f, cy, paint);
                    canvas.drawLine(cx, cy - gap * 2.5f, cx, cy - gap, paint);
                    canvas.drawLine(cx, cy + gap, cx, cy + gap * 2.5f, paint);
                    paint.setAlpha(255);

                    // Chấm tâm sáng
                    paintFill.setColor(0xFFFFFFFF);
                    canvas.drawCircle(cx, cy, thick, paintFill);
                    break;
                }

                // ── 11: SPIN PULSE (vòng nảy + chấm xoay quỹ đạo) ───────────
                case 11: {
                    // Tính pulse scale từ spinAngle3 (0°→360° = 1 chu kỳ)
                    double rad3 = Math.toRadians(spinAngle3);
                    float pulse = 1.0f + 0.18f * (float) Math.sin(rad3);

                    Paint ps = new Paint(Paint.ANTI_ALIAS_FLAG);
                    ps.setStyle(Paint.Style.STROKE);
                    ps.setColor(aimColor);

                    // Vòng ngoài nảy theo pulse
                    ps.setStrokeWidth(thick);
                    ps.setAlpha(200);
                    float pr = half * pulse;
                    canvas.drawCircle(cx, cy, pr, ps);

                    // Vòng trong cố định
                    ps.setAlpha(120);
                    ps.setStrokeWidth(thick * 0.6f);
                    canvas.drawCircle(cx, cy, half * 0.45f, ps);

                    // 4 chấm nhỏ xoay trên quỹ đạo vòng ngoài
                    canvas.save();
                    canvas.rotate(spinAngle, cx, cy);
                    paintFill.setColor(aimColor);
                    for (int i = 0; i < 4; i++) {
                        float ang = (float) Math.toRadians(i * 90);
                        float ox = (float)(Math.cos(ang) * pr);
                        float oy = (float)(Math.sin(ang) * pr);
                        canvas.drawCircle(cx + ox, cy + oy, thick * 1.1f, paintFill);
                    }
                    canvas.restore();

                    // 4 vạch nhỏ xoay ngược trên vòng giữa
                    canvas.save();
                    canvas.rotate(spinAngle2, cx, cy);
                    ps.setAlpha(180);
                    ps.setStrokeWidth(thick * 0.7f);
                    for (int i = 0; i < 4; i++) {
                        canvas.drawArc(
                            new RectF(cx - half * 0.45f, cy - half * 0.45f,
                                      cx + half * 0.45f, cy + half * 0.45f),
                            i * 90f + 20f, 55f, false, ps);
                    }
                    canvas.restore();

                    // Chấm tâm trắng
                    paintFill.setColor(0xFFFFFFFF);
                    canvas.drawCircle(cx, cy, thick * 0.9f, paintFill);
                    break;
                }

                // ── 12: SPIN FLOWER (cánh hoa xoay) ──────────────────────────
                case 12: {
                    int petals = 6;

                    // Cánh hoa lớp ngoài xoay thuận
                    canvas.save();
                    canvas.rotate(spinAngle, cx, cy);
                    paintFill.setColor(aimColor);
                    paintFill.setAlpha(130);
                    for (int i = 0; i < petals; i++) {
                        float ang = (float) Math.toRadians(i * (360f / petals));
                        float px = (float)(Math.cos(ang) * half * 0.55f);
                        float py = (float)(Math.sin(ang) * half * 0.55f);
                        canvas.drawCircle(cx + px, cy + py, half * 0.32f, paintFill);
                    }
                    canvas.restore();

                    // Cánh hoa lớp trong xoay ngược (nhỏ hơn, warna berbeda)
                    canvas.save();
                    canvas.rotate(spinAngle2, cx, cy);
                    int innerColor = ((~aimColor) & 0x00FFFFFF) | (aimColor & 0xFF000000);
                    paintFill.setColor(aimColor);
                    paintFill.setAlpha(170);
                    for (int i = 0; i < petals; i++) {
                        float ang = (float) Math.toRadians(i * (360f / petals) + 30f);
                        float px = (float)(Math.cos(ang) * half * 0.3f);
                        float py = (float)(Math.sin(ang) * half * 0.3f);
                        canvas.drawCircle(cx + px, cy + py, half * 0.18f, paintFill);
                    }
                    canvas.restore();

                    // Vòng viền xoay nhanh
                    Paint fw = new Paint(Paint.ANTI_ALIAS_FLAG);
                    fw.setStyle(Paint.Style.STROKE);
                    fw.setColor(aimColor);
                    fw.setAlpha(200);
                    fw.setStrokeWidth(thick * 0.8f);
                    canvas.save();
                    canvas.rotate(spinAngle3, cx, cy);
                    for (int i = 0; i < 3; i++) {
                        canvas.drawArc(
                            new RectF(cx - half * 0.88f, cy - half * 0.88f,
                                      cx + half * 0.88f, cy + half * 0.88f),
                            i * 120f + 15f, 80f, false, fw);
                    }
                    canvas.restore();

                    // Nhụy hoa (chấm trung tâm)
                    paintFill.setColor(0xFFFFFFFF);
                    paintFill.setAlpha(255);
                    canvas.drawCircle(cx, cy, thick * 1.2f, paintFill);
                    paintFill.setColor(aimColor);
                    canvas.drawCircle(cx, cy, thick * 0.6f, paintFill);
                    break;
                }

                // ── 13: SPIN PORTAL (cổng dịch chuyển – xoáy ốc ngược chiều) ─
                case 13: {
                    Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
                    pt.setStyle(Paint.Style.STROKE);
                    pt.setStrokeCap(Paint.Cap.ROUND);

                    // --- Lớp ngoài: 8 cung ngắn xoay thuận ---
                    pt.setColor(aimColor);
                    pt.setStrokeWidth(thick * 1.1f);
                    pt.setAlpha(255);
                    canvas.save();
                    canvas.rotate(spinAngle, cx, cy);
                    RectF pOuter = new RectF(cx - half, cy - half, cx + half, cy + half);
                    for (int i = 0; i < 8; i++) {
                        canvas.drawArc(pOuter, i * 45f + 5f, 32f, false, pt);
                    }
                    canvas.restore();

                    // --- Lớp giữa: 6 cung xoay ngược ---
                    pt.setAlpha(200);
                    pt.setStrokeWidth(thick * 0.9f);
                    // màu trắng pha
                    int mid = blendColor(aimColor, 0xFFFFFFFF, 0.4f);
                    pt.setColor(mid);
                    canvas.save();
                    canvas.rotate(spinAngle2, cx, cy);
                    RectF pMid = new RectF(cx - half * 0.68f, cy - half * 0.68f,
                                            cx + half * 0.68f, cy + half * 0.68f);
                    for (int i = 0; i < 6; i++) {
                        canvas.drawArc(pMid, i * 60f + 10f, 42f, false, pt);
                    }
                    canvas.restore();

                    // --- Lớp trong: 4 cung xoay nhanh ---
                    pt.setAlpha(180);
                    pt.setStrokeWidth(thick * 0.7f);
                    pt.setColor(0xFFFFFFFF);
                    canvas.save();
                    canvas.rotate(spinAngle3, cx, cy);
                    RectF pInner = new RectF(cx - half * 0.38f, cy - half * 0.38f,
                                              cx + half * 0.38f, cy + half * 0.38f);
                    for (int i = 0; i < 4; i++) {
                        canvas.drawArc(pInner, i * 90f + 15f, 60f, false, pt);
                    }
                    canvas.restore();

                    // --- Nhân cổng: chấm sáng trung tâm ---
                    paintFill.setColor(0xFFFFFFFF);
                    canvas.drawCircle(cx, cy, thick * 1.4f, paintFill);
                    paintFill.setColor(aimColor);
                    canvas.drawCircle(cx, cy, thick * 0.75f, paintFill);
                    break;
                }

                // ── 14: SPIN BRACKET ({·} xoay) ──────────────────────────────
                case 14: {
                    Paint bp = new Paint(Paint.ANTI_ALIAS_FLAG);
                    bp.setStyle(Paint.Style.STROKE);
                    bp.setStrokeCap(Paint.Cap.ROUND);
                    bp.setColor(aimColor);
                    bp.setStrokeWidth(thick);

                    // Bracket ngoài xoay thuận: 4 cặp cung bracket
                    canvas.save();
                    canvas.rotate(spinAngle, cx, cy);
                    for (int i = 0; i < 4; i++) {
                        float base = i * 90f;
                        // cung ngoài bracket
                        canvas.drawArc(
                            new RectF(cx - half, cy - half, cx + half, cy + half),
                            base + 20f, 50f, false, bp);
                        // cung gấp khúc trong bracket
                        bp.setStrokeWidth(thick * 0.7f);
                        canvas.drawArc(
                            new RectF(cx - half * 0.6f, cy - half * 0.6f,
                                      cx + half * 0.6f, cy + half * 0.6f),
                            base + 30f, 30f, false, bp);
                        bp.setStrokeWidth(thick);
                    }
                    canvas.restore();

                    // Bracket trong xoay ngược
                    bp.setStrokeWidth(thick * 0.6f);
                    bp.setAlpha(160);
                    canvas.save();
                    canvas.rotate(spinAngle2, cx, cy);
                    for (int i = 0; i < 4; i++) {
                        canvas.drawArc(
                            new RectF(cx - half * 0.38f, cy - half * 0.38f,
                                      cx + half * 0.38f, cy + half * 0.38f),
                            i * 90f + 10f, 65f, false, bp);
                    }
                    canvas.restore();

                    // Chấm trung tâm
                    paintFill.setColor(0xFFFFFFFF);
                    canvas.drawCircle(cx, cy, thick * 1.1f, paintFill);
                    paintFill.setColor(aimColor);
                    canvas.drawCircle(cx, cy, thick * 0.55f, paintFill);
                    break;
                }

                // ── 15: SPIN DOUBLE-X (2 chữ X xoay ngược chiều) ────────────
                case 15: {
                    Paint xp = new Paint(Paint.ANTI_ALIAS_FLAG);
                    xp.setStyle(Paint.Style.STROKE);
                    xp.setStrokeCap(Paint.Cap.ROUND);
                    xp.setStrokeWidth(thick);
                    xp.setColor(aimColor);

                    // X ngoài lớn xoay thuận
                    canvas.save();
                    canvas.rotate(spinAngle, cx, cy);
                    canvas.drawLine(cx - half * 0.85f, cy - half * 0.85f,
                                    cx - gap * 1.2f,   cy - gap * 1.2f, xp);
                    canvas.drawLine(cx + gap * 1.2f,   cy - gap * 1.2f,
                                    cx + half * 0.85f, cy - half * 0.85f, xp);
                    canvas.drawLine(cx - half * 0.85f, cy + half * 0.85f,
                                    cx - gap * 1.2f,   cy + gap * 1.2f, xp);
                    canvas.drawLine(cx + gap * 1.2f,   cy + gap * 1.2f,
                                    cx + half * 0.85f, cy + half * 0.85f, xp);
                    canvas.restore();

                    // X trong nhỏ xoay ngược
                    xp.setStrokeWidth(thick * 0.75f);
                    int xc2 = blendColor(aimColor, 0xFFFFFFFF, 0.5f);
                    xp.setColor(xc2);
                    canvas.save();
                    canvas.rotate(spinAngle2 + 45f, cx, cy);
                    canvas.drawLine(cx - half * 0.45f, cy - half * 0.45f,
                                    cx - gap * 0.8f,   cy - gap * 0.8f, xp);
                    canvas.drawLine(cx + gap * 0.8f,   cy - gap * 0.8f,
                                    cx + half * 0.45f, cy - half * 0.45f, xp);
                    canvas.drawLine(cx - half * 0.45f, cy + half * 0.45f,
                                    cx - gap * 0.8f,   cy + gap * 0.8f, xp);
                    canvas.drawLine(cx + gap * 0.8f,   cy + gap * 0.8f,
                                    cx + half * 0.45f, cy + half * 0.45f, xp);
                    canvas.restore();

                    // Vòng xoay nhanh ở giữa
                    Paint xring = new Paint(Paint.ANTI_ALIAS_FLAG);
                    xring.setStyle(Paint.Style.STROKE);
                    xring.setStrokeWidth(thick * 0.6f);
                    xring.setColor(aimColor);
                    xring.setAlpha(180);
                    canvas.save();
                    canvas.rotate(spinAngle3, cx, cy);
                    for (int i = 0; i < 4; i++) {
                        canvas.drawArc(
                            new RectF(cx - half * 0.28f, cy - half * 0.28f,
                                      cx + half * 0.28f, cy + half * 0.28f),
                            i * 90f + 20f, 55f, false, xring);
                    }
                    canvas.restore();

                    paintFill.setColor(0xFFFFFFFF);
                    canvas.drawCircle(cx, cy, thick, paintFill);
                    paintFill.setColor(aimColor);
                    canvas.drawCircle(cx, cy, thick * 0.5f, paintFill);
                    break;
                }

                // ── 16: SPIN PARALLEL (hai vạch song song xoay) ─────────────
                case 16: {
                    Paint pp2 = new Paint(Paint.ANTI_ALIAS_FLAG);
                    pp2.setStyle(Paint.Style.STROKE);
                    pp2.setStrokeCap(Paint.Cap.ROUND);
                    pp2.setStrokeWidth(thick);
                    pp2.setColor(aimColor);

                    // 3 cặp vạch song song xoay thuận
                    canvas.save();
                    canvas.rotate(spinAngle, cx, cy);
                    float sp = half * 0.28f;
                    canvas.drawLine(cx - half * 0.85f, cy - sp, cx + half * 0.85f, cy - sp, pp2);
                    canvas.drawLine(cx - half * 0.85f, cy + sp, cx + half * 0.85f, cy + sp, pp2);
                    canvas.restore();

                    // Cặp chéo 90° xoay ngược
                    pp2.setAlpha(180);
                    pp2.setStrokeWidth(thick * 0.7f);
                    int pc2 = blendColor(aimColor, 0xFFFFFFFF, 0.4f);
                    pp2.setColor(pc2);
                    canvas.save();
                    canvas.rotate(spinAngle2 + 90f, cx, cy);
                    canvas.drawLine(cx - half * 0.7f, cy - sp * 0.7f, cx + half * 0.7f, cy - sp * 0.7f, pp2);
                    canvas.drawLine(cx - half * 0.7f, cy + sp * 0.7f, cx + half * 0.7f, cy + sp * 0.7f, pp2);
                    canvas.restore();

                    // Vòng cung xoay nhanh ở giữa
                    Paint pring = new Paint(Paint.ANTI_ALIAS_FLAG);
                    pring.setStyle(Paint.Style.STROKE);
                    pring.setStrokeWidth(thick * 0.6f);
                    pring.setColor(aimColor);
                    canvas.save();
                    canvas.rotate(spinAngle3, cx, cy);
                    for (int i = 0; i < 4; i++) {
                        canvas.drawArc(
                            new RectF(cx - half * 0.5f, cy - half * 0.5f,
                                      cx + half * 0.5f, cy + half * 0.5f),
                            i * 90f + 15f, 60f, false, pring);
                    }
                    canvas.restore();

                    paintFill.setColor(0xFFFFFFFF);
                    canvas.drawCircle(cx, cy, thick * 0.9f, paintFill);
                    paintFill.setColor(aimColor);
                    canvas.drawCircle(cx, cy, thick * 0.45f, paintFill);
                    break;
                }

                // ── 17: SPIN ARROW CROSS (mũi tên + cross xoay) ─────────────
                case 17: {
                    Paint ap = new Paint(Paint.ANTI_ALIAS_FLAG);
                    ap.setStyle(Paint.Style.STROKE);
                    ap.setStrokeCap(Paint.Cap.ROUND);
                    ap.setStrokeWidth(thick);
                    ap.setColor(aimColor);

                    // 4 mũi tên hướng ra xoay thuận
                    canvas.save();
                    canvas.rotate(spinAngle, cx, cy);
                    float al = half * 0.8f;
                    float ah = half * 0.25f;
                    // mũi tên trên
                    canvas.drawLine(cx, cy - gap, cx, cy - al, ap);
                    canvas.drawLine(cx, cy - al, cx - ah, cy - al + ah, ap);
                    canvas.drawLine(cx, cy - al, cx + ah, cy - al + ah, ap);
                    // mũi tên dưới
                    canvas.drawLine(cx, cy + gap, cx, cy + al, ap);
                    canvas.drawLine(cx, cy + al, cx - ah, cy + al - ah, ap);
                    canvas.drawLine(cx, cy + al, cx + ah, cy + al - ah, ap);
                    // mũi tên trái
                    canvas.drawLine(cx - gap, cy, cx - al, cy, ap);
                    canvas.drawLine(cx - al, cy, cx - al + ah, cy - ah, ap);
                    canvas.drawLine(cx - al, cy, cx - al + ah, cy + ah, ap);
                    // mũi tên phải
                    canvas.drawLine(cx + gap, cy, cx + al, cy, ap);
                    canvas.drawLine(cx + al, cy, cx + al - ah, cy - ah, ap);
                    canvas.drawLine(cx + al, cy, cx + al - ah, cy + ah, ap);
                    canvas.restore();

                    // Vòng xoay ngược ở giữa
                    ap.setStrokeWidth(thick * 0.6f);
                    ap.setAlpha(160);
                    canvas.save();
                    canvas.rotate(spinAngle2, cx, cy);
                    for (int i = 0; i < 4; i++) {
                        canvas.drawArc(
                            new RectF(cx - half * 0.42f, cy - half * 0.42f,
                                      cx + half * 0.42f, cy + half * 0.42f),
                            i * 90f + 10f, 70f, false, ap);
                    }
                    canvas.restore();

                    paintFill.setColor(0xFFFFFFFF);
                    canvas.drawCircle(cx, cy, thick * 1.0f, paintFill);
                    paintFill.setColor(aimColor);
                    canvas.drawCircle(cx, cy, thick * 0.5f, paintFill);
                    break;
                }

                // ── 18: SPIN TRI-ARC (tam giác + cung xoay ngược) ────────────
                case 18: {
                    Paint tp2 = new Paint(Paint.ANTI_ALIAS_FLAG);
                    tp2.setStyle(Paint.Style.STROKE);
                    tp2.setStrokeCap(Paint.Cap.ROUND);
                    tp2.setColor(aimColor);
                    tp2.setStrokeWidth(thick);

                    // 3 cung lớn xoay thuận (120° mỗi cung, hở 20°)
                    canvas.save();
                    canvas.rotate(spinAngle, cx, cy);
                    for (int i = 0; i < 3; i++) {
                        canvas.drawArc(
                            new RectF(cx - half, cy - half, cx + half, cy + half),
                            i * 120f + 10f, 100f, false, tp2);
                    }
                    canvas.restore();

                    // 3 cung giữa xoay ngược
                    tp2.setStrokeWidth(thick * 0.75f);
                    int tc2 = blendColor(aimColor, 0xFFFFFFFF, 0.35f);
                    tp2.setColor(tc2);
                    canvas.save();
                    canvas.rotate(spinAngle2 + 60f, cx, cy);
                    for (int i = 0; i < 3; i++) {
                        canvas.drawArc(
                            new RectF(cx - half * 0.58f, cy - half * 0.58f,
                                      cx + half * 0.58f, cy + half * 0.58f),
                            i * 120f + 20f, 80f, false, tp2);
                    }
                    canvas.restore();

                    // Tam giác nhỏ xoay nhanh bên trong
                    Paint tf = new Paint(Paint.ANTI_ALIAS_FLAG);
                    tf.setStyle(Paint.Style.STROKE);
                    tf.setStrokeWidth(thick * 0.6f);
                    tf.setColor(aimColor);
                    tf.setAlpha(200);
                    canvas.save();
                    canvas.rotate(spinAngle3, cx, cy);
                    Path tri2 = new Path();
                    float tr2 = half * 0.3f;
                    for (int i = 0; i < 3; i++) {
                        float ang = (float)(Math.toRadians(i * 120f - 90f));
                        float px = cx + tr2 * (float) Math.cos(ang);
                        float py = cy + tr2 * (float) Math.sin(ang);
                        if (i == 0) tri2.moveTo(px, py); else tri2.lineTo(px, py);
                    }
                    tri2.close();
                    canvas.drawPath(tri2, tf);
                    canvas.restore();

                    paintFill.setColor(0xFFFFFFFF);
                    canvas.drawCircle(cx, cy, thick, paintFill);
                    paintFill.setColor(aimColor);
                    canvas.drawCircle(cx, cy, thick * 0.5f, paintFill);
                    break;
                }

                // ── 19: SPIN RAINBOW (đa màu cầu vồng xoay) ─────────────────
                case 19: {
                    // 6 màu cầu vồng
                    int[] rainbowColors = {
                        0xFFFF0000, 0xFFFF8800, 0xFFFFFF00,
                        0xFF00FF00, 0xFF0088FF, 0xFFCC00FF
                    };
                    Paint rp = new Paint(Paint.ANTI_ALIAS_FLAG);
                    rp.setStyle(Paint.Style.STROKE);
                    rp.setStrokeCap(Paint.Cap.ROUND);
                    rp.setStrokeWidth(thick * 1.1f);

                    // Vòng ngoài cầu vồng xoay thuận (mỗi cung 55°, màu riêng)
                    canvas.save();
                    canvas.rotate(spinAngle, cx, cy);
                    for (int i = 0; i < 6; i++) {
                        rp.setColor(rainbowColors[i]);
                        canvas.drawArc(
                            new RectF(cx - half, cy - half, cx + half, cy + half),
                            i * 60f, 50f, false, rp);
                    }
                    canvas.restore();

                    // Vòng giữa cầu vồng xoay ngược
                    rp.setStrokeWidth(thick * 0.8f);
                    canvas.save();
                    canvas.rotate(spinAngle2 + 30f, cx, cy);
                    for (int i = 0; i < 6; i++) {
                        rp.setColor(rainbowColors[(i + 3) % 6]);
                        rp.setAlpha(200);
                        canvas.drawArc(
                            new RectF(cx - half * 0.6f, cy - half * 0.6f,
                                      cx + half * 0.6f, cy + half * 0.6f),
                            i * 60f + 10f, 40f, false, rp);
                    }
                    canvas.restore();

                    // Vòng trong xoay nhanh
                    rp.setStrokeWidth(thick * 0.55f);
                    canvas.save();
                    canvas.rotate(spinAngle3, cx, cy);
                    for (int i = 0; i < 6; i++) {
                        rp.setColor(rainbowColors[i]);
                        rp.setAlpha(230);
                        canvas.drawArc(
                            new RectF(cx - half * 0.32f, cy - half * 0.32f,
                                      cx + half * 0.32f, cy + half * 0.32f),
                            i * 60f + 5f, 50f, false, rp);
                    }
                    canvas.restore();

                    // Chấm trắng trung tâm
                    paintFill.setColor(0xFFFFFFFF);
                    canvas.drawCircle(cx, cy, thick * 1.2f, paintFill);
                    break;
                }

                // ── 20: SPIN STAR4 (ngôi sao 4 cánh nhọn xoay) ───────────────
                case 20: {
                    Paint sp4 = new Paint(Paint.ANTI_ALIAS_FLAG);
                    sp4.setStyle(Paint.Style.STROKE);
                    sp4.setStrokeCap(Paint.Cap.ROUND);
                    sp4.setStrokeWidth(thick);
                    sp4.setColor(aimColor);

                    // Ngôi sao 4 cánh nhọn xoay thuận
                    canvas.save();
                    canvas.rotate(spinAngle, cx, cy);
                    for (int i = 0; i < 4; i++) {
                        float ang = (float) Math.toRadians(i * 90f);
                        float tipX = cx + half * (float) Math.cos(ang);
                        float tipY = cy + half * (float) Math.sin(ang);
                        float sideAng1 = (float) Math.toRadians(i * 90f + 90f);
                        float sideAng2 = (float) Math.toRadians(i * 90f - 90f);
                        float sw = half * 0.22f;
                        float s1x = cx + sw * (float) Math.cos(sideAng1);
                        float s1y = cy + sw * (float) Math.sin(sideAng1);
                        float s2x = cx + sw * (float) Math.cos(sideAng2);
                        float s2y = cy + sw * (float) Math.sin(sideAng2);
                        Path sp4path = new Path();
                        sp4path.moveTo(s1x, s1y);
                        sp4path.lineTo(tipX, tipY);
                        sp4path.lineTo(s2x, s2y);
                        sp4.setStyle(Paint.Style.STROKE);
                        canvas.drawPath(sp4path, sp4);
                    }
                    canvas.restore();

                    // Ngôi sao 4 cánh nhỏ xoay ngược (offset 45°)
                    sp4.setStrokeWidth(thick * 0.65f);
                    int sc2 = blendColor(aimColor, 0xFFFFFFFF, 0.45f);
                    sp4.setColor(sc2);
                    canvas.save();
                    canvas.rotate(spinAngle2 + 45f, cx, cy);
                    for (int i = 0; i < 4; i++) {
                        float ang = (float) Math.toRadians(i * 90f);
                        float tipX = cx + half * 0.55f * (float) Math.cos(ang);
                        float tipY = cy + half * 0.55f * (float) Math.sin(ang);
                        float sw = half * 0.13f;
                        float s1x = cx + sw * (float) Math.cos(ang + (float)Math.PI/2);
                        float s1y = cy + sw * (float) Math.sin(ang + (float)Math.PI/2);
                        float s2x = cx + sw * (float) Math.cos(ang - (float)Math.PI/2);
                        float s2y = cy + sw * (float) Math.sin(ang - (float)Math.PI/2);
                        Path sp4b = new Path();
                        sp4b.moveTo(s1x, s1y);
                        sp4b.lineTo(tipX, tipY);
                        sp4b.lineTo(s2x, s2y);
                        canvas.drawPath(sp4b, sp4);
                    }
                    canvas.restore();

                    // Vòng xoay nhanh giữa
                    Paint sr = new Paint(Paint.ANTI_ALIAS_FLAG);
                    sr.setStyle(Paint.Style.STROKE);
                    sr.setStrokeWidth(thick * 0.55f);
                    sr.setColor(aimColor);
                    sr.setAlpha(170);
                    canvas.save();
                    canvas.rotate(spinAngle3, cx, cy);
                    canvas.drawCircle(cx, cy, half * 0.22f, sr);
                    canvas.restore();

                    paintFill.setColor(0xFFFFFFFF);
                    canvas.drawCircle(cx, cy, thick * 0.9f, paintFill);
                    paintFill.setColor(aimColor);
                    canvas.drawCircle(cx, cy, thick * 0.45f, paintFill);
                    break;
                }

                // ── 21: SPIN COMET (sao chổi 4 đuôi xoay) ───────────────────
                case 21: {
                    Paint cp = new Paint(Paint.ANTI_ALIAS_FLAG);
                    cp.setStyle(Paint.Style.STROKE);
                    cp.setStrokeCap(Paint.Cap.ROUND);
                    cp.setStrokeWidth(thick * 1.1f);
                    cp.setColor(aimColor);

                    // 4 "sao chổi" — đầu tròn + đuôi cung dài xoay thuận
                    canvas.save();
                    canvas.rotate(spinAngle, cx, cy);
                    for (int i = 0; i < 4; i++) {
                        float base = i * 90f;
                        // Đuôi cung dài
                        canvas.drawArc(
                            new RectF(cx - half, cy - half, cx + half, cy + half),
                            base, 65f, false, cp);
                        // Đầu chấm sao chổi
                        float headAng = (float) Math.toRadians(base);
                        paintFill.setColor(aimColor);
                        canvas.drawCircle(
                            cx + half * (float) Math.cos(headAng),
                            cy + half * (float) Math.sin(headAng),
                            thick * 1.3f, paintFill);
                    }
                    canvas.restore();

                    // 4 sao chổi nhỏ xoay ngược (offset 45°, vòng giữa)
                    cp.setStrokeWidth(thick * 0.65f);
                    int cc2 = blendColor(aimColor, 0xFFFFFFFF, 0.4f);
                    cp.setColor(cc2);
                    cp.setAlpha(200);
                    canvas.save();
                    canvas.rotate(spinAngle2 + 45f, cx, cy);
                    for (int i = 0; i < 4; i++) {
                        float base = i * 90f;
                        canvas.drawArc(
                            new RectF(cx - half * 0.55f, cy - half * 0.55f,
                                      cx + half * 0.55f, cy + half * 0.55f),
                            base + 5f, 55f, false, cp);
                    }
                    canvas.restore();

                    // Vòng nhỏ trung tâm xoay nhanh
                    Paint cr = new Paint(Paint.ANTI_ALIAS_FLAG);
                    cr.setStyle(Paint.Style.STROKE);
                    cr.setStrokeWidth(thick * 0.55f);
                    cr.setColor(aimColor);
                    cr.setAlpha(180);
                    canvas.save();
                    canvas.rotate(spinAngle3, cx, cy);
                    for (int i = 0; i < 3; i++) {
                        canvas.drawArc(
                            new RectF(cx - half * 0.27f, cy - half * 0.27f,
                                      cx + half * 0.27f, cy + half * 0.27f),
                            i * 120f + 15f, 85f, false, cr);
                    }
                    canvas.restore();

                    // Chấm sáng trung tâm
                    paintFill.setColor(0xFFFFFFFF);
                    canvas.drawCircle(cx, cy, thick * 1.1f, paintFill);
                    paintFill.setColor(aimColor);
                    canvas.drawCircle(cx, cy, thick * 0.55f, paintFill);
                    break;
                }

                // ── 22: SPIN NOVA (ngôi sao nổ 8 tia sáng xoay) ─────────────
                case 22: {
                    Paint np = new Paint(Paint.ANTI_ALIAS_FLAG);
                    np.setStrokeCap(Paint.Cap.ROUND);

                    // 8 tia dài xoay thuận — mỗi tia mờ dần từ gốc ra đầu
                    canvas.save();
                    canvas.rotate(spinAngle, cx, cy);
                    for (int i = 0; i < 8; i++) {
                        float ang = (float) Math.toRadians(i * 45f);
                        float cosA = (float) Math.cos(ang);
                        float sinA = (float) Math.sin(ang);
                        // Tia chính — dày ở gốc, nhọn ở đầu
                        np.setStyle(Paint.Style.STROKE);
                        np.setStrokeWidth(thick * 1.4f);
                        np.setColor(aimColor);
                        np.setAlpha(220);
                        float gapF = gap * 1.1f;
                        canvas.drawLine(cx + cosA * gapF, cy + sinA * gapF,
                                        cx + cosA * half, cy + sinA * half, np);
                        // Tia phụ (ngắn hơn, trong suốt)
                        np.setStrokeWidth(thick * 0.5f);
                        np.setAlpha(80);
                        canvas.drawLine(cx + cosA * (half * 0.55f), cy + sinA * (half * 0.55f),
                                        cx + cosA * (half * 1.0f), cy + sinA * (half * 1.0f), np);
                    }
                    canvas.restore();

                    // Vòng trung tâm xoay ngược — đứt đoạn 4 cung
                    Paint nr = new Paint(Paint.ANTI_ALIAS_FLAG);
                    nr.setStyle(Paint.Style.STROKE);
                    nr.setStrokeCap(Paint.Cap.ROUND);
                    nr.setStrokeWidth(thick * 0.9f);
                    nr.setColor(blendColor(aimColor, 0xFFFFFFFF, 0.5f));
                    nr.setAlpha(210);
                    canvas.save();
                    canvas.rotate(spinAngle2, cx, cy);
                    float nr2 = half * 0.42f;
                    for (int i = 0; i < 4; i++) {
                        canvas.drawArc(new RectF(cx - nr2, cy - nr2, cx + nr2, cy + nr2),
                                i * 90f + 10f, 65f, false, nr);
                    }
                    canvas.restore();

                    // Hào quang vòng ngoài xoay nhanh
                    Paint ng = new Paint(Paint.ANTI_ALIAS_FLAG);
                    ng.setStyle(Paint.Style.STROKE);
                    ng.setStrokeWidth(thick * 0.5f);
                    ng.setColor(aimColor);
                    ng.setAlpha(120);
                    canvas.save();
                    canvas.rotate(spinAngle3 * 1.5f, cx, cy);
                    canvas.drawCircle(cx, cy, half * 0.75f, ng);
                    canvas.restore();

                    // Nhân sáng trung tâm
                    paintFill.setColor(0xFFFFFFFF);
                    canvas.drawCircle(cx, cy, thick * 1.5f, paintFill);
                    paintFill.setColor(aimColor);
                    canvas.drawCircle(cx, cy, thick * 0.75f, paintFill);
                    break;
                }

                // ── 23: SPIN HELIX (xoắn ốc kép 3D) ─────────────────────────
                case 23: {
                    Paint hp = new Paint(Paint.ANTI_ALIAS_FLAG);
                    hp.setStyle(Paint.Style.STROKE);
                    hp.setStrokeCap(Paint.Cap.ROUND);

                    // Dải xoắn ốc ngoài — 12 điểm elipse xoay thuận
                    canvas.save();
                    canvas.rotate(spinAngle, cx, cy);
                    int helixPts = 12;
                    for (int i = 0; i < helixPts; i++) {
                        float t = (float) i / helixPts;
                        float ang = (float) Math.toRadians(i * (360f / helixPts));
                        float rx = half * (float) Math.cos(ang);
                        float ry = half * 0.45f * (float) Math.sin(ang); // elipse = hiệu ứng 3D
                        int alpha = 100 + (int)(155 * ((float) Math.sin(ang * 0.5f + 1) * 0.5f + 0.5f));
                        hp.setStrokeWidth(thick * (0.6f + 0.8f * ((float)Math.sin(ang) * 0.5f + 0.5f)));
                        hp.setColor(blendColor(aimColor, 0xFFFFFFFF, t));
                        hp.setAlpha(alpha);
                        float nx = half * (float) Math.cos(ang + 0.55f);
                        float ny = half * 0.45f * (float) Math.sin(ang + 0.55f);
                        canvas.drawLine(cx + rx, cy + ry, cx + nx, cy + ny, hp);
                    }
                    canvas.restore();

                    // Dải xoắn ốc trong xoay ngược + offset 180°
                    canvas.save();
                    canvas.rotate(spinAngle2 + 180f, cx, cy);
                    for (int i = 0; i < helixPts; i++) {
                        float ang = (float) Math.toRadians(i * (360f / helixPts));
                        float rx = half * 0.55f * (float) Math.cos(ang);
                        float ry = half * 0.25f * (float) Math.sin(ang);
                        int ac = blendColor(aimColor, 0xFFFFFFFF, 0.6f);
                        hp.setStrokeWidth(thick * 0.5f);
                        hp.setColor(ac);
                        hp.setAlpha(160);
                        float nx = half * 0.55f * (float) Math.cos(ang + 0.6f);
                        float ny = half * 0.25f * (float) Math.sin(ang + 0.6f);
                        canvas.drawLine(cx + rx, cy + ry, cx + nx, cy + ny, hp);
                    }
                    canvas.restore();

                    // Vòng trục xoay nhanh
                    Paint hring = new Paint(Paint.ANTI_ALIAS_FLAG);
                    hring.setStyle(Paint.Style.STROKE);
                    hring.setStrokeWidth(thick * 0.65f);
                    hring.setColor(0xFFFFFFFF);
                    hring.setAlpha(160);
                    canvas.save();
                    canvas.rotate(spinAngle3, cx, cy);
                    for (int i = 0; i < 3; i++) {
                        canvas.drawArc(new RectF(cx - half * 0.22f, cy - half * 0.22f,
                                cx + half * 0.22f, cy + half * 0.22f),
                                i * 120f + 20f, 80f, false, hring);
                    }
                    canvas.restore();

                    paintFill.setColor(0xFFFFFFFF);
                    canvas.drawCircle(cx, cy, thick * 1.2f, paintFill);
                    paintFill.setColor(aimColor);
                    canvas.drawCircle(cx, cy, thick * 0.6f, paintFill);
                    break;
                }

                // ── 24: SPIN CLOCK (đồng hồ đa kim xoay) ────────────────────
                case 24: {
                    Paint ck = new Paint(Paint.ANTI_ALIAS_FLAG);
                    ck.setStrokeCap(Paint.Cap.ROUND);

                    // Mặt đồng hồ — vòng ngoài
                    ck.setStyle(Paint.Style.STROKE);
                    ck.setStrokeWidth(thick * 0.8f);
                    ck.setColor(aimColor);
                    ck.setAlpha(180);
                    canvas.drawCircle(cx, cy, half, ck);

                    // 12 vạch giờ trên mặt đồng hồ
                    ck.setAlpha(120);
                    ck.setStrokeWidth(thick * 0.5f);
                    for (int i = 0; i < 12; i++) {
                        float ang = (float) Math.toRadians(i * 30f);
                        float inner = (i % 3 == 0) ? half * 0.78f : half * 0.86f;
                        canvas.drawLine(cx + (float)Math.cos(ang) * inner,
                                        cy + (float)Math.sin(ang) * inner,
                                        cx + (float)Math.cos(ang) * half,
                                        cy + (float)Math.sin(ang) * half, ck);
                    }

                    // Kim giờ (xoay thuận chậm)
                    ck.setStyle(Paint.Style.STROKE);
                    ck.setStrokeWidth(thick * 1.4f);
                    ck.setColor(aimColor);
                    ck.setAlpha(255);
                    canvas.save();
                    canvas.rotate(spinAngle * 0.5f, cx, cy);
                    canvas.drawLine(cx, cy, cx, cy - half * 0.55f, ck);
                    canvas.restore();

                    // Kim phút (xoay thuận nhanh hơn)
                    ck.setStrokeWidth(thick * 0.9f);
                    ck.setColor(blendColor(aimColor, 0xFFFFFFFF, 0.4f));
                    canvas.save();
                    canvas.rotate(spinAngle * 1.2f, cx, cy);
                    canvas.drawLine(cx, cy, cx, cy - half * 0.8f, ck);
                    canvas.restore();

                    // Kim giây (xoay nhanh — màu trắng)
                    ck.setStrokeWidth(thick * 0.55f);
                    ck.setColor(0xFFFFFFFF);
                    ck.setAlpha(220);
                    canvas.save();
                    canvas.rotate(spinAngle3 * 0.9f, cx, cy);
                    canvas.drawLine(cx, cy + half * 0.2f, cx, cy - half * 0.9f, ck);
                    canvas.restore();

                    // Chấm trung tâm
                    paintFill.setColor(0xFFFFFFFF);
                    canvas.drawCircle(cx, cy, thick * 1.1f, paintFill);
                    paintFill.setColor(aimColor);
                    canvas.drawCircle(cx, cy, thick * 0.55f, paintFill);
                    break;
                }

                // ── 25: SPIN CRYSTAL (tinh thể lục giác đa lớp) ─────────────
                case 25: {
                    Paint crp = new Paint(Paint.ANTI_ALIAS_FLAG);
                    crp.setStyle(Paint.Style.STROKE);
                    crp.setStrokeCap(Paint.Cap.ROUND);

                    // Lớp ngoài: lục giác xoay thuận
                    canvas.save();
                    canvas.rotate(spinAngle, cx, cy);
                    Path hex1 = new Path();
                    for (int i = 0; i < 6; i++) {
                        float a = (float) Math.toRadians(i * 60f);
                        float px = cx + half * (float) Math.cos(a);
                        float py = cy + half * (float) Math.sin(a);
                        if (i == 0) hex1.moveTo(px, py); else hex1.lineTo(px, py);
                    }
                    hex1.close();
                    crp.setColor(aimColor);
                    crp.setStrokeWidth(thick);
                    crp.setAlpha(200);
                    canvas.drawPath(hex1, crp);
                    // 6 đường chéo từ tâm ra đỉnh
                    crp.setStrokeWidth(thick * 0.5f);
                    crp.setAlpha(100);
                    for (int i = 0; i < 6; i++) {
                        float a = (float) Math.toRadians(i * 60f);
                        canvas.drawLine(cx, cy,
                                cx + half * (float) Math.cos(a),
                                cy + half * (float) Math.sin(a), crp);
                    }
                    canvas.restore();

                    // Lớp giữa: lục giác nhỏ xoay ngược
                    canvas.save();
                    canvas.rotate(spinAngle2 + 30f, cx, cy);
                    Path hex2 = new Path();
                    for (int i = 0; i < 6; i++) {
                        float a = (float) Math.toRadians(i * 60f);
                        float px = cx + half * 0.55f * (float) Math.cos(a);
                        float py = cy + half * 0.55f * (float) Math.sin(a);
                        if (i == 0) hex2.moveTo(px, py); else hex2.lineTo(px, py);
                    }
                    hex2.close();
                    crp.setColor(blendColor(aimColor, 0xFFFFFFFF, 0.45f));
                    crp.setStrokeWidth(thick * 0.75f);
                    crp.setAlpha(180);
                    canvas.drawPath(hex2, crp);
                    canvas.restore();

                    // Lớp trong: lục giác nhỏ nhất xoay nhanh
                    canvas.save();
                    canvas.rotate(spinAngle3, cx, cy);
                    Path hex3 = new Path();
                    for (int i = 0; i < 6; i++) {
                        float a = (float) Math.toRadians(i * 60f);
                        float px = cx + half * 0.26f * (float) Math.cos(a);
                        float py = cy + half * 0.26f * (float) Math.sin(a);
                        if (i == 0) hex3.moveTo(px, py); else hex3.lineTo(px, py);
                    }
                    hex3.close();
                    crp.setColor(0xFFFFFFFF);
                    crp.setStrokeWidth(thick * 0.55f);
                    crp.setAlpha(200);
                    canvas.drawPath(hex3, crp);
                    canvas.restore();

                    paintFill.setColor(0xFFFFFFFF);
                    canvas.drawCircle(cx, cy, thick * 1.1f, paintFill);
                    paintFill.setColor(aimColor);
                    canvas.drawCircle(cx, cy, thick * 0.55f, paintFill);
                    break;
                }

                // ── 26: SPIN VORTEX (xoáy hút từ ngoài vào tâm) ─────────────
                case 26: {
                    Paint vp = new Paint(Paint.ANTI_ALIAS_FLAG);
                    vp.setStyle(Paint.Style.STROKE);
                    vp.setStrokeCap(Paint.Cap.ROUND);

                    // 5 lớp cung xoay với tốc độ tăng dần vào tâm
                    float[] radii   = {1.0f, 0.78f, 0.58f, 0.40f, 0.24f};
                    float[] speeds  = {1.0f, 1.4f,  1.9f,  2.6f,  3.5f};
                    int[]   arcs    = {6,    5,      4,      4,     3};
                    float[] widths  = {1.1f, 0.95f, 0.8f,  0.65f, 0.5f};
                    int[]   alphas  = {200,  210,   220,   230,   255};
                    for (int layer = 0; layer < radii.length; layer++) {
                        float r = half * radii[layer];
                        float rot = spinAngle * speeds[layer];
                        float t = (float) layer / (radii.length - 1);
                        vp.setColor(blendColor(aimColor, 0xFFFFFFFF, t * 0.6f));
                        vp.setStrokeWidth(thick * widths[layer]);
                        vp.setAlpha(alphas[layer]);
                        canvas.save();
                        canvas.rotate(rot, cx, cy);
                        int n = arcs[layer];
                        float sweep = (360f / n) * 0.62f;
                        for (int i = 0; i < n; i++) {
                            canvas.drawArc(new RectF(cx - r, cy - r, cx + r, cy + r),
                                    i * (360f / n), sweep, false, vp);
                        }
                        canvas.restore();
                    }

                    paintFill.setColor(0xFFFFFFFF);
                    canvas.drawCircle(cx, cy, thick * 1.3f, paintFill);
                    paintFill.setColor(aimColor);
                    canvas.drawCircle(cx, cy, thick * 0.65f, paintFill);
                    break;
                }

                // ── 27: SPIN WEB (mạng nhện xoay) ────────────────────────────
                case 27: {
                    Paint wp = new Paint(Paint.ANTI_ALIAS_FLAG);
                    wp.setStyle(Paint.Style.STROKE);
                    wp.setStrokeCap(Paint.Cap.ROUND);
                    wp.setColor(aimColor);

                    // 6 sợi dọc từ tâm ra (frame mạng) — xoay thuận
                    canvas.save();
                    canvas.rotate(spinAngle, cx, cy);
                    wp.setStrokeWidth(thick * 0.55f);
                    wp.setAlpha(160);
                    for (int i = 0; i < 6; i++) {
                        float a = (float) Math.toRadians(i * 60f);
                        canvas.drawLine(cx, cy,
                                cx + half * (float)Math.cos(a),
                                cy + half * (float)Math.sin(a), wp);
                    }
                    // Vòng mạng ngoài
                    wp.setStrokeWidth(thick * 0.7f);
                    wp.setAlpha(180);
                    canvas.drawCircle(cx, cy, half,        wp);
                    canvas.drawCircle(cx, cy, half * 0.65f, wp);
                    canvas.drawCircle(cx, cy, half * 0.32f, wp);
                    canvas.restore();

                    // Lớp trong xoay ngược — web thu nhỏ offset 30°
                    canvas.save();
                    canvas.rotate(spinAngle2 + 30f, cx, cy);
                    wp.setStrokeWidth(thick * 0.45f);
                    int wc2 = blendColor(aimColor, 0xFFFFFFFF, 0.5f);
                    wp.setColor(wc2);
                    wp.setAlpha(140);
                    for (int i = 0; i < 6; i++) {
                        float a = (float) Math.toRadians(i * 60f);
                        canvas.drawLine(cx, cy,
                                cx + half * 0.6f * (float)Math.cos(a),
                                cy + half * 0.6f * (float)Math.sin(a), wp);
                    }
                    canvas.restore();

                    // 6 chấm trên vòng ngoài xoay nhanh
                    canvas.save();
                    canvas.rotate(spinAngle3, cx, cy);
                    paintFill.setColor(aimColor);
                    paintFill.setAlpha(220);
                    for (int i = 0; i < 6; i++) {
                        float a = (float) Math.toRadians(i * 60f);
                        canvas.drawCircle(cx + half * (float)Math.cos(a),
                                          cy + half * (float)Math.sin(a),
                                          thick * 0.85f, paintFill);
                    }
                    paintFill.setAlpha(255);
                    canvas.restore();

                    paintFill.setColor(0xFFFFFFFF);
                    canvas.drawCircle(cx, cy, thick * 1.1f, paintFill);
                    paintFill.setColor(aimColor);
                    canvas.drawCircle(cx, cy, thick * 0.55f, paintFill);
                    break;
                }

                // ── 28: SPIN OMEGA (vòng Ω kép xoay ngược chiều) ────────────
                case 28: {
                    Paint op = new Paint(Paint.ANTI_ALIAS_FLAG);
                    op.setStyle(Paint.Style.STROKE);
                    op.setStrokeCap(Paint.Cap.ROUND);

                    // Vòng Omega ngoài xoay thuận — 2 cung lớn + 2 chân
                    canvas.save();
                    canvas.rotate(spinAngle, cx, cy);
                    op.setColor(aimColor);
                    op.setStrokeWidth(thick * 1.1f);
                    op.setAlpha(230);
                    // Cung trên (200°)
                    canvas.drawArc(new RectF(cx - half, cy - half, cx + half, cy + half),
                            200f, 200f, false, op);
                    // Hai chân Omega
                    op.setStrokeWidth(thick * 0.85f);
                    canvas.drawLine(cx - half * 0.65f, cy + half * 0.64f,
                                    cx - half * 0.9f,  cy + half * 0.64f, op);
                    canvas.drawLine(cx + half * 0.65f, cy + half * 0.64f,
                                    cx + half * 0.9f,  cy + half * 0.64f, op);
                    canvas.restore();

                    // Vòng Omega trong xoay ngược — offset 90°, màu blend
                    canvas.save();
                    canvas.rotate(spinAngle2 + 90f, cx, cy);
                    int oc2 = blendColor(aimColor, 0xFFFFFFFF, 0.45f);
                    op.setColor(oc2);
                    op.setStrokeWidth(thick * 0.75f);
                    op.setAlpha(200);
                    canvas.drawArc(new RectF(cx - half * 0.6f, cy - half * 0.6f,
                                    cx + half * 0.6f, cy + half * 0.6f),
                            200f, 200f, false, op);
                    op.setStrokeWidth(thick * 0.6f);
                    canvas.drawLine(cx - half * 0.4f, cy + half * 0.38f,
                                    cx - half * 0.55f, cy + half * 0.38f, op);
                    canvas.drawLine(cx + half * 0.4f, cy + half * 0.38f,
                                    cx + half * 0.55f, cy + half * 0.38f, op);
                    canvas.restore();

                    // Vòng xoay nhanh bên trong
                    Paint oring = new Paint(Paint.ANTI_ALIAS_FLAG);
                    oring.setStyle(Paint.Style.STROKE);
                    oring.setStrokeWidth(thick * 0.55f);
                    oring.setColor(0xFFFFFFFF);
                    oring.setAlpha(170);
                    canvas.save();
                    canvas.rotate(spinAngle3, cx, cy);
                    for (int i = 0; i < 3; i++) {
                        canvas.drawArc(new RectF(cx - half * 0.28f, cy - half * 0.28f,
                                cx + half * 0.28f, cy + half * 0.28f),
                                i * 120f + 20f, 75f, false, oring);
                    }
                    canvas.restore();

                    paintFill.setColor(0xFFFFFFFF);
                    canvas.drawCircle(cx, cy, thick * 1.1f, paintFill);
                    paintFill.setColor(aimColor);
                    canvas.drawCircle(cx, cy, thick * 0.55f, paintFill);
                    break;
                }

                // ── 29: SPIN AURORA (cực quang cầu vồng 3 lớp) ──────────────
                case 29: {
                    // Bảng màu cực quang: tím → xanh → xanh lá → vàng → hồng
                    int[] aurora = {
                        0xFFCC00FF, 0xFF7B00FF, 0xFF0066FF,
                        0xFF00CCFF, 0xFF00FFB2, 0xFF69FF00,
                        0xFFFFEE00, 0xFFFF6600, 0xFFFF0066
                    };

                    Paint ap = new Paint(Paint.ANTI_ALIAS_FLAG);
                    ap.setStyle(Paint.Style.STROKE);
                    ap.setStrokeCap(Paint.Cap.ROUND);

                    // Lớp ngoài — 9 cung màu sắc xoay thuận
                    canvas.save();
                    canvas.rotate(spinAngle, cx, cy);
                    ap.setStrokeWidth(thick * 1.2f);
                    for (int i = 0; i < 9; i++) {
                        ap.setColor(aurora[i]);
                        ap.setAlpha(210);
                        canvas.drawArc(new RectF(cx - half, cy - half, cx + half, cy + half),
                                i * 40f, 33f, false, ap);
                    }
                    canvas.restore();

                    // Lớp giữa — 9 cung màu lệch xoay ngược
                    canvas.save();
                    canvas.rotate(spinAngle2 + 20f, cx, cy);
                    ap.setStrokeWidth(thick * 0.85f);
                    for (int i = 0; i < 9; i++) {
                        ap.setColor(aurora[(i + 4) % 9]);
                        ap.setAlpha(180);
                        canvas.drawArc(new RectF(cx - half * 0.62f, cy - half * 0.62f,
                                cx + half * 0.62f, cy + half * 0.62f),
                                i * 40f + 12f, 25f, false, ap);
                    }
                    canvas.restore();

                    // Lớp trong — 6 cung xoay nhanh
                    canvas.save();
                    canvas.rotate(spinAngle3, cx, cy);
                    ap.setStrokeWidth(thick * 0.6f);
                    for (int i = 0; i < 6; i++) {
                        ap.setColor(aurora[i % aurora.length]);
                        ap.setAlpha(220);
                        canvas.drawArc(new RectF(cx - half * 0.32f, cy - half * 0.32f,
                                cx + half * 0.32f, cy + half * 0.32f),
                                i * 60f + 5f, 48f, false, ap);
                    }
                    canvas.restore();

                    // Nhân sáng trắng trung tâm
                    paintFill.setColor(0xFFFFFFFF);
                    canvas.drawCircle(cx, cy, thick * 1.4f, paintFill);
                    paintFill.setColor(aurora[0]);
                    canvas.drawCircle(cx, cy, thick * 0.7f, paintFill);
                    break;
                }
            }
        }
    }

    /** Trộn 2 màu với tỉ lệ t (0.0=a, 1.0=b) */
    private static int blendColor(int a, int b, float t) {
        int ra = (a >> 16) & 0xFF; int ga = (a >> 8) & 0xFF; int ba2 = a & 0xFF;
        int rb = (b >> 16) & 0xFF; int gb = (b >> 8) & 0xFF; int bb2 = b & 0xFF;
        int r = (int)(ra + (rb - ra) * t);
        int g = (int)(ga + (gb - ga) * t);
        int bl= (int)(ba2+ (bb2- ba2)* t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private void createAimView() {
        aimView = new AimView(this);

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        int panelSz = 300;

        aimLp = new WindowManager.LayoutParams(
                panelSz, panelSz, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);

        // Dùng Gravity.CENTER - hệ thống tự căn giữa, không cần tính x/y
        // Tự đúng cả portrait lẫn landscape khi xoay
        aimLp.gravity = Gravity.CENTER;
        aimLp.x = 0;
        aimLp.y = 0;

        if (aimStyle >= 8) startSpin();
    }

    private void showAim() {
        aimVisible = true;
        if (!aimAdded && aimView != null) {
            try {
                wm.addView(aimView, aimLp);
                aimAdded = true;
                if (aimStyle >= 8) startSpin();
            } catch (Exception ignored) {}
        }
    }

    private void hideAim() {
        aimVisible = false;
        stopSpin();
        if (aimAdded && aimView != null) {
            try {
                wm.removeView(aimView);
                aimAdded = false;
            } catch (Exception ignored) {}
        }
    }

    private void refreshAim() {
        if (aimAdded && aimView != null) {
            aimView.invalidate();
        }
    }

    // ================= DRAG =================
    class DragTouch implements View.OnTouchListener {
        int ix, iy;
        int lastX, lastY;
        float tx, ty;
        boolean dragging;
        boolean layoutPosted;
        View dragView;
        WindowManager.LayoutParams p;
        String keyX, keyY;
        final int touchSlop;
        final Handler dragHandler = new Handler(Looper.getMainLooper());

        final Runnable layoutRunnable = new Runnable() {
            @Override
            public void run() {
                layoutPosted = false;
                if (dragView == null || p == null) return;
                try {
                    clampParamsToScreen(p, dragView);
                    wm.updateViewLayout(dragView, p);
                } catch (Exception ignored) {}
            }
        };

        DragTouch(WindowManager.LayoutParams pp, String kx, String ky) {
            p = pp;
            keyX = kx;
            keyY = ky;
            int slop = ViewConfiguration.get(MediaRenderService.this).getScaledTouchSlop();
            touchSlop = Math.max(8, slop);
        }

        @Override
        public boolean onTouch(final View v, MotionEvent e) {
            int action = e.getActionMasked();

            if (action == MotionEvent.ACTION_DOWN) {
                dragView = v;
                ix = p.x;
                iy = p.y;
                lastX = p.x;
                lastY = p.y;
                tx = e.getRawX();
                ty = e.getRawY();
                dragging = false;
                layoutPosted = false;
                dragHandler.removeCallbacks(layoutRunnable);
                if (v.getParent() != null) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                }
                try {
                    v.animate().cancel();
                    v.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                    v.animate().scaleX(0.96f).scaleY(0.96f).translationY(1f).setDuration(55).start();
                } catch (Exception ignored) {}
                return true;
            }

            if (action == MotionEvent.ACTION_MOVE) {
                if (isLocked()) return true;

                float dx = e.getRawX() - tx;
                float dy = e.getRawY() - ty;
                if (!dragging && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) {
                    dragging = true;
                }

                if (dragging) {
                    int nx = ix + Math.round(dx);
                    int ny = iy + Math.round(dy);
                    if (nx != lastX || ny != lastY) {
                        p.x = nx;
                        p.y = ny;
                        clampParamsToScreen(p, v);
                        nx = p.x;
                        ny = p.y;
                        lastX = nx;
                        lastY = ny;
                        postLayoutOnNextFrame(v);
                    }
                }
                return true;
            }

            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                dragHandler.removeCallbacks(layoutRunnable);
                layoutPosted = false;
                try {
                    clampParamsToScreen(p, v);
                    wm.updateViewLayout(v, p);
                } catch (Exception ignored) {}
                try {
                    v.animate().cancel();
                    v.animate().scaleX(1f).scaleY(1f).translationY(0f).setDuration(90).start();
                    v.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try { v.setLayerType(View.LAYER_TYPE_NONE, null); } catch (Exception ignored) {}
                        }
                    }, 110);
                } catch (Exception ignored) {}

                if (dragging) {
                    prefs.edit().putInt(keyX, p.x).putInt(keyY, p.y).apply();
                    dragging = false;
                    return true;
                }

                if (action == MotionEvent.ACTION_UP) {
                    v.performClick();
                }
                return true;
            }

            return true;
        }

        private void postLayoutOnNextFrame(View v) {
            dragView = v;
            if (layoutPosted) return;
            layoutPosted = true;
            if (Build.VERSION.SDK_INT >= 16) {
                v.postOnAnimation(layoutRunnable);
            } else {
                dragHandler.postDelayed(layoutRunnable, 16);
            }
        }
    }

    // ================= PREFERENCES =================
    private SharedPreferences prefs() { return getSharedPreferences("cfg", 0); }

    public int getFreezeSize() { return prefs().getInt("sf_z", SIZE_DEFAULT); }
    public int getGhostSize()  { return prefs().getInt("sg_z",  SIZE_DEFAULT); }
    public int getTeleSize()   { return prefs().getInt("st_z",   SIZE_DEFAULT); }
    private int getVpnSize()   { return prefs().getInt("sv_z", Math.max(46, getFreezeSize() / 2)); }

    public int m_gfa() { return prefs().getInt("af_a", 100); }
    public int m_gga()  { return prefs().getInt("ag_a",  100); }
    public int m_gta()   { return prefs().getInt("at_a",   100); }
    public int m_gva()    { return prefs().getInt("av_a",    100); }

    public boolean isSound1On() { return prefs().getBoolean("sound1", true); }
    private boolean isLocked()  { return prefs().getBoolean("lock", false); }

    // ================= UTIL =================
    private void play(MediaPlayer mp) {
        if (mp == null || isServiceDestroyed) return;
        try {
            if (mp.isPlaying()) mp.seekTo(0);
            mp.start();
        } catch (Exception ignored) {}
    }

    private void setSafeCircleBackground(TextView btn, int color) {
        if (btn == null) return;
        try {
            // Chừa viền trong suốt nhỏ để stroke/bo góc không bị WindowManager cắt khi đổi trạng thái.
            int inset = Math.max(2, (int) (2f * getResources().getDisplayMetrics().density + 0.5f));
            btn.setBackgroundDrawable(new android.graphics.drawable.InsetDrawable(makeCircle(color), inset, inset, inset, inset));
            if (Build.VERSION.SDK_INT >= 21) {
                btn.setClipToOutline(false);
            }
        } catch (Exception e) {
            try { btn.setBackgroundDrawable(makeCircle(color)); } catch (Exception ignored) {}
        }
    }

    private GradientDrawable makeCircle(int color) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(color);
        gd.setStroke(3, UI_WHITE_SOFT);
        return gd;
    }

    private void clampAndUpdateView(View view, WindowManager.LayoutParams params) {
        if (view == null || params == null || !isViewAttached(view)) return;
        try {
            clampParamsToScreen(params, view);
            wm.updateViewLayout(view, params);
        } catch (Exception ignored) {}
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Khi xoay màn hình: Gravity.CENTER tự xử lý, chỉ cần updateViewLayout để apply lại
        if (aimAdded && aimView != null && aimLp != null) {
            try { wm.updateViewLayout(aimView, aimLp); } catch (Exception ignored) {}
        }
        clampAndUpdateView(fxPanel, fxLp);
        clampAndUpdateView(vxPanel, vxLp);
        clampAndUpdateView(gxPanel, gxLp);
        clampAndUpdateView(txPanel, txLp);
        clampAndUpdateView(mxBtn, mxLp);
        clampAndUpdateView(mxPanel, mpLp);
        clampAndUpdateView(tmView, tmLp);
    }

    public void onDestroy() {
        // FIX: set destroyed flag FIRST to stop background threads touching views/service
        isServiceDestroyed = true;

        stopSpin();
        m_cfat();
        if (fxOn) {
            try {
                Intent i = new Intent(this, GameBoosterVpnService.class);
                i.setAction(GameBoosterVpnService.A_F1);
                i.putExtra("enabled", false);
                startService(i);
            } catch (Exception ignored) {}
            fxOn = false;
        }
        if (gxOn) {
            try {
                Intent i = new Intent(this, GameBoosterVpnService.class);
                i.setAction(GameBoosterVpnService.A_G1);
                i.putExtra("enabled", false);
                startService(i);
            } catch (Exception ignored) {}
            gxOn = false;
        }

        if (fxLp != null) prefs().edit()
                .putInt("fx_x", fxLp.x).putInt("fx_y", fxLp.y).apply();
        if (vxLp != null) prefs().edit()
                .putInt("vx_x", vxLp.x).putInt("vx_y", vxLp.y).apply();
        if (gxLp != null) prefs().edit()
                .putInt("gx_x", gxLp.x).putInt("gx_y", gxLp.y).apply();
        if (txLp != null) prefs().edit()
                .putInt("tx_x", txLp.x).putInt("tx_y", txLp.y).apply();
        if (tmLp != null) prefs().edit()
                .putInt("tm_x", tmLp.x).putInt("tm_y", tmLp.y).apply();
        if (mxLp != null) prefs().edit()
                .putInt("mx_x", mxLp.x).putInt("mx_y", mxLp.y).apply();

        try { unregisterReceiver(statusReceiver); }     catch (Exception ignored) {}

        if (fxPanel != null)  try { wm.removeView(fxPanel);  } catch (Exception ignored) {}
        if (vxPanel != null)     try { wm.removeView(vxPanel);     } catch (Exception ignored) {}
        if (gxPanel != null)   try { wm.removeView(gxPanel);   } catch (Exception ignored) {}
        if (txPanel != null)    try { wm.removeView(txPanel);    } catch (Exception ignored) {}
        if (treMaTranAdded && tmView != null)
                                 try { wm.removeView(tmView); } catch (Exception ignored) {}
        if (mxBtn != null) try { wm.removeView(mxBtn); } catch (Exception ignored) {}
        closeMenuPanel();
        hideAim();

        // FIX: release MediaPlayer to prevent crash/leak
        if (mpOn != null)  { try { mpOn.release();  } catch (Exception ignored) {} mpOn  = null; }
        if (mpOff != null) { try { mpOff.release(); } catch (Exception ignored) {} mpOff = null; }

        super.onDestroy();
    }
}
