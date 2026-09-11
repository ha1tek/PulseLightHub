package com.antigravity.pulselight;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class AppAdapter extends BaseAdapter implements Filterable {

    public interface OnAppToggleListener {
        void onAppToggled(AppItem item, boolean isEnabled);
    }

    public interface OnFilterResultListener {
        void onFilterResult(int totalCount, int filteredCount);
    }

    private final Context context;
    private final PackageManager pm;
    private final List<AppItem> originalList;
    private final List<AppItem> filteredList;
    private final LayoutInflater inflater;
    private final Handler mainHandler;
    private final ExecutorService iconExecutor;
    private final OnAppToggleListener toggleListener;
    private OnFilterResultListener filterResultListener;
    private String currentQuery = "";
    private boolean onlyActive = false;
    private boolean isFlinging = false;

    public AppAdapter(Context context, List<AppItem> list, OnAppToggleListener toggleListener) {
        this.context = context;
        this.pm = context.getPackageManager();
        this.originalList = new ArrayList<>(list);
        this.filteredList = new ArrayList<>(list);
        this.inflater = LayoutInflater.from(context);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.toggleListener = toggleListener;

        // Background thread with low priority to prevent any UI thread contention
        this.iconExecutor = Executors.newFixedThreadPool(2, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(() -> {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    r.run();
                });
                t.setDaemon(true);
                return t;
            }
        });
    }

    public void setFlinging(boolean flinging) {
        if (this.isFlinging != flinging) {
            this.isFlinging = flinging;
            if (!flinging) {
                notifyDataSetChanged();
            }
        }
    }

    public void setOnFilterResultListener(OnFilterResultListener listener) {
        this.filterResultListener = listener;
    }

    public void updateData(List<AppItem> newList) {
        originalList.clear();
        originalList.addAll(newList);
        applyFilters();
    }

    public void setFilterMode(boolean onlyActive) {
        this.onlyActive = onlyActive;
        applyFilters();
    }

    public void filterQuery(String query) {
        this.currentQuery = (query == null) ? "" : query.trim().toLowerCase(Locale.getDefault());
        applyFilters();
    }

    public int getActiveCount() {
        int count = 0;
        for (AppItem item : originalList) {
            if (item.isEnabled()) count++;
        }
        return count;
    }

    public int getTotalCount() {
        return originalList.size();
    }

    private void applyFilters() {
        filteredList.clear();
        for (AppItem item : originalList) {
            if (onlyActive && !item.isEnabled()) {
                continue;
            }
            if (!currentQuery.isEmpty()) {
                boolean matchesName = item.getAppName().toLowerCase(Locale.getDefault()).contains(currentQuery);
                boolean matchesPkg = item.getPackageName().toLowerCase(Locale.getDefault()).contains(currentQuery);
                if (!matchesName && !matchesPkg) {
                    continue;
                }
            }
            filteredList.add(item);
        }
        notifyDataSetChanged();
        if (filterResultListener != null) {
            filterResultListener.onFilterResult(originalList.size(), filteredList.size());
        }
    }

    @Override
    public int getCount() {
        return filteredList.size();
    }

    @Override
    public AppItem getItem(int position) {
        if (position >= 0 && position < filteredList.size()) {
            return filteredList.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_app, parent, false);
            holder = new ViewHolder();
            holder.cardContainer = convertView.findViewById(R.id.card_container);
            holder.appIcon = convertView.findViewById(R.id.app_icon);
            holder.appName = convertView.findViewById(R.id.app_name);
            holder.appPackage = convertView.findViewById(R.id.app_package);
            holder.appSwitch = convertView.findViewById(R.id.app_switch);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final AppItem item = getItem(position);
        if (item != null) {
            int bgColor = ThemeManager.getBackgroundColor(context);
            int cardBgColor = ThemeManager.getCardBackgroundColor(bgColor);
            int cardStrokeColor = ThemeManager.getCardStrokeColor(cardBgColor);
            holder.cardContainer.setBackground(ThemeManager.createCardDrawable(cardBgColor, cardStrokeColor, 16, context));

            holder.appName.setText(item.getAppName());
            holder.appPackage.setText(item.getPackageName());

            // Instant state without animation during scroll/bind
            holder.appSwitch.setChecked(item.isEnabled(), false);

            // Handle card clicks with smooth 200ms vector animation
            holder.cardContainer.setOnClickListener(v -> {
                boolean newState = !item.isEnabled();
                item.setEnabled(newState);
                holder.appSwitch.setChecked(newState, true);
                PulseLightManager.setAppEnabled(context, item.getPackageName(), newState);
                if (toggleListener != null) {
                    toggleListener.onAppToggled(item, newState);
                }
            });

            // Lazy icon loading
            Drawable icon = item.getIcon();
            if (icon != null) {
                holder.appIcon.setImageDrawable(icon);
            } else {
                holder.appIcon.setImageResource(R.drawable.ic_app_placeholder);
                if (!isFlinging) {
                    final String pkg = item.getPackageName();
                    holder.appIcon.setTag(pkg);

                    iconExecutor.execute(() -> {
                        Drawable loadedIcon = item.loadIconSync(pm);
                        if (loadedIcon != null) {
                            mainHandler.post(() -> {
                                if (pkg.equals(holder.appIcon.getTag())) {
                                    holder.appIcon.setImageDrawable(loadedIcon);
                                }
                            });
                        }
                    });
                }
            }
        }

        return convertView;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                return new FilterResults();
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filterQuery(constraint == null ? "" : constraint.toString());
            }
        };
    }

    private static class ViewHolder {
        View cardContainer;
        ImageView appIcon;
        TextView appName;
        TextView appPackage;
        ModernSwitch appSwitch;
    }
}
