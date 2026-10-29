package com.nison.freedeck.data.apps;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import com.nison.freedeck.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Finds the user-installed apps that can be launched. */
public class AppRepository {

    private static final int ACCENT_SAMPLE_PX = 32;

    private final Context context;
    private final PackageManager packageManager;

    public AppRepository(Context context) {
        this.context = com.nison.freedeck.ui.common.AppContext.wrap(context.getApplicationContext());
        this.packageManager = this.context.getPackageManager();
    }

    /** Blocking (it loads every icon); call from a background thread. Sorted by name. */
    public List<InstalledApp> load() {
        Map<String, Candidate> candidates = new LinkedHashMap<>();
        // TV entry points first, so an app with both keeps its TV banner.
        collect(candidates, Intent.CATEGORY_LEANBACK_LAUNCHER);
        collect(candidates, Intent.CATEGORY_LAUNCHER);

        List<InstalledApp> apps = new ArrayList<>(candidates.size());
        for (Candidate candidate : candidates.values()) {
            apps.add(toApp(candidate));
        }
        Collections.sort(apps, (a, b) -> a.label.compareToIgnoreCase(b.label));
        return apps;
    }

    @SuppressWarnings("deprecation") // The flags overload that replaces this needs API 33; minSdk is 26.
    private void collect(Map<String, Candidate> into, String category) {
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
        for (ResolveInfo info : packageManager.queryIntentActivities(query, 0)) {
            ActivityInfo activity = info.activityInfo;
            if (AppFilter.shouldShow(activity.applicationInfo.flags, activity.packageName, context.getPackageName())) {
                into.putIfAbsent(activity.packageName, new Candidate(info, category));
            }
        }
    }

    private InstalledApp toApp(Candidate candidate) {
        ActivityInfo activity = candidate.info.activityInfo;
        Drawable icon = activity.loadIcon(packageManager);
        Drawable banner = activity.loadBanner(packageManager);
        Intent launch = new Intent(Intent.ACTION_MAIN)
                .addCategory(candidate.category)
                .setComponent(new ComponentName(activity.packageName, activity.name))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        ApplicationInfo application = activity.applicationInfo;
        PackageInfo packageInfo = packageInfoOf(activity.packageName);
        CharSequence category = ApplicationInfo.getCategoryTitle(context, application.category);
        String label = candidate.info.loadLabel(packageManager).toString();
        AppOverrides.Override override = AppOverrides.forPackage(activity.packageName);
        if (override != null) {
            label = context.getString(override.label);
            banner = context.getDrawable(override.banner);
        }
        return new InstalledApp(
                activity.packageName,
                label,
                banner != null ? banner : icon,
                banner != null,
                DominantColor.fromPixels(samplePixels(icon), context.getColor(R.color.glow_default)),
                launch,
                AppDescription.clean(application.loadDescription(packageManager)),
                AppDescription.clean(category),
                packageInfo != null ? AppDescription.clean(packageInfo.versionName) : null,
                packageInfo != null ? packageInfo.firstInstallTime : 0L);
    }

    private PackageInfo packageInfoOf(String packageName) {
        try {
            return packageManager.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /** Draws a copy of the drawable small, so its colours can be read without disturbing the original. */
    private static int[] samplePixels(Drawable drawable) {
        Drawable.ConstantState state = drawable.getConstantState();
        Drawable copy = state != null ? state.newDrawable().mutate() : drawable;
        Bitmap bitmap = Bitmap.createBitmap(ACCENT_SAMPLE_PX, ACCENT_SAMPLE_PX, Bitmap.Config.ARGB_8888);
        copy.setBounds(0, 0, ACCENT_SAMPLE_PX, ACCENT_SAMPLE_PX);
        copy.draw(new Canvas(bitmap));
        int[] pixels = new int[ACCENT_SAMPLE_PX * ACCENT_SAMPLE_PX];
        bitmap.getPixels(pixels, 0, ACCENT_SAMPLE_PX, 0, 0, ACCENT_SAMPLE_PX, ACCENT_SAMPLE_PX);
        bitmap.recycle();
        return pixels;
    }

    private static final class Candidate {
        final ResolveInfo info;
        final String category;

        Candidate(ResolveInfo info, String category) {
            this.info = info;
            this.category = category;
        }
    }
}
