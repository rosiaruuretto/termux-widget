package com.termux.widget;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import androidx.annotation.Nullable;

import com.google.common.base.Joiner;
import com.termux.shared.file.FileUtils;
import com.termux.shared.file.TermuxFileUtils;
import com.termux.shared.file.filesystem.FileType;
import com.termux.shared.logger.Logger;
import com.termux.shared.packages.PackageUtils;
import com.termux.shared.settings.preferences.TermuxWidgetAppSharedPreferences;
import com.termux.shared.shell.ShellUtils;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_SERVICE;
import com.termux.shared.termux.TermuxConstants.TERMUX_WIDGET.TERMUX_WIDGET_PROVIDER;
import com.termux.shared.termux.TermuxConstants.TERMUX_WIDGET;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxUtils;
import com.termux.widget.NaturalOrderComparator;
import com.termux.widget.R;
import com.termux.widget.TermuxCreateShortcutActivity;
import com.termux.widget.TermuxWidgetService;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ShortcutFile {

    private static final String LOG_TAG = "ShortcutFile";

    public final File file;
    public final String label;

    public ShortcutFile(File file) {
        this.file = file;
        this.label = ShellUtils.getExecutableBasename(this.getPath());
    }

    public String getPath() {
        return file.getAbsolutePath();
    }

    public ShortcutFile(File file, int depth) {
        this.file = file;
        this.label = (depth > 0 ? (file.getParentFile().getName() + "/") : "")
                + file.getName();
    }

    public Intent getExecutionIntent(Context context) {
        Uri scriptUri = new Uri.Builder().scheme(TERMUX_SERVICE.URI_SCHEME_SERVICE_EXECUTE).path(this.getPath()).build();
        Intent executionIntent = new Intent(context, TermuxLaunchShortcutActivity.class);
        executionIntent.setAction(TERMUX_SERVICE.ACTION_SERVICE_EXECUTE); // Mandatory for pinned shortcuts
        executionIntent.setData(scriptUri);
        executionIntent.putExtra(TERMUX_WIDGET.EXTRA_TOKEN_NAME, TermuxWidgetAppSharedPreferences.getGeneratedToken(context));
        return executionIntent;
    }

    public ShortcutInfo getShortcut(Context context, boolean logUsedIcon) {
        String path = file.getPath();

        ShortcutInfo.Builder builder = new ShortcutInfo.Builder(context, path);
        builder.setIntent(this.getExecutionIntent(context));
        builder.setShortLabel(this.label);

        // Set icon if existent.
        File shortcutIconFile = this.getIconFile(context, logUsedIcon);
        if (shortcutIconFile != null)
            builder.setIcon(Icon.createWithBitmap(((BitmapDrawable) Drawable.createFromPath(shortcutIconFile.getAbsolutePath())).getBitmap()));
        else
            builder.setIcon(Icon.createWithResource(context, R.drawable.ic_launcher));

        return builder.build();
    }

    public Intent getStaticShortcutIntent(Context context) {
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, this.getExecutionIntent(context));
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, label);

        // Set icon if existent.
        File shortcutIconFile = this.getIconFile(context, true);
        if (shortcutIconFile != null)
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, ((BitmapDrawable) Drawable.createFromPath(shortcutIconFile.getAbsolutePath())).getBitmap());
        else
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context, R.drawable.ic_launcher));

        return intent;
    }

    public RemoteViews getListWidgetView(Context context) {
        // Position will always range from 0 to getCount() - 1.
        // Construct remote views item based on the item xml file and set text based on position.
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_item);
        rv.setTextViewText(R.id.widget_item, this.label);

        // Next, we set a fill-intent which will be used to fill-in the pending intent template
        // which is set on the collection view in TermuxAppWidgetProvider.
        Intent fillInIntent = new Intent().putExtra(TERMUX_WIDGET_PROVIDER.EXTRA_FILE_CLICKED, this.getPath());
        rv.setOnClickFillInIntent(R.id.widget_item_layout, fillInIntent);

        return rv;
    }

    @Nullable
    private File getIconFile(Context context, boolean logUsedIcon) {
        String errmsg;
        String shortcutIconFilePath = FileUtils.getCanonicalPath(
                TermuxConstants.TERMUX_SHORTCUT_SCRIPT_ICONS_DIR_PATH +
                "/" + ShellUtils.getExecutableBasename(this.getPath()) + ".png", null);

        FileType fileType = FileUtils.getFileType(shortcutIconFilePath, true);
        //  Ensure file or symlink points to a regular file that exists
        if (fileType != FileType.REGULAR) {
            if (fileType != FileType.NO_EXIST) {
                errmsg = context.getString(R.string.error_icon_not_a_regular_file, fileType.getName()) +
                        "\n" + context.getString(R.string.msg_icon_absolute_path, shortcutIconFilePath);
                Logger.logErrorAndShowToast(context, LOG_TAG, errmsg);
            }
            return null;
        }

        // Do not allow shortcut icons files not under SHORTCUT_ICONS_FILES_ALLOWED_PATHS_LIST
        if (!FileUtils.isPathInDirPaths(shortcutIconFilePath, TermuxWidgetService.SHORTCUT_ICONS_FILES_ALLOWED_PATHS_LIST, true)) {
            errmsg = context.getString(R.string.error_icon_not_under_shortcut_icons_directories,
                    Joiner.on(", ").skipNulls().join(TermuxFileUtils.getUnExpandedTermuxPaths(TermuxWidgetService.SHORTCUT_ICONS_FILES_ALLOWED_PATHS_LIST))) +
                    "\n" + context.getString(R.string.msg_icon_absolute_path, shortcutIconFilePath);
            Logger.logErrorAndShowToast(context, LOG_TAG, errmsg);
            return null;
        }

        Logger.logInfo(LOG_TAG, "Using file at \"" + shortcutIconFilePath + "\" as shortcut icon file");
        if (logUsedIcon) {
            Logger.showToast(context, "Using file at \"" + shortcutIconFilePath + "\" as shortcut icon file", true);
        }

        return new File(shortcutIconFilePath);
    }

}
