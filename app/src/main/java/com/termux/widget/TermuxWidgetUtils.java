package com.termux.widget;

import com.termux.shared.termux.TermuxConstants;

import java.io.File;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class TermuxWidgetUtils {

    public static final int MAX_DEPTH = 5;

    public static void enumerateShortcutFiles(List<ShortcutFile> files, boolean sorted) {
        enumerateShortcutFiles(files, TermuxConstants.TERMUX_SHORTCUT_SCRIPTS_DIR, sorted, 0);
    }

    public static void enumerateShortcutFiles(List<ShortcutFile> files, File dir, boolean sorted) {
        enumerateShortcutFiles(files, dir, sorted, 0);
    }

    public static void enumerateShortcutFiles(List<ShortcutFile> files, File dir, boolean sorted, int depth) {
        if (depth > MAX_DEPTH) return;

        File[] current_files = dir.listFiles(TermuxWidgetService.SHORTCUT_FILES_FILTER);

        if (current_files == null) return;

        if (sorted) {
            Arrays.sort(current_files, (lhs, rhs) -> {
                if (lhs.isDirectory() != rhs.isDirectory()) {
                    return lhs.isDirectory() ? 1 : -1;
                }
                return NaturalOrderComparator.compare(lhs.getName(), rhs.getName());
            });
        }

        for (File file : current_files) {
            if (file.isDirectory()) {
                enumerateShortcutFiles(files, file, sorted, depth + 1);
            } else {
                files.add(new ShortcutFile(file, depth));
            }
        }
    }
}
