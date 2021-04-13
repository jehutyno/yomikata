package com.jehutyno.yomikata.util;

import android.content.Context;

import java.io.File;

public class FileUtils {

    public static File getDataDir(Context context) {
        String path = context.getFilesDir().getAbsolutePath() + "/SampleZip";
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }

        return file;
    }

    public static File getDataDir(Context context, String folder) {
        String path = context.getFilesDir().getAbsolutePath() + "/" + folder;
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }

        return file;
    }

    public static void deleteFolder(Context context, String folder) {
        File dir = new File(context.getFilesDir().getAbsolutePath() + "/" + folder);
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                new File(dir, children[i]).delete();
            }
        }
    }
}