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

    private TextView tvPermStatus;
    private ListView appListView;
    private ProgressBar loadingProgress;
    private TextView emptyView;

    // Header Views
    private View headerView;
    private View heroCard;
    private TextView tvStats;
    private TextView btnReset;
    private EditText etSearch;
    private TextView tabAll;
    private TextView tabActive;

    // Preset Chips
    private TextView chipYandex;
    private TextView chipSpotify;
    private TextView chipVk;
    private TextView chipYt;
    private TextView chipSound;
    private TextView chipPoweramp;

    private AppAdapter adapter;
    private final List<AppItem> allAppItems = new ArrayList<>();
    private final ExecutorService scanExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        enforceHighRefreshRate();
        initViews();
        setupListeners();
        checkPermission();
        updateStatsAndPresets();

        // Delay background scan slightly so the window renders its first frame at 120Hz with 0ms freeze!
        mainHandler.postDelayed(this::loadApplications, 100);
    }

    private void enforceHighRefreshRate() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                android.view.Display display = getDisplay();
                if (display != null) {
                    android.view.Display.Mode[] modes = display.getSupportedModes();
                    android.view.Display.Mode bestMode = null;
                    float highestRate = 60.0f;
                    for (android.view.Display.Mode m : modes) {
                        if (m.getRefreshRate() > highestRate) {
                            highestRate = m.getRefreshRate();
                            bestMode = m;
                        }
                    }
                    if (bestMode != null) {
                        android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
                        params.preferredDisplayModeId = bestMode.getModeId();
                        params.preferredRefreshRate = bestMode.getRefreshRate();
                        getWindow().setAttributes(params);
                        Log.d(TAG, "Enforced display mode " + bestMode.getModeId() + " @ " + bestMode.getRefreshRate() + "Hz");
                    }
                }
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
                params.preferredRefreshRate = 120.0f;
                getWindow().setAttributes(params);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Could not set preferred refresh rate", t);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        enforceHighRefreshRate();
        checkPermission();
        updateStatsAndPresets();
    }

    private void initViews() {
        tvPermStatus = findViewById(R.id.tv_perm_status);
        appListView = findViewById(R.id.app_list);
        loadingProgress = findViewById(R.id.loading_progress);
        emptyView = findViewById(R.id.empty_view);

        // Inflate scrollable header
        headerView = getLayoutInflater().inflate(R.layout.header_main, appListView, false);
        heroCard = headerView.findViewById(R.id.hero_card);
        tvStats = headerView.findViewById(R.id.tv_stats);
        btnReset = headerView.findViewById(R.id.btn_reset);
        etSearch = headerView.findViewById(R.id.et_search);
        tabAll = headerView.findViewById(R.id.tab_all);
        tabActive = headerView.findViewById(R.id.tab_active);

        chipYandex = headerView.findViewById(R.id.chip_preset_yandex);
        chipSpotify = headerView.findViewById(R.id.chip_preset_spotify);
        chipVk = headerView.findViewById(R.id.chip_preset_vk);
        chipYt = headerView.findViewById(R.id.chip_preset_yt);
        chipSound = headerView.findViewById(R.id.chip_preset_sound);
        chipPoweramp = headerView.findViewById(R.id.chip_preset_poweramp);

        // Add header to ListView so the entire screen scrolls as one fluid list
        appListView.addHeaderView(headerView, null, false);
    }

    private void setupListeners() {
        tvPermStatus.setOnClickListener(v -> {
            if (!PulseLightManager.hasPermission(this)) {
                showPermissionDialog();
            } else {
                Toast.makeText(this, "Доступ WRITE_SECURE_SETTINGS активен ✓", Toast.LENGTH_SHORT).show();
            }
        });

        heroCard.setOnClickListener(v -> {
            int active = PulseLightManager.getActiveCount(this);
            Toast.makeText(this, "Pulse Light активен для " + active + " плееров. Диод синхронизируется при воспроизведении!", Toast.LENGTH_LONG).show();
        });

        btnReset.setOnClickListener(v -> showResetDialog());

        appListView.setOnScrollListener(new android.widget.AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(android.widget.AbsListView view, int scrollState) {
                if (adapter != null) {
                    adapter.setFlinging(scrollState == SCROLL_STATE_FLING);
                }
            }

            @Override
            public void onScroll(android.widget.AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {}
        });

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
                if (adapter != null) {
                    adapter.filterQuery(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        setupPresetChip(chipYandex, "ru.yandex.music");
        setupPresetChip(chipSpotify, "com.spotify.music");
        setupPresetChip(chipVk, "com.vkontakte.android");
        setupPresetChip(chipYt, "com.google.android.apps.youtube.music");
        setupPresetChip(chipSound, "ru.sberbank.sberzvuk");
        setupPresetChip(chipPoweramp, "com.maxmpz.audioplayer");
    }

    private void setupPresetChip(TextView chip, String packageName) {
        chip.setOnClickListener(v -> {
            boolean currentState = PulseLightManager.isAppEnabled(this, packageName);
            boolean newState = !currentState;
            PulseLightManager.setAppEnabled(this, packageName, newState);

            // Update in-memory list
            for (AppItem item : allAppItems) {
                if (item.getPackageName().equals(packageName)) {
                    item.setEnabled(newState);
                    break;
                }
            }
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            updateStatsAndPresets();
            String msg = (newState ? "Включено: " : "Отключено: ") + chip.getText();
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
    }

    private void checkPermission() {
        boolean granted = PulseLightManager.hasPermission(this);
        if (granted) {
            tvPermStatus.setText("ACTIVE");
            tvPermStatus.setBackgroundResource(R.drawable.bg_badge_active);
            tvPermStatus.setTextColor(getColor(R.color.text_black));
        } else {
            tvPermStatus.setText("NO PERM");
            tvPermStatus.setBackgroundResource(R.drawable.bg_badge_inactive);
            tvPermStatus.setTextColor(getColor(R.color.status_red));
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

                // Sort: Enabled apps first, then alphabetical
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

                    // Background sequential icon preloader: once all icons are in RAM, scrolling is 100% locked 120 FPS
                    scanExecutor.execute(() -> {
                        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                        PackageManager packageManager = getPackageManager();
                        for (AppItem appItem : allAppItems) {
                            if (appItem.getIcon() == null) {
                                appItem.loadIconSync(packageManager);
                            }
                        }
                        runOnUiThread(() -> {
                            if (adapter != null) {
                                adapter.notifyDataSetChanged();
                            }
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
        tvStats.setText(activeCount + " активных плееров в подсветке");

        tabAll.setText("Все (" + allAppItems.size() + ")");
        tabActive.setText("Только активные (" + activeCount + ")");

        updatePresetChipView(chipYandex, "ru.yandex.music");
        updatePresetChipView(chipSpotify, "com.spotify.music");
        updatePresetChipView(chipVk, "com.vkontakte.android");
        updatePresetChipView(chipYt, "com.google.android.apps.youtube.music");
        updatePresetChipView(chipSound, "ru.sberbank.sberzvuk");
        updatePresetChipView(chipPoweramp, "com.maxmpz.audioplayer");
    }

    private void updatePresetChipView(TextView chip, String packageName) {
        if (chip == null) return;
        boolean enabled = PulseLightManager.isAppEnabled(this, packageName);
        if (enabled) {
            chip.setBackgroundResource(R.drawable.bg_pill_lime);
            chip.setTextColor(getColor(R.color.text_black));
        } else {
            chip.setBackgroundResource(R.drawable.bg_pill_dark);
            chip.setTextColor(getColor(R.color.text_white));
        }
    }
}
