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

import com.termux.shared.packages.PackageUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.widget.R;
import com.termux.shared.shell.ShellUtils;
import com.termux.widget.TermuxWidgetService;
import com.termux.widget.TermuxCreateShortcutActivity;
import com.termux.widget.TermuxWidgetUtils;
import com.termux.widget.ShortcutFile;
import com.termux.shared.logger.Logger;

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

    @TargetApi(Build.VERSION_CODES.O)
    private void clearDynamicShortcuts(Context context) {
        ShortcutManager shortcutManager = (ShortcutManager) context.getSystemService(Context.SHORTCUT_SERVICE);
        if (shortcutManager == null) return;

        shortcutManager.removeAllDynamicShortcuts();
        Logger.showToast(context, "Removing dynamic shortcuts successful.", true);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createDynamicShortcuts(Context context) {
        ShortcutManager shortcutManager = (ShortcutManager) context.getSystemService(Context.SHORTCUT_SERVICE);
        if (shortcutManager == null) return;

        List<ShortcutFile> files = new ArrayList<>();
        TermuxWidgetUtils.enumerateShortcutFiles(files, false);

        List<ShortcutInfo> shortcuts = new ArrayList<>();
        for (ShortcutFile file : files) {
            shortcuts.add(file.getShortcut(context));
        }

        // Remove shortcuts that can not be added.
        int maximumShortcuts = shortcutManager.getMaxShortcutCountPerActivity();
        if (shortcuts.size() > maximumShortcuts) {
            Logger.logErrorAndShowToast(context, LOG_TAG, "Too many shorcuts. Commands to increase limit can be fonud in the readme. Current limit is: "+maximumShortcuts);
            for (int i=0; i<shortcuts.size()-maximumShortcuts; i++) {
                Logger.showToast(context, "Skipping "+shortcuts.get(shortcuts.size()-1).getShortLabel(), true);
                shortcuts.remove(shortcuts.size()-1);
            }
        }

        shortcutManager.removeAllDynamicShortcuts();
        shortcutManager.addDynamicShortcuts(shortcuts);
        Logger.showToast(context, "Adding dynamic shortcuts successful.", true);
    }
}
