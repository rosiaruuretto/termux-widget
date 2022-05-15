package com.termux.widget.activities;

import android.annotation.TargetApi;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;

import com.termux.shared.logger.Logger;
import com.termux.shared.packages.PackageUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.widget.R;
import com.termux.widget.TermuxWidgetService;
import com.termux.widget.TermuxCreateShortcutActivity;
import com.termux.widget.NaturalOrderComparator;

import java.io.File;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class TermuxWidgetActivity extends AppCompatActivity {

    private static final String LOG_TAG = "TermuxWidgetActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_termux_widget);

        TextView pluginInfo = findViewById(R.id.textview_plugin_info);
        pluginInfo.setText(getString(R.string.plugin_info, TermuxConstants.TERMUX_GITHUB_REPO_URL,
                TermuxConstants.TERMUX_WIDGET_GITHUB_REPO_URL));

        Button disableLauncherIconButton = findViewById(R.id.btn_disable_launcher_icon);
        disableLauncherIconButton.setOnClickListener(v -> {
            String message = getString(R.string.msg_disabling_launcher_icon, TermuxConstants.TERMUX_WIDGET_APP_NAME);
            Logger.logInfo(LOG_TAG, message);
            PackageUtils.setComponentState(TermuxWidgetActivity.this,
                    TermuxConstants.TERMUX_WIDGET_PACKAGE_NAME, TermuxConstants.TERMUX_WIDGET.TERMUX_WIDGET_ACTIVITY_NAME,
                    false, message, true);
        });

        Button createDynamicShortcutsButton = findViewById(R.id.btn_create_dynamic_shortcuts);
        createDynamicShortcutsButton.setOnClickListener(v -> {
            // Create directory if necessary so user more easily finds where to put shortcuts:
            TermuxConstants.TERMUX_SHORTCUT_SCRIPTS_DIR.mkdirs();

            final Context context = TermuxWidgetActivity.this;
            createDynamicShortcuts(context);
        });

        Button clearDynamicShortcutsButton = findViewById(R.id.btn_clear_dynamic_shortcuts);
        clearDynamicShortcutsButton.setOnClickListener(v -> {
            final Context context = TermuxWidgetActivity.this;
            clearDynamicShortcuts(context);
        });
    }

    private static void enumerateShortcutFiles(List<TermuxWidgetService.TermuxWidgetItem> items, File dir, boolean sorted) {
        enumerateShortcutFiles(items, dir, sorted, 0);
    }

    private static void enumerateShortcutFiles(List<TermuxWidgetService.TermuxWidgetItem> items, File dir, boolean sorted, int depth) {
        if (depth > 5) return;

        File[] files = dir.listFiles(TermuxWidgetService.SHORTCUT_FILES_FILTER);

        if (files == null) return;

        if (sorted) {
            Arrays.sort(files, (lhs, rhs) -> {
                if (lhs.isDirectory() != rhs.isDirectory()) {
                    return lhs.isDirectory() ? 1 : -1;
                }
                return NaturalOrderComparator.compare(lhs.getName(), rhs.getName());
            });
        }

        for (File file : files) {
            if (file.isDirectory()) {
                enumerateShortcutFiles(items, file, sorted, depth + 1);
            } else {
                items.add(new TermuxWidgetService.TermuxWidgetItem(file, depth));
            }
        }

    }

    @TargetApi(Build.VERSION_CODES.O)
    private void clearDynamicShortcuts(Context context) {
        ShortcutManager shortcutManager = (ShortcutManager) context.getSystemService(Context.SHORTCUT_SERVICE);
        if (shortcutManager == null) return;

        shortcutManager.removeAllDynamicShortcuts();
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createDynamicShortcuts(Context context) {
        ShortcutManager shortcutManager = (ShortcutManager) context.getSystemService(Context.SHORTCUT_SERVICE);
        if (shortcutManager == null) return;

        List<TermuxWidgetService.TermuxWidgetItem> items = new ArrayList<>();
        enumerateShortcutFiles(items, TermuxConstants.TERMUX_SHORTCUT_SCRIPTS_DIR, false);

        List<ShortcutInfo> shortcuts = new ArrayList<>();
        for (TermuxWidgetService.TermuxWidgetItem item : items) {
            ShortcutInfo.Builder builder = new ShortcutInfo.Builder(context, item.mFile);
            builder.setIntent(TermuxCreateShortcutActivity.getExecutionIntent(context, item.mFile));
            builder.setShortLabel(item.mLabel);

            File shortcutIconFile = TermuxCreateShortcutActivity.getShortcutIconFile(context, item.mFile);
            if (shortcutIconFile != null)
                builder.setIcon(Icon.createWithBitmap(((BitmapDrawable) Drawable.createFromPath(shortcutIconFile.getAbsolutePath())).getBitmap()));
            else
                builder.setIcon(Icon.createWithResource(context, R.drawable.ic_launcher));

            shortcuts.add(builder.build());
        }

        // Logger.showToast(context, context.getString(R.string.msg_request_create_pinned_shortcut,
        //         TermuxFileUtils.getUnExpandedTermuxPath(shortcutFilePath)), true);
        // try {
            // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //     shortcutManager.pushDynamicShortcut(builder.build());
            // } else {
        shortcutManager.addDynamicShortcuts(shortcuts);
            // }
        // } catch (Exception e) {
            // String message = context.getString(
            //         isPinnedShortcutSupported ? R.string.error_create_pinned_shortcut_failed : R.string.error_create_static_shortcut_failed,
            //         TermuxFileUtils.getUnExpandedTermuxPath(shortcutFilePath));
            // Logger.logErrorAndShowToast(context, LOG_TAG, message + ": " + e.getMessage());
            // Logger.logStackTraceWithMessage(LOG_TAG, message, e);
        // }
    }
}
