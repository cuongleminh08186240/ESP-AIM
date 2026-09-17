package FakePingZzz.Plus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    private static final int REQ_OVERLAY = 1001;
    private static final int REQ_VPN = 1002;

    private static final int COLOR_BG_TOP = 0xFF060A11;
    private static final int COLOR_BG_MID = 0xFF0B1420;
    private static final int COLOR_BG_BOTTOM = 0xFF0F1B28;
    private static final int COLOR_PANEL = 0xF0141D2A;
    private static final int COLOR_PANEL_ALT = 0xFF162130;
    private static final int COLOR_PANEL_INNER = 0xFF0C141E;
    private static final int COLOR_STROKE = 0xFF2A3950;
    private static final int COLOR_STROKE_SOFT = 0x66546C8C;
    private static final int COLOR_WHITE = 0xFFF7FAFF;
    private static final int COLOR_WHITE_SOFT = 0xFFD9E2EF;
    private static final int COLOR_WHITE_MUTED = 0xFF95A5BB;
    private static final int COLOR_ACCENT = 0xFF5C9DFF;
    private static final int COLOR_ACCENT_DARK = 0xFF2B6FE8;
    private static final int COLOR_ACCENT_SOFT = 0x225C9DFF;
    private static final int COLOR_PURPLE = 0xFF8A63FF;
    private static final int COLOR_PURPLE_DARK = 0xFF6543E8;
    private static final int COLOR_GLOW_SOFT = 0x408A63FF;
    private static final int COLOR_DANGER = 0xFFFF616B;
    private static final int COLOR_START_BORDER = 0xFF6AA8FF;
    private static final int COLOR_SUCCESS_FILL = 0xFF102034;
    private static final int COLOR_SUCCESS_BORDER = 0xFF61C28B;
    private static final int COLOR_OFF = 0xFF0A1018;
    private static final int COLOR_ON_GREEN = 0xFF00E676;
    private static final int COLOR_FREEZE_BLUE = 0xFF6AA8FF;
    private static final int COLOR_GHOST_PURPLE = 0xFFC084FC;
    private static final int COLOR_GHOST_PINK = 0xFFFF7AD9;
    private static final int COLOR_TELE_CYAN = 0xFF31D7FF;

    private SeekBar seekSizeFreeze, seekSizeGhost, seekSizeTele;
    private TextView tvSizeFreeze, tvSizeGhost, tvSizeTele;
    private SeekBar seekAlphaFreeze, seekAlphaGhost, seekAlphaTele;
    private TextView tvAlphaFreeze, tvAlphaGhost, tvAlphaTele;
    private Switch switchPen, switchFreeze, switchGhost, switchTele, switchDownloadBoost, switchReduceFpsDrop;
    private Button btnStart, btnStop, btnGrantOverlay, btnGrantVpn, btnFreezeDropConfig, btnGhostDropConfig, btnFreezeTimeConfig;
    private TextView tvOverlayStatus, tvVpnStatus, tvFreezeDropRange, tvGhostDropRange, tvFreezeTimeStatus, tvFirewallSummary, tvHomeVpnBoostStatus, tvHomeAdvancedStatus;
    private View homePageView, settingsPageView, firewallPageView;
    private LinearLayout homePageContainer, settingsPageContainer, firewallPageContainer, firewallIpListContainer, firewallDomainListContainer;
    private View mainShellView, bottomNavigationView;
    private View tabHome, tabSettings, tabFirewall;
    private int currentTabIndex = 0;
    private boolean isTabTransitionRunning = false;
    private boolean hasPlayedMenuIntro = false;
    private boolean _uwts = false; 
    public static boolean _svcUp = false; 

    private SharedPreferences prefs;
    private boolean isEnglish = false;
    private volatile boolean isDestroyed = false;

    private final BroadcastReceiver vpnRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            m_rvp();
        }
    };

    private void registerReceiverCompat(BroadcastReceiver receiver, IntentFilter filter) {
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                Context.class.getMethod("registerReceiver", BroadcastReceiver.class, IntentFilter.class, int.class)
                        .invoke(this, receiver, filter, 4);
                return;
            } catch (Exception ignored) {
            }
        }
        registerReceiver(receiver, filter);
    }

    private String t(String vi, String en) {
        return isEnglish ? en : vi;
    }

    private void applyLanguage(String code) {
        if (prefs == null) return;
        prefs.edit().putString("ui_lang", code).apply();
        boolean newEnglish = "en".equals(code);
        if (isEnglish != newEnglish) {
            isEnglish = newEnglish;
            recreate();
        }
    }

    private String getTimeRemainingText(int _rd, int _rh, int _rm) {
        if (_rd > 0) {
            return isEnglish
                    ? (_rd + " day " + _rh + " hour")
                    : (_rd + " ngày " + _rh + " giờ");
        } else if (_rh > 0) {
            return isEnglish
                    ? (_rh + " hour " + _rm + " minute")
                    : (_rh + " giờ " + _rm + " phút");
        } else if (_rm > 0) {
            return isEnglish ? (_rm + " minute") : (_rm + " phút");
        }
        return t("sắp hết hạn", "expires soon");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("cfg", MODE_PRIVATE);
        isEnglish = "en".equals(prefs.getString("ui_lang", "vi"));
        setContentView(buildMainContent());
        initApp();
    }

    private void initApp() {
        if (isDestroyed) return;

        int sizeFreeze = prefs.getInt("sf_z", 150);
        int sizeGhost  = prefs.getInt("sg_z", 90);
        int sizeTele   = prefs.getInt("st_z", 90);
        int alphaFreeze = prefs.getInt("af_a", 100);
        int alphaGhost  = prefs.getInt("ag_a", 100);
        int alphaTele   = prefs.getInt("at_a", 100);

        seekSizeFreeze.setProgress(Math.max(0, sizeFreeze - 60));
        seekSizeGhost.setProgress(Math.max(0, sizeGhost - 60));
        seekSizeTele.setProgress(Math.max(0, sizeTele - 60));
        seekAlphaFreeze.setProgress(alphaFreeze);
        seekAlphaGhost.setProgress(alphaGhost);
        seekAlphaTele.setProgress(alphaTele);

        tvSizeFreeze.setText(sizeFreeze + " dp");
        tvSizeGhost.setText(sizeGhost + " dp");
        tvSizeTele.setText(sizeTele + " dp");
        tvAlphaFreeze.setText(alphaFreeze + "%");
        tvAlphaGhost.setText(alphaGhost + "%");
        tvAlphaTele.setText(alphaTele + "%");

        switchPen.setChecked(prefs.getBoolean("sw_p", false));
        switchFreeze.setChecked(prefs.getBoolean("sw_f", true));
        switchGhost.setChecked(prefs.getBoolean("sw_g", true));
        switchTele.setChecked(prefs.getBoolean("sw_t", true));
        if (switchDownloadBoost != null) {
            switchDownloadBoost.setChecked(prefs.getBoolean("sw_db", false));
        }
        if (switchReduceFpsDrop != null) {
            switchReduceFpsDrop.setChecked(prefs.getBoolean("sw_rf", true));
        }

        applySwitchStyle(switchPen);
        applySwitchStyle(switchFreeze);
        applySwitchStyle(switchGhost);
        applySwitchStyle(switchTele);
        if (switchDownloadBoost != null) applySwitchStyle(switchDownloadBoost);
        if (switchReduceFpsDrop != null) applySwitchStyle(switchReduceFpsDrop);

        refreshPermissionStatus();
        updateFreezeDropRangeStatus();
        updateGhostDropRangeStatus();
        updateFreezeTimeStatus();
        switchToTab(0, false);

        registerReceiverCompat(vpnRequestReceiver, new IntentFilter("FakePingZzz.Plus.md.PERM"));

        if (getIntent() != null && "FakePingZzz.Plus.md.PERM".equals(getIntent().getAction())) {
            m_rvp();
        }

        bindListeners();
        showTelegramGateIfNeeded();
        playMenuIntroAnimation();
    }

    private void showTelegramGateIfNeeded() {
        if (prefs == null) return;
        if (prefs.getBoolean(PREF_TELEGRAM_GATE_UNLOCKED, false)) {
            return;
        }
        boolean j1 = prefs.getBoolean(PREF_TELEGRAM_GATE_JOIN_1, false);
        boolean j2 = prefs.getBoolean(PREF_TELEGRAM_GATE_JOIN_2, false);
        long endAt = prefs.getLong(PREF_TELEGRAM_GATE_END_AT, 0L);
        if (j1 && j2 && endAt != 0L && System.currentTimeMillis() >= endAt) {
            clearTelegramGateState();
            return;
        }
        showTelegramGateDialog();
    }

    private void openTelegramGroup(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, t("Không mở được Telegram link.", "Could not open the Telegram link."), Toast.LENGTH_SHORT).show();
        }
    }

    private void clearTelegramGateState() {
        if (prefs != null) {
            prefs.edit()
                    .remove(PREF_TELEGRAM_GATE_JOIN_1)
                    .remove(PREF_TELEGRAM_GATE_JOIN_2)
                    .remove(PREF_TELEGRAM_GATE_END_AT)
                    .putBoolean(PREF_TELEGRAM_GATE_UNLOCKED, true)
                    .apply();
        }
    }

    private View _iosFrostOverlay = null;

    private void setAppDimmed(boolean dimmed) {
        if (mainShellView == null) return;
        mainShellView.animate().cancel();
        if (dimmed) {
            mainShellView.animate().alpha(0.6f).setDuration(200L).start();
            if (_iosFrostOverlay == null && mainShellView.getParent() instanceof android.view.ViewGroup) {
                _iosFrostOverlay = new View(MainActivity.this);
                _iosFrostOverlay.setBackgroundColor(0xCC060A11); 
                android.view.ViewGroup parent = (android.view.ViewGroup) mainShellView.getParent();
                android.view.ViewGroup.LayoutParams flp = new android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT);
                _iosFrostOverlay.setAlpha(0f);
                parent.addView(_iosFrostOverlay, flp);
                _iosFrostOverlay.animate().alpha(1f).setDuration(200L).start();
            }
        } else {
            mainShellView.animate().alpha(1f).setDuration(180L).start();
            if (_iosFrostOverlay != null && _iosFrostOverlay.getParent() instanceof android.view.ViewGroup) {
                final View overlay = _iosFrostOverlay;
                overlay.animate().alpha(0f).setDuration(180L)
                    .withEndAction(new Runnable() {
                        @Override public void run() {
                            try {
                                ((android.view.ViewGroup) overlay.getParent()).removeView(overlay);
                            } catch (Exception ignored) {}
                        }
                    }).start();
                _iosFrostOverlay = null;
            }
        }
    }

    private void showTelegramGateDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        final LinearLayout outer = makeDialogOuter();
        outer.setPadding(dp(16), dp(16), dp(16), dp(16));

        final LinearLayout card = makePrettyDialogCard(
                "📢",
                t("Join Telegram", "Join Telegram"),
                t("Tham gia đủ 2 nhóm để mở khóa toàn bộ chức năng của app.", "Join both groups to unlock all app features."),
                COLOR_TELE_CYAN);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        outer.addView(card, defaultLp());

        final TextView status = makeStatusView();
        status.setTextColor(COLOR_WHITE);
        status.setBackground(makeStatusBackground(COLOR_OFF, COLOR_STROKE));
        card.addView(status, marginTop(dp(14)));

        final TextView countdown = makeText("50s", 26, true, COLOR_WHITE);
        countdown.setGravity(Gravity.CENTER);
        countdown.setPadding(dp(12), dp(15), dp(12), dp(15));
        countdown.setBackground(makeAccentSoftBackground(COLOR_TELE_CYAN, dp(20)));
        card.addView(countdown, marginTop(dp(12)));

        TextView hint = makeText(
                t("Bấm vào 2 nút bên dưới, sau đó chờ đủ 50 giây.", "Tap both buttons below, then wait 50 seconds."),
                11, false, COLOR_WHITE_MUTED);
        hint.setGravity(Gravity.CENTER_HORIZONTAL);
        card.addView(hint, marginTop(dp(12)));

        final TextView btnJoin1 = makeDialogActionButton(t("Join Nhóm 1", "Join Group 1"), COLOR_TELE_CYAN, true);
        final TextView btnJoin2 = makeDialogActionButton(t("Join Nhóm 2", "Join Group 2"), COLOR_PURPLE, true);
        final TextView btnClose = makeDialogActionButton(t("Thoát", "Exit"), COLOR_DANGER, false);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lpRow = defaultLp();
        lpRow.topMargin = dp(14);
        row1.setLayoutParams(lpRow);

        LinearLayout.LayoutParams halfLp1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        halfLp1.rightMargin = dp(8);
        row1.addView(btnJoin1, halfLp1);
        row1.addView(btnJoin2, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(row1);

        card.addView(btnClose, marginTop(dp(12)));

        final boolean[] joined1 = {prefs.getBoolean(PREF_TELEGRAM_GATE_JOIN_1, false)};
        final boolean[] joined2 = {prefs.getBoolean(PREF_TELEGRAM_GATE_JOIN_2, false)};
        final long[] endAt = {prefs.getLong(PREF_TELEGRAM_GATE_END_AT, 0L)};
        final CountDownTimer[] timerHolder = new CountDownTimer[1];

        final Runnable refreshState = new Runnable() {
            @Override
            public void run() {
                status.setText(
                        t("Nhóm 1: ", "Group 1: ") + (joined1[0] ? t("Đã join", "Joined") : t("Chưa join", "Not joined")) + "\n" +
                        t("Nhóm 2: ", "Group 2: ") + (joined2[0] ? t("Đã join", "Joined") : t("Chưa join", "Not joined")) + "\n" +
                        t("Sau khi đủ 2 nhóm, chờ đủ 50 giây để mở khóa.", "After both groups are joined, wait 50 seconds to unlock.")
                );
                if (joined1[0] && joined2[0]) {
                    status.setBackground(makeStatusBackground(COLOR_SUCCESS_FILL, COLOR_SUCCESS_BORDER));
                } else {
                    status.setBackground(makeStatusBackground(COLOR_OFF, COLOR_STROKE));
                }

                if (joined1[0] && joined2[0] && endAt[0] > 0L) {
                    long remaining = endAt[0] - System.currentTimeMillis();
                    if (remaining < 0L) remaining = 0L;
                    long sec = (remaining + 999L) / 1000L;
                    countdown.setText(String.valueOf(sec) + "s");
                } else {
                    countdown.setText("50s");
                }
            }
        };

        btnJoin1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                joined1[0] = true;
                prefs.edit().putBoolean(PREF_TELEGRAM_GATE_JOIN_1, true).apply();
                openTelegramGroup(TELEGRAM_GROUP_1_URL);
                refreshState.run();
                maybeStartTelegramCountdown(dialog, countdown, status, joined1, joined2, endAt, timerHolder);
            }
        });

        btnJoin2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                joined2[0] = true;
                prefs.edit().putBoolean(PREF_TELEGRAM_GATE_JOIN_2, true).apply();
                openTelegramGroup(TELEGRAM_GROUP_2_URL);
                refreshState.run();
                maybeStartTelegramCountdown(dialog, countdown, status, joined1, joined2, endAt, timerHolder);
            }
        });

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (timerHolder[0] != null) timerHolder[0].cancel();
                finish();
            }
        });

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                setAppDimmed(true);
                Window w = dialog.getWindow();
                if (w != null) {
                    w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    w.setDimAmount(0f);
                    w.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                    int dialogWidth = Math.min(getResources().getDisplayMetrics().widthPixels - dp(28), dp(432));
                    w.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
                }
            }
        });

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface d) {
                setAppDimmed(false);
            }
        });

        if (joined1[0] && joined2[0] && endAt[0] != 0L && System.currentTimeMillis() >= endAt[0]) {
            clearTelegramGateState();
            Toast.makeText(this, t("Đã mở khóa toàn bộ chức năng!", "All features unlocked!"), Toast.LENGTH_SHORT).show();
            return;
        } else if (joined1[0] && joined2[0] && endAt[0] > System.currentTimeMillis()) {
            startTelegramCountdown(dialog, countdown, status, joined1, joined2, endAt[0], timerHolder);
        } else if (joined1[0] && joined2[0] && endAt[0] == 0L) {
            long newEnd = System.currentTimeMillis() + TELEGRAM_GATE_COUNTDOWN_MS;
            endAt[0] = newEnd;
            prefs.edit().putLong(PREF_TELEGRAM_GATE_END_AT, newEnd).apply();
            startTelegramCountdown(dialog, countdown, status, joined1, joined2, newEnd, timerHolder);
        } else {
            refreshState.run();
        }

        showPrettyDialog(dialog, outer);
    }

    private void maybeStartTelegramCountdown(Dialog dialog, TextView countdown, TextView status,
                                             boolean[] joined1, boolean[] joined2, long[] endAt,
                                             CountDownTimer[] timerHolder) {
        if (joined1[0] && joined2[0] && endAt[0] == 0L) {
            startTelegramCountdown(dialog, countdown, status, joined1, joined2, System.currentTimeMillis() + TELEGRAM_GATE_COUNTDOWN_MS, timerHolder);
        }
    }

    private void startTelegramCountdown(final Dialog dialog, final TextView countdown, final TextView status,
                                        final boolean[] joined1, final boolean[] joined2, final long targetEndAt,
                                        final CountDownTimer[] timerHolder) {
        if (timerHolder[0] != null) {
            timerHolder[0].cancel();
        }
        long now = System.currentTimeMillis();
        long remaining = targetEndAt - now;
        if (remaining <= 0L) {
            clearTelegramGateState();
            countdown.setText(t("Thành công", "Success"));
            status.setText(t("Đã mở khóa toàn bộ chức năng.", "All features unlocked."));
            status.setBackground(makeStatusBackground(COLOR_SUCCESS_FILL, COLOR_SUCCESS_BORDER));
            Toast.makeText(MainActivity.this,
                    t("Thành công! Bạn đã mở khóa chức năng.", "Success! Features unlocked."),
                    Toast.LENGTH_LONG).show();
            try {
                dialog.dismiss();
            } catch (Exception ignored) {}
            return;
        }
        prefs.edit().putLong(PREF_TELEGRAM_GATE_END_AT, targetEndAt).apply();

        timerHolder[0] = new CountDownTimer(remaining, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                long sec = (millisUntilFinished + 999L) / 1000L;
                countdown.setText(String.valueOf(sec) + "s");
                status.setText(
                        t("Nhóm 1: Đã join", "Group 1: Joined") + "\n" +
                        t("Nhóm 2: Đã join", "Group 2: Joined") + "\n" +
                        t("Đang đếm ngược để mở khóa chức năng.", "Counting down to unlock features.")
                );
                status.setBackground(makeStatusBackground(COLOR_SUCCESS_FILL, COLOR_SUCCESS_BORDER));
            }

            @Override
            public void onFinish() {
                clearTelegramGateState();
                countdown.setText(t("Thành công", "Success"));
                status.setText(t("Đã mở khóa toàn bộ chức năng.", "All features unlocked."));
                status.setBackground(makeStatusBackground(COLOR_SUCCESS_FILL, COLOR_SUCCESS_BORDER));
                Toast.makeText(MainActivity.this,
                        t("Thành công! Bạn đã mở khóa chức năng.", "Success! Features unlocked."),
                        Toast.LENGTH_LONG).show();
                try {
                    dialog.dismiss();
                } catch (Exception ignored) {}
                timerHolder[0] = null;
            }
        }.start();
    }

    private void bindListeners() {
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!SecurityManager.performSecurityCheck(MainActivity.this)) {
                    Toast.makeText(MainActivity.this, t("Ứng dụng đã bị chặn do môi trường không an toàn.", "The app was blocked due to an unsafe environment."), Toast.LENGTH_SHORT).show();
                    return;
                }
                // PATCHED: skip key check
                _uwts = true;
                if (!Settings.canDrawOverlays(MainActivity.this)) {
                    startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName())), REQ_OVERLAY);
                    return;
                }
                m_rvp();
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _svcUp = false;
                Intent vpn = new Intent(MainActivity.this, GameBoosterVpnService.class);
                vpn.setAction(GameBoosterVpnService.A_SX);
                startService(vpn);
                stopService(new Intent(MainActivity.this, MediaRenderService.class));
            }
        });

        seekSizeFreeze.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int v, boolean f) {
                int sz = v + 60;
                tvSizeFreeze.setText(sz + " dp");
                prefs.edit().putInt("sf_z", sz).apply();
                sendFloatingIntUpdate("UPDATE_SIZE_FREEZE", "size", sz);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        seekSizeGhost.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int v, boolean f) {
                int sz = v + 60;
                tvSizeGhost.setText(sz + " dp");
                prefs.edit().putInt("sg_z", sz).apply();
                sendFloatingIntUpdate("UPDATE_SIZE_GHOST", "size", sz);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        seekSizeTele.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int v, boolean f) {
                int sz = v + 60;
                tvSizeTele.setText(sz + " dp");
                prefs.edit().putInt("st_z", sz).apply();
                sendFloatingIntUpdate("UPDATE_SIZE_TELE", "size", sz);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        seekAlphaFreeze.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int v, boolean f) {
                tvAlphaFreeze.setText(v + "%");
                prefs.edit().putInt("af_a", v).apply();
                sendFloatingIntUpdate("UPDATE_ALPHA_FREEZE", "alpha", v);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        seekAlphaGhost.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int v, boolean f) {
                tvAlphaGhost.setText(v + "%");
                prefs.edit().putInt("ag_a", v).apply();
                sendFloatingIntUpdate("UPDATE_ALPHA_GHOST", "alpha", v);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        seekAlphaTele.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int v, boolean f) {
                tvAlphaTele.setText(v + "%");
                prefs.edit().putInt("at_a", v).apply();
                sendFloatingIntUpdate("UPDATE_ALPHA_TELE", "alpha", v);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        if (btnGrantOverlay != null) {
            btnGrantOverlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName())), REQ_OVERLAY);
                }
            });
        }

        if (btnGrantVpn != null) {
            btnGrantVpn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    m_rvp();
                }
            });
        }

        if (tabHome != null) {
            tabHome.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchToTab(0, true);
                }
            });
        }

        if (tabSettings != null) {
            tabSettings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchToTab(1, true);
                }
            });
        }

        if (tabFirewall != null) {
            tabFirewall.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchToTab(2, true);
                }
            });
        }

        if (btnFreezeDropConfig != null) {
            btnFreezeDropConfig.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showFreezeDropRangeDialog();
                }
            });
        }

        if (btnFreezeTimeConfig != null) {
            btnFreezeTimeConfig.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showFreezeTimeDialog();
                }
            });
        }

        if (btnGhostDropConfig != null) {
            btnGhostDropConfig.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showGhostDropRangeDialog();
                }
            });
        }

        switchPen.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean v) {
                animateSwitchState(b, v);
                prefs.edit().putBoolean("sw_p", v).apply();
                if (_svcUp) {
                    Intent i = new Intent(MainActivity.this, MediaRenderService.class);
                    i.setAction("UPDATE_TOGGLE_PEN");
                    i.putExtra("enabled", v);
                    startService(i);
                }
            }
        });

        switchFreeze.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean v) {
                animateSwitchState(b, v);
                prefs.edit().putBoolean("sw_f", v).apply();
                if (_svcUp) {
                    Intent i = new Intent(MainActivity.this, MediaRenderService.class);
                    i.setAction("UPDATE_TOGGLE_FREEZE");
                    i.putExtra("enabled", v);
                    startService(i);
                }
            }
        });

        switchGhost.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean v) {
                animateSwitchState(b, v);
                prefs.edit().putBoolean("sw_g", v).apply();
                if (_svcUp) {
                    Intent i = new Intent(MainActivity.this, MediaRenderService.class);
                    i.setAction("UPDATE_TOGGLE_GHOST");
                    i.putExtra("enabled", v);
                    startService(i);
                }
            }
        });

        switchTele.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean v) {
                animateSwitchState(b, v);
                prefs.edit().putBoolean("sw_t", v).apply();
                if (_svcUp) {
                    Intent i = new Intent(MainActivity.this, MediaRenderService.class);
                    i.setAction("UPDATE_TOGGLE_TELE");
                    i.putExtra("enabled", v);
                    startService(i);
                }
            }
        });

        if (switchDownloadBoost != null) {
            switchDownloadBoost.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton b, boolean v) {
                    animateSwitchState(b, v);
                    prefs.edit().putBoolean("sw_db", v).apply();
                    updateHomeAdvancedStatus();
                    if (_svcUp) {
                        Intent i = new Intent(MainActivity.this, GameBoosterVpnService.class);
                        i.setAction(GameBoosterVpnService.A_ST);
                        startService(i);
                    }
                    Toast.makeText(MainActivity.this,
                            v ? t("Đã bật Tăng Tốc VPN.", "VPN Boost enabled.")
                              : t("Đã tắt Tăng Tốc VPN.", "VPN Boost disabled."),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (switchReduceFpsDrop != null) {
            switchReduceFpsDrop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton b, boolean v) {
                    animateSwitchState(b, v);
                    prefs.edit().putBoolean("sw_rf", v).apply();
                    updateHomeAdvancedStatus();
                    if (_svcUp) {
                        Intent i = new Intent(MainActivity.this, MediaRenderService.class);
                        i.setAction("UPDATE_REDUCE_FPS_DROP");
                        i.putExtra("enabled", v);
                        startService(i);
                    }
                    Toast.makeText(MainActivity.this,
                            v ? t("Đã bật Giảm Drop FPS.", "Reduce FPS drop enabled.")
                              : t("Đã tắt Giảm Drop FPS.", "Reduce FPS drop disabled."),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private View buildMainContent() {
        FrameLayout root = new FrameLayout(this);
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setBackground(makeAppBackground());

        View orbTop = new View(this);
        orbTop.setBackground(makeOrbDrawable(0x556AA8FF));
        orbTop.setAlpha(0.9f);
        FrameLayout.LayoutParams orbTopLp = new FrameLayout.LayoutParams(dp(220), dp(220));
        orbTopLp.gravity = Gravity.TOP | Gravity.END;
        orbTopLp.topMargin = -dp(70);
        orbTopLp.rightMargin = -dp(40);
        root.addView(orbTop, orbTopLp);

        View orbBottom = new View(this);
        orbBottom.setBackground(makeOrbDrawable(0x225C9DFF));
        orbBottom.setAlpha(0.7f);
        FrameLayout.LayoutParams orbBottomLp = new FrameLayout.LayoutParams(dp(180), dp(180));
        orbBottomLp.gravity = Gravity.BOTTOM | Gravity.START;
        orbBottomLp.bottomMargin = -dp(60);
        orbBottomLp.leftMargin = -dp(30);
        root.addView(orbBottom, orbBottomLp);

        View orbCenter = new View(this);
        orbCenter.setBackground(makeOrbDrawable(0x18FFFFFF));
        orbCenter.setAlpha(0.65f);
        FrameLayout.LayoutParams orbCenterLp = new FrameLayout.LayoutParams(dp(140), dp(140));
        orbCenterLp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        orbCenterLp.topMargin = dp(180);
        root.addView(orbCenter, orbCenterLp);

        SnowfallView snow = new SnowfallView(this);
        snow.setClickable(false);
        snow.setFocusable(false);
        root.addView(snow, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        mainShellView = shell;

        FrameLayout contentHolder = new FrameLayout(this);
        LinearLayout.LayoutParams contentLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        contentHolder.setLayoutParams(contentLp);

        homePageView = buildHomePage();
        settingsPageView = buildSettingsPage();
        firewallPageView = buildFirewallPage();
        contentHolder.addView(homePageView);
        contentHolder.addView(settingsPageView);
        contentHolder.addView(firewallPageView);

        shell.addView(contentHolder);
        bottomNavigationView = buildBottomNavigation();
        shell.addView(bottomNavigationView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(shell);
        return root;
    }

    private void playMenuIntroAnimation() {
        if (hasPlayedMenuIntro) return;

        final View menuView = homePageView;
        final View navView = bottomNavigationView;
        final LinearLayout container = homePageContainer;
        if (menuView == null || navView == null) return;

        hasPlayedMenuIntro = true;

        if (mainShellView != null) {
            mainShellView.setAlpha(1f);
            mainShellView.setTranslationY(0f);
        }

        menuView.animate().cancel();
        navView.animate().cancel();

        menuView.setAlpha(0f);
        menuView.setTranslationY(dp(28));
        menuView.setScaleX(0.97f);
        menuView.setScaleY(0.97f);

        navView.setAlpha(0f);
        navView.setTranslationY(dp(36));
        navView.setScaleX(0.96f);
        navView.setScaleY(0.96f);

        if (container != null) {
            for (int i = 0; i < container.getChildCount(); i++) {
                View child = container.getChildAt(i);
                child.animate().cancel();
                child.setAlpha(0f);
                child.setTranslationY(dp(18));
                child.setScaleX(0.96f);
                child.setScaleY(0.96f);
            }
        }

        menuView.post(new Runnable() {
            @Override
            public void run() {
                menuView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                menuView.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(520)
                        .setInterpolator(new android.view.animation.OvershootInterpolator(0.6f))
                        .withEndAction(new Runnable() {
                            @Override public void run() {
                                try { menuView.setLayerType(View.LAYER_TYPE_NONE, null); } catch (Exception ignored) {}
                            }
                        })
                        .start();

                if (container != null) {
                    for (int i = 0; i < container.getChildCount(); i++) {
                        final View child = container.getChildAt(i);
                        final long delay = 60L + (i * 55L);
                        child.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                child.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                                child.animate()
                                        .alpha(1f)
                                        .translationY(0f)
                                        .scaleX(1f)
                                        .scaleY(1f)
                                        .setDuration(480)
                                        .setInterpolator(new android.view.animation.OvershootInterpolator(0.55f))
                                        .withEndAction(new Runnable() {
                                            @Override public void run() {
                                                try { child.setLayerType(View.LAYER_TYPE_NONE, null); } catch (Exception ignored) {}
                                            }
                                        })
                                        .start();
                            }
                        }, delay);
                    }
                }

                navView.postDelayed(new Runnable() {
                    @Override public void run() {
                        navView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                        navView.animate()
                                .alpha(1f)
                                .translationY(0f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(440)
                                .setInterpolator(new android.view.animation.OvershootInterpolator(0.7f))
                                .withEndAction(new Runnable() {
                                    @Override public void run() {
                                        try { navView.setLayerType(View.LAYER_TYPE_NONE, null); } catch (Exception ignored) {}
                                    }
                                })
                                .start();
                    }
                }, 240L);
            }
        });
    }

    private View buildHomePage() {
        ScrollView scrollView = createPageScroll();
        LinearLayout container = createPageContainer();
        homePageContainer = container;
        scrollView.addView(container, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        container.addView(buildHeaderCard());
        // PATCHED: activation card removed
        container.addView(buildPermissionCard());
        container.addView(buildLanguageCard());
        container.addView(buildActionCard());
        container.addView(buildHomeAdvancedCard());
        container.addView(buildSizeCard());
        container.addView(buildOpacityCard());
        container.addView(buildFooterCard());
        return scrollView;
    }

    private View buildSettingsPage() {
        ScrollView scrollView = createPageScroll();
        LinearLayout container = createPageContainer();
        settingsPageContainer = container;
        scrollView.addView(container, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        container.addView(buildSettingsHeaderCard());
        container.addView(buildSettingsToggleCard());
        container.addView(buildSettingsAdvancedCard());
        container.addView(buildFreezeTimeCard());
        container.addView(buildFreezeDropRangeCard());
        container.addView(buildGhostDropRangeCard());
        container.addView(buildFooterCard());
        return scrollView;
    }

    private View buildFirewallPage() {
        ScrollView scrollView = createPageScroll();
        LinearLayout container = createPageContainer();
        firewallPageContainer = container;
        scrollView.addView(container, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        container.addView(buildFirewallHeaderCard());
        container.addView(buildFirewallMasterCard());
        container.addView(buildFirewallAddActionCard());
        container.addView(buildFirewallListCard(false));
        container.addView(buildFirewallListCard(true));
        container.addView(buildFooterCard());
        refreshFirewallLists();
        return scrollView;
    }

    private ScrollView createPageScroll() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scrollView.setVerticalScrollBarEnabled(false);
        return scrollView;
    }

    private LinearLayout createPageContainer() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pagePadding = dp(16);
        container.setPadding(pagePadding, dp(18), pagePadding, dp(28));
        return container;
    }

    private View buildBottomNavigation() {
        LinearLayout navWrap = new LinearLayout(this);
        navWrap.setOrientation(LinearLayout.VERTICAL);
        navWrap.setPadding(dp(14), dp(8), dp(14), dp(20));
        navWrap.setClipToPadding(false);
        navWrap.setClipChildren(false);

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(dp(8), dp(8), dp(8), dp(8));
        nav.setClipToPadding(false);
        nav.setClipChildren(false);
        nav.setBackground(makeBottomNavBackground());

        tabHome = makeTabButton("⊙", t("Home", "Home"));
        LinearLayout.LayoutParams tabLp1 = new LinearLayout.LayoutParams(0, dp(64), 1f);
        tabLp1.rightMargin = dp(6);
        nav.addView(tabHome, tabLp1);

        tabSettings = makeTabButton("⚙", t("Setting", "Setting"));
        LinearLayout.LayoutParams tabLp2 = new LinearLayout.LayoutParams(0, dp(64), 1f);
        tabLp2.rightMargin = dp(6);
        nav.addView(tabSettings, tabLp2);

        tabFirewall = makeTabButton("◈", t("Tường Lửa", "Firewall"));
        nav.addView(tabFirewall, new LinearLayout.LayoutParams(0, dp(64), 1f));

        navWrap.addView(nav);
        return navWrap;
    }

    private View buildSettingsHeaderCard() {
        LinearLayout card = makeCard(true);
        card.addView(makeSectionTitle(t("Setting", "Setting")));
        card.addView(makeSectionSubTitle(t("Bật hoặc tắt nhanh từng thành phần hiển thị trên màn hình.", "Quickly turn on or off each element shown on screen.")), marginTop(dp(6)));

        TextView desc = makeText(t("Bạn có thể điều khiển Tâm ảo, Freeze, Ghost và Tele ngay trong tab này.", "You can control Virtual Aim, Freeze, Ghost and Tele right in this tab."), 12, false, COLOR_WHITE_SOFT);
        desc.setLineSpacing(0f, 1.15f);
        card.addView(desc, marginTop(dp(14)));
        return card;
    }

    private View buildPermissionCard() {
        LinearLayout card = makeCard(false);
        card.addView(makeSectionTitle(t("Cấp quyền", "Permissions")));
        card.addView(makeSectionSubTitle(t("Kiểm tra quyền hiển thị nổi và quyền VPN trước khi chạy.", "Check overlay and VPN permissions before running.")), marginTop(dp(6)));

        tvOverlayStatus = makeStatusView();
        LinearLayout.LayoutParams overlayLp = defaultLp();
        overlayLp.topMargin = dp(16);
        card.addView(tvOverlayStatus, overlayLp);

        btnGrantOverlay = makeButton(t("Cấp quyền overlay", "Grant overlay permission"), false);
        LinearLayout.LayoutParams overlayBtnLp = defaultLp();
        overlayBtnLp.topMargin = dp(10);
        card.addView(btnGrantOverlay, overlayBtnLp);

        tvVpnStatus = makeStatusView();
        LinearLayout.LayoutParams vpnLp = defaultLp();
        vpnLp.topMargin = dp(16);
        card.addView(tvVpnStatus, vpnLp);

        btnGrantVpn = makeButton(t("Cấp quyền VPN", "Grant VPN permission"), false);
        LinearLayout.LayoutParams vpnBtnLp = defaultLp();
        vpnBtnLp.topMargin = dp(10);
        card.addView(btnGrantVpn, vpnBtnLp);
        return card;
    }

    private View buildHeaderCard() {
        LinearLayout card = makeCard(true);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);

        FrameLayout badge = new FrameLayout(this);
        badge.setBackground(makeBadgeBackground());
        badge.setPadding(dp(3), dp(3), dp(3), dp(3));

        ImageView avatar = new ImageView(this);
        avatar.setImageResource(R.drawable.rp_avatar);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable avatarShape = new GradientDrawable();
        avatarShape.setShape(GradientDrawable.OVAL);
        avatarShape.setColor(COLOR_PANEL_INNER);
        avatar.setBackground(avatarShape);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            avatar.setClipToOutline(true);
        }

        FrameLayout.LayoutParams avatarLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        badge.addView(avatar, avatarLp);

        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(dp(76), dp(76));
        badgeLp.rightMargin = dp(16);
        card.addView(badge, badgeLp);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textsLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);

        TextView title = makeText("RealPing", 28, true, COLOR_WHITE);
        title.setLetterSpacing(0.04f);
        texts.addView(title);

        TextView subtitle = makeText(t("Antiban · Giảm ping · Tối ưu FPS · Kết nối ổn định.", "Antiban · Ping reduction · FPS boost · Stable connection."), 12, false, COLOR_WHITE_SOFT);
        subtitle.setLineSpacing(0f, 1.5f);
        LinearLayout.LayoutParams subtitleLp = defaultLp();
        subtitleLp.topMargin = dp(5);
        texts.addView(subtitle, subtitleLp);

        LinearLayout chipRow = new LinearLayout(this);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams chipRowLp = defaultLp();
        chipRowLp.topMargin = dp(12);
        chipRow.addView(makeChip(t("Antiban", "Antiban"), true));
        TextView chip2 = makeChip(t("Fix999+", "Fix999+"), false);
        LinearLayout.LayoutParams chip2Lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        chip2Lp.leftMargin = dp(8);
        chipRow.addView(chip2, chip2Lp);
        TextView chip3 = makeChip(t("Nâng Cấp", "Update"), false);
        LinearLayout.LayoutParams chip3Lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        chip3Lp.leftMargin = dp(8);
        chipRow.addView(chip3, chip3Lp);
        texts.addView(chipRow, chipRowLp);

        TextView miniInfo = makeText("Package: RealPing.Plus", 11, false, COLOR_WHITE_MUTED);
        LinearLayout.LayoutParams miniInfoLp = defaultLp();
        miniInfoLp.topMargin = dp(10);
        texts.addView(miniInfo, miniInfoLp);

        card.addView(texts, textsLp);
        return card;
    }

    

    private View buildLanguageCard() {
        LinearLayout card = makeCard(false);
        card.addView(makeSectionTitle(t("Tùy chỉnh", "Customization")));
        card.addView(makeSectionSubTitle(t("Đổi nhanh ngôn ngữ giao diện ngay trong Home.", "Quickly switch the interface language right from Home.")), marginTop(dp(6)));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp = defaultLp();
        rowLp.topMargin = dp(14);
        row.setLayoutParams(rowLp);

        final TextView viChip = makeChip(t("Tiếng Việt", "Vietnamese"), !isEnglish);
        final TextView enChip = makeChip("English", isEnglish);

        LinearLayout.LayoutParams viLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        viLp.rightMargin = dp(8);
        row.addView(viChip, viLp);
        row.addView(enChip, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        viChip.setGravity(Gravity.CENTER);
        enChip.setGravity(Gravity.CENTER);

        viChip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyLanguage("vi");
            }
        });

        enChip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyLanguage("en");
            }
        });

        card.addView(row);
        return card;
    }

    private View buildActionCard() {
        LinearLayout card = makeCard(false);
        card.addView(makeSectionTitle(t("Điều khiển nhanh", "Quick Controls")));
        card.addView(makeSectionSubTitle(t("Khởi động hoặc dừng toàn bộ overlay/VPN nhanh từ Home.", "Start or stop all overlay/VPN features quickly from Home.")), marginTop(dp(6)));

        // Hero Start button - full width
        btnStart = makeButton(t("Khởi chạy", "Start"), true);
        btnStart.setBackground(makeActionButtonOn());
        btnStart.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f);
        btnStart.setMinHeight(dp(64));
        LinearLayout.LayoutParams startLp = defaultLp();
        startLp.topMargin = dp(18);
        card.addView(btnStart, startLp);

        // Stop button - smaller, below
        btnStop = makeButton(t("■  Dừng", "■  Stop"), false);
        btnStop.setBackground(makeActionButtonDanger());
        btnStop.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        btnStop.setMinHeight(dp(52));
        LinearLayout.LayoutParams stopLp = defaultLp();
        stopLp.topMargin = dp(10);
        card.addView(btnStop, stopLp);
        return card;
    }

    private View buildHomeAdvancedCard() {
        LinearLayout card = makeCard(false);
        card.addView(makeSectionTitle(t("Nâng Cao", "Advanced")));
        card.addView(makeSectionSubTitle(
                t("Các tuỳ chọn tối ưu cho VPN, FPS và tường lửa.",
                        "Optimization options for VPN, FPS and firewall.")),
                marginTop(dp(6)));

        boolean vpnEnabled = prefs != null && prefs.getBoolean("sw_db", false);
        boolean fpsEnabled = prefs != null && prefs.getBoolean("sw_rf", true);
        boolean firewallEnabled = prefs != null && prefs.getBoolean("fw_e", false);

        tvHomeVpnBoostStatus = makeStatusView();
        tvHomeVpnBoostStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvHomeVpnBoostStatus.setGravity(Gravity.CENTER_VERTICAL);
        tvHomeVpnBoostStatus.setBackground(makeAccentStatusBackground(COLOR_TELE_CYAN));
        card.addView(tvHomeVpnBoostStatus, marginTop(dp(14)));

        tvHomeAdvancedStatus = makeStatusView();
        tvHomeAdvancedStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvHomeAdvancedStatus.setGravity(Gravity.CENTER_VERTICAL);
        tvHomeAdvancedStatus.setBackground(makeAccentStatusBackground(COLOR_PURPLE));
        card.addView(tvHomeAdvancedStatus, marginTop(dp(12)));
        updateHomeAdvancedStatus();

        Button quickEnable = makeButton(vpnEnabled
                ? t("⚡  Đã bật Tăng Tốc VPN", "⚡  VPN Boost Enabled")
                : t("⚡  Bật Tăng Tốc VPN", "⚡  Enable VPN Boost"), false);
        quickEnable.setBackground(makeAccentActionButton(COLOR_TELE_CYAN));
        quickEnable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.edit().putBoolean("sw_db", true).apply();
                updateHomeAdvancedStatus();
                Toast.makeText(MainActivity.this,
                        t("Đã bật Tăng Tốc VPN.", "VPN Boost enabled."),
                        Toast.LENGTH_SHORT).show();
                recreate();
            }
        });
        card.addView(quickEnable, marginTop(dp(12)));
        return card;
    }

    private void updateHomeAdvancedStatus() {
        if (prefs == null) return;
        boolean vpnEnabled = prefs.getBoolean("sw_db", false);
        boolean fpsEnabled = prefs.getBoolean("sw_rf", true);
        boolean firewallEnabled = prefs.getBoolean("fw_e", false);

        if (tvHomeVpnBoostStatus != null) {
            tvHomeVpnBoostStatus.setText("⚡  " + t("Tăng Tốc VPN", "VPN Boost") + "\n" +
                    (vpnEnabled
                            ? t("Trạng thái: ĐANG BẬT. Tối ưu buffer mạng và kết nối VPN đã được áp dụng.",
                                "Status: ON. Network buffer and VPN connection optimizations are applied.")
                            : t("Trạng thái: ĐANG TẮT. Bật trong Setting > Nâng Cao để dùng.",
                                "Status: OFF. Turn it on in Setting > Advanced.")));
        }

        if (tvHomeAdvancedStatus != null) {
            tvHomeAdvancedStatus.setText("🎮  " + t("Giảm Drop FPS", "Reduce FPS Drop") + "\n" +
                    (fpsEnabled
                            ? t("Trạng thái: ĐANG BẬT. Hiệu ứng overlay đã được giảm tải để mượt hơn.",
                                "Status: ON. Overlay effects are reduced for smoother performance.")
                            : t("Trạng thái: ĐANG TẮT. Overlay sẽ giữ hiệu ứng đầy đủ.",
                                "Status: OFF. Overlay keeps full visual effects.")) +
                    "\n\n🛡  " + t("Tường Lửa", "Firewall") + ": " +
                    (firewallEnabled ? t("ĐANG BẬT", "ON") : t("ĐANG TẮT", "OFF")));
        }
    }

    private View buildSettingsAdvancedCard() {
        LinearLayout card = makeSettingControlCard(
                "⚡",
                t("Nâng Cao", "Advanced"),
                t("Các cấu hình nâng cao dành cho tăng tốc VPN và giảm drop FPS.",
                        "Advanced settings for VPN boost and reducing FPS drops."),
                COLOR_TELE_CYAN);

        card.addView(makeMiniInfoStrip(
                t("VPN BOOST", "VPN BOOST"),
                t("Tối ưu tốc độ và buffer kết nối VPN", "Optimize VPN speed and connection buffers"),
                COLOR_TELE_CYAN), marginTop(dp(14)));

        TextView info = makeStatusView();
        info.setText("🚀  " + t("Tăng Tốc VPN", "VPN Boost") + "\n" +
                t("Khi bật, VPN service sẽ tăng buffer mạng và ưu tiên cấu hình ổn định hơn để tải dữ liệu mượt hơn.",
                        "When enabled, the VPN service increases network buffers and prefers a more stable configuration for smoother data transfer."));
        info.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        info.setGravity(Gravity.CENTER_VERTICAL);
        info.setBackground(makeAccentStatusBackground(COLOR_TELE_CYAN));
        card.addView(info, marginTop(dp(12)));

        switchDownloadBoost = makeSwitch();
        card.addView(makeSwitchRow(
                t("Tăng Tốc VPN", "VPN Boost"),
                t("Bật tối ưu mạng trong VPN service.",
                        "Enable network optimization inside the VPN service."),
                switchDownloadBoost));

        switchReduceFpsDrop = makeSwitch();
        card.addView(makeSwitchRow(
                t("Giảm Drop FPS", "Reduce FPS Drop"),
                t("Giảm tải hiệu ứng overlay để mượt hơn khi đang chơi.",
                        "Reduce overlay effect load for smoother gameplay."),
                switchReduceFpsDrop));

        Button quickEnable = makeButton(t("⚡  Áp dụng tối ưu", "⚡  Apply Optimization"), false);
        quickEnable.setBackground(makeAccentActionButton(COLOR_TELE_CYAN));
        quickEnable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchDownloadBoost != null && !switchDownloadBoost.isChecked()) {
                    switchDownloadBoost.setChecked(true);
                }
                if (switchReduceFpsDrop != null && !switchReduceFpsDrop.isChecked()) {
                    switchReduceFpsDrop.setChecked(true);
                }
                Toast.makeText(MainActivity.this,
                        t("Đã áp dụng tối ưu Nâng Cao.", "Advanced optimization applied."),
                        Toast.LENGTH_SHORT).show();
            }
        });
        card.addView(quickEnable, marginTop(dp(12)));
        return card;
    }

    private View buildFirewallHeaderCard() {
        LinearLayout card = makeCard(true);
        card.addView(makeSectionTitle(t("Tường Lửa", "Firewall")));
        card.addView(makeSectionSubTitle(
                t("Quản lý riêng danh sách IP và tên miền bị chặn ngay trong app.",
                        "Manage blocked IP and domain lists directly inside the app.")), marginTop(dp(6)));

        TextView desc = makeText(
                t("Giao diện mới cho phép thêm và xoá từng IP / domain riêng lẻ, đẹp và dễ nhìn hơn.",
                        "The new UI lets you add and remove each IP / domain individually with a cleaner layout."),
                12, false, COLOR_WHITE_SOFT);
        desc.setLineSpacing(0f, 1.15f);
        card.addView(desc, marginTop(dp(14)));
        return card;
    }

    private View buildFirewallMasterCard() {
        LinearLayout card = makeSettingControlCard(
                "🛡",
                t("Điều Khiển Tường Lửa", "Firewall Control"),
                t("Bật hoặc tắt toàn bộ tường lửa trong app.",
                        "Enable or disable the entire firewall in the app."),
                COLOR_PURPLE);

        final Switch firewallSwitch = makeSwitch();
        firewallSwitch.setChecked(prefs.getBoolean("fw_e", false));
        applySwitchStyle(firewallSwitch);
        card.addView(makeSwitchRow(
                t("Bật Tường Lửa", "Enable Firewall"),
                t("Khi bật, app sẽ chặn IP và domain trong danh sách bên dưới.",
                        "When enabled, the app blocks the IPs and domains listed below."),
                firewallSwitch));

        tvFirewallSummary = makeStatusView();
        tvFirewallSummary.setText(buildFirewallSummary());
        tvFirewallSummary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvFirewallSummary.setGravity(Gravity.CENTER_VERTICAL);
        tvFirewallSummary.setBackground(makeAccentStatusBackground(COLOR_PURPLE));
        card.addView(tvFirewallSummary, marginTop(dp(12)));

        firewallSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                animateSwitchState(buttonView, isChecked);
                prefs.edit().putBoolean("fw_e", isChecked).apply();
                if (tvFirewallSummary != null) tvFirewallSummary.setText(buildFirewallSummary());
                updateHomeAdvancedStatus();
                notifyFirewallConfigChanged();
                Toast.makeText(MainActivity.this,
                        isChecked ? t("Đã bật Tường Lửa.", "Firewall enabled.")
                                  : t("Đã tắt Tường Lửa.", "Firewall disabled."),
                        Toast.LENGTH_SHORT).show();
            }
        });
        return card;
    }

    private View buildFirewallAddActionCard() {
        LinearLayout card = makeCard(false);
        card.addView(makeSectionTitle(t("Thêm Mục Chặn", "Add Blocked Item")));
        card.addView(makeSectionSubTitle(
                t("Thêm nhanh từng IP hoặc tên miền riêng lẻ.",
                        "Quickly add individual IPs or domain names.")), marginTop(dp(6)));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp = defaultLp();
        rowLp.topMargin = dp(18);
        row.setLayoutParams(rowLp);

        Button btnAddIp = makeButton(t("+ Thêm IP", "+ Add IP"), false);
        btnAddIp.setBackground(makeAccentActionButton(COLOR_TELE_CYAN));
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, dp(54), 1f);
        lp1.rightMargin = dp(8);
        row.addView(btnAddIp, lp1);

        Button btnAddDomain = makeButton(t("+ Thêm Domain", "+ Add Domain"), false);
        btnAddDomain.setBackground(makeAccentActionButton(COLOR_PURPLE));
        row.addView(btnAddDomain, new LinearLayout.LayoutParams(0, dp(54), 1f));

        btnAddIp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddFirewallItemDialog(false);
            }
        });

        btnAddDomain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddFirewallItemDialog(true);
            }
        });

        card.addView(row);
        return card;
    }

    private View buildFirewallListCard(boolean domainList) {
        int accent = domainList ? COLOR_PURPLE : COLOR_TELE_CYAN;
        LinearLayout card = makeCard(false);
        card.addView(makeSectionTitle(domainList
                ? t("Danh Sách Domain", "Domain List")
                : t("Danh Sách IP", "IP List")));
        card.addView(makeSectionSubTitle(domainList
                ? t("Các tên miền sẽ bị chặn khi tường lửa bật.", "Domains that will be blocked when the firewall is enabled.")
                : t("Các địa chỉ IP sẽ bị chặn khi tường lửa bật.", "IP addresses that will be blocked when the firewall is enabled.")), marginTop(dp(6)));

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listLp = defaultLp();
        listLp.topMargin = dp(14);
        list.setLayoutParams(listLp);
        list.setBackground(makeInnerPanelBackground(accent));
        list.setPadding(dp(12), dp(12), dp(12), dp(12));

        if (domainList) {
            firewallDomainListContainer = list;
        } else {
            firewallIpListContainer = list;
        }

        card.addView(list);
        return card;
    }

    private String buildFirewallSummary() {
        boolean enabled = prefs != null && prefs.getBoolean("fw_e", false);
        int ipCount = getFirewallItems(false).size();
        int domainCount = getFirewallItems(true).size();
        return "🛡  " + t("Tường Lửa", "Firewall") + "\n" +
                t("Trạng thái: ", "Status: ") + (enabled ? t("ĐANG BẬT", "ON") : t("ĐANG TẮT", "OFF")) +
                "\n" + t("IP đang chặn: ", "Blocked IPs: ") + ipCount +
                "\n" + t("Tên miền đang chặn: ", "Blocked domains: ") + domainCount;
    }

    private int countNonEmptyLines(String raw) {
        if (raw == null || raw.trim().isEmpty()) return 0;
        String[] parts = raw.split("\n");
        int count = 0;
        for (String part : parts) {
            if (part != null && !part.trim().isEmpty()) count++;
        }
        return count;
    }

    private String sanitizeFirewallLines(String raw) {
        if (raw == null) return "";
        String normalized = raw.replace("\r\n", "\n").replace("\r", "\n");
        String[] parts = normalized.split("\n");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part == null) continue;
            String line = part.trim();
            if (line.isEmpty()) continue;
            if (sb.length() > 0) sb.append("\n");
            sb.append(line);
        }
        return sb.toString();
    }

    private List<String> getFirewallItems(boolean domains) {
        String key = domains ? "fw_d" : "fw_i";
        String raw = prefs != null ? prefs.getString(key, "") : "";
        String normalized = sanitizeFirewallLines(raw);
        Set<String> unique = new LinkedHashSet<String>();
        if (!normalized.isEmpty()) {
            String[] parts = normalized.split("\n");
            for (String part : parts) {
                if (part == null) continue;
                String item = part.trim();
                if (item.isEmpty()) continue;
                unique.add(domains ? item.toLowerCase() : item);
            }
        }
        return new ArrayList<String>(unique);
    }

    private void saveFirewallItems(boolean domains, List<String> items) {
        String key = domains ? "fw_d" : "fw_i";
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            if (item == null) continue;
            String cleaned = item.trim();
            if (cleaned.isEmpty()) continue;
            if (sb.length() > 0) sb.append("\n");
            sb.append(domains ? cleaned.toLowerCase() : cleaned);
        }
        prefs.edit().putString(key, sb.toString()).apply();
    }

    private void notifyFirewallConfigChanged() {
        if (_svcUp) {
            Intent i = new Intent(MainActivity.this, GameBoosterVpnService.class);
            i.setAction(GameBoosterVpnService.A_ST);
            startService(i);
        }
        updateHomeAdvancedStatus();
        refreshFirewallLists();
    }

    private void refreshFirewallLists() {
        if (tvFirewallSummary != null) tvFirewallSummary.setText(buildFirewallSummary());
        refreshFirewallListContainer(false);
        refreshFirewallListContainer(true);
    }

    private void refreshFirewallListContainer(final boolean domains) {
        LinearLayout target = domains ? firewallDomainListContainer : firewallIpListContainer;
        if (target == null) return;

        target.removeAllViews();
        List<String> items = getFirewallItems(domains);
        if (items.isEmpty()) {
            TextView empty = makeText(
                    domains ? t("Chưa có domain nào được thêm.", "No domains added yet.")
                            : t("Chưa có IP nào được thêm.", "No IPs added yet."),
                    12, false, COLOR_WHITE_SOFT);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(12), dp(14), dp(12), dp(14));
            target.addView(empty, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            final String item = items.get(i);
            if (i > 0) {
                View divider = new View(this);
                divider.setBackgroundColor(withAlpha(COLOR_WHITE, 24));
                target.addView(divider, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
            }
            target.addView(makeFirewallItemRow(item, domains));
        }
    }

    private View makeFirewallItemRow(final String item, final boolean domains) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(10), dp(10), dp(10));

        TextView value = makeText(item, 13, true, COLOR_WHITE);
        value.setSingleLine(false);
        value.setLineSpacing(0f, 1.1f);
        LinearLayout.LayoutParams valueLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        valueLp.rightMargin = dp(10);
        row.addView(value, valueLp);

        Button delete = makeButton(t("Xoá", "Delete"), false);
        delete.setMinWidth(dp(86));
        delete.setBackground(makeAccentActionButton(COLOR_DANGER));
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> items = getFirewallItems(domains);
                items.remove(item);
                saveFirewallItems(domains, items);
                notifyFirewallConfigChanged();
                Toast.makeText(MainActivity.this,
                        domains ? t("Đã xoá domain.", "Domain removed.")
                                : t("Đã xoá IP.", "IP removed."),
                        Toast.LENGTH_SHORT).show();
            }
        });
        row.addView(delete, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private void showAddFirewallItemDialog(final boolean domains) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        final LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackground(makeCardBackground(true));

        root.addView(makeSectionTitle(domains ? t("Thêm Domain", "Add Domain") : t("Thêm IP", "Add IP")));
        root.addView(makeSectionSubTitle(
                domains ? t("Nhập 1 tên miền cần chặn.", "Enter one domain to block.")
                        : t("Nhập 1 địa chỉ IP cần chặn.", "Enter one IP address to block.")), marginTop(dp(6)));

        final EditText input = new EditText(this);
        input.setTextColor(COLOR_WHITE);
        input.setHintTextColor(COLOR_WHITE_MUTED);
        input.setBackground(makeInnerPanelBackground(domains ? COLOR_PURPLE : COLOR_TELE_CYAN));
        input.setPadding(dp(14), dp(14), dp(14), dp(14));
        input.setSingleLine(true);
        input.setHint(domains ? "example.com" : "1.1.1.1");
        if (!domains) input.setInputType(InputType.TYPE_CLASS_TEXT);
        root.addView(input, marginTop(dp(14)));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams btnLp = defaultLp();
        btnLp.topMargin = dp(16);
        buttons.setLayoutParams(btnLp);

        Button cancel = makeButton(t("Huỷ", "Cancel"), false);
        cancel.setBackground(makeAccentActionButton(COLOR_DANGER));
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(0, dp(52), 1f);
        cancelLp.rightMargin = dp(8);
        buttons.addView(cancel, cancelLp);

        Button save = makeButton(t("Thêm", "Add"), false);
        save.setBackground(makeAccentActionButton(domains ? COLOR_PURPLE : COLOR_TELE_CYAN));
        buttons.addView(save, new LinearLayout.LayoutParams(0, dp(52), 1f));

        root.addView(buttons);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = String.valueOf(input.getText()).trim();
                if (value.isEmpty()) {
                    Toast.makeText(MainActivity.this,
                            domains ? t("Vui lòng nhập domain.", "Please enter a domain.")
                                    : t("Vui lòng nhập IP.", "Please enter an IP."),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                List<String> items = getFirewallItems(domains);
                String finalValue = domains ? value.toLowerCase() : value;
                if (!items.contains(finalValue)) {
                    items.add(finalValue);
                    saveFirewallItems(domains, items);
                    notifyFirewallConfigChanged();
                    Toast.makeText(MainActivity.this,
                            domains ? t("Đã thêm domain mới.", "New domain added.")
                                    : t("Đã thêm IP mới.", "New IP added."),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this,
                            domains ? t("Domain này đã tồn tại.", "This domain already exists.")
                                    : t("IP này đã tồn tại.", "This IP already exists."),
                            Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            }
        });

        dialog.setContentView(root);
        final Window wfw = dialog.getWindow();
        if (wfw != null) {
            wfw.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            wfw.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        root.setAlpha(0f);
        root.setScaleX(0.3f);
        root.setScaleY(0.3f);

        dialog.show();

        root.post(new Runnable() {
            @Override public void run() {
                root.setPivotX(root.getWidth() / 2f);
                root.setPivotY(root.getHeight() / 2f);
                root.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                root.animate().cancel();
                root.animate()
                        .alpha(1f).scaleX(1f).scaleY(1f)
                        .setDuration(360)
                        .setInterpolator(new android.view.animation.OvershootInterpolator(0.72f))
                        .withEndAction(new Runnable() {
                            @Override public void run() {
                                try { root.setLayerType(View.LAYER_TYPE_NONE, null); } catch (Exception ignored) {}
                            }
                        })
                        .start();
            }
        });
    }

    private View buildSizeCard() {

        LinearLayout card = makeCard(false);
        card.addView(makeSectionTitle(t("Chỉnh to nhỏ nút", "Button Size")));
        card.addView(makeSectionSubTitle(t("Tăng hoặc giảm kích thước cho từng nút nổi trên màn hình.", "Increase or decrease the size of each floating button on screen.")), marginTop(dp(6)));

        seekSizeFreeze = makeSeekBar(220, COLOR_ON_GREEN, COLOR_TELE_CYAN);
        tvSizeFreeze = makeValueText(COLOR_ON_GREEN);
        card.addView(makeSliderItem("Freeze", t("Nút chính để thao tác nhanh", "Main button for quick actions"), seekSizeFreeze, tvSizeFreeze, COLOR_ON_GREEN));

        seekSizeGhost = makeSeekBar(220, COLOR_PURPLE, COLOR_GHOST_PINK);
        tvSizeGhost = makeValueText(COLOR_GHOST_PURPLE);
        card.addView(makeSliderItem("Ghost", t("Nút phụ dùng cho thao tác hỗ trợ", "Secondary button for support actions"), seekSizeGhost, tvSizeGhost, COLOR_GHOST_PURPLE));

        seekSizeTele = makeSeekBar(220, COLOR_ACCENT, COLOR_TELE_CYAN);
        tvSizeTele = makeValueText(COLOR_ACCENT);
        card.addView(makeSliderItem("Tele", t("Nút tác vụ tốc độ cao", "High speed action button"), seekSizeTele, tvSizeTele, COLOR_ACCENT));
        return card;
    }

    private View buildOpacityCard() {
        LinearLayout card = makeCard(false);
        card.addView(makeSectionTitle(t("Chỉnh mờ nút", "Button Opacity")));
        card.addView(makeSectionSubTitle(t("Điều chỉnh độ trong suốt để nút gọn mắt hơn khi chơi.", "Adjust transparency so the buttons look cleaner while playing.")), marginTop(dp(6)));

        seekAlphaFreeze = makeSeekBar(100, COLOR_ON_GREEN, COLOR_TELE_CYAN);
        tvAlphaFreeze = makeValueText(COLOR_ON_GREEN);
        card.addView(makeSliderItem("Freeze", t("Độ mờ nút Freeze", "Freeze button opacity"), seekAlphaFreeze, tvAlphaFreeze, COLOR_ON_GREEN));

        seekAlphaGhost = makeSeekBar(100, COLOR_PURPLE, COLOR_GHOST_PINK);
        tvAlphaGhost = makeValueText(COLOR_GHOST_PURPLE);
        card.addView(makeSliderItem("Ghost", t("Độ mờ nút Ghost", "Ghost button opacity"), seekAlphaGhost, tvAlphaGhost, COLOR_GHOST_PURPLE));

        seekAlphaTele = makeSeekBar(100, COLOR_ACCENT, COLOR_TELE_CYAN);
        tvAlphaTele = makeValueText(COLOR_ACCENT);
        card.addView(makeSliderItem("Tele", t("Độ mờ nút Tele", "Tele button opacity"), seekAlphaTele, tvAlphaTele, COLOR_ACCENT));
        return card;
    }

    private View buildSettingsToggleCard() {
        LinearLayout card = makeCard(false);
        card.addView(makeSectionTitle(t("Bật / Tắt", "On / Off")));
        card.addView(makeSectionSubTitle(t("Quản lý nhanh Tâm ảo, Freeze, Ghost và Tele ngay trong Settings.", "Quickly manage Virtual Aim, Freeze, Ghost and Tele inside Settings.")), marginTop(dp(6)));

        switchPen = makeSwitch();
        card.addView(makeSwitchRow(t("Bật Tắt Tâm Ảo", "Toggle Virtual Aim"), t("Hiển thị hoặc ẩn tâm ảo ở giữa màn hình.", "Show or hide the virtual aim in the center of the screen."), switchPen));

        switchFreeze = makeSwitch();
        card.addView(makeSwitchRow("Freeze", t("Hiển thị hoặc ẩn nút Freeze.", "Show or hide the Freeze button."), switchFreeze));

        switchGhost = makeSwitch();
        card.addView(makeSwitchRow("Ghost", t("Hiển thị hoặc ẩn nút Ghost.", "Show or hide the Ghost button."), switchGhost));

        switchTele = makeSwitch();
        card.addView(makeSwitchRow("Tele", t("Hiển thị hoặc ẩn nút Tele.", "Show or hide the Tele button."), switchTele));
        return card;
    }

    private View buildFreezeTimeCard() {
        LinearLayout card = makeSettingControlCard(
                "⏱",
                t("Thời gian Freeze", "Freeze Timer"),
                t("Chỉnh thời gian tự tắt khi Freeze đang ON. Giao diện mới rõ hơn, dễ kéo hơn.",
                        "Set how long Freeze stays ON before it turns off. Cleaner and easier to adjust."),
                COLOR_ON_GREEN);

        card.addView(makeMiniInfoStrip(
                t("AUTO OFF", "AUTO OFF"),
                t("1 - 10 giây • bước 0,5s", "1 - 10 seconds • 0.5s step"),
                COLOR_ON_GREEN), marginTop(dp(14)));

        tvFreezeTimeStatus = makeStatusView();
        tvFreezeTimeStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvFreezeTimeStatus.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(tvFreezeTimeStatus, marginTop(dp(12)));

        btnFreezeTimeConfig = makeButton(t("⚙  Chỉnh thời gian Freeze", "⚙  Edit Freeze Timer"), false);
        btnFreezeTimeConfig.setBackground(makeAccentActionButton(COLOR_ON_GREEN));
        card.addView(btnFreezeTimeConfig, marginTop(dp(12)));
        return card;
    }

    private View buildFreezeDropRangeCard() {
        LinearLayout card = makeSettingControlCard(
                "❄",
                t("Freeze Drop", "Freeze Drop"),
                t("Chỉnh khoảng payload để Freeze drop theo Min / Max. Trạng thái hiển thị gọn hơn.",
                        "Set the Freeze drop payload range by Min / Max. Status is easier to read."),
                COLOR_FREEZE_BLUE);

        card.addView(makeMiniInfoStrip(
                t("PAYLOAD RANGE", "PAYLOAD RANGE"),
                t("Min / Max bytes", "Min / Max bytes"),
                COLOR_FREEZE_BLUE), marginTop(dp(14)));

        tvFreezeDropRange = makeStatusView();
        tvFreezeDropRange.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvFreezeDropRange.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(tvFreezeDropRange, marginTop(dp(12)));

        btnFreezeDropConfig = makeButton(t("⚙  Chỉnh Freeze Drop", "⚙  Edit Freeze Drop"), false);
        btnFreezeDropConfig.setBackground(makeAccentActionButton(COLOR_FREEZE_BLUE));
        card.addView(btnFreezeDropConfig, marginTop(dp(12)));
        return card;
    }

    private View buildGhostDropRangeCard() {
        LinearLayout card = makeSettingControlCard(
                "👻",
                t("Ghost Drop", "Ghost Drop"),
                t("Chỉnh khoảng payload cho Ghost drop. Form nhập mới đẹp và dễ nhìn hơn.",
                        "Set the Ghost drop payload range. The new form is cleaner and easier to read."),
                COLOR_GHOST_PURPLE);

        card.addView(makeMiniInfoStrip(
                t("PAYLOAD RANGE", "PAYLOAD RANGE"),
                t("Min / Max bytes", "Min / Max bytes"),
                COLOR_GHOST_PURPLE), marginTop(dp(14)));

        tvGhostDropRange = makeStatusView();
        tvGhostDropRange.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvGhostDropRange.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(tvGhostDropRange, marginTop(dp(12)));

        btnGhostDropConfig = makeButton(t("⚙  Chỉnh Ghost Drop", "⚙  Edit Ghost Drop"), false);
        btnGhostDropConfig.setBackground(makeAccentActionButton(COLOR_GHOST_PURPLE));
        card.addView(btnGhostDropConfig, marginTop(dp(12)));
        return card;
    }

    private View buildFooterCard() {
        LinearLayout card = makeCard(false);
        card.addView(makeSectionTitle("Admin RealPing"));
        card.addView(makeSectionSubTitle(t("Cường Hack Games", "Cuong Hack Games")), marginTop(dp(6)));
        card.addView(makeSectionSubTitle(t("HasakeModz", "HasakeModz")), marginTop(dp(6)));

        TextView desc = makeText(t("Home chứa nhập key, chỉnh kích thước nút, chỉnh độ mờ nút, đổi ngôn ngữ và cấp quyền. Settings chứa bật/tắt nhanh cho Tâm ảo, Freeze, Ghost và Tele.", "Home contains key entry, button size, button opacity, language switch and permissions. Settings contains quick toggles for Virtual Aim, Freeze, Ghost and Tele."), 12, false, COLOR_WHITE_SOFT);
        desc.setLineSpacing(0f, 1.15f);
        card.addView(desc, marginTop(dp(14)));
        return card;
    }

    private LinearLayout makeCard(boolean heroCard) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(22), dp(22), dp(22), dp(22));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(heroCard ? 10 : 5));
        }
        card.setBackground(makeCardBackground(heroCard));
        LinearLayout.LayoutParams lp = defaultLp();
        lp.bottomMargin = dp(14);
        card.setLayoutParams(lp);
        return card;
    }

    private LinearLayout makeSliderItem(String title, String subtitle, SeekBar seekBar, TextView valueView, int accentColor) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(14), dp(16), dp(14));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            box.setElevation(dp(2));
        }
        box.setBackground(makeInnerPanelBackground(accentColor));
        LinearLayout.LayoutParams boxLp = defaultLp();
        boxLp.topMargin = dp(10);
        box.setLayoutParams(boxLp);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout textWrap = new LinearLayout(this);
        textWrap.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textWrap.addView(makeText(title, 15, true, COLOR_WHITE));
        TextView sub = makeText(subtitle, 12, false, COLOR_WHITE_MUTED);
        LinearLayout.LayoutParams subLp = defaultLp();
        subLp.topMargin = dp(3);
        textWrap.addView(sub, subLp);
        topRow.addView(textWrap, textLp);

        topRow.addView(valueView);
        box.addView(topRow);

        LinearLayout.LayoutParams seekLp = defaultLp();
        seekLp.topMargin = dp(14);
        box.addView(seekBar, seekLp);
        return box;
    }

    private LinearLayout makeSwitchRow(String title, String subtitle, final Switch sw) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(16), dp(16), dp(16));
        row.setBackground(makeInnerPanelBackground());
        row.setClickable(true);
        row.setFocusable(true);

        LinearLayout.LayoutParams rowLp = defaultLp();
        rowLp.topMargin = dp(8);
        row.setLayoutParams(rowLp);

        LinearLayout textWrap = new LinearLayout(this);
        textWrap.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        TextView titleTv = makeText(title, 15, true, COLOR_WHITE);
        textWrap.addView(titleTv);
        if (subtitle != null && !subtitle.isEmpty()) {
            TextView tvSub = makeText(subtitle, 12, false, COLOR_WHITE_MUTED);
            LinearLayout.LayoutParams subLp = defaultLp();
            subLp.topMargin = dp(3);
            textWrap.addView(tvSub, subLp);
        }
        row.addView(textWrap, textLp);

        row.addView(sw);
        applySmoothPress(row, 0.99f);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sw.toggle();
            }
        });
        return row;
    }

    private LinearLayout makeSettingControlCard(String icon, String title, String subtitle, int accentColor) {
        LinearLayout card = makeCard(false);
        card.setBackground(makeSettingFeatureBackground(accentColor));
        card.setClipToPadding(false);
        card.setClipChildren(false);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView iconView = makeFeatureIcon(icon, accentColor);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(44), dp(44));
        iconLp.rightMargin = dp(14);
        header.addView(iconView, iconLp);

        LinearLayout textWrap = new LinearLayout(this);
        textWrap.setOrientation(LinearLayout.VERTICAL);
        TextView titleTv = makeText(title, 16, true, COLOR_WHITE);
        textWrap.addView(titleTv);
        TextView sub = makeText(subtitle, 12, false, COLOR_WHITE_MUTED);
        sub.setLineSpacing(0f, 1.3f);
        LinearLayout.LayoutParams subLp = defaultLp();
        subLp.topMargin = dp(3);
        textWrap.addView(sub, subLp);
        header.addView(textWrap, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        card.addView(header, defaultLp());
        return card;
    }

    private TextView makeFeatureIcon(String icon, int accentColor) {
        TextView tv = makeText(icon, 22, true, COLOR_WHITE);
        tv.setGravity(Gravity.CENTER);
        tv.setBackground(makeFeatureIconBackground(accentColor));
        return tv;
    }

    private TextView makeMiniInfoStrip(String left, String right, int accentColor) {
        TextView strip = makeText(left + "  •  " + right, 11, true, COLOR_WHITE_SOFT);
        strip.setGravity(Gravity.CENTER);
        strip.setPadding(dp(12), dp(9), dp(12), dp(9));
        strip.setBackground(makeAccentSoftBackground(accentColor, dp(16)));
        return strip;
    }

    private GradientDrawable makeSettingFeatureBackground(int accentColor) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xF0172334, 0xF00E1826});
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(dp(26));
        d.setStroke(dp(1), withAlpha(accentColor, 120));
        return d;
    }

    private GradientDrawable makeFeatureIconBackground(int accentColor) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{withAlpha(accentColor, 200), withAlpha(accentColor, 100)});
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(dp(14));
        d.setStroke(dp(1), withAlpha(COLOR_WHITE, 90));
        return d;
    }

    private GradientDrawable makeAccentSoftBackground(int accentColor, int radius) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{withAlpha(accentColor, 42), 0x18101824});
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(radius);
        d.setStroke(dp(1), withAlpha(accentColor, 115));
        return d;
    }

    private GradientDrawable makeAccentStatusBackground(int accentColor) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF0B1520, 0xFF0E1A28});
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(dp(16));
        d.setStroke(dp(1), withAlpha(accentColor, 155));
        return d;
    }

    private GradientDrawable makeAccentActionButton(int accentColor) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{withAlpha(accentColor, 175), withAlpha(accentColor, 95)});
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(dp(16));
        d.setStroke(dp(1), withAlpha(COLOR_WHITE, 100));
        return d;
    }

    private int withAlpha(int color, int alpha) {
        if (alpha < 0) alpha = 0;
        if (alpha > 255) alpha = 255;
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private LinearLayout makePrettyDialogCard(String icon, String title, String subtitle, int accentColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(makeSettingFeatureBackground(accentColor));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView iconView = makeFeatureIcon(icon, accentColor);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(46), dp(46));
        iconLp.rightMargin = dp(12);
        header.addView(iconView, iconLp);

        LinearLayout textWrap = new LinearLayout(this);
        textWrap.setOrientation(LinearLayout.VERTICAL);
        textWrap.addView(makeSectionTitle(title));
        TextView sub = makeSectionSubTitle(subtitle);
        LinearLayout.LayoutParams subLp = defaultLp();
        subLp.topMargin = dp(4);
        textWrap.addView(sub, subLp);
        header.addView(textWrap, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(header);
        return card;
    }

    private EditText makeNumberField(String hint, String value, int accentColor) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setHintTextColor(COLOR_WHITE_MUTED);
        input.setTextColor(COLOR_WHITE);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setSingleLine(true);
        input.setPadding(dp(16), dp(14), dp(16), dp(14));
        input.setBackground(makeAccentSoftBackground(accentColor, dp(16)));
        input.setText(value);
        return input;
    }

    private TextView makeDialogActionButton(String text, int accentColor, boolean filled) {
        TextView btn = makeText(text, 14, true, COLOR_WHITE);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(14), dp(13), dp(14), dp(13));
        btn.setBackground(filled ? makeAccentActionButton(accentColor) : makeAccentSoftBackground(COLOR_DANGER, dp(18)));
        applySmoothPress(btn, 0.975f);
        return btn;
    }

    private LinearLayout makeDialogOuter() {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(dp(14), dp(14), dp(14), dp(14));
        return outer;
    }

    private TextView makeTelegramPill(String text, int accentColor) {
        TextView tv = makeText(text, 12, true, COLOR_WHITE);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(12), dp(10), dp(12), dp(10));
        tv.setBackground(makeAccentSoftBackground(accentColor, dp(18)));
        return tv;
    }

    private void showPrettyDialog(final Dialog dialog, final View content) {
        dialog.setContentView(content);

        // Chuẩn bị trạng thái ban đầu — nhỏ + trong suốt
        content.setAlpha(0f);
        content.setScaleX(0.3f);
        content.setScaleY(0.3f);

        dialog.show();

        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Post 1 frame để view đã measure xong rồi mới animate
        content.post(new Runnable() {
            @Override public void run() {
                content.setPivotX(content.getWidth() / 2f);
                content.setPivotY(content.getHeight() / 2f);
                content.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                content.animate().cancel();
                content.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(380)
                        .setInterpolator(new android.view.animation.OvershootInterpolator(0.72f))
                        .withEndAction(new Runnable() {
                            @Override public void run() {
                                try { content.setLayerType(View.LAYER_TYPE_NONE, null); } catch (Exception ignored) {}
                            }
                        })
                        .start();
            }
        });
    }

    private TextView makeSectionTitle(String text) {
        TextView tv = makeText(text, 17, true, COLOR_WHITE);
        tv.setLetterSpacing(0.02f);
        LinearLayout.LayoutParams lp = defaultLp();
        lp.bottomMargin = dp(4);
        tv.setLayoutParams(lp);
        return tv;
    }

    private TextView makeSectionSubTitle(String text) {
        TextView tv = makeText(text, 12, false, COLOR_WHITE_MUTED);
        tv.setLineSpacing(0f, 1.5f);
        return tv;
    }

    private TextView makeText(String text, int sp, boolean bold, int color) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        if (bold) tv.setTypeface(Typeface.DEFAULT_BOLD);
        return tv;
    }

    private TextView makeValueText() {
        return makeValueText(COLOR_ACCENT);
    }

    private TextView makeValueText(int accentColor) {
        TextView tv = makeText("", 12, true, COLOR_WHITE);
        tv.setPadding(dp(14), dp(8), dp(14), dp(8));
        tv.setGravity(Gravity.CENTER);
        tv.setBackground(makeValuePillBackground(accentColor));
        return tv;
    }

    private TextView makeStatusView() {
        TextView tv = makeText(t("CHƯA KÍCH HOẠT", "NOT ACTIVATED"), 13, true, COLOR_WHITE);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        tv.setLineSpacing(0f, 1.45f);
        tv.setPadding(dp(18), dp(18), dp(18), dp(18));
        tv.setBackground(makeStatusBackground(COLOR_OFF, COLOR_STROKE));
        return tv;
    }

    private TextView makeChip(String text, boolean accent) {
        TextView tv = makeText(text, 11, true, COLOR_WHITE);
        tv.setPadding(dp(14), dp(8), dp(14), dp(8));
        tv.setBackground(accent ? makeChipAccentBackground() : makeChipNeutralBackground());
        tv.setAlpha(accent ? 1f : 0.9f);
        applySmoothPress(tv, 0.97f);
        return tv;
    }

    private Button makeButton(String text, boolean active) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setAllCaps(false);
        btn.setTextColor(COLOR_WHITE);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f);
        btn.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        btn.setLetterSpacing(0.02f);
        btn.setPadding(dp(22), 0, dp(22), 0);
        btn.setMinHeight(dp(60));
        btn.setBackground(active ? makeActionButtonOn() : makeActionButtonOff());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            btn.setElevation(dp(5));
            btn.setStateListAnimator(null);
        }
        applySmoothPress(btn, 0.96f);
        return btn;
    }

    private Switch makeSwitch() {
        Switch sw = new Switch(this);
        sw.setText("");
        sw.setShowText(false);
        applySwitchMotion(sw);
        return sw;
    }

    private SeekBar makeSeekBar(int max) {
        return makeSeekBar(max, COLOR_ACCENT, COLOR_PURPLE);
    }

    private SeekBar makeSeekBar(int max, int startColor, int endColor) {
        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(max);
        applySeekBarStyle(seekBar, startColor, endColor);
        return seekBar;
    }

    private void applySeekBarStyle(SeekBar seekBar, int startColor, int endColor) {
        GradientDrawable track = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF182333, 0xFF101A28});
        track.setShape(GradientDrawable.RECTANGLE);
        track.setCornerRadius(dp(999));
        track.setStroke(dp(1), withAlpha(COLOR_WHITE, 20));
        track.setSize(-1, dp(8));

        GradientDrawable secondary = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{withAlpha(startColor, 30), withAlpha(endColor, 26)});
        secondary.setShape(GradientDrawable.RECTANGLE);
        secondary.setCornerRadius(dp(999));
        secondary.setSize(-1, dp(8));

        GradientDrawable progress = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{startColor, endColor});
        progress.setShape(GradientDrawable.RECTANGLE);
        progress.setCornerRadius(dp(999));
        progress.setSize(-1, dp(8));

        InsetDrawable trackInset    = new InsetDrawable(track,    dp(2), dp(8), dp(2), dp(8));
        InsetDrawable secondaryInset= new InsetDrawable(secondary,dp(2), dp(8), dp(2), dp(8));
        InsetDrawable progressInset = new InsetDrawable(progress, dp(2), dp(8), dp(2), dp(8));
        ClipDrawable secondaryClip  = new ClipDrawable(secondaryInset, Gravity.START, ClipDrawable.HORIZONTAL);
        ClipDrawable progressClip   = new ClipDrawable(progressInset,  Gravity.START, ClipDrawable.HORIZONTAL);

        LayerDrawable layer = new LayerDrawable(new android.graphics.drawable.Drawable[]{trackInset, secondaryClip, progressClip});
        layer.setId(0, android.R.id.background);
        layer.setId(1, android.R.id.secondaryProgress);
        layer.setId(2, android.R.id.progress);
        seekBar.setProgressDrawable(layer);

        // Thumb glow halo
        GradientDrawable thumbGlow = new GradientDrawable();
        thumbGlow.setShape(GradientDrawable.OVAL);
        thumbGlow.setColor(withAlpha(endColor, 55));
        thumbGlow.setSize(dp(32), dp(32));

        // White thumb core with accent ring
        GradientDrawable thumbCore = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFFFFFFFF, 0xFFF0F4FF});
        thumbCore.setShape(GradientDrawable.OVAL);
        thumbCore.setSize(dp(20), dp(20));
        thumbCore.setStroke(dp(2), endColor);

        InsetDrawable thumbCoreInset = new InsetDrawable(thumbCore, dp(6), dp(6), dp(6), dp(6));
        LayerDrawable thumbLayer = new LayerDrawable(
                new android.graphics.drawable.Drawable[]{thumbGlow, thumbCoreInset});
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            seekBar.setThumb(thumbLayer);
            seekBar.setSplitTrack(false);
        }
        seekBar.setThumbOffset(0);
        seekBar.setPadding(0, 0, 0, 0);
        seekBar.setMinimumHeight(dp(32));
    }

    private GradientDrawable makeSliderPanelBackground(int accentColor) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xD0142233, 0xD00D1622});
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(dp(22));
        d.setStroke(dp(1.2f), withAlpha(accentColor, 110));
        return d;
    }

    private GradientDrawable makeSliderColorBarBackground(int accentColor) {
        int endColor = accentColor == COLOR_ON_GREEN ? COLOR_TELE_CYAN
                : (accentColor == COLOR_GHOST_PURPLE ? COLOR_GHOST_PINK
                : (accentColor == COLOR_ACCENT ? COLOR_TELE_CYAN : COLOR_PURPLE));
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{accentColor, accentColor, endColor});
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(dp(999));
        d.setStroke(dp(1), withAlpha(COLOR_WHITE, 75));
        return d;
    }

    private void applySwitchMotion(final Switch sw) {
        if (sw == null) return;
        sw.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                switch (event.getActionMasked()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        try {
                            v.animate().cancel();
                            v.animate().scaleX(0.94f).scaleY(0.94f)
                                    .setDuration(55).start();
                        } catch (Exception ignored) {}
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        try {
                            v.animate().cancel();
                            v.animate().scaleX(1f).scaleY(1f)
                                    .setDuration(200)
                                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.5f))
                                    .start();
                        } catch (Exception ignored) {}
                        break;
                }
                return false;
            }
        });
    }

    private void animateSwitchState(final CompoundButton button, boolean checked) {
        if (button == null) return;
        try {
            button.animate().cancel();
            button.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            button.setScaleX(0.92f);
            button.setScaleY(0.92f);
            button.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(260)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.6f))
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            try { button.setLayerType(View.LAYER_TYPE_NONE, null); } catch (Exception ignored) {}
                        }
                    })
                    .start();
        } catch (Exception ignored) {
            try {
                button.setScaleX(1f);
                button.setScaleY(1f);
                button.setLayerType(View.LAYER_TYPE_NONE, null);
            } catch (Exception e) {}
        }
    }

    private void applySwitchStyle(Switch sw) {
        if (sw == null) return;
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };
        int[] trackColors = new int[]{0x886AA8FF, 0x33253648};
        int[] thumbColors = new int[]{COLOR_WHITE, 0xFFD1D9E6};
        sw.setTrackTintList(new ColorStateList(states, trackColors));
        sw.setThumbTintList(new ColorStateList(states, thumbColors));
        sw.setShowText(false);
        sw.setSplitTrack(false);
    }

    private GradientDrawable makeAppBackground() {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{COLOR_BG_TOP, COLOR_BG_MID, COLOR_BG_BOTTOM});
        drawable.setShape(GradientDrawable.RECTANGLE);
        return drawable;
    }

    private GradientDrawable makeOrbDrawable(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        d.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        d.setGradientRadius(dp(160));
        return d;
    }

    private GradientDrawable makeCardBackground(boolean heroCard) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                heroCard
                        ? new int[]{0xF21D2A3E, 0xF5111A28}
                        : new int[]{0xEF162030, 0xF20D1620});
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(dp(28));
        d.setStroke(dp(1), heroCard ? 0x664466AA : 0x553A5878);
        return d;
    }

    private GradientDrawable makeInnerPanelBackground() {
        return makeInnerPanelBackground(COLOR_ACCENT);
    }

    private GradientDrawable makeInnerPanelBackground(int accentColor) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF0F1E2E, 0xFF0A1420});
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(dp(18));
        d.setStroke(dp(1), withAlpha(accentColor, 90));
        return d;
    }

    private GradientDrawable makeBadgeBackground() {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFFEEF4FF, 0xFFD6E5FF});
        d.setShape(GradientDrawable.OVAL);
        d.setStroke(dp(1.5f), 0x88FFFFFF);
        return d;
    }

    private GradientDrawable makeValuePillBackground() {
        return makeValuePillBackground(COLOR_ACCENT);
    }

    private GradientDrawable makeValuePillBackground(int accentColor) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(withAlpha(accentColor, 22));
        d.setCornerRadius(dp(999));
        d.setStroke(dp(1), withAlpha(accentColor, 100));
        return d;
    }

    private GradientDrawable makeChipNeutralBackground() {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(0xFF0C1520);
        d.setCornerRadius(dp(999));
        d.setStroke(dp(1), 0x55607898);
        return d;
    }

    private GradientDrawable makeChipAccentBackground() {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF172E50, 0xFF0F1F38});
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(dp(999));
        d.setStroke(dp(1), 0x885FA0E0);
        return d;
    }

    private GradientDrawable makeActionButtonOn() {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF1E4270, 0xFF0E2B52});
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(dp(20));
        d.setStroke(dp(1), 0xFF8AC8FF);
        return d;
    }

    private GradientDrawable makeActionButtonOff() {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF16283E, 0xFF0D1C30});
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(dp(20));
        d.setStroke(dp(1), 0x9970A0D0);
        return d;
    }

    private GradientDrawable makeActionButtonDanger() {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF26161C, 0xFF140C12});
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(dp(20));
        d.setStroke(dp(1), 0xFFE87080);
        return d;
    }

    private GradientDrawable makeActionButtonSuccess() {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF143322, 0xFF0C231A});
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(dp(20));
        d.setStroke(dp(1), 0xFF55BF80);
        return d;
    }

    private GradientDrawable makeBottomNavBackground() {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xF4121E30, 0xF90C1622});
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(dp(32));
        d.setStroke(dp(1), 0x66607898);
        return d;
    }

    private GradientDrawable makeTabSelectedBackground() {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF1F4070, 0xFF122A4E});
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(dp(22));
        d.setStroke(dp(1), 0xFF72B8FF);
        return d;
    }

    private GradientDrawable makeTabIdleBackground() {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(0x00000000);
        d.setCornerRadius(dp(20));
        return d;
    }

    private GradientDrawable makeStatusBackground(int fillColor, int strokeColor) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{fillColor, fillColor});
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(dp(18));
        d.setStroke(dp(1), strokeColor);
        return d;
    }

    private LinearLayout.LayoutParams defaultLp() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams marginTop(int top) {
        LinearLayout.LayoutParams lp = defaultLp();
        lp.topMargin = top;
        return lp;
    }

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()));
    }

    private void applySmoothPress(final View view, final float pressedScale) {
        if (view == null) return;
        view.setClickable(true);
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        v.animate().cancel();
                        v.animate().scaleX(pressedScale).scaleY(pressedScale)
                                .alpha(0.88f).translationY(dp(1.5f))
                                .setDuration(70)
                                .setInterpolator(new DecelerateInterpolator(1.5f))
                                .start();
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        v.animate().cancel();
                        v.animate().scaleX(1f).scaleY(1f).alpha(1f).translationY(0f)
                                .setDuration(220)
                                .setInterpolator(new android.view.animation.OvershootInterpolator(1.4f))
                                .start();
                        break;
                }
                return false;
            }
        });
    }

    private View makeTabButton(String icon, String text) {
        LinearLayout tab = new LinearLayout(this);
        tab.setOrientation(LinearLayout.VERTICAL);
        tab.setGravity(Gravity.CENTER);
        tab.setPadding(dp(4), dp(10), dp(4), dp(10));
        tab.setBackground(makeTabIdleBackground());
        tab.setClickable(true);
        tab.setFocusable(true);

        TextView iconView = new TextView(this);
        iconView.setText(icon);
        iconView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        iconView.setTextColor(COLOR_WHITE_MUTED);
        iconView.setGravity(Gravity.CENTER);
        iconView.setTag("icon");

        TextView labelView = new TextView(this);
        labelView.setText(text);
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        labelView.setTextColor(COLOR_WHITE_MUTED);
        labelView.setGravity(Gravity.CENTER);
        labelView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        labelView.setLetterSpacing(0.02f);
        labelView.setTag("label");
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelLp.topMargin = dp(3);

        tab.addView(iconView);
        tab.addView(labelView, labelLp);
        applyTabPressEffect(tab);
        return tab;
    }

    private void applyTabPressEffect(final View view) {
        if (view == null) return;
        view.setClickable(true);
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        v.animate().cancel();
                        v.animate().scaleX(0.93f).scaleY(0.93f).alpha(0.85f)
                                .setDuration(65)
                                .setInterpolator(new DecelerateInterpolator(1.5f))
                                .start();
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        v.animate().cancel();
                        v.animate().scaleX(1f).scaleY(1f).alpha(1f)
                                .setDuration(260)
                                .setInterpolator(new android.view.animation.OvershootInterpolator(1.3f))
                                .start();
                        break;
                }
                return false;
            }
        });
    }

    private void sendFloatingIntUpdate(String action, String extraKey, int value) {
        if (!_svcUp) return;
        if (!Settings.canDrawOverlays(this)) {
            return;
        }
        Intent i = new Intent(this, MediaRenderService.class);
        i.setAction(action);
        i.putExtra(extraKey, value);
        startService(i);
    }

    private void updatePermissionButtonState(Button button, boolean granted, String grantedText, String requestText) {
        if (button == null) return;
        button.setEnabled(!granted);
        button.setClickable(!granted);
        button.setText(granted ? grantedText : requestText);
        button.setTextColor(COLOR_WHITE);
        button.setBackground(granted ? makeActionButtonSuccess() : makeActionButtonOn());
        button.setAlpha(granted ? 0.96f : 1f);
    }

    private void switchToTab(boolean homeSelected) {
        switchToTab(homeSelected ? 0 : 1, true);
    }

    private void switchToTab(int tabIndex) {
        switchToTab(tabIndex, true);
    }

    private void switchToTab(final int tabIndex, boolean animate) {
        if (tabIndex == 0) updateHomeAdvancedStatus();
        updateTabAppearance(tabIndex);

        final View[] pages = new View[]{homePageView, settingsPageView, firewallPageView};
        if (tabIndex < 0 || tabIndex >= pages.length) return;

        final View incoming = pages[tabIndex];
        final View outgoing = pages[currentTabIndex];

        if (incoming == null) {
            currentTabIndex = tabIndex;
            return;
        }

        // Nếu đang transition → cancel hết, reset về sạch rồi nhảy tới tab mới
        if (isTabTransitionRunning) {
            for (View p : pages) {
                if (p == null) continue;
                p.animate().cancel();
                p.setAlpha(1f);
                p.setTranslationX(0f);
                try { p.setLayerType(View.LAYER_TYPE_NONE, null); } catch (Exception ignored) {}
            }
            // FIX: reset visibility — chỉ giữ page hiện tại VISIBLE, ẩn hết còn lại
            for (int i = 0; i < pages.length; i++) {
                if (pages[i] != null) {
                    pages[i].setVisibility(i == currentTabIndex ? View.VISIBLE : View.GONE);
                }
            }
            isTabTransitionRunning = false;
        }

        if (!animate) {
            for (int i = 0; i < pages.length; i++) {
                if (pages[i] == null) continue;
                pages[i].setAlpha(1f);
                pages[i].setTranslationX(0f);
                pages[i].setVisibility(i == tabIndex ? View.VISIBLE : View.GONE);
            }
            currentTabIndex = tabIndex;
            return;
        }

        if (currentTabIndex == tabIndex) return;

        final int oldIndex = currentTabIndex;
        currentTabIndex = tabIndex;
        isTabTransitionRunning = true;

        final boolean goingRight = tabIndex > oldIndex;

        // Khoảng trượt nhẹ — không quá xa, tạo cảm giác kéo trang
        final float SLIDE = dp(46);
        final float inStart  =  goingRight ?  SLIDE : -SLIDE;  // incoming từ phải/trái
        final float outEnd   =  goingRight ? -SLIDE :  SLIDE;  // outgoing trượt ra

        // Duration đồng nhất cho cả 2
        final long DUR = 270L;

        // Interpolator giống nhau — cubic ease-out mượt, cùng nhịp
        final android.view.animation.Interpolator easing =
                new DecelerateInterpolator(2.0f);

        // --- Ẩn các page không liên quan ---
        for (int i = 0; i < pages.length; i++) {
            if (pages[i] == null) continue;
            if (i != oldIndex && i != tabIndex) {
                pages[i].animate().cancel();
                pages[i].setVisibility(View.GONE);
                pages[i].setAlpha(1f);
                pages[i].setTranslationX(0f);
            }
        }

        // --- INCOMING: bắt đầu lệch + trong suốt, trượt vào ---
        incoming.animate().cancel();
        incoming.setVisibility(View.VISIBLE);
        incoming.bringToFront();
        incoming.setAlpha(0f);
        incoming.setTranslationX(inStart);
        incoming.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        incoming.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(DUR)
                .setInterpolator(easing)
                .withEndAction(new Runnable() {
                    @Override public void run() {
                        try {
                            incoming.setAlpha(1f);
                            incoming.setTranslationX(0f);
                            incoming.setLayerType(View.LAYER_TYPE_NONE, null);
                        } catch (Exception ignored) {}
                        for (int i = 0; i < pages.length; i++) {
                            View p = pages[i];
                            if (p == null) continue;
                            if (i != tabIndex) {
                                p.setVisibility(View.GONE);
                                p.setAlpha(1f);
                                p.setTranslationX(0f);
                                try { p.setLayerType(View.LAYER_TYPE_NONE, null); } catch (Exception ignored) {}
                            }
                        }
                        isTabTransitionRunning = false;
                    }
                })
                .start();

        // --- OUTGOING: trượt ra + mờ dần — cùng duration, cùng easing ---
        if (outgoing != null && outgoing != incoming) {
            outgoing.animate().cancel();
            outgoing.setVisibility(View.VISIBLE);
            outgoing.setAlpha(1f);
            outgoing.setTranslationX(0f);
            outgoing.setLayerType(View.LAYER_TYPE_HARDWARE, null);

            outgoing.animate()
                    .alpha(0f)
                    .translationX(outEnd)
                    .setDuration(DUR)
                    .setInterpolator(easing)          // ← cùng easing với incoming
                    .withEndAction(new Runnable() {
                        @Override public void run() {
                            try {
                                outgoing.setVisibility(View.GONE);
                                outgoing.setAlpha(1f);
                                outgoing.setTranslationX(0f);
                                outgoing.setLayerType(View.LAYER_TYPE_NONE, null);
                            } catch (Exception ignored) {}
                        }
                    })
                    .start();
        }
    }

    private void updateTabAppearance(int tabIndex) {
        updateSingleTab(tabHome,     tabIndex == 0);
        updateSingleTab(tabSettings, tabIndex == 1);
        updateSingleTab(tabFirewall, tabIndex == 2);
    }

    private void updateSingleTab(View tabView, boolean selected) {
        if (tabView == null) return;
        tabView.animate().cancel();
        tabView.setBackground(selected ? makeTabSelectedBackground() : makeTabIdleBackground());
        if (tabView instanceof LinearLayout) {
            LinearLayout ll = (LinearLayout) tabView;
            for (int i = 0; i < ll.getChildCount(); i++) {
                View child = ll.getChildAt(i);
                if (child instanceof TextView) {
                    ((TextView) child).setTextColor(selected ? COLOR_WHITE : COLOR_WHITE_MUTED);
                }
            }
        }
        if (selected) {
            tabView.setScaleX(0.92f);
            tabView.setScaleY(0.92f);
            tabView.animate()
                    .scaleX(1f).scaleY(1f).alpha(1f).translationY(0f)
                    .setDuration(320)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.1f))
                    .start();
        } else {
            tabView.animate()
                    .scaleX(1f).scaleY(1f).alpha(0.82f).translationY(0f)
                    .setDuration(200)
                    .setInterpolator(new DecelerateInterpolator(1.2f))
                    .start();
        }
    }

    private void refreshPermissionStatus() {

        boolean overlayGranted = Settings.canDrawOverlays(this);
        boolean vpnReady = VpnService.prepare(this) == null;

        if (tvOverlayStatus != null) {
            tvOverlayStatus.setText(overlayGranted
                    ? t("✓ Overlay đã được cấp quyền", "✓ Overlay permission granted")
                    : t("⚠ Overlay chưa được cấp quyền", "⚠ Overlay permission not granted"));
            tvOverlayStatus.setTextColor(COLOR_WHITE);
            tvOverlayStatus.setBackground(makeStatusBackground(
                    overlayGranted ? COLOR_SUCCESS_FILL : COLOR_OFF,
                    overlayGranted ? COLOR_SUCCESS_BORDER : COLOR_STROKE));
        }
        updatePermissionButtonState(btnGrantOverlay, overlayGranted, t("Overlay đã cấp xong", "Overlay granted"), t("Cấp quyền overlay", "Grant overlay permission"));

        if (tvVpnStatus != null) {
            tvVpnStatus.setText(vpnReady
                    ? t("✓ VPN đã sẵn sàng", "✓ VPN is ready")
                    : t("⚠ VPN chưa được cấp quyền", "⚠ VPN permission not granted"));
            tvVpnStatus.setTextColor(COLOR_WHITE);
            tvVpnStatus.setBackground(makeStatusBackground(
                    vpnReady ? COLOR_SUCCESS_FILL : COLOR_OFF,
                    vpnReady ? COLOR_SUCCESS_BORDER : COLOR_STROKE));
        }
        updatePermissionButtonState(btnGrantVpn, vpnReady, t("VPN đã cấp xong", "VPN granted"), t("Cấp quyền VPN", "Grant VPN permission"));
    }

    

    private void openPrivateDnsSettings() {
        try {
            Intent intent;
            if (Build.VERSION.SDK_INT >= 28) {
                intent = new Intent("android.settings.PRIVATE_DNS_SETTINGS");
            } else {
                intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            Toast.makeText(this,
                    t("Đã mở cài đặt DNS riêng tư.", "Opened Private DNS settings."),
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            try {
                Intent fallback = new Intent(Settings.ACTION_SETTINGS);
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(fallback);
            } catch (Exception ignored) {}
            Toast.makeText(this,
                    t("Không mở được Private DNS trực tiếp, đã mở Cài đặt hệ thống.",
                            "Could not open Private DNS directly, opened system Settings."),
                    Toast.LENGTH_SHORT).show();
        }
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

    private int getSavedFreezeTimerMs() {
        int fallbackMs = sanitizeFreezeTimerMs(prefs.getInt("fat_s", 3) * 1000);
        return sanitizeFreezeTimerMs(prefs.getInt("fat_ms", fallbackMs));
    }

    private String formatFreezeTimerMs(int ms) {
        ms = sanitizeFreezeTimerMs(ms);
        if (ms % 1000 == 0) return (ms / 1000) + "s";
        int whole = ms / 1000;
        return isEnglish ? (whole + ".5s") : (whole + ",5s");
    }

    private void updateFreezeTimeStatus() {
        if (prefs == null || tvFreezeTimeStatus == null) return;
        int offMs = getSavedFreezeTimerMs();
        tvFreezeTimeStatus.setText("⏱  " + t("Freeze ON", "Freeze ON") + "\n" +
                t("Tự tắt sau ", "Auto off after ") + formatFreezeTimerMs(offMs) +
                "  •  " + t("Bước 0,5s • Giới hạn 1-10s", "Step 0.5s • Range 1-10s"));
        tvFreezeTimeStatus.setTextColor(COLOR_WHITE);
        tvFreezeTimeStatus.setBackground(makeAccentStatusBackground(COLOR_ON_GREEN));
    }

    private void showFreezeTimeDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);

        final int savedOffMs = getSavedFreezeTimerMs();

        LinearLayout outer = makeDialogOuter();
        LinearLayout card = makePrettyDialogCard(
                "⏱",
                t("Thời gian Freeze", "Freeze Timer"),
                t("Kéo thanh để chọn thời gian tự tắt, hỗ trợ 3,5s.",
                        "Drag the slider to choose when Freeze turns off while ON."),
                COLOR_ON_GREEN);
        outer.addView(card, defaultLp());

        LinearLayout sliderPanel = new LinearLayout(this);
        sliderPanel.setOrientation(LinearLayout.VERTICAL);
        sliderPanel.setPadding(dp(14), dp(12), dp(14), dp(14));
        sliderPanel.setBackground(makeSliderPanelBackground(COLOR_ON_GREEN));
        card.addView(sliderPanel, marginTop(dp(14)));

        TextView sliderTag = makeMiniInfoStrip(
                t("Freeze ON", "Freeze ON"),
                t("Tự tắt sau khi hết thời gian", "Turns OFF when time is over"),
                COLOR_ON_GREEN);
        sliderPanel.addView(sliderTag, defaultLp());

        final TextView valueText = makeText(formatFreezeTimerMs(savedOffMs), 30, true, COLOR_WHITE);
        valueText.setGravity(Gravity.CENTER);
        valueText.setPadding(0, dp(14), 0, dp(8));
        sliderPanel.addView(valueText, defaultLp());

        final SeekBar seekOff = makeSeekBar(18, COLOR_ON_GREEN, COLOR_TELE_CYAN);
        seekOff.setProgress((savedOffMs - 1000) / 500);
        LinearLayout.LayoutParams seekLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        seekLp.topMargin = dp(6);
        sliderPanel.addView(seekOff, seekLp);

        LinearLayout labelsRow = new LinearLayout(this);
        labelsRow.setOrientation(LinearLayout.HORIZONTAL);
        labelsRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams labelsLp = defaultLp();
        labelsLp.topMargin = dp(4);

        TextView minText = makeText("1s", 12, true, COLOR_WHITE_SOFT);
        TextView maxText = makeText("10s", 12, true, COLOR_WHITE_SOFT);
        LinearLayout.LayoutParams minLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        LinearLayout.LayoutParams maxLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        maxText.setGravity(Gravity.END);
        labelsRow.addView(minText, minLp);
        labelsRow.addView(maxText, maxLp);
        sliderPanel.addView(labelsRow, labelsLp);

        TextView helper = makeMiniInfoStrip(
                t("Ví dụ", "Example"),
                t("3,5s = Freeze ON rồi tự tắt sau 3,5 giây", "3.5s = Freeze turns OFF after 3.5 seconds"),
                COLOR_ON_GREEN);
        card.addView(helper, marginTop(dp(12)));

        seekOff.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueText.setText(formatFreezeTimerMs(1000 + progress * 500));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        TextView cancel = makeDialogActionButton(t("Huỷ", "Cancel"), COLOR_DANGER, false);
        TextView save = makeDialogActionButton(t("Lưu", "Save"), COLOR_ON_GREEN, true);
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        cancelLp.rightMargin = dp(8);
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        saveLp.leftMargin = dp(8);
        actions.addView(cancel, cancelLp);
        actions.addView(save, saveLp);
        card.addView(actions, marginTop(dp(16)));

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dialog.dismiss(); }
        });
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int offMs = sanitizeFreezeTimerMs(1000 + seekOff.getProgress() * 500);
                prefs.edit()
                        .putInt("fat_ms", offMs)
                        .putInt("fat_s", Math.max(1, Math.round(offMs / 1000f)))
                        .putInt("fan_s", 0)
                        .apply();
                updateFreezeTimeStatus();

                if (_svcUp) {
                    Intent i = new Intent(MainActivity.this, MediaRenderService.class);
                    i.setAction("UPDATE_FREEZE_TIME");
                    i.putExtra("off_ms", offMs);
                    i.putExtra("off_sec", Math.max(1, Math.round(offMs / 1000f)));
                    startService(i);
                }

                Toast.makeText(MainActivity.this,
                        t("Đã lưu: Freeze ON tự tắt sau ", "Saved: Freeze ON auto off after ") + formatFreezeTimerMs(offMs),
                        Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        showPrettyDialog(dialog, outer);
    }

    private int sanitizeFreezeDropByte(int value, int fallback) {
        if (value < 0) return 0;
        if (value > 4096) return 4096;
        return value;
    }

    private void updateFreezeDropRangeStatus() {
        if (prefs == null || tvFreezeDropRange == null) return;
        int min = sanitizeFreezeDropByte(prefs.getInt("fdm_n", 20), 20);
        int max = sanitizeFreezeDropByte(prefs.getInt("fdm_x", 450), 450);
        if (max < min) {
            int t = min;
            min = max;
            max = t;
        }
        tvFreezeDropRange.setText("❄  Freeze Drop\n" +
                "Min > " + min + " bytes" + "  •  " + "Max < " + max + " bytes");
        tvFreezeDropRange.setTextColor(COLOR_WHITE);
        tvFreezeDropRange.setBackground(makeAccentStatusBackground(COLOR_FREEZE_BLUE));
    }

    private void showFreezeDropRangeDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);

        int savedMin = sanitizeFreezeDropByte(prefs.getInt("fdm_n", 20), 20);
        int savedMax = sanitizeFreezeDropByte(prefs.getInt("fdm_x", 450), 450);

        LinearLayout outer = makeDialogOuter();
        LinearLayout card = makePrettyDialogCard(
                "❄",
                "Freeze Drop",
                t("Nhập khoảng Min / Max cho payload. Nếu nhập ngược, app sẽ tự đổi lại đúng thứ tự.",
                        "Enter Min / Max payload range. If reversed, the app will sort them automatically."),
                COLOR_FREEZE_BLUE);
        outer.addView(card, defaultLp());

        TextView helper = makeMiniInfoStrip(
                t("Gợi ý", "Hint"),
                t("Ưu tiên 30 - 450 bytes", "Preferred 30 - 450 bytes"),
                COLOR_FREEZE_BLUE);
        card.addView(helper, marginTop(dp(14)));

        final EditText inputMin = makeNumberField(t("Min bytes", "Min bytes"), String.valueOf(savedMin), COLOR_FREEZE_BLUE);
        final EditText inputMax = makeNumberField(t("Max bytes", "Max bytes"), String.valueOf(savedMax), COLOR_FREEZE_BLUE);
        card.addView(inputMin, marginTop(dp(12)));
        card.addView(inputMax, marginTop(dp(12)));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        TextView cancel = makeDialogActionButton(t("Huỷ", "Cancel"), COLOR_DANGER, false);
        TextView save = makeDialogActionButton(t("Lưu", "Save"), COLOR_FREEZE_BLUE, true);
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        cancelLp.rightMargin = dp(8);
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        saveLp.leftMargin = dp(8);
        actions.addView(cancel, cancelLp);
        actions.addView(save, saveLp);
        card.addView(actions, marginTop(dp(16)));

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dialog.dismiss(); }
        });
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int min = 20;
                int max = 450;
                try {
                    String minText = inputMin.getText().toString().trim();
                    String maxText = inputMax.getText().toString().trim();
                    if (!minText.isEmpty()) min = Integer.parseInt(minText);
                    if (!maxText.isEmpty()) max = Integer.parseInt(maxText);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this,
                            t("Giá trị không hợp lệ, đã dùng mặc định 20 - 450", "Invalid value, using default 20 - 450"),
                            Toast.LENGTH_SHORT).show();
                    min = 20;
                    max = 450;
                }

                min = sanitizeFreezeDropByte(min, 20);
                max = sanitizeFreezeDropByte(max, 450);
                if (max < min) {
                    int t = min;
                    min = max;
                    max = t;
                }

                prefs.edit()
                        .putInt("fdm_n", min)
                        .putInt("fdm_x", max)
                        .remove("fdt_v")
                        .apply();
                updateFreezeDropRangeStatus();
                Toast.makeText(MainActivity.this,
                        t("Đã lưu Freeze Drop: > ", "Saved Freeze Drop: > ") + min + t(" và < ", " and < ") + max,
                        Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        showPrettyDialog(dialog, outer);
    }

    private int sanitizeGhostDropByte(int value, int fallback) {
        if (value < 0) return 0;
        if (value > 4096) return 4096;
        return value;
    }

    private void updateGhostDropRangeStatus() {
        if (prefs == null || tvGhostDropRange == null) return;
        int min = sanitizeGhostDropByte(prefs.getInt("gdm_n", 50), 50);
        int max = sanitizeGhostDropByte(prefs.getInt("gdm_x", 200), 200);
        if (max < min) {
            int t = min;
            min = max;
            max = t;
        }
        tvGhostDropRange.setText("👻  Ghost Drop\n" +
                "Min > " + min + " bytes" + "  •  " + "Max < " + max + " bytes");
        tvGhostDropRange.setTextColor(COLOR_WHITE);
        tvGhostDropRange.setBackground(makeAccentStatusBackground(COLOR_GHOST_PURPLE));
    }

    private void showGhostDropRangeDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);

        int savedMin = sanitizeGhostDropByte(prefs.getInt("gdm_n", 50), 50);
        int savedMax = sanitizeGhostDropByte(prefs.getInt("gdm_x", 200), 200);

        LinearLayout outer = makeDialogOuter();
        LinearLayout card = makePrettyDialogCard(
                "👻",
                "Ghost Drop",
                t("Nhập khoảng Min / Max cho Ghost drop. Form mới dùng khung tối và viền tím nổi bật.",
                        "Enter Min / Max for Ghost drop. The new form uses a dark card with purple accent."),
                COLOR_GHOST_PURPLE);
        outer.addView(card, defaultLp());

        TextView helper = makeMiniInfoStrip(
                t("Gợi ý", "Hint"),
                t("Min / Max bytes", "Min / Max bytes"),
                COLOR_GHOST_PURPLE);
        card.addView(helper, marginTop(dp(14)));

        final EditText inputMin = makeNumberField(t("Min bytes", "Min bytes"), String.valueOf(savedMin), COLOR_GHOST_PURPLE);
        final EditText inputMax = makeNumberField(t("Max bytes", "Max bytes"), String.valueOf(savedMax), COLOR_GHOST_PURPLE);
        card.addView(inputMin, marginTop(dp(12)));
        card.addView(inputMax, marginTop(dp(12)));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        TextView cancel = makeDialogActionButton(t("Huỷ", "Cancel"), COLOR_DANGER, false);
        TextView save = makeDialogActionButton(t("Lưu", "Save"), COLOR_GHOST_PURPLE, true);
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        cancelLp.rightMargin = dp(8);
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        saveLp.leftMargin = dp(8);
        actions.addView(cancel, cancelLp);
        actions.addView(save, saveLp);
        card.addView(actions, marginTop(dp(16)));

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dialog.dismiss(); }
        });
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int min = 50;
                int max = 200;
                try {
                    String minText = inputMin.getText().toString().trim();
                    String maxText = inputMax.getText().toString().trim();
                    if (!minText.isEmpty()) min = Integer.parseInt(minText);
                    if (!maxText.isEmpty()) max = Integer.parseInt(maxText);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this,
                            t("Giá trị không hợp lệ, đã dùng mặc định 50 - 200", "Invalid value, using default 50 - 200"),
                            Toast.LENGTH_SHORT).show();
                    min = 50;
                    max = 200;
                }

                min = sanitizeGhostDropByte(min, 50);
                max = sanitizeGhostDropByte(max, 200);
                if (max < min) {
                    int t = min;
                    min = max;
                    max = t;
                }

                prefs.edit()
                        .putInt("gdm_n", min)
                        .putInt("gdm_x", max)
                        .apply();
                updateGhostDropRangeStatus();
                Toast.makeText(MainActivity.this,
                        t("Đã lưu Ghost Drop: > ", "Saved Ghost Drop: > ") + min + t(" và < ", " and < ") + max,
                        Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        showPrettyDialog(dialog, outer);
    }



    private void m_rvp() {
        Intent prepare = VpnService.prepare(this);
        if (prepare != null) {
            startActivityForResult(prepare, REQ_VPN);
        } else {
            if (_uwts) {
                _uwts = false;
                _svcUp = true;
                startService(new Intent(this, MediaRenderService.class));
                startVpn();
            }
        }
    }

    private void startVpn() {
        Intent i = new Intent(this, GameBoosterVpnService.class);
        i.setAction(GameBoosterVpnService.A_S1);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i);
        else startService(i);
    }

    private boolean _vpnCheckPassed = false;

    private void checkExternalVpnAndKick() {
        if (isDestroyed) return;
        if (_svcUp) return;
        if (GameBoosterVpnService.isVpnRunning) return;
        new Thread(new Runnable() {
            @Override public void run() {
                try { Thread.sleep(300); } catch (Exception ignored) {}
                if (_svcUp || GameBoosterVpnService.isVpnRunning) return;
                final VpnDetector.DetectResult r = VpnDetector.fullDetect(MainActivity.this);
                if (!r.vpnDetected) { _vpnCheckPassed = true; return; }
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        if (isDestroyed) return;
                        if (_svcUp || GameBoosterVpnService.isVpnRunning) return;
                        new android.app.AlertDialog.Builder(MainActivity.this)
                            .setTitle(t("Phát hiện VPN", "VPN Detected"))
                            .setMessage(t(
                                "Ứng dụng không hỗ trợ dùng khi đang bật VPN ngoài.\nVui lòng tắt VPN rồi thử lại.",
                                "This app does not support external VPN.\nPlease disable your VPN and try again."))
                            .setCancelable(false)
                            .setPositiveButton(t("Thoát", "Exit"),
                                new android.content.DialogInterface.OnClickListener() {
                                    @Override public void onClick(android.content.DialogInterface d, int w) {
                                        d.dismiss(); finish();
                                    }
                                })
                            .show();
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkExternalVpnAndKick();
        refreshPermissionStatus();
    }

    @Override
    protected void onDestroy() {
        isDestroyed = true;
        super.onDestroy();
        try {
            unregisterReceiver(vpnRequestReceiver);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OVERLAY) {
            if (Settings.canDrawOverlays(this) && _uwts) {
                m_rvp();
            } else {
                _uwts = false;
            }
        }
        if (requestCode == REQ_VPN && resultCode == RESULT_OK && _uwts) {
            _uwts = false;
            _svcUp = true;
            startService(new Intent(this, MediaRenderService.class));
            startVpn();
        } else if (requestCode == REQ_VPN) {
            _uwts = false;
        }
        refreshPermissionStatus();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && "FakePingZzz.Plus.md.PERM".equals(intent.getAction())) {
            m_rvp();
        }
    }


}
