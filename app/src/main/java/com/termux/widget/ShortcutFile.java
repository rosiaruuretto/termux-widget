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

import com.termux.shared.file.FileUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.packages.PackageUtils;
import com.termux.shared.settings.preferences.TermuxWidgetAppSharedPreferences;
import com.termux.shared.shell.ShellUtils;
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

    public final File file;
    public final String label;

    public ShortcutFile(File file, int depth) {
        this.file = file;
        this.label = (depth > 0 ? (file.getParentFile().getName() + "/") : "")
                + file.getName();
    }

    public Intent getExecutionIntent(Context context) {
        String path = file.getAbsolutePath();
        Uri scriptUri = new Uri.Builder().scheme(TERMUX_SERVICE.URI_SCHEME_SERVICE_EXECUTE).path(path).build();
        Intent executionIntent = new Intent(context, TermuxLaunchShortcutActivity.class);
        executionIntent.setAction(TERMUX_SERVICE.ACTION_SERVICE_EXECUTE); // Mandatory for pinned shortcuts
        executionIntent.setData(scriptUri);
        executionIntent.putExtra(TERMUX_WIDGET.EXTRA_TOKEN_NAME, TermuxWidgetAppSharedPreferences.getGeneratedToken(context));
        return executionIntent;
    }

    public ShortcutInfo getShortcut(Context context) {
        String path = file.getAbsolutePath();

        ShortcutInfo.Builder builder = new ShortcutInfo.Builder(context, path);
        builder.setIntent(this.getExecutionIntent(context));
        builder.setShortLabel(this.label);

        // Set icon if existent.
        File shortcutIconFile = TermuxCreateShortcutActivity.getShortcutIconFile(context, ShellUtils.getExecutableBasename(path));
        if (shortcutIconFile != null)
            builder.setIcon(Icon.createWithBitmap(((BitmapDrawable) Drawable.createFromPath(shortcutIconFile.getAbsolutePath())).getBitmap()));
        else
            builder.setIcon(Icon.createWithResource(context, R.drawable.ic_launcher));

        return builder.build();
    }

    public RemoteViews getListWidgetView(Context context) {
        // Position will always range from 0 to getCount() - 1.
        // Construct remote views item based on the item xml file and set text based on position.
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_item);
        rv.setTextViewText(R.id.widget_item, this.label);

        // Next, we set a fill-intent which will be used to fill-in the pending intent template
        // which is set on the collection view in TermuxAppWidgetProvider.
        Intent fillInIntent = new Intent().putExtra(TERMUX_WIDGET_PROVIDER.EXTRA_FILE_CLICKED, this.file.getAbsolutePath());
        rv.setOnClickFillInIntent(R.id.widget_item_layout, fillInIntent);

        return rv;
    }
}
