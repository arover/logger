package com.arover.app.logger;

import android.os.Handler;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * log file compress thread.
 */
class LogCompressor extends Thread {
    private static final String TAG = "LogCompressor";
    private static final int BUFFER_SIZE = 4096;
    private final WeakReference<Handler> logThreadHandler;
    private final String logFolder;
    private final String currentWritingLogFile;

    public LogCompressor(Handler handler, String dir, String currentLogFileName) {
        logThreadHandler = new WeakReference<>(handler);
        logFolder = dir;
        currentWritingLogFile = currentLogFileName;
    }

    @Override
    public void run() {
        try {
            FileFilter logFilter = new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.getName().contains("log");
                }
            };

            File[] logFiles = new File(logFolder).listFiles(logFilter);

            if(logFiles == null){
                return;
            }

            for (File logFile : logFiles) {

                if (!logFile.getName().endsWith(".log")) {
                    continue;
                }

                if (logFile.getName().endsWith(currentWritingLogFile)) {
                    Log.d(TAG, "skip current log file=" + logFile.getName());
                    continue;
                }

                Log.d(TAG, "found log file=" + logFile.getName() +" compressing...");

                zip(logFile.getName(), logFile.getName().replaceFirst("\\.log", ".zip"));

                if (!logFile.delete()) {
                    Log.w(TAG, "failed to delete old log:" + logFile.getName());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "findAllOldLogsAndCompress:" + e.getMessage(), e);
        }
        Handler handler = logThreadHandler.get();
        if (handler != null) {
            handler.sendEmptyMessage(LogWriterThread.MSG_COMPRESS_COMPLETED);
        }
    }

    private void zip(String filename, String zipFileName) {
        Log.d(TAG, "zip filename:" + filename + ",zip file name:" + zipFileName);
        FileOutputStream dest = null;
        ZipOutputStream out = null;
        FileInputStream fi = null;
        BufferedInputStream origin = null;
        try {
            dest = new FileOutputStream(logFolder + File.separator + zipFileName);
            out = new ZipOutputStream(new BufferedOutputStream(dest));
            byte data[] = new byte[BUFFER_SIZE];

            fi = new FileInputStream(logFolder + File.separator + filename);
            origin = new BufferedInputStream(fi, BUFFER_SIZE);

            ZipEntry entry = new ZipEntry(filename.substring(filename.lastIndexOf("/") + 1));
            out.putNextEntry(entry);
            int count;

            while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1) {
                out.write(data, 0, count);
            }

        } catch (Exception e) {
            Log.e(TAG, "zip error:", e);
        } finally {

            try {
                if (fi != null) fi.close();
            } catch (Exception ignored) {
            }

            try {
                if (origin != null) origin.close();
            } catch (Exception ignored) {
            }

            try {
                if (out != null) out.close();
            } catch (Exception ignored) {
            }

            try {
                if (dest != null) dest.close();
            } catch (Exception ignored) {
            }
        }
    }
}
