package com.termux.widget;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.Nullable;

import com.google.common.base.Joiner;
import com.termux.shared.data.DataUtils;
import com.termux.shared.file.FileUtils;
import com.termux.shared.file.TermuxFileUtils;
import com.termux.shared.file.filesystem.FileType;
import com.termux.shared.logger.Logger;
import com.termux.shared.settings.preferences.TermuxWidgetAppSharedPreferences;
import com.termux.shared.shell.ShellUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_SERVICE;
import com.termux.shared.termux.TermuxConstants.TERMUX_WIDGET;
import com.termux.shared.termux.TermuxUtils;

import java.io.File;
import java.util.Arrays;

public class TermuxCreateShortcutActivity extends Activity {

    private ListView mListView;
    private File mCurrentDirectory;
    private File[] mCurrentFiles;

    private static final String LOG_TAG = "TermuxCreateShortcutActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shortcuts_listview);
        mListView = findViewById(R.id.list);
    }

    @Override
    protected void onResume() {
        super.onResume();

        String errmsg = TermuxUtils.isTermuxAppAccessible(this);
        if (errmsg != null) {
            Logger.logErrorAndShowToast(this, LOG_TAG, errmsg);
            finish();
            return;
        }

        updateListview(TermuxConstants.TERMUX_SHORTCUT_SCRIPTS_DIR);

        mListView.setOnItemClickListener((parent, view, position, id) -> {
            final Context context = TermuxCreateShortcutActivity.this;
            File clickedFile = mCurrentFiles[position];
            if (clickedFile.isDirectory()) {
                updateListview(clickedFile);
            } else {
                createShortcut(context, clickedFile);
                finish();
            }
        });
    }

    private void updateListview(File directory) {
        mCurrentDirectory = directory;
        mCurrentFiles = directory.listFiles(TermuxWidgetService.SHORTCUT_FILES_FILTER);

        if (mCurrentFiles == null) mCurrentFiles = new File[0];

        Arrays.sort(mCurrentFiles, (f1, f2) -> f1.getName().compareTo(f2.getName()));

        final boolean isTopDir = directory.equals(TermuxConstants.TERMUX_SHORTCUT_SCRIPTS_DIR);
        getActionBar().setDisplayHomeAsUpEnabled(!isTopDir);

        if (isTopDir && mCurrentFiles.length == 0) {
            // Create if necessary so user can more easily add.
            TermuxConstants.TERMUX_SHORTCUT_SCRIPTS_DIR.mkdirs();
            new AlertDialog.Builder(this)
                    .setMessage(R.string.msg_no_shortcut_scripts)
                    .setOnDismissListener(dialog -> finish()).show();
            return;
        }

        final String[] values = new String[mCurrentFiles.length];
        for (int i = 0; i < values.length; i++)
            values[i] = mCurrentFiles[i].getName() +
                    (mCurrentFiles[i].isDirectory() ? "/" : "");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, values);
        mListView.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            updateListview(DataUtils.getDefaultIfNull(mCurrentDirectory.getParentFile(), mCurrentDirectory));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }



    private void createShortcut(Context context, File clickedFile) {
        boolean isPinnedShortcutSupported = false;

        ShortcutFile file = new ShortcutFile(clickedFile);

        try {
            if (isPinnedShortcutSupported)
                createPinnedShortcut(context, file);
            else
                createStaticShortcut(context, file);
        } catch (Exception e) {
            String message = context.getString(
                    isPinnedShortcutSupported ? R.string.error_create_pinned_shortcut_failed : R.string.error_create_static_shortcut_failed,
                    TermuxFileUtils.getUnExpandedTermuxPath(file.getPath()));
            Logger.logErrorAndShowToast(context, LOG_TAG, message + ": " + e.getMessage());
            Logger.logStackTraceWithMessage(LOG_TAG, message, e);
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createPinnedShortcut(Context context, ShortcutFile file) {
        ShortcutManager shortcutManager = (ShortcutManager) context.getSystemService(Context.SHORTCUT_SERVICE);
        if (shortcutManager == null) return;

        Logger.showToast(context, context.getString(R.string.msg_request_create_pinned_shortcut,
                TermuxFileUtils.getUnExpandedTermuxPath(file.getPath())), true);
        shortcutManager.requestPinShortcut(file.getShortcut(context, true), null);
    }

    private void createStaticShortcut(Context context, ShortcutFile file) {
        Logger.showToast(context, context.getString(R.string.msg_request_create_static_shortcut,
                TermuxFileUtils.getUnExpandedTermuxPath(file.getPath())), true);
        setResult(RESULT_OK, file.getStaticShortcutIntent(context));
    }

}
