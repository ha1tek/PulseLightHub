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
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.SeekBar;
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
    // Themeable Layout Containers
    private View layoutRoot;
    private View layoutTopHeader;
    private View layoutNavBar;
    private View navTabsLayout;
    private View navTabIndicator;
    private View pageStudioInner;
    private View pageEngineInner;

    // Navigation Tabs (2 tabs: 0 - Glyph Studio, 1 - Settings)
    private TextView navTabStudio;
    private TextView navTabEngine;

    private android.widget.FrameLayout pageContainer;
    private int mCurrentTab = 0;
    private View pageStudio;
    private View pageEngine;

    // Header Views
    private TextView tvPermStatus;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // --- Page 2: Glyph Studio ---
    private RealmeGlyphView glyphVectorView;
    private ColorSliderView colorSliderPicker;
    private TextView btnPulseAll;
    private TextView btnTurnOff;

    private SpectrumVisualizerView studioSpectrumVisualizer;
    private TextView btnModeFast, btnModeDeep;
    private View layoutDeepSpectrumSelector;
    private TextView btnSpecNarrow, btnSpecWide;
    private TextView tvSpectrumRmsVal;
    private TextView btnAutoCalibrate;
    private TextView btnRandomConfig;
    private TextView btnResetDefaults;
    private TextView tvDiagramIntervalVal;
    private SeekBar seekDiagramInterval;
    private TextView tvSpectrumGainVal;
    private SeekBar seekSpectrumGain;
    private long mLastDiagramUpdateTime = 0;

    private View containerFastMode, containerDeepMode;
    private LinearLayout layoutQuickTriggersList;
    private final List<TextView> mQuickTriggerButtons = new ArrayList<>();
    private ModernSwitch switchBandEnabled;
    private TextView tvSelectedBandTitle, tvSelectedBandGainVal;
    private SeekBar seekBandGain;
    private TextView tvSelectedBandThreshVal;
    private SeekBar seekBandThresh;

    private final TextView[] mColPatternButtons = new TextView[12];

    // Preset Dropdown
    private LinearLayout layoutPresetDropdown;
    private TextView tvPresetDropdownName, tvPresetDropdownBadge, tvPresetDropdownArrow;
    private TextView btnStudioAddPreset, btnStudioExportPreset, btnStudioImportPreset;

    // Glyph Beat Hold Times
    private ModernSwitch switchGlyphMinTime, switchGlyphMaxTime;
    private View containerGlyphMinTime, containerGlyphMaxTime;
    private TextView tvGlyphMinTimeVal, tvGlyphMaxTimeVal;
    private SeekBar seekGlyphMinTime, seekGlyphMaxTime;

    // Expanded Audio Filters
    private ModernSwitch switchFilterOnset, switchFilterLoudness;
    private View containerLoudnessSlider;
    private TextView tvLoudnessGateVal;
    private SeekBar seekLoudnessGate;
    private ModernSwitch switchFilterFInterp, switchFilterVariation, switchFilterLimiter;
    private View containerFilterFInterp, containerFilterVariation;
    private TextView tvFilterFInterpVal, tvFilterVariationVal;
    private SeekBar seekFilterFInterp, seekFilterVariation;

    private int mSelectedBandIndex = 0;
    private boolean mSelectedBandIsWide = false;

    // --- Page 3: Settings (Engine & Customization) ---
    private ModernSwitch switchAudioEngine;
    private ModernSwitch switchEngineBandThreshold;
    private TextView tvEngineStatusDesc;
    private SpectrumVisualizerView engineSpectrumVisualizer;
    private ThemePaletteView paletteBgColor, paletteAccentColor;
    private TextView tvSensitivityValue;
    private SeekBar seekSensitivity;
    private TextView tvDecayValue;
    private SeekBar seekDecay;

    private static final int REQUEST_MEDIA_PROJECTION = 1001;
    private boolean mIsUpdatingEngineUI = false;

    private AudioAnalyzer mAudioAnalyzer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAudioAnalyzer = new AudioAnalyzer(this);

        initViews();
        setupNavigation();
        setupStudioPage();
        setupEnginePage();

        RealmeGlyphDriver.init(this);
        updateDriverStatusBadge();

        // Ensure engine is OFF on app launch - user must turn it ON manually
        AudioAnalyzer.setEngineEnabled(this, false);
        PulseAudioService.stopEngine(this);

        // Start on Tab 0 (Glyph Studio)
        selectTab(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyThemeColors(ThemeManager.getBackgroundColor(this), ThemeManager.getAccentColor(this));
        updateDriverStatusBadge();
        syncColorTargetUI();
        updateEngineControls();
        attachFrameListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        PulseAudioService.setFrameListener(null);
    }

    private void attachFrameListener() {
        PulseAudioService.setFrameListener((result, currentColor) -> {
            mainHandler.post(() -> {
                long now = System.currentTimeMillis();
                int interval = (mAudioAnalyzer != null) ? mAudioAnalyzer.getDiagramIntervalMs() : 100;
                boolean shouldUpdateDiagram = (now - mLastDiagramUpdateTime >= interval);

                if (shouldUpdateDiagram) {
                    mLastDiagramUpdateTime = now;
                    int accentColor = ThemeManager.getAccentColor(MainActivity.this);
                    if (mCurrentTab == 0) {
                        if (studioSpectrumVisualizer != null) {
                            studioSpectrumVisualizer.setBarColor(accentColor);
                            studioSpectrumVisualizer.updateData(result);
                        }
                        if (tvSpectrumRmsVal != null) {
                            tvSpectrumRmsVal.setText(String.format(java.util.Locale.US, "RMS: %d%%", (int) (result.rmsLoudness * 100)));
                        }
                    } else if (mCurrentTab == 1) {
                        if (engineSpectrumVisualizer != null) {
                            engineSpectrumVisualizer.setBarColor(accentColor);
                            engineSpectrumVisualizer.updateData(result);
                        }
                    }
                }

                if (glyphVectorView != null && (mCurrentTab == 0 || mCurrentTab == 1)) {
                    if (result.activeLedMask != 0) {
                        glyphVectorView.setSegmentIntensity(result.activeLedMask, result.intensity, currentColor);
                    } else {
                        glyphVectorView.fadeSegmentToResting(RealmeGlyphDriver.LED_ALL, 120);
                    }
                }
            });
        });
    }

    private void initViews() {
        layoutRoot = findViewById(R.id.layout_root);
        layoutTopHeader = findViewById(R.id.layout_top_header);
        layoutNavBar = findViewById(R.id.layout_nav_bar);
        navTabsLayout = findViewById(R.id.nav_tabs_layout);
        navTabIndicator = findViewById(R.id.nav_tab_indicator);

        if (navTabsLayout != null) {
            navTabsLayout.addOnLayoutChangeListener((v, left, top, right, bottom, oldL, oldT, oldR, oldB) -> {
                if (right - left != oldR - oldL) {
                    animateTabSwitch(mCurrentTab, mCurrentTab, false);
                }
            });
        }

        tvPermStatus = findViewById(R.id.tv_perm_status);

        navTabStudio = findViewById(R.id.nav_tab_studio);
        navTabEngine = findViewById(R.id.nav_tab_engine);

        pageContainer = findViewById(R.id.page_container);
        pageStudio = findViewById(R.id.page_studio);
        pageEngine = findViewById(R.id.page_engine);

        pageStudioInner = findViewById(R.id.page_studio_inner);
        pageEngineInner = findViewById(R.id.page_engine_inner);

        if (pageStudio != null) pageStudio.setVisibility(View.VISIBLE);
        if (pageEngine != null) pageEngine.setVisibility(View.GONE);
    }

    // =========================================================================
    // NAVIGATION (2 Tabs: 0 - Glyph Studio, 1 - Settings)
    // =========================================================================
    private void setupNavigation() {
        navTabStudio.setOnClickListener(v -> selectTab(0));
        navTabEngine.setOnClickListener(v -> selectTab(1));
    }

    private void applyButtonFeedback(View view) {
        if (view == null) return;
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                    break;
            }
            return false;
        });
    }

    private View getPageView(int tabIndex) {
        switch (tabIndex) {
            case 0: return pageStudio;
            case 1: return pageEngine;
            default: return null;
        }
    }

    private void selectTab(int index) {
        int oldIndex = mCurrentTab;
        mCurrentTab = index;

        View oldPage = getPageView(oldIndex);
        View newPage = getPageView(index);

        if (oldPage != null && newPage != null && oldIndex != index && oldPage.getVisibility() == View.VISIBLE) {
            float slideDist = 20.0f * getResources().getDisplayMetrics().density;
            float direction = (index > oldIndex) ? 1.0f : -1.0f;

            oldPage.animate()
                    .alpha(0.0f)
                    .translationX(-direction * slideDist)
                    .setDuration(140)
                    .withEndAction(() -> {
                        oldPage.setVisibility(View.GONE);
                        oldPage.setAlpha(1.0f);
                        oldPage.setTranslationX(0.0f);
                    })
                    .start();

            newPage.setVisibility(View.VISIBLE);
            newPage.setAlpha(0.0f);
            newPage.setTranslationX(direction * slideDist);
            newPage.animate()
                    .alpha(1.0f)
                    .translationX(0.0f)
                    .setDuration(200)
                    .start();
        } else {
            if (pageStudio != null) pageStudio.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
            if (pageEngine != null) pageEngine.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        }

        if (index == 0) {
            syncColorTargetUI();
            updateStudioSpectrumUI();
            updateStudioPatternsUI();
            updatePresetDropdownUI();
        } else if (index == 1) {
            updateEngineControls();
            if (engineSpectrumVisualizer != null && mAudioAnalyzer != null) {
                engineSpectrumVisualizer.setSpectrumMode(mAudioAnalyzer.getSpectrumMode());
            }
        }

        animateTabSwitch(oldIndex, index, oldIndex != index);
    }

    private void animateTabSwitch(int oldIndex, int newIndex, boolean animate) {
        if (navTabsLayout == null || navTabIndicator == null) return;

        int accent = ThemeManager.getAccentColor(this);
        int activeTextColor = ThemeManager.getContrastTextColor(accent);
        int inactiveTextColor = getColor(R.color.text_secondary);

        navTabIndicator.setBackground(ThemeManager.createPillDrawable(accent, 999, this));

        int usableWidth = navTabsLayout.getWidth() - navTabsLayout.getPaddingLeft() - navTabsLayout.getPaddingRight();
        if (usableWidth <= 0) {
            navTabsLayout.post(() -> animateTabSwitch(oldIndex, newIndex, false));
            return;
        }

        TextView targetTab = (newIndex == 0) ? navTabStudio : navTabEngine;
        int targetWidth;
        float targetX;
        if (targetTab != null && targetTab.getWidth() > 0) {
            targetWidth = targetTab.getWidth();
            targetX = (float) targetTab.getLeft();
        } else {
            targetWidth = usableWidth / 2;
            targetX = (float) (newIndex * targetWidth);
        }

        ViewGroup.LayoutParams lp = navTabIndicator.getLayoutParams();
        if (lp.width != targetWidth) {
            lp.width = targetWidth;
            navTabIndicator.setLayoutParams(lp);
        }

        TextView[] tabs = new TextView[]{navTabStudio, navTabEngine};
        if (animate) {
            navTabIndicator.animate()
                    .translationX(targetX)
                    .setDuration(220)
                    .setInterpolator(new DecelerateInterpolator(1.8f))
                    .start();

            for (int i = 0; i < tabs.length; i++) {
                TextView tab = tabs[i];
                if (tab == null) continue;
                boolean isSelected = (i == newIndex);
                int fromColor = tab.getCurrentTextColor();
                int toColor = isSelected ? activeTextColor : inactiveTextColor;

                if (isSelected) {
                    tab.setScaleX(0.92f);
                    tab.setScaleY(0.92f);
                    tab.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(220)
                            .setInterpolator(new DecelerateInterpolator(1.8f))
                            .start();
                }

                ValueAnimator colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), fromColor, toColor);
                colorAnim.setDuration(200);
                colorAnim.addUpdateListener(anim -> tab.setTextColor((int) anim.getAnimatedValue()));
                colorAnim.start();
            }
        } else {
            navTabIndicator.setTranslationX(targetX);
            for (int i = 0; i < tabs.length; i++) {
                if (tabs[i] != null) {
                    tabs[i].setTextColor(i == newIndex ? activeTextColor : inactiveTextColor);
                }
            }
        }
    }

    private void updateDriverStatusBadge() {
        int accent = ThemeManager.getAccentColor(this);
        if (tvPermStatus != null) {
            tvPermStatus.setText("ACTIVE");
            tvPermStatus.setBackground(ThemeManager.createPillDrawable(accent, 999, this));
            tvPermStatus.setTextColor(ThemeManager.getContrastTextColor(accent));
            tvPermStatus.setOnClickListener(v -> {
                Toast.makeText(this, RealmeGlyphDriver.getStatus(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private static int intervalProgressToMs(int progress) {
        if (progress <= 0) return 10;
        float fraction = progress / 100.0f;
        return Math.max(10, Math.min(1000, (int) Math.round(Math.pow(fraction, 2.0) * 990.0 + 10.0)));
    }

    private static int intervalMsToProgress(int ms) {
        if (ms <= 10) return 0;
        double frac = Math.sqrt((Math.min(1000, Math.max(10, ms)) - 10.0) / 990.0);
        return Math.max(0, Math.min(100, (int) Math.round(frac * 100.0)));
    }

    private static String formatIntervalLabel(int ms) {
        if (ms <= 10) {
            return "0.010 сек • 10 мс • реалтайм";
        } else {
            return String.format(java.util.Locale.US, "%.3f сек • %d мс", ms / 1000.0f, ms);
        }
    }

    // =========================================================================
    // PAGE 2: GLYPH STUDIO (Direct Settings.Global + Always-On Aura)
    // =========================================================================
    private void setupStudioPage() {
        setupAudioEngineSwitch();
        glyphVectorView = findViewById(R.id.glyph_vector_view);
        colorSliderPicker = findViewById(R.id.color_slider_view);
        btnTurnOff = findViewById(R.id.btn_turn_off_hal);

        if (colorSliderPicker != null) {
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
        }

        if (btnTurnOff != null) {
            applyButtonFeedback(btnTurnOff);
            btnTurnOff.setOnClickListener(v -> {
                RealmeGlyphDriver.setAlwaysOn(false, 0);
                RealmeGlyphDriver.turnOffImmediate();
                if (glyphVectorView != null) {
                    glyphVectorView.setPower(false);
                }
                Toast.makeText(this, "Подсветка выключена", Toast.LENGTH_SHORT).show();
            });
        }

        // Preset Dropdown
        layoutPresetDropdown = findViewById(R.id.layout_preset_dropdown);
        tvPresetDropdownName = findViewById(R.id.tv_preset_dropdown_name);
        tvPresetDropdownBadge = findViewById(R.id.tv_preset_dropdown_badge);
        tvPresetDropdownArrow = findViewById(R.id.tv_preset_dropdown_arrow);
        if (layoutPresetDropdown != null) {
            applyButtonFeedback(layoutPresetDropdown);
            layoutPresetDropdown.setOnClickListener(v -> showPresetDropdownDialog());
        }
        updatePresetDropdownUI();

        btnStudioAddPreset = findViewById(R.id.btn_studio_add_preset);
        btnStudioExportPreset = findViewById(R.id.btn_studio_export_preset);
        btnStudioImportPreset = findViewById(R.id.btn_studio_import_preset);
        if (btnStudioAddPreset != null) {
            applyButtonFeedback(btnStudioAddPreset);
            btnStudioAddPreset.setOnClickListener(v -> showSavePresetDialog());
        }
        if (btnStudioExportPreset != null) {
            applyButtonFeedback(btnStudioExportPreset);
            btnStudioExportPreset.setOnClickListener(v -> exportActivePresetToClipboard());
        }
        if (btnStudioImportPreset != null) {
            applyButtonFeedback(btnStudioImportPreset);
            btnStudioImportPreset.setOnClickListener(v -> showImportPresetDialog());
        }

        // Spectrum Visualizer & Controls
        studioSpectrumVisualizer = findViewById(R.id.studio_spectrum_visualizer);
        btnModeFast = findViewById(R.id.btn_mode_fast);
        btnModeDeep = findViewById(R.id.btn_mode_deep);
        layoutDeepSpectrumSelector = findViewById(R.id.layout_deep_spectrum_selector);
        btnSpecNarrow = findViewById(R.id.btn_spec_narrow);
        btnSpecWide = findViewById(R.id.btn_spec_wide);
        tvSpectrumRmsVal = findViewById(R.id.tv_spectrum_rms_val);
        btnAutoCalibrate = findViewById(R.id.btn_auto_calibrate);
        btnRandomConfig = findViewById(R.id.btn_random_config);
        btnResetDefaults = findViewById(R.id.btn_reset_defaults);
        tvDiagramIntervalVal = findViewById(R.id.tv_diagram_interval_val);
        seekDiagramInterval = findViewById(R.id.seek_diagram_interval);

        applyButtonFeedback(btnModeFast);
        applyButtonFeedback(btnModeDeep);
        applyButtonFeedback(btnSpecNarrow);
        applyButtonFeedback(btnSpecWide);
        applyButtonFeedback(btnAutoCalibrate);
        applyButtonFeedback(btnRandomConfig);
        applyButtonFeedback(btnResetDefaults);

        containerFastMode = findViewById(R.id.container_fast_mode);
        containerDeepMode = findViewById(R.id.container_deep_mode);
        layoutQuickTriggersList = findViewById(R.id.layout_quick_triggers_list);

        tvSelectedBandTitle = findViewById(R.id.tv_selected_band_title);
        tvSelectedBandGainVal = findViewById(R.id.tv_selected_band_gain_val);
        seekBandGain = findViewById(R.id.seek_band_gain);
        tvSelectedBandThreshVal = findViewById(R.id.tv_selected_band_thresh_val);
        seekBandThresh = findViewById(R.id.seek_band_thresh);

        if (btnAutoCalibrate != null) {
            btnAutoCalibrate.setOnClickListener(v -> showAutoCalibrationDialog());
        }

        if (btnRandomConfig != null) {
            btnRandomConfig.setOnClickListener(v -> {
                if (mAudioAnalyzer != null) {
                    mAudioAnalyzer.generateRandomConfig(this);
                }
                PulseAudioService.randomizeAnalyzerConfig(this);
                mSelectedBandIndex = 0;
                mSelectedBandIsWide = (mAudioAnalyzer != null && mAudioAnalyzer.getSpectrumMode() == AudioAnalyzer.SPECTRUM_MODE_WIDE);
                updateStudioSpectrumUI();
                updateBandGainControls();
                updateBandPatternButtonsUI();
                renderQuickTriggersList();
                updateEngineControls();
                updateAllPresetsUI();

                int curColor = GlyphColorManager.getUnifiedColor(this);
                if (glyphVectorView != null) {
                    glyphVectorView.setSegmentIntensity(RealmeGlyphDriver.LED_ALL, 1.0f, curColor);
                    mainHandler.postDelayed(() -> {
                        if (glyphVectorView != null) {
                            glyphVectorView.fadeSegmentToResting(RealmeGlyphDriver.LED_ALL, 180);
                        }
                    }, 250);
                }
                RealmeGlyphDriver.flashSegment(RealmeGlyphDriver.LED_ALL, curColor, 250);

                Toast.makeText(this, "Сгенерирован случайный конфиг!", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnResetDefaults != null) {
            btnResetDefaults.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Сброс настроек")
                        .setMessage("Сбросить все множители, пороги, интервал диаграммы и паттерны глифов на стандартные значения?")
                        .setPositiveButton("Сбросить", (dialog, which) -> resetAllToDefaults())
                        .setNegativeButton("Отмена", null)
                        .show();
            });
        }

        if (seekDiagramInterval != null) {
            int currentMs = (mAudioAnalyzer != null) ? mAudioAnalyzer.getDiagramIntervalMs() : 1;
            int initialProg = intervalMsToProgress(currentMs);
            seekDiagramInterval.setProgress(initialProg);
            if (tvDiagramIntervalVal != null) {
                tvDiagramIntervalVal.setText(formatIntervalLabel(currentMs));
            }

            seekDiagramInterval.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int intervalMs = intervalProgressToMs(progress);
                    if (fromUser && mAudioAnalyzer != null) {
                        mAudioAnalyzer.setDiagramIntervalMs(intervalMs);
                        mAudioAnalyzer.saveSettings(MainActivity.this);
                    }
                    if (tvDiagramIntervalVal != null) {
                        tvDiagramIntervalVal.setText(formatIntervalLabel(intervalMs));
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        tvSpectrumGainVal = findViewById(R.id.tv_spectrum_gain_val);
        seekSpectrumGain = findViewById(R.id.seek_spectrum_gain);
        if (seekSpectrumGain != null && mAudioAnalyzer != null) {
            float gain = mAudioAnalyzer.getSpectrumVisualGain();
            int prog = Math.round(((gain - 0.5f) / 2.5f) * 50.0f);
            seekSpectrumGain.setProgress(Math.max(0, Math.min(50, prog)));
            if (tvSpectrumGainVal != null) {
                tvSpectrumGainVal.setText(String.format(java.util.Locale.US, "%.2fx", gain));
            }
            seekSpectrumGain.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && mAudioAnalyzer != null) {
                        float g = 0.5f + (progress / 50.0f) * 2.5f;
                        g = Math.round(g * 100.0f) / 100.0f;
                        mAudioAnalyzer.setSpectrumVisualGain(g, MainActivity.this);
                        if (tvSpectrumGainVal != null) {
                            tvSpectrumGainVal.setText(String.format(java.util.Locale.US, "%.2fx", g));
                        }
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        View btnInfoSpectrumGain = findViewById(R.id.btn_info_spectrum_gain);
        if (btnInfoSpectrumGain != null) {
            btnInfoSpectrumGain.setOnClickListener(v -> showInfoBubble(v, "Чувствительность спектра",
                    "Масштабирование высоты графических столбов от 0.50x до 3.00x.\n\nПозволяет визуально приподнять тихие композиции или приглушить всплески на громких треках. Не влияет на физические вспышки глифов."));
        }

        switchBandEnabled = findViewById(R.id.switch_band_enabled);

        if (btnModeFast != null) btnModeFast.setOnClickListener(v -> setStudioAnalysisMode(AudioAnalyzer.STUDIO_MODE_FAST));
        if (btnModeDeep != null) btnModeDeep.setOnClickListener(v -> setStudioAnalysisMode(AudioAnalyzer.STUDIO_MODE_DEEP));
        if (btnSpecNarrow != null) btnSpecNarrow.setOnClickListener(v -> setStudioSpectrumMode(AudioAnalyzer.SPECTRUM_MODE_NARROW));
        if (btnSpecWide != null) btnSpecWide.setOnClickListener(v -> setStudioSpectrumMode(AudioAnalyzer.SPECTRUM_MODE_WIDE));

        if (studioSpectrumVisualizer != null) {
            studioSpectrumVisualizer.setOnBandSelectedListener((bandIndex, isWide) -> {
                mSelectedBandIndex = bandIndex;
                mSelectedBandIsWide = isWide;
                updateBandGainControls();
                updateBandPatternButtonsUI();
            });
        }

        if (seekBandGain != null) {
            seekBandGain.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        float gain = 0.20f + (progress / 280.0f) * 2.80f;
                        if (mSelectedBandIsWide) {
                            mAudioAnalyzer.setWideGain(mSelectedBandIndex, gain);
                        } else {
                            mAudioAnalyzer.setNarrowGain(mSelectedBandIndex, gain);
                        }
                        mAudioAnalyzer.saveSettings(MainActivity.this);
                        if (tvSelectedBandGainVal != null) {
                            tvSelectedBandGainVal.setText(String.format(java.util.Locale.US, "%.2fx", gain));
                        }
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        if (seekBandThresh != null) {
            seekBandThresh.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        float thresh = progress / 100.0f;
                        if (mSelectedBandIsWide) {
                            mAudioAnalyzer.setWideThreshold(mSelectedBandIndex, thresh);
                        } else {
                            mAudioAnalyzer.setNarrowThreshold(mSelectedBandIndex, thresh);
                        }
                        mAudioAnalyzer.saveSettings(MainActivity.this);
                        if (tvSelectedBandThreshVal != null) {
                            tvSelectedBandThreshVal.setText(progress + "%");
                        }
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        // Per-Band Pattern Buttons (Deep Mode)
        mColPatternButtons[0] = findViewById(R.id.pattern_col_off);
        mColPatternButtons[1] = findViewById(R.id.pattern_col_top);
        mColPatternButtons[2] = findViewById(R.id.pattern_col_bottom);
        mColPatternButtons[3] = findViewById(R.id.pattern_col_left);
        mColPatternButtons[4] = findViewById(R.id.pattern_col_right);
        mColPatternButtons[5] = findViewById(R.id.pattern_col_top_bottom);
        mColPatternButtons[6] = findViewById(R.id.pattern_col_left_right);
        mColPatternButtons[7] = findViewById(R.id.pattern_col_top_left);
        mColPatternButtons[8] = findViewById(R.id.pattern_col_top_right);
        mColPatternButtons[9] = findViewById(R.id.pattern_col_bottom_left);
        mColPatternButtons[10] = findViewById(R.id.pattern_col_bottom_right);
        mColPatternButtons[11] = findViewById(R.id.pattern_col_all);

        final int[] colPatternVals = {
            AudioAnalyzer.PATTERN_OFF,
            AudioAnalyzer.PATTERN_TOP,
            AudioAnalyzer.PATTERN_BOTTOM,
            AudioAnalyzer.PATTERN_LEFT,
            AudioAnalyzer.PATTERN_RIGHT,
            AudioAnalyzer.PATTERN_TOP_BOTTOM,
            AudioAnalyzer.PATTERN_LEFT_RIGHT,
            AudioAnalyzer.PATTERN_TOP_LEFT,
            AudioAnalyzer.PATTERN_TOP_RIGHT,
            AudioAnalyzer.PATTERN_BOTTOM_LEFT,
            AudioAnalyzer.PATTERN_BOTTOM_RIGHT,
            AudioAnalyzer.PATTERN_ALL
        };

        for (int i = 0; i < mColPatternButtons.length; i++) {
            final int pVal = colPatternVals[i];
            if (mColPatternButtons[i] != null) {
                mColPatternButtons[i].setOnClickListener(v -> selectBandPattern(pVal));
            }
        }

        // Glyph Beat Hold Times
        switchGlyphMinTime = findViewById(R.id.switch_glyph_min_time);
        containerGlyphMinTime = findViewById(R.id.container_glyph_min_time);
        tvGlyphMinTimeVal = findViewById(R.id.tv_glyph_min_time_val);
        seekGlyphMinTime = findViewById(R.id.seek_glyph_min_time);

        switchGlyphMaxTime = findViewById(R.id.switch_glyph_max_time);
        containerGlyphMaxTime = findViewById(R.id.container_glyph_max_time);
        tvGlyphMaxTimeVal = findViewById(R.id.tv_glyph_max_time_val);
        seekGlyphMaxTime = findViewById(R.id.seek_glyph_max_time);

        if (switchGlyphMinTime != null) {
            switchGlyphMinTime.setChecked(mAudioAnalyzer.isEnableMinHoldTime());
            switchGlyphMinTime.setOnCheckedChangeListener((view, isChecked) -> {
                mAudioAnalyzer.setEnableMinHoldTime(isChecked);
                mAudioAnalyzer.saveSettings(MainActivity.this);
                if (containerGlyphMinTime != null) {
                    containerGlyphMinTime.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                }
            });
        }
        if (containerGlyphMinTime != null) {
            containerGlyphMinTime.setVisibility(mAudioAnalyzer.isEnableMinHoldTime() ? View.VISIBLE : View.GONE);
        }
        if (seekGlyphMinTime != null) {
            int holdMs = mAudioAnalyzer.getMinHoldTimeMs();
            seekGlyphMinTime.setProgress(Math.max(0, Math.min(290, holdMs - 10)));
            if (tvGlyphMinTimeVal != null) {
                tvGlyphMinTimeVal.setText(holdMs + " мс");
            }
            seekGlyphMinTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        int ms = 10 + progress;
                        mAudioAnalyzer.setMinHoldTimeMs(ms);
                        mAudioAnalyzer.saveSettings(MainActivity.this);
                        if (tvGlyphMinTimeVal != null) {
                            tvGlyphMinTimeVal.setText(ms + " мс");
                        }
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        if (switchGlyphMaxTime != null) {
            switchGlyphMaxTime.setChecked(mAudioAnalyzer.isEnableMaxHoldTime());
            switchGlyphMaxTime.setOnCheckedChangeListener((view, isChecked) -> {
                mAudioAnalyzer.setEnableMaxHoldTime(isChecked);
                mAudioAnalyzer.saveSettings(MainActivity.this);
                if (containerGlyphMaxTime != null) {
                    containerGlyphMaxTime.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                }
            });
        }
        if (containerGlyphMaxTime != null) {
            containerGlyphMaxTime.setVisibility(mAudioAnalyzer.isEnableMaxHoldTime() ? View.VISIBLE : View.GONE);
        }
        if (seekGlyphMaxTime != null) {
            int maxMs = mAudioAnalyzer.getMaxHoldTimeMs();
            seekGlyphMaxTime.setProgress(Math.max(0, Math.min(950, maxMs - 50)));
            if (tvGlyphMaxTimeVal != null) {
                tvGlyphMaxTimeVal.setText(maxMs + " мс");
            }
            seekGlyphMaxTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        int ms = 50 + progress;
                        mAudioAnalyzer.setMaxHoldTimeMs(ms);
                        mAudioAnalyzer.saveSettings(MainActivity.this);
                        if (tvGlyphMaxTimeVal != null) {
                            tvGlyphMaxTimeVal.setText(ms + " мс");
                        }
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        View btnInfoGlyphMinTime = findViewById(R.id.btn_info_glyph_min_time);
        if (btnInfoGlyphMinTime != null) {
            btnInfoGlyphMinTime.setOnClickListener(v -> showInfoBubble(v, "Минимальное время удержания",
                    "Минимальная длительность свечения глифа при каждом распознанном бите от 10 до 300 мс.\n\nГарантирует четкую вспышку без микро-моргания."));
        }
        View btnInfoGlyphMaxTime = findViewById(R.id.btn_info_glyph_max_time);
        if (btnInfoGlyphMaxTime != null) {
            btnInfoGlyphMaxTime.setOnClickListener(v -> showInfoBubble(v, "Максимальное время свечения",
                    "Предельное время непрерывного свечения светодиодов от 50 до 1000 мс.\n\nГасит подсветку при затянутом басе во избежание ослепления."));
        }

        // Audio Analyzer Filters
        switchFilterOnset = findViewById(R.id.switch_filter_onset);
        switchFilterLoudness = findViewById(R.id.switch_filter_loudness);
        containerLoudnessSlider = findViewById(R.id.container_loudness_slider);
        tvLoudnessGateVal = findViewById(R.id.tv_loudness_gate_val);
        seekLoudnessGate = findViewById(R.id.seek_loudness_gate);

        if (switchFilterOnset != null) {
            switchFilterOnset.setChecked(mAudioAnalyzer.isEnableOnset());
            switchFilterOnset.setOnCheckedChangeListener((view, isChecked) -> {
                mAudioAnalyzer.setEnableOnset(isChecked);
                mAudioAnalyzer.saveSettings(MainActivity.this);
            });
        }

        if (switchFilterLoudness != null) {
            switchFilterLoudness.setChecked(mAudioAnalyzer.isEnableLoudnessGate());
            switchFilterLoudness.setOnCheckedChangeListener((view, isChecked) -> {
                mAudioAnalyzer.setEnableLoudnessGate(isChecked);
                mAudioAnalyzer.saveSettings(MainActivity.this);
                if (containerLoudnessSlider != null) {
                    containerLoudnessSlider.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                }
            });
        }
        if (containerLoudnessSlider != null) {
            containerLoudnessSlider.setVisibility(mAudioAnalyzer.isEnableLoudnessGate() ? View.VISIBLE : View.GONE);
        }

        if (seekLoudnessGate != null) {
            float lg = mAudioAnalyzer.getLoudnessGateThreshold();
            seekLoudnessGate.setProgress(Math.round(lg * 100.0f));
            if (tvLoudnessGateVal != null) {
                tvLoudnessGateVal.setText(Math.round(lg * 100.0f) + "%");
            }
            seekLoudnessGate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        float gate = progress / 100.0f;
                        mAudioAnalyzer.setLoudnessGateThreshold(gate);
                        mAudioAnalyzer.saveSettings(MainActivity.this);
                        if (tvLoudnessGateVal != null) {
                            tvLoudnessGateVal.setText(progress + "%");
                        }
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        // Filter 3: FInterp
        switchFilterFInterp = findViewById(R.id.switch_filter_finterp);
        containerFilterFInterp = findViewById(R.id.container_filter_finterp);
        tvFilterFInterpVal = findViewById(R.id.tv_filter_finterp_val);
        seekFilterFInterp = findViewById(R.id.seek_filter_finterp);

        if (switchFilterFInterp != null) {
            switchFilterFInterp.setChecked(mAudioAnalyzer.isEnableFInterp());
            switchFilterFInterp.setOnCheckedChangeListener((view, isChecked) -> {
                mAudioAnalyzer.setEnableFInterp(isChecked);
                mAudioAnalyzer.saveSettings(MainActivity.this);
                if (containerFilterFInterp != null) {
                    containerFilterFInterp.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                }
            });
        }
        if (containerFilterFInterp != null) {
            containerFilterFInterp.setVisibility(mAudioAnalyzer.isEnableFInterp() ? View.VISIBLE : View.GONE);
        }
        if (seekFilterFInterp != null) {
            float sp = mAudioAnalyzer.getFInterpSpeed();
            seekFilterFInterp.setProgress(Math.max(0, Math.min(45, Math.round(sp - 5.0f))));
            if (tvFilterFInterpVal != null) {
                tvFilterFInterpVal.setText(String.format(java.util.Locale.US, "%.1f", sp));
            }
            seekFilterFInterp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        float speed = 5.0f + progress;
                        mAudioAnalyzer.setFInterpSpeed(speed);
                        mAudioAnalyzer.saveSettings(MainActivity.this);
                        if (tvFilterFInterpVal != null) {
                            tvFilterFInterpVal.setText(String.format(java.util.Locale.US, "%.1f", speed));
                        }
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        // Filter 4: Random Variation
        switchFilterVariation = findViewById(R.id.switch_filter_variation);
        containerFilterVariation = findViewById(R.id.container_filter_variation);
        tvFilterVariationVal = findViewById(R.id.tv_filter_variation_val);
        seekFilterVariation = findViewById(R.id.seek_filter_variation);

        if (switchFilterVariation != null) {
            switchFilterVariation.setChecked(mAudioAnalyzer.isEnableRandomVariation());
            switchFilterVariation.setOnCheckedChangeListener((view, isChecked) -> {
                mAudioAnalyzer.setEnableRandomVariation(isChecked);
                mAudioAnalyzer.saveSettings(MainActivity.this);
                if (containerFilterVariation != null) {
                    containerFilterVariation.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                }
            });
        }
        if (containerFilterVariation != null) {
            containerFilterVariation.setVisibility(mAudioAnalyzer.isEnableRandomVariation() ? View.VISIBLE : View.GONE);
        }
        if (seekFilterVariation != null) {
            float depth = mAudioAnalyzer.getRandomVariationDepth();
            seekFilterVariation.setProgress(Math.max(0, Math.min(35, Math.round((depth - 0.05f) * 100.0f))));
            if (tvFilterVariationVal != null) {
                tvFilterVariationVal.setText(Math.round(depth * 100.0f) + "%");
            }
            seekFilterVariation.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        float d = 0.05f + (progress / 100.0f);
                        mAudioAnalyzer.setRandomVariationDepth(d);
                        mAudioAnalyzer.saveSettings(MainActivity.this);
                        if (tvFilterVariationVal != null) {
                            tvFilterVariationVal.setText(Math.round(d * 100.0f) + "%");
                        }
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        // Filter 5: Peak Limiter
        switchFilterLimiter = findViewById(R.id.switch_filter_limiter);
        if (switchFilterLimiter != null) {
            switchFilterLimiter.setChecked(mAudioAnalyzer.isEnableLimiter());
            switchFilterLimiter.setOnCheckedChangeListener((view, isChecked) -> {
                mAudioAnalyzer.setEnableLimiter(isChecked);
                mAudioAnalyzer.saveSettings(MainActivity.this);
            });
        }

        // Info buttons in Glyph Studio
        View btnInfoAnalysisMode = findViewById(R.id.btn_info_analysis_mode);
        if (btnInfoAnalysisMode != null) {
            btnInfoAnalysisMode.setOnClickListener(v -> showInfoBubble(v, "Режимы анализа спектра",
                    "Быстрый — минимальная задержка и мгновенная реакция диодов.\n\nГлубокий — высочайшее частотное разрешение и разделение саб-баса и бочки с поканальной фильтрацией."));
        }

        View btnInfoSpectrumMode = findViewById(R.id.btn_info_spectrum_mode);
        if (btnInfoSpectrumMode != null) {
            btnInfoSpectrumMode.setOnClickListener(v -> showInfoBubble(v, "Режимы полос спектра",
                    "Узкий 4 полосы — классическое разделение частот на 4 зоны: Sub, Kick, Snare, Treble.\n\nШирокий 12 полос — детальная студийная раскладка по 12 частотным столбам с плавной динамической кривой сплайна от 30 Гц до 16 кГц."));
        }

        View btnInfoDiagramInterval = findViewById(R.id.btn_info_diagram_interval);
        if (btnInfoDiagramInterval != null) {
            btnInfoDiagramInterval.setOnClickListener(v -> showInfoBubble(v, "Интервал диаграммы",
                    "Частота обновления графика спектра в интерфейсе от 0.001 сек до 1 сек.\n\nРегулирует нагрузку на отрисовку экрана и экономию батареи, не влияя на скорость физических вспышек глифа."));
        }

        View btnInfoQuickTriggers = findViewById(R.id.btn_info_quick_triggers);
        if (btnInfoQuickTriggers != null) {
            btnInfoQuickTriggers.setOnClickListener(v -> showInfoBubble(v, "Быстрые комбинации триггеров",
                    "Библиотека из 12 готовых ритмических паттернов раскладки глифов: сплиты, вращения, стерео-боковины, диагональный крест, волны."));
        }

        View btnInfoBandSound = findViewById(R.id.btn_info_band_sound);
        if (btnInfoBandSound != null) {
            btnInfoBandSound.setOnClickListener(this::showSelectedBandSoundInfo);
        }

        View btnInfoBandGain = findViewById(R.id.btn_info_band_gain);
        if (btnInfoBandGain != null) {
            btnInfoBandGain.setOnClickListener(v -> showInfoBubble(v, "Чувствительность выбранной полосы",
                    "Множитель громкости от 0.20x до 3.00x для выбранной частотной полосы.\n\nПозволяет индивидуально усилить тихие звуки или приглушить чрезмерный низкочастотный гул."));
        }

        View btnInfoBandThreshold = findViewById(R.id.btn_info_band_threshold);
        if (btnInfoBandThreshold != null) {
            btnInfoBandThreshold.setOnClickListener(v -> showInfoBubble(v, "Минимальный порог полосы",
                    "Процент минимального всплеска энергии, необходимый для зажигания глифа на этой частоте.\n\nОтсекает фоновый шум. Если порог отключен тумблером в настройках, то полоса срабатывает без жесткого ограничения."));
        }

        View btnInfoBandPattern = findViewById(R.id.btn_info_band_pattern);
        if (btnInfoBandPattern != null) {
            btnInfoBandPattern.setOnClickListener(v -> showInfoBubble(v, "Привязка глифов к частоте",
                    "Определяет, какие светодиодные секции на задней панели смартфона: Верх, Низ, Бока, Диагонали или Все вспыхивают при звуке на этой частотной полосе."));
        }

        View btnInfoFilterOnset = findViewById(R.id.btn_info_filter_onset);
        if (btnInfoFilterOnset != null) {
            btnInfoFilterOnset.setOnClickListener(v -> showInfoBubble(v, "Детектор атак Onset",
                    "Математический фильтр спектрального потока Spectral Flux.\n\nРеагирует на резкие удары инструментов: транзиенты бочки, снейра, щелчки, игнорируя монотонный гул или протяжный вокал."));
        }

        View btnInfoFilterLoudness = findViewById(R.id.btn_info_filter_loudness);
        if (btnInfoFilterLoudness != null) {
            btnInfoFilterLoudness.setOnClickListener(v -> showInfoBubble(v, "Гейт громкости RMS",
                    "Шумоподавитель тихих фрагментов.\n\nБлокирует любые вспышки подсветки в паузах трека, шепоте или тихих диалогах, пропуская вспышки только тогда, когда энергия трека выше установленного порога RMS."));
        }

        View btnInfoFilterFInterp = findViewById(R.id.btn_info_filter_finterp);
        if (btnInfoFilterFInterp != null) {
            btnInfoFilterFInterp.setOnClickListener(v -> showInfoBubble(v, "Сглаживание FInterp",
                    "Экспоненциальная интерполяция FInterpTo для естественного перетекания яркости без ступенчатых рывков."));
        }

        View btnInfoFilterVariation = findViewById(R.id.btn_info_filter_variation);
        if (btnInfoFilterVariation != null) {
            btnInfoFilterVariation.setOnClickListener(v -> showInfoBubble(v, "Органическая вариация",
                    "Динамическая псевдослучайная микро-вариация силы вспышек для живого органичного эффекта."));
        }

        View btnInfoFilterLimiter = findViewById(R.id.btn_info_filter_limiter);
        if (btnInfoFilterLimiter != null) {
            btnInfoFilterLimiter.setOnClickListener(v -> showInfoBubble(v, "Пиковый лимитер атак",
                    "Предотвращает ослепляющее залипание при продолжительном грохоте."));
        }

        syncColorTargetUI();
        updateStudioSpectrumUI();
        updateStudioPatternsUI();
        updatePresetDropdownUI();
    }

    private void setStudioAnalysisMode(int mode) {
        if (mAudioAnalyzer == null) return;
        mAudioAnalyzer.setStudioAnalysisMode(mode);
        if (mode == AudioAnalyzer.STUDIO_MODE_FAST) {
            mAudioAnalyzer.setSpectrumMode(AudioAnalyzer.SPECTRUM_MODE_NARROW);
            if (studioSpectrumVisualizer != null) {
                studioSpectrumVisualizer.setSpectrumMode(AudioAnalyzer.SPECTRUM_MODE_NARROW);
            }
            mSelectedBandIsWide = false;
        }
        mAudioAnalyzer.saveSettings(this);
        updateStudioSpectrumUI();
    }

    private void setStudioSpectrumMode(int mode) {
        if (mAudioAnalyzer == null) return;
        mAudioAnalyzer.setSpectrumMode(mode);
        mAudioAnalyzer.saveSettings(this);
        if (studioSpectrumVisualizer != null) {
            studioSpectrumVisualizer.setSpectrumMode(mode);
        }
        mSelectedBandIndex = 0;
        mSelectedBandIsWide = (mode == AudioAnalyzer.SPECTRUM_MODE_WIDE);
        updateStudioSpectrumUI();
        updateBandGainControls();
        updateBandPatternButtonsUI();
    }

    private void resetAllToDefaults() {
        if (mAudioAnalyzer != null) {
            mAudioAnalyzer.resetToDefaults(this);
        }
        PulseAudioService.resetAnalyzerDefaults(this);
        if (studioSpectrumVisualizer != null) {
            for (int i = 0; i < 4; i++) studioSpectrumVisualizer.setBandEnabled(i, false, true);
            for (int i = 0; i < 12; i++) studioSpectrumVisualizer.setBandEnabled(i, true, true);
        }
        mSelectedBandIndex = 0;
        mSelectedBandIsWide = false;
        updateStudioSpectrumUI();
        updateBandGainControls();
        updateBandPatternButtonsUI();
        renderQuickTriggersList();
        updateEngineControls();
        updateAllPresetsUI();
        Toast.makeText(this, "Все значения сброшены на стандартные", Toast.LENGTH_SHORT).show();
    }

    private void updateStudioSpectrumUI() {
        if (mAudioAnalyzer == null) return;
        int studioMode = mAudioAnalyzer.getStudioAnalysisMode();
        updatePill(btnModeFast, studioMode == AudioAnalyzer.STUDIO_MODE_FAST);
        updatePill(btnModeDeep, studioMode == AudioAnalyzer.STUDIO_MODE_DEEP);

        if (studioMode == AudioAnalyzer.STUDIO_MODE_FAST) {
            if (layoutDeepSpectrumSelector != null) layoutDeepSpectrumSelector.setVisibility(View.GONE);
            if (containerFastMode != null) containerFastMode.setVisibility(View.VISIBLE);
            if (containerDeepMode != null) containerDeepMode.setVisibility(View.GONE);
        } else {
            if (layoutDeepSpectrumSelector != null) layoutDeepSpectrumSelector.setVisibility(View.VISIBLE);
            if (containerFastMode != null) containerFastMode.setVisibility(View.GONE);
            if (containerDeepMode != null) containerDeepMode.setVisibility(View.VISIBLE);
        }

        int specMode = mAudioAnalyzer.getSpectrumMode();
        updatePill(btnSpecNarrow, specMode == AudioAnalyzer.SPECTRUM_MODE_NARROW);
        updatePill(btnSpecWide, specMode == AudioAnalyzer.SPECTRUM_MODE_WIDE);

        if (studioSpectrumVisualizer != null) {
            studioSpectrumVisualizer.setSpectrumMode(specMode);
            int curColor = GlyphColorManager.getUnifiedColor(this);
            studioSpectrumVisualizer.setBarColor(curColor);
        }

        updateBandGainControls();
        updateStudioPatternsUI();

        // Diagram Refresh Interval UI
        if (seekDiagramInterval != null) {
            int currentMs = mAudioAnalyzer.getDiagramIntervalMs();
            seekDiagramInterval.setProgress(intervalMsToProgress(currentMs));
            if (tvDiagramIntervalVal != null) {
                tvDiagramIntervalVal.setText(formatIntervalLabel(currentMs));
            }
        }

        // Spectrum Gain UI
        if (seekSpectrumGain != null) {
            float gain = mAudioAnalyzer.getSpectrumVisualGain();
            int prog = Math.round(((gain - 0.5f) / 2.5f) * 50.0f);
            seekSpectrumGain.setProgress(Math.max(0, Math.min(50, prog)));
            if (tvSpectrumGainVal != null) {
                tvSpectrumGainVal.setText(String.format(java.util.Locale.US, "%.2fx", gain));
            }
        }

        // Filters UI
        if (switchFilterOnset != null) {
            switchFilterOnset.setChecked(mAudioAnalyzer.isEnableOnset());
        }
        if (switchFilterLoudness != null) {
            boolean enabled = mAudioAnalyzer.isEnableLoudnessGate();
            switchFilterLoudness.setChecked(enabled);
            if (containerLoudnessSlider != null) {
                containerLoudnessSlider.setVisibility(enabled ? View.VISIBLE : View.GONE);
            }
        }
        if (seekLoudnessGate != null) {
            int progress = Math.round(mAudioAnalyzer.getLoudnessGateThreshold() * 100.0f);
            seekLoudnessGate.setProgress(progress);
            if (tvLoudnessGateVal != null) {
                tvLoudnessGateVal.setText(progress + "%");
            }
        }
    }

    private static final String[] NARROW_BAND_NAMES = {"SUB 20-80 Гц", "KICK 80-180 Гц", "SNARE 220-900 Гц", "TREBLE 3.5-12 кГц"};
    private static final String[] WIDE_BAND_NAMES = {"30 Гц", "60 Гц", "120 Гц", "250 Гц", "500 Гц", "1 кГц", "2 кГц", "4 кГц", "6 кГц", "9 кГц", "12 кГц", "16 кГц"};

    private static final String[] NARROW_BAND_SOUND_TITLES = {
        "SUB: Саб-бас • 20 - 80 Гц",
        "KICK: Бочка и панч • 80 - 180 Гц",
        "SNARE: Малый барабан и тело • 220 - 900 Гц",
        "TREBLE: Высокие частоты • 3.5 - 12 кГц"
    };

    private static final String[] NARROW_BAND_SOUND_DESCS = {
        "Инфранизкий диапазон: саб-бас, 808 бас, синтезаторные саб-дропы и вибрация.",
        "Основной удар бочки Kick Drum, шлепок бас-гитары и плотный низкочастотный импульс.",
        "Тело рабочего барабана Snare, хлопки Clap, томы, мужской вокал и риффы ритм-гитар.",
        "Хай-хэты Hi-Hats, тарелки Crash/Ride, перкуссия, щелчки атаки и сибилянты вокала."
    };

    private static final String[] WIDE_BAND_SOUND_TITLES = {
        "30 Гц: Инфранизкий саб-бас",
        "60 Гц: Фундамент бочки Kick",
        "120 Гц: Бас-гитара и панч",
        "250 Гц: Нижняя середина",
        "500 Гц: Рабочий барабан Snare",
        "1 кГц: Вокал и соло",
        "2 кГц: Атака и хлопки Clap",
        "4 кГц: Презенс и хруст",
        "6 кГц: Высокая перкуссия",
        "9 кГц: Хай-хэты Hi-Hats",
        "12 кГц: Тарелки и блеск",
        "16 кГц: Воздух и атмосфера"
    };

    private static final String[] WIDE_BAND_SOUND_DESCS = {
        "20-45 Гц. Саб-бас, нижний тон 808-х басов, кино-сабвуферные дропы.",
        "45-90 Гц. Фундамент и тело акустической и электронной бочки Kick, нижние ноты бас-гитары.",
        "90-180 Гц. Панч бочки, средний регистр бас-гитары, плотный синтезаторный бас Reese Bass.",
        "180-350 Гц. Нижняя середина: низкий грудной вокал, тело рабочих барабанов, риффы ритм-гитары.",
        "350-700 Гц. Основной удар и резонанс рабочего барабана Snare, альты, томы, мужской вокал.",
        "700-1400 Гц. Средняя середина: основной тон вокала, аккорды клавишных, соло-гитары, синтезаторы Lead.",
        "1400-2800 Гц. Атака рабочего барабана, щелчок снейра, хлопки Clap, звон римшота, разборчивость речи.",
        "2800-4500 Гц. Презенс: щелчок колотушки бочки Beater click, хруст медиатора электрогитар, атака вокала.",
        "4500-7000 Гц. Высокая перкуссия: шейкеры, тамбурин, атака закрытых хай-хэтов.",
        "7000-10500 Гц. Хай-хэты Hi-Hats: быстрые дроби закрытых и открытых хэтов в трэпе и драм-н-бэйсе.",
        "10500-14500 Гц. Металлический звон тарелок Crash, Ride, яркость струн акустической гитары.",
        "14500-20000 Гц. Воздух Air, ультравысокие обертоны, дыхание вокала, хвосты реверберации синтезаторов."
    };

    private PopupWindow mCurrentInfoPopup = null;

    private void showInfoBubble(View anchor, String title, String description) {
        if (isFinishing() || isDestroyed()) return;
        if (mCurrentInfoPopup != null && mCurrentInfoPopup.isShowing()) {
            mCurrentInfoPopup.dismiss();
            mCurrentInfoPopup = null;
        }

        View popupView = getLayoutInflater().inflate(R.layout.dialog_info_bubble, null);
        TextView tvTitle = popupView.findViewById(R.id.tv_info_bubble_title);
        TextView tvDesc = popupView.findViewById(R.id.tv_info_bubble_desc);
        TextView btnCloseX = popupView.findViewById(R.id.btn_info_bubble_close_x);
        TextView btnOk = popupView.findViewById(R.id.btn_info_bubble_ok);

        int accentColor = ThemeManager.getAccentColor(this);
        int bgColor = ThemeManager.getBackgroundColor(this);
        int cardBgColor = ThemeManager.getCardBackgroundColor(bgColor);
        int cardStrokeColor = ThemeManager.getCardStrokeColor(cardBgColor);

        popupView.setBackground(ThemeManager.createCardDrawable(cardBgColor, cardStrokeColor, 20, this));

        if (tvTitle != null) {
            tvTitle.setText(title);
            tvTitle.setTextColor(accentColor);
        }
        if (tvDesc != null) tvDesc.setText(description);

        int maxWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.88f);
        int cardWidth = Math.min(maxWidth, (int) (380 * getResources().getDisplayMetrics().density));
        PopupWindow popup = new PopupWindow(popupView, cardWidth, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        popup.setElevation(20.0f);

        View.OnClickListener dismissListener = v -> popup.dismiss();
        if (btnCloseX != null) btnCloseX.setOnClickListener(dismissListener);
        if (btnOk != null) {
            btnOk.setBackground(ThemeManager.createPillDrawable(accentColor, 999, this));
            btnOk.setTextColor(ThemeManager.getContrastTextColor(accentColor));
            applyButtonFeedback(btnOk);
            btnOk.setOnClickListener(dismissListener);
        }

        mCurrentInfoPopup = popup;
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            popup.showAtLocation(rootView, Gravity.CENTER, 0, 0);
        }
    }

    private void showSelectedBandSoundInfo(View anchor) {
        String title;
        String desc;
        if (mSelectedBandIsWide) {
            int idx = Math.max(0, Math.min(WIDE_BAND_SOUND_TITLES.length - 1, mSelectedBandIndex));
            title = WIDE_BAND_SOUND_TITLES[idx];
            desc = WIDE_BAND_SOUND_DESCS[idx];
        } else {
            int idx = Math.max(0, Math.min(NARROW_BAND_SOUND_TITLES.length - 1, mSelectedBandIndex));
            title = NARROW_BAND_SOUND_TITLES[idx];
            desc = NARROW_BAND_SOUND_DESCS[idx];
        }
        showInfoBubble(anchor, title, desc);
    }

    private void updateBandGainControls() {
        if (mAudioAnalyzer == null) return;
        String name;
        float gain;
        float thresh;
        if (mSelectedBandIsWide) {
            int idx = Math.max(0, Math.min(WIDE_BAND_NAMES.length - 1, mSelectedBandIndex));
            name = WIDE_BAND_NAMES[idx];
            gain = mAudioAnalyzer.getWideGain(idx);
            thresh = mAudioAnalyzer.getWideThreshold(idx);
        } else {
            int idx = Math.max(0, Math.min(NARROW_BAND_NAMES.length - 1, mSelectedBandIndex));
            name = NARROW_BAND_NAMES[idx];
            gain = mAudioAnalyzer.getNarrowGain(idx);
            thresh = mAudioAnalyzer.getNarrowThreshold(idx);
        }

        if (tvSelectedBandTitle != null) {
            tvSelectedBandTitle.setText("Полоса " + name + ":");
        }
        if (tvSelectedBandGainVal != null) {
            tvSelectedBandGainVal.setText(String.format(java.util.Locale.US, "%.2fx", gain));
        }
        if (seekBandGain != null) {
            int progress = Math.round(((gain - 0.20f) / 2.80f) * 280.0f);
            seekBandGain.setProgress(Math.max(0, Math.min(280, progress)));
        }

        boolean threshEnabled = mAudioAnalyzer.isEnableBandThreshold();
        if (tvSelectedBandThreshVal != null) {
            if (threshEnabled) {
                tvSelectedBandThreshVal.setText(Math.round(thresh * 100.0f) + "%");
                tvSelectedBandThreshVal.setTextColor(ThemeManager.getAccentColor(this));
            } else {
                tvSelectedBandThreshVal.setText("Выкл в движке • " + Math.round(thresh * 100.0f) + "%");
                tvSelectedBandThreshVal.setTextColor(getColor(R.color.text_muted));
            }
        }
        if (seekBandThresh != null) {
            seekBandThresh.setProgress(Math.round(thresh * 100.0f));
            seekBandThresh.setEnabled(threshEnabled);
            seekBandThresh.setAlpha(threshEnabled ? 1.0f : 0.4f);
        }

        if (switchBandEnabled != null) {
            boolean enabled = mSelectedBandIsWide ? mAudioAnalyzer.isWideBandEnabled(mSelectedBandIndex) : mAudioAnalyzer.isNarrowBandEnabled(mSelectedBandIndex);
            switchBandEnabled.setOnCheckedChangeListener(null);
            switchBandEnabled.setChecked(enabled);
            switchBandEnabled.setOnCheckedChangeListener((view, isChecked) -> {
                if (mSelectedBandIsWide) {
                    mAudioAnalyzer.setWideBandEnabled(mSelectedBandIndex, isChecked, MainActivity.this);
                    if (studioSpectrumVisualizer != null) {
                        studioSpectrumVisualizer.setBandEnabled(mSelectedBandIndex, true, isChecked);
                    }
                } else {
                    mAudioAnalyzer.setNarrowBandEnabled(mSelectedBandIndex, isChecked, MainActivity.this);
                    if (studioSpectrumVisualizer != null) {
                        studioSpectrumVisualizer.setBandEnabled(mSelectedBandIndex, false, isChecked);
                    }
                }
            });
        }
    }

    private static final int[] COL_PATTERN_VALUES = {
        AudioAnalyzer.PATTERN_OFF,
        AudioAnalyzer.PATTERN_TOP,
        AudioAnalyzer.PATTERN_BOTTOM,
        AudioAnalyzer.PATTERN_LEFT,
        AudioAnalyzer.PATTERN_RIGHT,
        AudioAnalyzer.PATTERN_TOP_BOTTOM,
        AudioAnalyzer.PATTERN_LEFT_RIGHT,
        AudioAnalyzer.PATTERN_TOP_LEFT,
        AudioAnalyzer.PATTERN_TOP_RIGHT,
        AudioAnalyzer.PATTERN_BOTTOM_LEFT,
        AudioAnalyzer.PATTERN_BOTTOM_RIGHT,
        AudioAnalyzer.PATTERN_ALL
    };

    private void selectBandPattern(int patternIndex) {
        if (mAudioAnalyzer == null) return;
        if (mSelectedBandIsWide) {
            mAudioAnalyzer.setWidePattern(mSelectedBandIndex, patternIndex);
        } else {
            mAudioAnalyzer.setNarrowPattern(mSelectedBandIndex, patternIndex);
        }
        mAudioAnalyzer.saveSettings(this);
        updateBandPatternButtonsUI();

        int mask = AudioAnalyzer.getPatternLedMask(patternIndex);
        int curColor = GlyphColorManager.getUnifiedColor(this);
        if (glyphVectorView != null) {
            glyphVectorView.setSegmentIntensity(mask, 1.0f, curColor);
            mainHandler.postDelayed(() -> {
                if (glyphVectorView != null) {
                    glyphVectorView.fadeSegmentToResting(RealmeGlyphDriver.LED_ALL, 180);
                }
            }, 300);
        }
        if (mask != 0) {
            RealmeGlyphDriver.flashSegment(mask, curColor, 300);
        }
    }

    private void updateBandPatternButtonsUI() {
        if (mAudioAnalyzer == null) return;
        int activePattern;
        if (mSelectedBandIsWide) {
            activePattern = mAudioAnalyzer.getWidePattern(mSelectedBandIndex);
        } else {
            activePattern = mAudioAnalyzer.getNarrowPattern(mSelectedBandIndex);
        }

        for (int i = 0; i < mColPatternButtons.length; i++) {
            if (mColPatternButtons[i] != null) {
                int pVal = COL_PATTERN_VALUES[i];
                updatePill(mColPatternButtons[i], pVal == activePattern);
            }
        }
    }

    private static final String[] QUICK_PRESET_NAMES = {
        "1. Классический сплит • Саб: низ, Кик: верх, Снейр: бока, Вч: круг",
        "2. Вращение по кругу • Низ -> Лево -> Верх -> Право",
        "3. Центральный бас • Саб: все, Кик: верх/низ, Снейр: бока",
        "4. Только кик и бас • Саб и Кик: все глифы, середина и верх: выкл",
        "5. Стерео-боковины • Саб: бока, Кик: верх/низ, Снейр: лево, Вч: право",
        "6. Вертикальный пульс • Саб: низ, Кик: верх, Снейр: верх/низ, Вч: все",
        "7. Диагональный крест • Низ-лево, Верх-право, Низ-право, Верх-лево",
        "8. Клубный драйв • Саб: низ, Кик: бока, Снейр: верх/низ, Вч: все",
        "9. Полная аура • Все глифы синхронно на каждый бит",
        "10. Минимал низ • Только нижний глиф на тяжелый саб и кик",
        "11. Вокал и верх • Кик: низ, Снейр: верх, Высокие: верх и бока",
        "12. Волна по часовой • Низ -> Право -> Верх -> Лево"
    };

    private void selectQuickTriggerPreset(int presetIndex) {
        if (mAudioAnalyzer == null) return;
        mAudioAnalyzer.applyQuickTriggerPreset(presetIndex);
        mAudioAnalyzer.saveSettings(this);
        renderQuickTriggersList();

        int curColor = GlyphColorManager.getUnifiedColor(this);
        if (glyphVectorView != null) {
            glyphVectorView.setSegmentIntensity(RealmeGlyphDriver.LED_ALL, 1.0f, curColor);
            mainHandler.postDelayed(() -> {
                if (glyphVectorView != null) {
                    glyphVectorView.fadeSegmentToResting(RealmeGlyphDriver.LED_ALL, 180);
                }
            }, 300);
        }
        RealmeGlyphDriver.flashSegment(RealmeGlyphDriver.LED_ALL, curColor, 300);
    }

    private void renderQuickTriggersList() {
        if (layoutQuickTriggersList == null || mAudioAnalyzer == null) return;
        int activeQuick = mAudioAnalyzer.getQuickTriggerPreset();
        float dp = getResources().getDisplayMetrics().density;

        if (layoutQuickTriggersList.getChildCount() != QUICK_PRESET_NAMES.length) {
            layoutQuickTriggersList.removeAllViews();
            for (int i = 0; i < QUICK_PRESET_NAMES.length; i++) {
                final int pIndex = i;
                TextView item = new TextView(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        (int) (38 * dp)
                );
                lp.setMargins(0, 0, 0, (int) (6 * dp));
                item.setLayoutParams(lp);
                item.setGravity(android.view.Gravity.CENTER_VERTICAL);
                item.setPadding((int) (14 * dp), 0, (int) (14 * dp), 0);
                item.setText(QUICK_PRESET_NAMES[i]);
                item.setTextSize(12);
                item.setTypeface(null, android.graphics.Typeface.BOLD);
                item.setClickable(true);
                item.setFocusable(true);

                item.setOnClickListener(v -> selectQuickTriggerPreset(pIndex));
                layoutQuickTriggersList.addView(item);
            }
        }

        for (int i = 0; i < layoutQuickTriggersList.getChildCount(); i++) {
            View v = layoutQuickTriggersList.getChildAt(i);
            if (v instanceof TextView) {
                TextView item = (TextView) v;
                boolean isActive = (i == activeQuick);
                updatePill(item, isActive);
            }
        }
    }

    private void updateStudioPatternsUI() {
        renderQuickTriggersList();
        updateBandPatternButtonsUI();
    }

    private void saveTargetColor(int color) {
        PulseLightManager.setMusicColor(this, color);
        PulseLightManager.setMusicFlickerColor(this, color);
        PulseLightManager.setAlwaysOnColor(this, color);
        GlyphColorManager.setUnifiedColor(this, color);
    }

    private void applyTargetColor(int color) {
        if (colorSliderPicker != null) {
            colorSliderPicker.setColor(color);
        }
        if (glyphVectorView != null) {
            glyphVectorView.setPreviewColor(color);
        }
        if (studioSpectrumVisualizer != null) {
            studioSpectrumVisualizer.setBarColor(color);
        }
        RealmeGlyphDriver.flashSegment(RealmeGlyphDriver.LED_ALL, color, 600);
        saveTargetColor(color);
    }

    private void syncColorTargetUI() {
        int color = GlyphColorManager.getUnifiedColor(this);
        if (colorSliderPicker != null) {
            colorSliderPicker.setColor(color);
        }
        if (glyphVectorView != null) {
            glyphVectorView.updateColorsFromManager();
        }
        if (studioSpectrumVisualizer != null) {
            studioSpectrumVisualizer.setBarColor(color);
        }
    }

    private void updatePill(TextView tv, boolean active) {
        if (tv == null) return;
        if (active) {
            int accent = ThemeManager.getAccentColor(this);
            tv.setBackground(ThemeManager.createPillDrawable(accent, 999, this));
            tv.setTextColor(ThemeManager.getContrastTextColor(accent));
        } else {
            tv.setBackgroundResource(R.drawable.bg_pill_dark);
            tv.setTextColor(getColor(R.color.text_secondary));
        }
    }

    private void setupAudioEngineSwitch() {
        switchAudioEngine = findViewById(R.id.switch_audio_engine);
        if (switchAudioEngine != null) {
            switchAudioEngine.setOnCheckedChangeListener((view, isChecked) -> {
                if (mIsUpdatingEngineUI) return;

                if (isChecked) {
                    if (PulseAudioService.hasProjectionData()) {
                        PulseAudioService.startEngine(this);
                        if (tvEngineStatusDesc != null) {
                            tvEngineStatusDesc.setText("Аудио-движок активен • Системный звук");
                            tvEngineStatusDesc.setTextColor(ThemeManager.getAccentColor(this));
                        }
                        Toast.makeText(this, "Аудио-движок запущен", Toast.LENGTH_SHORT).show();
                    } else {
                        android.media.projection.MediaProjectionManager mpm =
                                (android.media.projection.MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                        if (mpm != null) {
                            try {
                                startActivityForResult(mpm.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
                            } catch (Throwable t) {
                                Log.e(TAG, "MediaProjection intent failed: " + t);
                                PulseAudioService.startEngine(this);
                                if (tvEngineStatusDesc != null) {
                                    tvEngineStatusDesc.setText("Аудио-движок активен • Системный звук");
                                    tvEngineStatusDesc.setTextColor(ThemeManager.getAccentColor(this));
                                }
                            }
                        } else {
                            PulseAudioService.startEngine(this);
                        }
                    }
                } else {
                    PulseAudioService.stopEngine(this);
                    if (tvEngineStatusDesc != null) {
                        tvEngineStatusDesc.setText("Аудио-движок выключен");
                        tvEngineStatusDesc.setTextColor(getColor(R.color.text_muted));
                    }
                    if (engineSpectrumVisualizer != null && mAudioAnalyzer != null) {
                        engineSpectrumVisualizer.updateData(mAudioAnalyzer.getEmptyResult());
                    }
                    if (studioSpectrumVisualizer != null && mAudioAnalyzer != null) {
                        studioSpectrumVisualizer.updateData(mAudioAnalyzer.getEmptyResult());
                    }
                    RealmeGlyphDriver.turnOff();
                    Toast.makeText(this, "Аудио-движок остановлен", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // =========================================================================
    // PAGE 3: AUDIO ENGINE (Local Music Tracking & Beat Detection)
    // =========================================================================
    private void setupEnginePage() {
        engineSpectrumVisualizer = findViewById(R.id.engine_spectrum_visualizer);
        if (engineSpectrumVisualizer != null && mAudioAnalyzer != null) {
            engineSpectrumVisualizer.setSpectrumMode(mAudioAnalyzer.getSpectrumMode());
            engineSpectrumVisualizer.setBarColor(GlyphColorManager.getUnifiedColor(this));
        }

        paletteBgColor = findViewById(R.id.palette_bg_color);
        paletteAccentColor = findViewById(R.id.palette_accent_color);
        setupThemeControls();

        tvSensitivityValue = findViewById(R.id.tv_sensitivity_value);
        seekSensitivity = findViewById(R.id.seek_sensitivity);
        tvDecayValue = findViewById(R.id.tv_decay_value);
        seekDecay = findViewById(R.id.seek_decay);


        // Sliders
        if (seekSensitivity != null) {
            seekSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && mAudioAnalyzer != null) {
                        float s = 0.80f + (progress / 100.0f) * 1.40f;
                        mAudioAnalyzer.setSensitivity(s);
                        mAudioAnalyzer.saveSettings(MainActivity.this);
                        if (tvSensitivityValue != null) {
                            tvSensitivityValue.setText(String.format(java.util.Locale.US, "%.2fx", s));
                        }
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        if (seekDecay != null) {
            seekDecay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && mAudioAnalyzer != null) {
                        int decay = 40 + progress;
                        mAudioAnalyzer.setDecayMs(decay);
                        mAudioAnalyzer.saveSettings(MainActivity.this);
                        if (tvDecayValue != null) {
                            tvDecayValue.setText(decay + " ms");
                        }
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }


        // Threshold gate switch
        switchEngineBandThreshold = findViewById(R.id.switch_engine_band_threshold);
        if (switchEngineBandThreshold != null) {
            switchEngineBandThreshold.setChecked(mAudioAnalyzer.isEnableBandThreshold());
            switchEngineBandThreshold.setOnCheckedChangeListener((view, isChecked) -> {
                mAudioAnalyzer.setEnableBandThreshold(isChecked);
                mAudioAnalyzer.saveSettings(MainActivity.this);
                AudioAnalyzer sAn = PulseAudioService.getAnalyzer();
                if (sAn != null) {
                    sAn.setEnableBandThreshold(isChecked);
                    sAn.saveSettings(MainActivity.this);
                }
                updateBandGainControls();
                Toast.makeText(MainActivity.this, isChecked ? "Минимальный порог срабатывания включен" : "Минимальный порог срабатывания выключен", Toast.LENGTH_SHORT).show();
            });
        }

        View btnInfoEngineSensitivity = findViewById(R.id.btn_info_engine_sensitivity);
        if (btnInfoEngineSensitivity != null) {
            btnInfoEngineSensitivity.setOnClickListener(v -> showInfoBubble(v, "Чувствительность удара",
                    "Общая чувствительность детектора битов ко всем ударам трека от 0.80x до 2.20x.\n\nЧем выше значение, тем активнее подсветка реагирует на более мелкие ритмические нюансы композиции."));
        }

        View btnInfoEngineDecay = findViewById(R.id.btn_info_engine_decay);
        if (btnInfoEngineDecay != null) {
            btnInfoEngineDecay.setOnClickListener(v -> showInfoBubble(v, "Время затухания вспышки",
                    "Время плавного спада яркости глифов после удара от 40 до 300 мс.\n\nКороткие значения дают резкие импульсы, длинные — мягкое аналоговое свечение."));
        }

        View btnInfoEngineThresholdToggle = findViewById(R.id.btn_info_engine_threshold_toggle);
        if (btnInfoEngineThresholdToggle != null) {
            btnInfoEngineThresholdToggle.setOnClickListener(v -> showInfoBubble(v, "Минимальный порог срабатывания",
                    "Тумблер включения и выключения пороговой фильтрации частот.\n\nПри отключении автокалибровка не трогает индивидуальные пороги, позволяя глифам откликаться даже на легкие звуки."));
        }

        updateEngineControls();
    }

    private void setupThemeControls() {
        if (paletteBgColor != null) {
            paletteBgColor.setOptions(ThemeManager.BG_OPTIONS, ThemeManager.getBackgroundColor(this));
            paletteBgColor.setOnColorSelectedListener((color, name) -> {
                ThemeManager.setBackgroundColor(MainActivity.this, color);
                applyThemeColors(color, ThemeManager.getAccentColor(MainActivity.this));
                Toast.makeText(MainActivity.this, "Фон: " + name, Toast.LENGTH_SHORT).show();
            });
        }

        if (paletteAccentColor != null) {
            paletteAccentColor.setOptions(ThemeManager.ACCENT_OPTIONS, ThemeManager.getAccentColor(this));
            paletteAccentColor.setOnColorSelectedListener((color, name) -> {
                ThemeManager.setAccentColor(MainActivity.this, color);
                applyThemeColors(ThemeManager.getBackgroundColor(MainActivity.this), color);
                Toast.makeText(MainActivity.this, "Акцент: " + name, Toast.LENGTH_SHORT).show();
            });
        }

        applyThemeColors(ThemeManager.getBackgroundColor(this), ThemeManager.getAccentColor(this));
    }

    private void applyThemeColors(int bgColor, int accentColor) {
        // 1. System status bar, nav bar & decor view
        if (getWindow() != null) {
            getWindow().getDecorView().setBackgroundColor(bgColor);
            getWindow().setStatusBarColor(bgColor);
            getWindow().setNavigationBarColor(bgColor);
        }

        // 2. Main screen surfaces
        if (layoutRoot != null) layoutRoot.setBackgroundColor(bgColor);
        if (layoutTopHeader != null) layoutTopHeader.setBackgroundColor(bgColor);
        if (layoutNavBar != null) layoutNavBar.setBackgroundColor(bgColor);
        if (pageContainer != null) pageContainer.setBackgroundColor(bgColor);
        if (pageStudio != null) pageStudio.setBackgroundColor(bgColor);
        if (pageStudioInner != null) pageStudioInner.setBackgroundColor(Color.TRANSPARENT);
        if (pageEngine != null) pageEngine.setBackgroundColor(bgColor);
        if (pageEngineInner != null) pageEngineInner.setBackgroundColor(Color.TRANSPARENT);

        // 3. Card theme calculation & application
        int cardBgColor = ThemeManager.getCardBackgroundColor(bgColor);
        int cardStrokeColor = ThemeManager.getCardStrokeColor(cardBgColor);
        applyCardTheming(layoutRoot, cardBgColor, cardStrokeColor);

        if (navTabsLayout != null) {
            navTabsLayout.setBackground(ThemeManager.createCardDrawable(cardBgColor, cardStrokeColor, 14, this));
        }
        if (layoutPresetDropdown != null) {
            layoutPresetDropdown.setBackground(ThemeManager.createCardDrawable(cardBgColor, cardStrokeColor, 14, this));
        }

        // 4. Accent elements:
        animateTabSwitch(mCurrentTab, mCurrentTab, false);

        if (mAudioAnalyzer != null) {
            int studioMode = mAudioAnalyzer.getStudioAnalysisMode();
            updatePill(btnModeFast, studioMode == AudioAnalyzer.STUDIO_MODE_FAST);
            updatePill(btnModeDeep, studioMode == AudioAnalyzer.STUDIO_MODE_DEEP);
            int specMode = mAudioAnalyzer.getSpectrumMode();
            updatePill(btnSpecNarrow, specMode == AudioAnalyzer.SPECTRUM_MODE_NARROW);
            updatePill(btnSpecWide, specMode == AudioAnalyzer.SPECTRUM_MODE_WIDE);
        }

        if (btnAutoCalibrate != null) {
            btnAutoCalibrate.setBackground(ThemeManager.createPillDrawable(accentColor, 999, this));
            btnAutoCalibrate.setTextColor(ThemeManager.getContrastTextColor(accentColor));
        }

        tintAllSeekBars(accentColor);

        if (studioSpectrumVisualizer != null) {
            studioSpectrumVisualizer.setBarColor(accentColor);
        }
        if (engineSpectrumVisualizer != null) {
            engineSpectrumVisualizer.setBarColor(accentColor);
        }

        updatePresetDropdownUI();
        updateDriverStatusBadge();
        updateAccentElements(accentColor);
        applyAccentTheming(layoutRoot, accentColor);
        if (btnRandomConfig != null) {
            btnRandomConfig.setTextColor(accentColor);
        }
        if (btnStudioAddPreset != null) {
            btnStudioAddPreset.setBackground(ThemeManager.createPillDrawable(accentColor, 999, this));
            btnStudioAddPreset.setTextColor(ThemeManager.getContrastTextColor(accentColor));
        }
        updateStudioPatternsUI();
    }

    private void applyAccentTheming(View view, int accentColor) {
        if (view == null) return;
        Object tag = view.getTag();
        if ("accent_tag".equals(tag) || "accent_text".equals(tag)) {
            if (view instanceof TextView) {
                ((TextView) view).setTextColor(accentColor);
            }
        } else if ("accent_pill".equals(tag)) {
            view.setBackground(ThemeManager.createPillDrawable(accentColor, 999, this));
            if (view instanceof TextView) {
                ((TextView) view).setTextColor(ThemeManager.getContrastTextColor(accentColor));
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyAccentTheming(vg.getChildAt(i), accentColor);
            }
        }
    }

    private void applyCardTheming(View view, int cardBgColor, int cardStrokeColor) {
        if (view == null) return;
        Object tag = view.getTag();
        if ("themed_card".equals(tag)) {
            view.setBackground(ThemeManager.createCardDrawable(cardBgColor, cardStrokeColor, 22, this));
        }
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyCardTheming(vg.getChildAt(i), cardBgColor, cardStrokeColor);
            }
        }
    }

    private void tintAllSeekBars(int accentColor) {
        SeekBar[] seekBars = new SeekBar[]{
                seekDiagramInterval, seekSpectrumGain, seekBandGain, seekBandThresh,
                seekGlyphMinTime, seekGlyphMaxTime, seekLoudnessGate,
                seekFilterFInterp, seekFilterVariation, seekSensitivity, seekDecay
        };
        for (SeekBar sb : seekBars) {
            if (sb == null) continue;
            Drawable progressDrawable = sb.getProgressDrawable();
            if (progressDrawable instanceof LayerDrawable) {
                LayerDrawable ld = (LayerDrawable) progressDrawable.mutate();
                Drawable progressItem = ld.findDrawableByLayerId(android.R.id.progress);
                if (progressItem != null) {
                    progressItem.setTint(accentColor);
                }
            } else if (progressDrawable != null) {
                progressDrawable.mutate().setTint(accentColor);
            }
            Drawable thumb = sb.getThumb();
            if (thumb != null) {
                thumb.mutate().setTint(accentColor);
            }
        }
    }

    private void updateAccentElements(int accentColor) {
        TextView[] accentViews = new TextView[]{
                tvSelectedBandGainVal, tvSelectedBandThreshVal, tvSensitivityValue,
                tvDecayValue, tvGlyphMinTimeVal, tvGlyphMaxTimeVal, tvLoudnessGateVal,
                tvFilterFInterpVal, tvFilterVariationVal, tvDiagramIntervalVal,
                tvSpectrumGainVal, tvPresetDropdownArrow
        };
        for (TextView tv : accentViews) {
            if (tv != null) {
                tv.setTextColor(accentColor);
            }
        }

        int[] infoIds = new int[]{
                R.id.btn_info_presets, R.id.btn_info_analysis_mode, R.id.btn_info_spectrum_mode,
                R.id.btn_info_diagram_interval, R.id.btn_info_spectrum_gain, R.id.btn_info_quick_triggers,
                R.id.btn_info_band_sound, R.id.btn_info_band_gain, R.id.btn_info_band_threshold,
                R.id.btn_info_band_pattern, R.id.btn_info_glyph_min_time, R.id.btn_info_glyph_max_time,
                R.id.btn_info_filter_onset, R.id.btn_info_filter_loudness, R.id.btn_info_filter_finterp,
                R.id.btn_info_filter_variation, R.id.btn_info_filter_limiter,
                R.id.btn_info_engine_sensitivity, R.id.btn_info_engine_decay,
                R.id.btn_info_engine_threshold_toggle
        };
        for (int id : infoIds) {
            TextView btn = findViewById(id);
            if (btn != null) {
                btn.setTextColor(accentColor);
            }
        }

        if (tvEngineStatusDesc != null && AudioAnalyzer.isEngineEnabled(this)) {
            tvEngineStatusDesc.setTextColor(accentColor);
        }
    }

    private void updatePresetDropdownUI() {
        if (tvPresetDropdownName == null) return;
        String activeId = AudioPresetManager.getActivePresetId(this);
        AudioPreset preset = AudioPresetManager.getPresetById(this, activeId);
        if (preset == null) {
            preset = AudioPresetManager.createStudioProPreset();
        }
        tvPresetDropdownName.setText(preset.name);
        int accent = ThemeManager.getAccentColor(this);
        if (tvPresetDropdownBadge != null) {
            tvPresetDropdownBadge.setText(preset.isBuiltIn ? "ВСТРОЕННЫЙ" : "ПОЛЬЗОВАТЕЛЬСКИЙ");
            tvPresetDropdownBadge.setTextColor(accent);
        }
        if (tvPresetDropdownArrow != null) {
            tvPresetDropdownArrow.setTextColor(accent);
        }
    }

    private AlertDialog mCurrentPresetDialog = null;

    private void showPresetDropdownDialog() {
        if (isFinishing() || isDestroyed()) return;
        if (mCurrentPresetDialog != null && mCurrentPresetDialog.isShowing()) {
            mCurrentPresetDialog.dismiss();
            mCurrentPresetDialog = null;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_preset_picker, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        int bgColor = ThemeManager.getBackgroundColor(this);
        int cardBg = ThemeManager.getCardBackgroundColor(bgColor);
        int cardStroke = ThemeManager.getCardStrokeColor(cardBg);
        int accent = ThemeManager.getAccentColor(this);
        dialogView.setBackground(ThemeManager.createCardDrawable(cardBg, cardStroke, 20, this));

        TextView tvPresetTag = dialogView.findViewById(R.id.tv_preset_dialog_tag);
        if (tvPresetTag != null) {
            tvPresetTag.setTextColor(accent);
        }

        View btnClose = dialogView.findViewById(R.id.btn_preset_dialog_close_x);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        LinearLayout listContainer = dialogView.findViewById(R.id.layout_preset_picker_list);
        populatePresetPickerList(listContainer, dialog);

        View btnAdd = dialogView.findViewById(R.id.btn_dialog_add_preset);
        if (btnAdd != null) {
            btnAdd.setBackground(ThemeManager.createPillDrawable(accent, 14, this));
            if (btnAdd instanceof TextView) {
                ((TextView) btnAdd).setTextColor(ThemeManager.getContrastTextColor(accent));
            }
            applyButtonFeedback(btnAdd);
            btnAdd.setOnClickListener(v -> {
                dialog.dismiss();
                showSavePresetDialog();
            });
        }

        View btnExport = dialogView.findViewById(R.id.btn_dialog_export_preset);
        if (btnExport != null) {
            applyButtonFeedback(btnExport);
            btnExport.setOnClickListener(v -> exportActivePresetToClipboard());
        }

        View btnImport = dialogView.findViewById(R.id.btn_dialog_import_preset);
        if (btnImport != null) {
            applyButtonFeedback(btnImport);
            btnImport.setOnClickListener(v -> {
                dialog.dismiss();
                showImportPresetDialog();
            });
        }

        mCurrentPresetDialog = dialog;
        dialog.show();
    }

    private void populatePresetPickerList(LinearLayout listContainer, AlertDialog dialog) {
        if (listContainer == null) return;
        listContainer.removeAllViews();

        List<AudioPreset> list = AudioPresetManager.getAllPresets(this);
        String activeId = AudioPresetManager.getActivePresetId(this);
        float dp = getResources().getDisplayMetrics().density;
        int bgColor = ThemeManager.getBackgroundColor(this);
        int cardBg = ThemeManager.getCardBackgroundColor(bgColor);
        int cardStroke = ThemeManager.getCardStrokeColor(cardBg);
        int accent = ThemeManager.getAccentColor(this);

        for (AudioPreset p : list) {
            boolean isActive = p.id.equals(activeId);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (int) (48 * dp));
            rowLp.setMargins(0, 0, 0, (int) (6 * dp));
            row.setLayoutParams(rowLp);
            row.setBackground(ThemeManager.createCardDrawable(cardBg, isActive ? accent : cardStroke, 12, this));
            row.setPadding((int) (14 * dp), 0, (int) (14 * dp), 0);
            row.setClickable(true);
            row.setFocusable(true);
            applyButtonFeedback(row);

            LinearLayout infoCol = new LinearLayout(this);
            infoCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            infoCol.setLayoutParams(infoLp);

            TextView tvName = new TextView(this);
            tvName.setText(p.name);
            tvName.setTextSize(13);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            tvName.setTextColor(isActive ? accent : getColor(R.color.text_white));
            infoCol.addView(tvName);

            TextView tvBadge = new TextView(this);
            tvBadge.setText(p.isBuiltIn ? "Встроенный профиль" : "Пользовательский профиль");
            tvBadge.setTextSize(10);
            tvBadge.setTextColor(getColor(R.color.text_secondary));
            infoCol.addView(tvBadge);

            row.addView(infoCol);

            if (isActive) {
                TextView tvCheck = new TextView(this);
                tvCheck.setText("✓");
                tvCheck.setTextSize(16);
                tvCheck.setTypeface(null, android.graphics.Typeface.BOLD);
                tvCheck.setTextColor(accent);
                row.addView(tvCheck);
            }

            row.setOnClickListener(v -> {
                applyAudioPreset(p);
                dialog.dismiss();
            });

            if (!p.isBuiltIn) {
                row.setOnLongClickListener(v -> {
                    showDeletePresetDialog(p);
                    return true;
                });
            }

            listContainer.addView(row);
        }
    }

    private int mSelectedCalibDurationMs = 5000;

    private void showAutoCalibrationDialog() {
        if (isFinishing() || isDestroyed()) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_calib_settings, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        int bgColor = ThemeManager.getBackgroundColor(this);
        int cardBg = ThemeManager.getCardBackgroundColor(bgColor);
        int cardStroke = ThemeManager.getCardStrokeColor(cardBg);
        int accent = ThemeManager.getAccentColor(this);
        dialogView.setBackground(ThemeManager.createCardDrawable(cardBg, cardStroke, 20, this));

        TextView tvCalibTag = dialogView.findViewById(R.id.tv_calib_dialog_tag);
        if (tvCalibTag != null) {
            tvCalibTag.setTextColor(accent);
        }

        View btnClose = dialogView.findViewById(R.id.btn_calib_dialog_close_x);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        // Duration buttons
        TextView btnDur3s = dialogView.findViewById(R.id.btn_calib_dur_3s);
        TextView btnDur5s = dialogView.findViewById(R.id.btn_calib_dur_5s);
        TextView btnDur10s = dialogView.findViewById(R.id.btn_calib_dur_10s);

        final int[] durMs = {mSelectedCalibDurationMs};

        Runnable updateDurationUI = () -> {
            updatePill(btnDur3s, durMs[0] == 3000);
            updatePill(btnDur5s, durMs[0] == 5000);
            updatePill(btnDur10s, durMs[0] == 10000);
        };
        updateDurationUI.run();

        if (btnDur3s != null) {
            btnDur3s.setOnClickListener(v -> {
                durMs[0] = 3000;
                updateDurationUI.run();
            });
        }
        if (btnDur5s != null) {
            btnDur5s.setOnClickListener(v -> {
                durMs[0] = 5000;
                updateDurationUI.run();
            });
        }
        if (btnDur10s != null) {
            btnDur10s.setOnClickListener(v -> {
                durMs[0] = 10000;
                updateDurationUI.run();
            });
        }

        // Switches
        ModernSwitch switchGains = dialogView.findViewById(R.id.switch_calib_gains);
        ModernSwitch switchThresh = dialogView.findViewById(R.id.switch_calib_thresholds);
        ModernSwitch switchSens = dialogView.findViewById(R.id.switch_calib_sens);
        ModernSwitch switchLoud = dialogView.findViewById(R.id.switch_calib_loudness);
        ModernSwitch switchDecay = dialogView.findViewById(R.id.switch_calib_decay);

        if (switchGains != null) switchGains.setChecked(true);
        if (switchThresh != null) switchThresh.setChecked(mAudioAnalyzer != null && mAudioAnalyzer.isEnableBandThreshold());
        if (switchSens != null) switchSens.setChecked(true);
        if (switchLoud != null) switchLoud.setChecked(mAudioAnalyzer != null && mAudioAnalyzer.isEnableLoudnessGate());
        if (switchDecay != null) switchDecay.setChecked(true);

        View btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel_calib);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        View btnStart = dialogView.findViewById(R.id.btn_dialog_start_calib);
        if (btnStart != null) {
            btnStart.setBackground(ThemeManager.createPillDrawable(accent, 14, this));
            if (btnStart instanceof TextView) {
                ((TextView) btnStart).setTextColor(ThemeManager.getContrastTextColor(accent));
            }
            applyButtonFeedback(btnStart);
            btnStart.setOnClickListener(v -> {
                mSelectedCalibDurationMs = durMs[0];
                boolean calibG = (switchGains == null || switchGains.isChecked());
                boolean calibT = (switchThresh == null || switchThresh.isChecked());
                boolean calibS = (switchSens == null || switchSens.isChecked());
                boolean calibL = (switchLoud == null || switchLoud.isChecked());
                boolean calibD = (switchDecay == null || switchDecay.isChecked());

                dialog.dismiss();
                startCalibrationProcess(durMs[0], calibG, calibT, calibS, calibL, calibD);
            });
        }

        dialog.show();
    }

    private void startCalibrationProcess(int durationMs, boolean calibGains, boolean calibThresholds,
                                         boolean calibSens, boolean calibLoudness, boolean calibDecay) {
        if (!PulseAudioService.isRunning()) {
            Toast.makeText(this, "Включите аудио-движок и воспроизведение музыки", Toast.LENGTH_LONG).show();
            return;
        }

        if (btnAutoCalibrate != null) {
            btnAutoCalibrate.setEnabled(false);
            btnAutoCalibrate.setText("Калибровка...");
        }

        AudioAnalyzer.CalibrationCallback cb = new AudioAnalyzer.CalibrationCallback() {
            @Override
            public void onCalibrationProgress(int secondsRemaining) {
                runOnUiThread(() -> {
                    if (btnAutoCalibrate != null) {
                        btnAutoCalibrate.setText("Замер: " + secondsRemaining + "с");
                    }
                });
            }

            @Override
            public void onCalibrationComplete() {
                runOnUiThread(() -> {
                    if (mAudioAnalyzer != null) {
                        mAudioAnalyzer.loadSettings(MainActivity.this);
                    }
                    updateStudioSpectrumUI();
                    updateBandGainControls();
                    updateBandPatternButtonsUI();
                    updateEngineControls();
                    if (btnAutoCalibrate != null) {
                        btnAutoCalibrate.setEnabled(true);
                        btnAutoCalibrate.setText("Автокалибровка");
                    }
                    Toast.makeText(MainActivity.this, "Автокалибровка успешно завершена", Toast.LENGTH_SHORT).show();
                });
            }
        };

        PulseAudioService.startAutoCalibration(durationMs, calibGains, calibThresholds, calibSens, calibLoudness, calibDecay, cb);
    }

    private void applyAudioPreset(AudioPreset preset) {
        if (preset == null) return;
        mAudioAnalyzer.applyPreset(preset, this);
        if (studioSpectrumVisualizer != null) {
            if (preset.narrowEnabled != null) {
                for (int i = 0; i < Math.min(4, preset.narrowEnabled.length); i++) {
                    studioSpectrumVisualizer.setBandEnabled(i, false, preset.narrowEnabled[i]);
                }
            }
            if (preset.wideEnabled != null) {
                for (int i = 0; i < Math.min(12, preset.wideEnabled.length); i++) {
                    studioSpectrumVisualizer.setBandEnabled(i, true, preset.wideEnabled[i]);
                }
            }
        }
        if (engineSpectrumVisualizer != null) {
            engineSpectrumVisualizer.setSpectrumMode(mAudioAnalyzer.getSpectrumMode());
            engineSpectrumVisualizer.setBarColor(GlyphColorManager.getUnifiedColor(this));
        }
        updateStudioSpectrumUI();
        updateStudioPatternsUI();
        updateEngineControls();
        updatePresetDropdownUI();
        Toast.makeText(this, "Применен пресет: " + preset.name, Toast.LENGTH_SHORT).show();
    }

    private void updateAllPresetsUI() {
        updatePresetDropdownUI();
    }

    private void showSavePresetDialog() {
        final EditText input = new EditText(this);
        input.setHint("Название пресета");
        input.setTextColor(getColor(R.color.text_white));
        input.setHintTextColor(getColor(R.color.text_muted));
        input.setBackgroundResource(R.drawable.bg_dialog_input);
        int pad = (int) (12 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        FrameLayout container = new FrameLayout(this);
        int margin = (int) (16 * getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(margin, margin / 2, margin, margin / 2);
        input.setLayoutParams(lp);
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("Сохранить текущие настройки")
                .setMessage("Будет создан пресет с текущими параметрами эквалайзера, чувствительности, фильтров и паттернов.")
                .setView(container)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        name = "Пресет " + (AudioPresetManager.getUserPresets(this).size() + 1);
                    }
                    String id = "user_" + System.currentTimeMillis();
                    AudioPreset newPreset = mAudioAnalyzer.exportCurrentAsPreset(id, name);
                    AudioPresetManager.saveUserPreset(this, newPreset);
                    applyAudioPreset(newPreset);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showDeletePresetDialog(AudioPreset preset) {
        new AlertDialog.Builder(this)
                .setTitle("Удалить пресет?")
                .setMessage("Удалить пользовательский пресет «" + preset.name + "»?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    AudioPresetManager.deleteUserPreset(this, preset.id);
                    if (preset.id.equals(AudioPresetManager.getActivePresetId(this))) {
                        AudioPreset def = AudioPresetManager.createStudioProPreset();
                        applyAudioPreset(def);
                    } else {
                        updateAllPresetsUI();
                    }
                    Toast.makeText(this, "Пресет удален", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void exportActivePresetToClipboard() {
        if (mAudioAnalyzer == null) return;
        String activeId = AudioPresetManager.getActivePresetId(this);
        AudioPreset preset = AudioPresetManager.getPresetById(this, activeId);
        if (preset == null) {
            preset = mAudioAnalyzer.exportCurrentAsPreset("custom_" + System.currentTimeMillis(), "Текущий пресет");
        } else if (!preset.isBuiltIn) {
            preset = mAudioAnalyzer.exportCurrentAsPreset(preset.id, preset.name);
        }
        String json = AudioPresetManager.exportPresetToJson(preset);
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("PulseLight Preset", json);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Пресет «" + preset.name + "» скопирован в буфер", Toast.LENGTH_SHORT).show();
        }
    }

    private void showImportPresetDialog() {
        String clipText = "";
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
                CharSequence text = clipboard.getPrimaryClip().getItemAt(0).getText();
                if (text != null && text.toString().trim().startsWith("{")) {
                    clipText = text.toString().trim();
                }
            }
        } catch (Throwable ignored) {}

        final EditText et = new EditText(this);
        et.setHint("Вставьте JSON код пресета");
        et.setText(clipText);
        et.setTextColor(getColor(R.color.text_white));
        et.setHintTextColor(getColor(R.color.text_muted));
        et.setTextSize(12);
        et.setMinLines(4);
        et.setMaxLines(10);
        et.setGravity(Gravity.TOP | Gravity.START);
        et.setBackgroundResource(R.drawable.bg_dialog_input);
        int pad = (int) (12 * getResources().getDisplayMetrics().density);
        et.setPadding(pad, pad, pad, pad);

        FrameLayout container = new FrameLayout(this);
        int margin = (int) (16 * getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(margin, margin / 2, margin, margin / 2);
        et.setLayoutParams(lp);
        container.addView(et);

        new AlertDialog.Builder(this)
                .setTitle("Импорт пресета")
                .setMessage("Вставьте JSON конфигурацию пресета:")
                .setView(container)
                .setPositiveButton("Импортировать", (dialog, which) -> {
                    String input = et.getText().toString().trim();
                    if (input.isEmpty()) {
                        Toast.makeText(MainActivity.this, "Поле пустое", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    AudioPreset imported = AudioPresetManager.importPresetFromJson(MainActivity.this, input);
                    if (imported != null) {
                        applyAudioPreset(imported);
                        Toast.makeText(MainActivity.this, "Пресет «" + imported.name + "» импортирован", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Ошибка: некорректный JSON формат", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void updateEngineControls() {
        if (mAudioAnalyzer == null) return;
        mAudioAnalyzer.loadSettings(this);

        boolean isEngineOn = AudioAnalyzer.isEngineEnabled(this);
        mIsUpdatingEngineUI = true;
        if (switchAudioEngine != null) {
            switchAudioEngine.setChecked(isEngineOn);
        }
        mIsUpdatingEngineUI = false;

        if (tvEngineStatusDesc != null) {
            if (isEngineOn) {
                tvEngineStatusDesc.setText("Аудио-движок активен • Системный звук");
                tvEngineStatusDesc.setTextColor(ThemeManager.getAccentColor(this));
            } else {
                tvEngineStatusDesc.setText("Аудио-движок выключен • Нажмите для активации");
                tvEngineStatusDesc.setTextColor(getColor(R.color.text_muted));
            }
        }

        if (engineSpectrumVisualizer != null) {
            engineSpectrumVisualizer.setSpectrumMode(mAudioAnalyzer.getSpectrumMode());
            engineSpectrumVisualizer.setBarColor(ThemeManager.getAccentColor(this));
        }

        updatePresetDropdownUI();

        // Update Sliders
        float sens = mAudioAnalyzer.getSensitivity();
        int sensProgress = Math.round(((sens - 0.80f) / 1.40f) * 100.0f);
        if (seekSensitivity != null) {
            seekSensitivity.setProgress(Math.max(0, Math.min(100, sensProgress)));
        }
        if (tvSensitivityValue != null) {
            tvSensitivityValue.setText(String.format(java.util.Locale.US, "%.2fx", sens));
        }

        int decay = mAudioAnalyzer.getDecayMs();
        int decayProgress = decay - 40;
        if (seekDecay != null) {
            seekDecay.setProgress(Math.max(0, Math.min(260, decayProgress)));
        }
        if (tvDecayValue != null) {
            tvDecayValue.setText(decay + " ms");
        }


        if (switchEngineBandThreshold != null) {
            switchEngineBandThreshold.setChecked(mAudioAnalyzer.isEnableBandThreshold());
        }

        if (seekLoudnessGate != null) {
            float lg = mAudioAnalyzer.getLoudnessGateThreshold();
            seekLoudnessGate.setProgress(Math.round(lg * 100.0f));
            if (tvLoudnessGateVal != null) {
                tvLoudnessGateVal.setText(Math.round(lg * 100.0f) + "%");
            }
        }
        if (switchFilterLoudness != null) {
            switchFilterLoudness.setChecked(mAudioAnalyzer.isEnableLoudnessGate());
            if (containerLoudnessSlider != null) {
                containerLoudnessSlider.setVisibility(mAudioAnalyzer.isEnableLoudnessGate() ? View.VISIBLE : View.GONE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK && data != null) {
                PulseAudioService.startSystemAudioEngine(this, resultCode, data);
                mIsUpdatingEngineUI = true;
                if (switchAudioEngine != null) {
                    switchAudioEngine.setChecked(true);
                }
                mIsUpdatingEngineUI = false;
                if (tvEngineStatusDesc != null) {
                    tvEngineStatusDesc.setText("Аудио-движок активен • Системный звук");
                    tvEngineStatusDesc.setTextColor(ThemeManager.getAccentColor(this));
                }
                Toast.makeText(this, "Захват системного звука активирован", Toast.LENGTH_SHORT).show();
            } else {
                mIsUpdatingEngineUI = true;
                if (switchAudioEngine != null) {
                    switchAudioEngine.setChecked(false);
                }
                mIsUpdatingEngineUI = false;
                AudioAnalyzer.setEngineEnabled(this, false);
                Toast.makeText(this, "Требуется разрешение для захвата звука", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

