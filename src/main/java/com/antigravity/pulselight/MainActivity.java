package com.antigravity.pulselight;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final String TAG = "PulseLightHub";
    private static final String GRANT_COMMAND =
            "adb shell pm grant com.antigravity.pulselight android.permission.WRITE_SECURE_SETTINGS";

    // Navigation Tabs
    private TextView navTabApps;
    private TextView navTabStudio;
    private TextView navTabEngine;

    private android.widget.FrameLayout pageContainer;
    private int mCurrentTab = 0;
    private View pageApps;
    private View pageStudio;
    private View pageEngine;

    // Header Views
    private TextView tvPermStatus;

    // --- Page 1: Apps ---
    private ListView appListView;
    private ProgressBar loadingProgress;
    private TextView emptyView;
    private View headerView;
    private TextView btnReset;
    private EditText etSearch;
    private TextView tabAll;
    private TextView tabActive;

    private AppAdapter adapter;
    private final List<AppItem> allAppItems = new ArrayList<>();
    private final ExecutorService scanExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // --- Page 2: Glyph Studio ---
    private static final int TARGET_MUSIC_BASE = 0;
    private static final int TARGET_MUSIC_FLICKER = 1;
    private static final int TARGET_ALWAYS_ON = 2;
    private int mSelectedColorTarget = TARGET_MUSIC_BASE;

    private RealmeGlyphView glyphVectorView;
    private TextView btnColorModeUnified;
    private TextView btnColorModeSegment;
    private TextView btnColorModeRandom;
    private ColorSliderView colorSliderPicker;
    private TextView btnPulseAll;
    private TextView btnTurnOff;

    // --- Page 3: System Engine / 24h & Presets ---
    private ModernSwitch switchAudioEngine;
    private TextView tvEngineStatusDesc;
    private TextView chipPresetNothing, chipPresetPhonk, chipPresetRock, chipPresetEdm, chipPresetCustom;
    private TextView tvDaemonStatus, btnReconnectDaemon;
    private int mSelectedPresetIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupNavigation();
        setupAppsPage();
        setupStudioPage();
        setupEnginePage();

        RealmeGlyphDriver.init(this);

        checkPermission();
        updateStatsAndPresets();

        // Start on Tab 0 (Players)
        selectTab(0);

        mainHandler.postDelayed(this::loadApplications, 100);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermission();
        updateStatsAndPresets();
        if (mCurrentTab == 1) {
            syncColorTargetUI();
        } else if (mCurrentTab == 2) {
            updateEngineControls();
        }
    }

    private void initViews() {
        tvPermStatus = findViewById(R.id.tv_perm_status);

        navTabApps = findViewById(R.id.nav_tab_apps);
        navTabStudio = findViewById(R.id.nav_tab_studio);
        navTabEngine = findViewById(R.id.nav_tab_engine);

        pageContainer = findViewById(R.id.page_container);
        pageApps = findViewById(R.id.page_apps);
        pageStudio = findViewById(R.id.page_studio);
        pageEngine = findViewById(R.id.page_engine);

        // Keep all views attached in page_container, switch strictly via visibility
        if (pageApps != null) pageApps.setVisibility(View.VISIBLE);
        if (pageStudio != null) pageStudio.setVisibility(View.GONE);
        if (pageEngine != null) pageEngine.setVisibility(View.GONE);
    }

    // =========================================================================
    // NAVIGATION (Zero tearing - strictly setVisibility)
    // =========================================================================
    private void setupNavigation() {
        navTabApps.setOnClickListener(v -> selectTab(0));
        navTabStudio.setOnClickListener(v -> selectTab(1));
        navTabEngine.setOnClickListener(v -> selectTab(2));
    }

    private void selectTab(int index) {
        mCurrentTab = index;

        if (pageApps != null) pageApps.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        if (pageStudio != null) pageStudio.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        if (pageEngine != null) pageEngine.setVisibility(index == 2 ? View.VISIBLE : View.GONE);

        if (index == 1) {
            syncColorTargetUI();
        } else if (index == 2) {
            updateEngineControls();
        }

        updateTabButton(navTabApps, index == 0);
        updateTabButton(navTabStudio, index == 1);
        updateTabButton(navTabEngine, index == 2);
    }

    private void updateTabButton(TextView btn, boolean isSelected) {
        if (btn == null) return;
        if (isSelected) {
            btn.setBackgroundResource(R.drawable.bg_pill_lime);
            btn.setTextColor(getColor(R.color.text_black));
        } else {
            btn.setBackgroundResource(0);
            btn.setTextColor(getColor(R.color.text_secondary));
        }
    }

    // =========================================================================
    // PAGE 1: APPS WHITELIST
    // =========================================================================
    private void setupAppsPage() {
        appListView = findViewById(R.id.app_list);
        loadingProgress = findViewById(R.id.loading_progress);
        emptyView = findViewById(R.id.empty_view);

        headerView = getLayoutInflater().inflate(R.layout.header_main, appListView, false);
        btnReset = headerView.findViewById(R.id.btn_reset);
        etSearch = headerView.findViewById(R.id.et_search);
        tabAll = headerView.findViewById(R.id.tab_all);
        tabActive = headerView.findViewById(R.id.tab_active);

        appListView.addHeaderView(headerView, null, false);

        tvPermStatus.setOnClickListener(v -> {
            if (!PulseLightManager.hasPermission(this)) {
                showPermissionDialog();
            } else {
                Toast.makeText(this, "Системные права WRITE_SECURE_SETTINGS активны ✓", Toast.LENGTH_SHORT).show();
            }
        });

        btnReset.setOnClickListener(v -> showResetDialog());

        tabAll.setOnClickListener(v -> {
            tabAll.setBackgroundResource(R.drawable.bg_pill_lime);
            tabAll.setTextColor(getColor(R.color.text_black));
            tabActive.setBackgroundResource(R.drawable.bg_pill_dark);
            tabActive.setTextColor(getColor(R.color.text_secondary));
            if (adapter != null) adapter.setFilterMode(false);
        });

        tabActive.setOnClickListener(v -> {
            tabActive.setBackgroundResource(R.drawable.bg_pill_lime);
            tabActive.setTextColor(getColor(R.color.text_black));
            tabAll.setBackgroundResource(R.drawable.bg_pill_dark);
            tabAll.setTextColor(getColor(R.color.text_secondary));
            if (adapter != null) adapter.setFilterMode(true);
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) adapter.filterQuery(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void checkPermission() {
        boolean secGranted = PulseLightManager.hasPermission(this);

        if (secGranted) {
            tvPermStatus.setText("ACTIVE");
            tvPermStatus.setBackgroundResource(R.drawable.bg_badge_active);
            tvPermStatus.setTextColor(getColor(R.color.text_black));
        } else {
            tvPermStatus.setText("ТРЕБУЕТСЯ ADB");
            tvPermStatus.setBackgroundResource(R.drawable.bg_badge_inactive);
            tvPermStatus.setTextColor(getColor(R.color.status_red));
        }

        if (tvDaemonStatus != null) {
            tvDaemonStatus.setText(RealmeGlyphDriver.getStatus());
            tvDaemonStatus.setTextColor(RealmeGlyphDriver.isConnected() ? getColor(R.color.volt_lime) : (secGranted ? getColor(R.color.text_secondary) : getColor(R.color.status_red)));
        }
    }

    private void showPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.perm_dialog_title)
                .setMessage(R.string.perm_dialog_msg)
                .setPositiveButton(R.string.perm_copy, (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("ADB Grant Command", GRANT_COMMAND);
                    if (clipboard != null) clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, R.string.toast_copied, Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton(R.string.perm_check, (dialog, which) -> checkPermission())
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void showResetDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.reset_confirm_title)
                .setMessage(R.string.reset_confirm_msg)
                .setPositiveButton(R.string.btn_reset, (dialog, which) -> {
                    PulseLightManager.resetToStock(this);
                    loadApplications();
                    Toast.makeText(this, "Заводской список восстановлен!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void loadApplications() {
        loadingProgress.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        scanExecutor.execute(() -> {
            try {
                PackageManager pm = getPackageManager();
                Map<String, Boolean> musicAppsMap = PulseLightManager.getMusicAppsMap(this);

                Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> launcherApps = pm.queryIntentActivities(mainIntent, 0);

                Set<String> addedPackages = new HashSet<>();
                List<AppItem> items = new ArrayList<>();

                for (ResolveInfo ri : launcherApps) {
                    if (ri.activityInfo == null) continue;
                    String pkg = ri.activityInfo.packageName;
                    if (addedPackages.contains(pkg)) continue;
                    addedPackages.add(pkg);

                    String label = ri.loadLabel(pm).toString();
                    boolean enabled = Boolean.TRUE.equals(musicAppsMap.get(pkg));
                    boolean isSystem = (ri.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;

                    items.add(new AppItem(label, pkg, ri, null, enabled, isSystem));
                }

                for (String whitelistedPkg : musicAppsMap.keySet()) {
                    if (!addedPackages.contains(whitelistedPkg)) {
                        try {
                            ApplicationInfo ai = pm.getApplicationInfo(whitelistedPkg, 0);
                            String label = pm.getApplicationLabel(ai).toString();
                            boolean enabled = Boolean.TRUE.equals(musicAppsMap.get(whitelistedPkg));
                            items.add(new AppItem(label, whitelistedPkg, null, null, enabled, true));
                            addedPackages.add(whitelistedPkg);
                        } catch (PackageManager.NameNotFoundException ignored) {}
                    }
                }

                Collections.sort(items, (a, b) -> {
                    if (a.isEnabled() != b.isEnabled()) {
                        return a.isEnabled() ? -1 : 1;
                    }
                    return a.getAppName().compareToIgnoreCase(b.getAppName());
                });

                runOnUiThread(() -> {
                    allAppItems.clear();
                    allAppItems.addAll(items);
                    adapter = new AppAdapter(this, allAppItems, (item, isEnabled) -> {
                        updateStatsAndPresets();
                        Toast.makeText(this, (isEnabled ? "Включено: " : "Отключено: ") + item.getAppName(), Toast.LENGTH_SHORT).show();
                    });
                    adapter.setOnFilterResultListener((total, filtered) -> {
                        emptyView.setVisibility(filtered == 0 ? View.VISIBLE : View.GONE);
                    });
                    appListView.setAdapter(adapter);
                    loadingProgress.setVisibility(View.GONE);
                    updateStatsAndPresets();

                    scanExecutor.execute(() -> {
                        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                        PackageManager packageManager = getPackageManager();
                        for (AppItem appItem : allAppItems) {
                            if (appItem.getIcon() == null) {
                                appItem.loadIconSync(packageManager);
                            }
                        }
                        runOnUiThread(() -> {
                            if (adapter != null) adapter.notifyDataSetChanged();
                        });
                    });
                });

            } catch (Throwable t) {
                Log.e(TAG, "Error loading applications", t);
                runOnUiThread(() -> loadingProgress.setVisibility(View.GONE));
            }
        });
    }

    private void updateStatsAndPresets() {
        int activeCount = PulseLightManager.getActiveCount(this);
        tabAll.setText("Все (" + allAppItems.size() + ")");
        tabActive.setText("Только активные (" + activeCount + ")");
    }

    // =========================================================================
    // PAGE 2: GLYPH STUDIO (Direct Settings.Global + Always-On Aura)
    // =========================================================================
    private void setupStudioPage() {
        glyphVectorView = findViewById(R.id.glyph_vector_view);
        btnColorModeUnified = findViewById(R.id.btn_color_mode_unified);
        btnColorModeSegment = findViewById(R.id.btn_color_mode_segment);
        btnColorModeRandom = findViewById(R.id.btn_color_mode_random);

        colorSliderPicker = findViewById(R.id.color_slider_picker);

        btnPulseAll = findViewById(R.id.btn_pulse_all);
        btnTurnOff = findViewById(R.id.btn_turn_off);

        btnColorModeUnified.setOnClickListener(v -> setColorTarget(TARGET_MUSIC_BASE));
        btnColorModeSegment.setOnClickListener(v -> setColorTarget(TARGET_MUSIC_FLICKER));
        btnColorModeRandom.setOnClickListener(v -> setColorTarget(TARGET_ALWAYS_ON));

        colorSliderPicker.setOnColorChangeListener(new ColorSliderView.OnColorChangeListener() {
            @Override
            public void onColorChanged(int color, boolean fromUser) {
                if (fromUser) {
                    applyTargetColor(color);
                }
            }

            @Override
            public void onColorChangeStop(int color) {
                applyTargetColor(color);
            }
        });

        btnPulseAll.setOnClickListener(v -> {
            int curColor = getCurrentTargetColor();
            RealmeGlyphDriver.setAlwaysOn(true, curColor);
            if (glyphVectorView != null) {
                glyphVectorView.setPower(true);
            }
            Toast.makeText(this, "Always-On аура включена на корпусе!", Toast.LENGTH_SHORT).show();
        });

        btnTurnOff.setOnClickListener(v -> {
            RealmeGlyphDriver.setAlwaysOn(false, 0);
            RealmeGlyphDriver.turnOffImmediate();
            if (glyphVectorView != null) {
                glyphVectorView.setPower(false);
            }
            Toast.makeText(this, "Подсветка выключена", Toast.LENGTH_SHORT).show();
        });

        setColorTarget(TARGET_MUSIC_BASE);
    }

    private void setColorTarget(int target) {
        mSelectedColorTarget = target;
        updatePill(btnColorModeUnified, target == TARGET_MUSIC_BASE);
        updatePill(btnColorModeSegment, target == TARGET_MUSIC_FLICKER);
        updatePill(btnColorModeRandom, target == TARGET_ALWAYS_ON);
        syncColorTargetUI();
    }

    private int getCurrentTargetColor() {
        if (mSelectedColorTarget == TARGET_MUSIC_FLICKER) {
            return PulseLightManager.getMusicFlickerColor(this);
        } else if (mSelectedColorTarget == TARGET_ALWAYS_ON) {
            return PulseLightManager.getAlwaysOnColor(this);
        } else {
            return PulseLightManager.getMusicColor(this);
        }
    }

    private void saveTargetColor(int color) {
        if (mSelectedColorTarget == TARGET_MUSIC_FLICKER) {
            PulseLightManager.setMusicFlickerColor(this, color);
        } else if (mSelectedColorTarget == TARGET_ALWAYS_ON) {
            PulseLightManager.setAlwaysOnColor(this, color);
        } else {
            PulseLightManager.setMusicColor(this, color);
        }
        GlyphColorManager.setUnifiedColor(this, color);
    }

    private void applyTargetColor(int color) {
        if (colorSliderPicker != null) {
            colorSliderPicker.setColor(color);
        }
        if (glyphVectorView != null) {
            glyphVectorView.setPreviewColor(color);
        }
        RealmeGlyphDriver.flashSegment(RealmeGlyphDriver.LED_ALL, color, 600);
        saveTargetColor(color);
    }

    private void syncColorTargetUI() {
        int color = getCurrentTargetColor();
        if (colorSliderPicker != null) {
            colorSliderPicker.setColor(color);
        }
        if (glyphVectorView != null) {
            glyphVectorView.updateColorsFromManager();
        }
    }

    private void updatePill(TextView tv, boolean active) {
        if (tv == null) return;
        if (active) {
            tv.setBackgroundResource(R.drawable.bg_pill_lime);
            tv.setTextColor(getColor(R.color.text_black));
        } else {
            tv.setBackgroundResource(R.drawable.bg_pill_dark);
            tv.setTextColor(getColor(R.color.text_secondary));
        }
    }

    // =========================================================================
    // PAGE 3: SYSTEM ENGINE (24/7 Unlimited & ColorOS Presets)
    // =========================================================================
    private void setupEnginePage() {
        switchAudioEngine = findViewById(R.id.switch_audio_engine);
        tvEngineStatusDesc = findViewById(R.id.tv_engine_status_desc);

        chipPresetNothing = findViewById(R.id.chip_preset_nothing);
        chipPresetPhonk = findViewById(R.id.chip_preset_phonk);
        chipPresetRock = findViewById(R.id.chip_preset_rock);
        chipPresetEdm = findViewById(R.id.chip_preset_edm);
        chipPresetCustom = findViewById(R.id.chip_preset_custom);

        tvDaemonStatus = findViewById(R.id.tv_daemon_status);
        btnReconnectDaemon = findViewById(R.id.btn_reconnect_daemon);

        // Switch 24/7 Mode
        switchAudioEngine.setOnCheckedChangeListener((view, isChecked) -> {
            PulseLightManager.set24hMode(this, isChecked);
            if (isChecked) {
                tvEngineStatusDesc.setText("Режим 24/7 активен • Подсветка работает круглосуточно");
                tvEngineStatusDesc.setTextColor(getColor(R.color.volt_lime));
                Toast.makeText(this, "Активирован режим 24/7 (00:00 - 23:59)", Toast.LENGTH_SHORT).show();
            } else {
                tvEngineStatusDesc.setText("Лимит времени активен (08:00 - 23:00)");
                tvEngineStatusDesc.setTextColor(getColor(R.color.text_muted));
                Toast.makeText(this, "Установлен интервал (08:00 - 23:00)", Toast.LENGTH_SHORT).show();
            }
        });

        // Presets Click Listeners - direct System ColorOS settings!
        chipPresetNothing.setOnClickListener(v -> applySystemPreset(0xFFFDFFFB, 0xFFFDFFFB, 0));
        chipPresetPhonk.setOnClickListener(v -> applySystemPreset(0xFF9B51E0, 0xFFFF0055, 1));
        chipPresetRock.setOnClickListener(v -> applySystemPreset(0xFFFF6B00, 0xFFFFE600, 2));
        chipPresetEdm.setOnClickListener(v -> applySystemPreset(0xFF00E5FF, 0xFF00FF66, 3));
        chipPresetCustom.setOnClickListener(v -> applySystemPreset(0xFF71BBFF, 0xFFFF8175, 4));

        // Status check button -> reconnects hardware daemon and verifies permission
        btnReconnectDaemon.setOnClickListener(v -> {
            RealmeGlyphDriver.connectAsync();
            checkPermission();
            mainHandler.postDelayed(this::checkPermission, 600);
            Toast.makeText(this, "Подключение к драйверу подсветки...", Toast.LENGTH_SHORT).show();
        });

        updateEngineControls();
    }

    private void applySystemPreset(int baseColor, int flickerColor, int presetIdx) {
        mSelectedPresetIndex = presetIdx;
        PulseLightManager.setMusicColor(this, baseColor);
        PulseLightManager.setMusicFlickerColor(this, flickerColor);
        PulseLightManager.setAlwaysOnColor(this, baseColor);
        updatePresetChipsUI();

        String name;
        switch (presetIdx) {
            case 0: name = "Nothing Phone Pure (Белый)"; break;
            case 1: name = "Phonk / 808 Bass (Пурпур / Неон)"; break;
            case 2: name = "Rock / Drums (Огонь / Желтый)"; break;
            case 3: name = "EDM / Cyberpunk (Cyan / Lime)"; break;
            default: name = "Realme GT Mode"; break;
        }
        Toast.makeText(this, "Применен пресет: " + name, Toast.LENGTH_SHORT).show();
    }

    private void updatePresetChipsUI() {
        updatePill(chipPresetNothing, mSelectedPresetIndex == 0);
        updatePill(chipPresetPhonk, mSelectedPresetIndex == 1);
        updatePill(chipPresetRock, mSelectedPresetIndex == 2);
        updatePill(chipPresetEdm, mSelectedPresetIndex == 3);
        updatePill(chipPresetCustom, mSelectedPresetIndex == 4);
    }

    private void updateEngineControls() {
        boolean is24h = PulseLightManager.is24hMode(this);
        switchAudioEngine.setChecked(is24h);
        if (is24h) {
            tvEngineStatusDesc.setText("Режим 24/7 активен • Подсветка работает круглосуточно");
            tvEngineStatusDesc.setTextColor(getColor(R.color.volt_lime));
        } else {
            tvEngineStatusDesc.setText("Лимит времени активен (08:00 - 23:00)");
            tvEngineStatusDesc.setTextColor(getColor(R.color.text_muted));
        }

        if (tvDaemonStatus != null) {
            boolean hasPerm = PulseLightManager.hasPermission(this);
            tvDaemonStatus.setText(hasPerm ? "Native Settings.Global (Offline) ✓" : "Требуется WRITE_SECURE_SETTINGS");
            tvDaemonStatus.setTextColor(hasPerm ? getColor(R.color.volt_lime) : getColor(R.color.status_red));
        }

        updatePresetChipsUI();
    }
}

