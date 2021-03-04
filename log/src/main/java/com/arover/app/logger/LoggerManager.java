package com.arover.app.logger;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * the manager and context of this logger
 */
public class LoggerManager {
    private static final String TAG = "LoggerManager";
    private static final int DEFAULT_MAX_LOG_IN_DAYS = 15;
    private static final long DELETE_LOG_DELAY = 3* 1000;
    private final Context context;
    private String logDirFullPath;
    private boolean enableLogcat;
    private Level level = Level.VERBOSE;
    LogExecutor logExecutor;
    private String logFolderName = "log";

    //application context
    @SuppressLint("StaticFieldLeak")
    private static LoggerManager INSTANCE;

    public static class Builder {
        private final LoggerManager mgr;

        public Builder(Context context) {
            mgr = new LoggerManager(context);
        }

        public Builder level(Level level) {
            mgr.setLevel(level);
            return this;
        }

        public Builder folder(String folderName) {
            mgr.logFolderName = folderName;
            return this;
        }

        public Builder enableLogcat(boolean isEnable) {
            mgr.enableLogcat = isEnable;
            return this;
        }

        public Builder logGenerator(LogExecutor executor) {
            mgr.logExecutor = executor;
            return this;
        }

        public LoggerManager build() {
            LoggerManager.INSTANCE = mgr;
            mgr.initialize();
            return mgr;
        }
    }

    private LoggerManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public boolean enableLogcat() {
        return enableLogcat;
    }

    public void setLogPrinterWorker(LogExecutor logGenerator) {
        if (logExecutor != null) {
            logExecutor.tearDown();
        }
        logExecutor = logGenerator;
    }

    /**
     * config folder name : the log storage file name
     *
     * @return log folder absolute path
     */
    public String getLogDirFullPath() {
        return logDirFullPath;
    }

    public Level getLevel() {
        return level;
    }

    public enum Level {
        FATAL(0, " [F] "),
        ERROR(1, " [E] "),
        WARN(2, " [W] "),
        INFO(3, " [I] "),
        DEBUG(4, " [D] "),
        VERBOSE(5, " [V] ");

        public final String prefix;
        public final int code;

        Level(int code, String prefix) {
            this.code = code;
            this.prefix = prefix;
        }
    }

    private void initialize() {
        if (logFolderName == null) {
            throw new NullPointerException("folder name is null");
        }

        if (level == null) {
            throw new NullPointerException("log level is null");
        }
        if (logExecutor == null) {
            logExecutor = new DefaultLogExecutor();
        }

        initStorageFolder(logFolderName);

        if (logDirFullPath == null) {
            android.util.Log.e(TAG, "initialize log failed.");
            return;
        }

        Log.init(this);
    }

    private void initStorageFolder(String folder) {
        if (isExternalStorageWritable()) {
            File dir = context.getExternalFilesDir(null);
            if (dir == null) {
                dir = context.getFilesDir();
            }
            File logDir = new File(dir, folder);
            if (!logDir.exists() && !logDir.mkdirs()) {
                android.util.Log.e(TAG, "log folder not created " + logDir.getAbsolutePath());
                return;
            }

            android.util.Log.i(TAG, "log dir=" + logDir.getAbsolutePath());
            this.logDirFullPath = logDir.getAbsolutePath();
        } else {
            android.util.Log.e(TAG, "initStorageFolder external storage is not writable");
        }
    }

    /* Checks if external storage is available for read and write */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /*
     * 删除N天前日志
     *  using executor
     */
    public void deleteOldLogs(int days) {
        if (days <= 0) {
            days = DEFAULT_MAX_LOG_IN_DAYS;
        }
        if (logFolderName == null) {
            throw new IllegalStateException("storage folder is null.");
        }

        if(logExecutor != null) {
            logExecutor.execute(new DeleteLogTask(logFolderName, days));
        }
    }

    /**
     * delete old logs in n days.
     * operation post delayed in 3 seconds to avoid occupy cpu time for app launch.
     * @param days
     */
    public void deleteOldLogsDelayed(int days) {
        new Handler(Looper.getMainLooper()).postDelayed(createDeleteLogTask(days), DELETE_LOG_DELAY);
    }

    public Runnable createDeleteLogTask(int days) {
        if (days <= 0) {
            days = DEFAULT_MAX_LOG_IN_DAYS;
        }
        if (logFolderName == null) {
            throw new IllegalStateException("log folder name is null.");
        }

        return new DeleteLogTask(logFolderName, days);
    }

    private static class DeleteLogTask implements Runnable {
        private final int days;
        private final String folder;

        public DeleteLogTask(String folder, int days) {
            this.days = days;
            this.folder = folder;
        }

        @Override
        public void run() {
            File file = new File(folder);

            if (file.exists() && file.isDirectory()) {
                File files[] = file.listFiles();
                if (files == null) return;

                if (files.length <= days) return;
                Log.i(TAG, "delete log file length = " + (files.length - days));

                for (int i = files.length - 1; i >= days; i--) {
                    if (!files[i].delete())
                        Log.w(TAG, "delete log file failed,name" + files[i].getName());
                }
            }
        }
    }

    private static class DefaultLogExecutor extends LogExecutor {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        @Override
        public void execute(Runnable runnable) {
            executor.execute(runnable);
        }

        @Override
        public void tearDown() {
            executor.shutdown();
            executor = null;
        }

    }
}
