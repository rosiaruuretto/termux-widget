package com.termux.widget;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.termux.shared.file.FileUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxConstants.TERMUX_WIDGET.TERMUX_WIDGET_PROVIDER;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class TermuxWidgetService extends RemoteViewsService {

    /* Allowed paths under which shortcut files can exist. */
    public static final List<String> SHORTCUT_FILES_ALLOWED_PATHS_LIST = Arrays.asList(
            TermuxConstants.TERMUX_SHORTCUT_SCRIPTS_DIR_PATH,
            TermuxConstants.TERMUX_DATA_HOME_DIR_PATH);

    /* Allowed paths under which shortcut icons files can exist. */
    public static final List<String> SHORTCUT_ICONS_FILES_ALLOWED_PATHS_LIST = Arrays.asList(
            TermuxConstants.TERMUX_SHORTCUT_SCRIPT_ICONS_DIR_PATH,
            TermuxConstants.TERMUX_DATA_HOME_DIR_PATH);

    public static final FileFilter SHORTCUT_FILES_FILTER = new FileFilter() {
        public boolean accept(File file) {
            // Do not show hidden files starting with a dot.
            if (file.getName().startsWith("."))
                return false;
            // Do not show broken symlinks
            else if (!FileUtils.fileExists(file.getAbsolutePath(), true))
                return false;
            // Do not show files that are not under SHORTCUT_FILES_ALLOWED_PATHS_LIST
            else if (!FileUtils.isPathInDirPaths(file.getAbsolutePath(), SHORTCUT_FILES_ALLOWED_PATHS_LIST, true))
                return false;
            // Do not show files under TERMUX_SHORTCUT_SCRIPT_ICONS_DIR_PATH
            else if (TermuxConstants.TERMUX_SHORTCUT_SCRIPTS_DIR.equals(file.getParentFile()) &&
                    file.getName().equals(TermuxConstants.TERMUX_SHORTCUT_SCRIPT_ICONS_DIR_BASENAME))
                return false;
            return true;
        }
    };

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new ListRemoteViewsFactory(getApplicationContext());
    }

    public static class ListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
        private final List<ShortcutFile> shortcutFiles = new ArrayList<>();
        private final Context mContext;

        public ListRemoteViewsFactory(Context context) {
            mContext = context;
        }

        @Override
        public void onCreate() {
            // In onCreate() you setup any connections / cursors to your data source. Heavy lifting,
            // for example downloading or creating content etc, should be deferred to onDataSetChanged()
            // or getViewAt(). Taking more than 20 seconds in this call will result in an ANR.
        }

        @Override
        public void onDestroy() {
            shortcutFiles.clear();
        }

        @Override
        public int getCount() {
            return shortcutFiles.size();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            // Position will always range from 0 to getCount() - 1.
            return shortcutFiles.get(position).getListWidgetView(mContext);
        }

        @Override
        public RemoteViews getLoadingView() {
            // You can create a custom loading view (for instance when getViewAt() is slow.) If you
            // return null here, you will get the default loading view.
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public void onDataSetChanged() {
            // This is triggered when you call AppWidgetManager notifyAppWidgetViewDataChanged
            // on the collection view corresponding to this factory. You can do heaving lifting in
            // here, synchronously. For example, if you need to process an image, fetch something
            // from the network, etc., it is ok to do it here, synchronously. The widget will remain
            // in its current state while work is being done here, so you don't need to worry about
            // locking up the widget.
            shortcutFiles.clear();
            // Create directory if necessary so user more easily finds where to put shortcuts:
            TermuxConstants.TERMUX_SHORTCUT_SCRIPTS_DIR.mkdirs();

            TermuxWidgetUtils.enumerateShortcutFiles(shortcutFiles, true);
        }
    }

}
