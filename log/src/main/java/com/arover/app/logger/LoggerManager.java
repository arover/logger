package com.arover.app.logger;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import com.arover.app.util.ProcessUtil;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * the manager and context of this logger
 */
public class LoggerManager {
    private static final String TAG = "LoggerManager";
    private static final int DEFAULT_MAX_LOG_IN_DAYS = 10;
    private static final long DELETE_LOG_DELAY = 20 * 1000;

    private final Context context;
    private String logDirFullPath;
    private boolean enableLogcat;
    private Level level = Level.VERBOSE;
    LogExecutor logExecutor;
    private String rootFolder = "logs";
    private String publicKey;
    private String processLogFolder = "";
    private static String ext = ".log";

    public static class Builder {
        private final LoggerManager mgr;

        public Builder(Context context) {
            mgr = new LoggerManager(context);
            //默认支持多进程。
            String processName = ProcessUtil.getProcessName(context);
            mgr.processLogFolder = ProcessUtil.getProcessNameWithPrefix(context, processName,  "process_");
            mgr.rootFolder = "logs";
        }

        public Builder level(Level level) {
            mgr.setLevel(level);
            return this;
        }
        /**
         * config root log folder
         * @param logExt
         * @return
         */
        public Builder logFileExt(String logExt) {
            if(!logExt.startsWith("."))
                throw new InvalidParameterException("日志后缀名需包含\".\", 如.log, .txt");
            ext = logExt;
            return this;
        }
        /**
         * config root log folder
         * @param folderName
         * @return
         */
        public Builder rootFolder(String folderName) {
            mgr.rootFolder = folderName;
            return this;
        }

        public Builder processLogFolder(String prefixName) {
            mgr.processLogFolder = prefixName;
            return this;
        }

        public Builder enableLogcat(boolean isEnable) {
            mgr.enableLogcat = isEnable;
            return this;
        }

        /**
         * empty or null will disable log encryption. default is null;
         *
         * @param publicKey nullable, RSA public key hex String.
         */
        public Builder encryptWithPublicKey(String publicKey) {
            mgr.publicKey = publicKey;
            return this;
        }

        public Builder logTaskExecutor(LogExecutor executor) {
            mgr.logExecutor = executor;
            return this;
        }

        public LoggerManager build() {
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

    public String getPublicKey() {
        return publicKey;
    }

    public String getLogFileExt() {
        return ext;
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

    void perform(Runnable runnable) {
        if (logExecutor != null) {
            logExecutor.execute(runnable);
        } else {
            new Thread(runnable).start();
        }
    }

    private void initialize() {
        if (rootFolder == null) {
            throw new NullPointerException("folder name is null");
        }

        if (level == null) {
            throw new NullPointerException("log level is null");
        }
        if (logExecutor == null) {
            logExecutor = new DefaultLogExecutor();
        }

        initStorageFolder(rootFolder, processLogFolder);

        if (logDirFullPath == null) {
            android.util.Log.e(TAG, "initialize log failed.");
            return;
        }

        Alog.init(this);
    }

    private void initStorageFolder(String rootFolder, String processLogFolder) {
        if (isExternalStorageWritable()) {
            File dir = context.getExternalFilesDir(null);
            if (dir == null) {
                dir = context.getFilesDir();
            }
            initDir(dir, rootFolder, processLogFolder);
        } else {
            android.util.Log.e(TAG, "initStorageFolder external storage is not writable");
            initDir(context.getFilesDir(), rootFolder, processLogFolder);
        }
    }


    private void initDir(File dir, String rootFolder, String processLogFolder) {
        Alog.rootDir = dir.getAbsolutePath() + "/" + rootFolder;
        String path = dir + "/" + rootFolder + "/" + processLogFolder;
//        android.util.Log.i(TAG, "log folder:" + path);

        File logDir = new File(path);

        if (!logDir.exists() && !logDir.mkdirs()) {
            android.util.Log.e(TAG, "log folder not created " + logDir.getAbsolutePath());
            return;
        }

        android.util.Log.i(TAG, "log dir=" + logDir.getAbsolutePath());
        this.logDirFullPath = logDir.getAbsolutePath();
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
    public void deleteOldLogs(int logFileDays) {
        final int days;
        if (logFileDays <= 0) {
            days = DEFAULT_MAX_LOG_IN_DAYS;
        } else {
            days = logFileDays;
        }
        if (Alog.getRootDir() == null) {
            Alog.e(TAG, "storage folder is null.");
            return;
        }

        Alog.d(TAG, "deleteOldLogs days="+logFileDays+",dir="+ Alog.getRootDir());

        if (logExecutor != null) {
            logExecutor.execute(new DeleteLogTask(Alog.getRootDir(), days));
        } else {
            new Thread(new DeleteLogTask(Alog.getRootDir(), days)).start();
        }
    }

    /**
     * delete old logs in n days.
     * operation post delayed in 3 seconds to avoid occupy cpu time for app launch.
     *
     * @param days
     */
    public void deleteOldLogsDelayed(final int days) {
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> deleteOldLogs(days),
                DELETE_LOG_DELAY);
    }

    private static class DeleteLogTask implements Runnable {
        private static final int DAY_IN_MILLS = 24 * 3600 * 1000;
        private final int days;
        private final String folder;

        public DeleteLogTask(String folder, int days) {
            this.days = days;
            this.folder = folder;
        }

        @Override
        public void run() {
            File file = new File(folder);
            Alog.i(TAG, "DeleteLogTask checking old logs in folder = " + file);
            if (!file.exists()) {
                Alog.e(TAG, "run DeleteLogTask root folder not exist.");
                return;
            }
            removeLogs(file);
        }

        private void removeLogs(File logRootFolder) {
            File[] logs = logRootFolder.listFiles(File::isDirectory);
            if(logs == null){
                return;
            }
            List<File> folders = new ArrayList<>(Arrays.asList(logs));
            folders.add(logRootFolder);

            for (File folder : folders) {

                Alog.d(TAG, "DeleteLogTask remove Logs of folder: " + folder);

                File[] logFiles = folder.listFiles(f ->
                        (f.getName().contains("zip") || f.getName().contains(LoggerManager.ext)));

                if (logFiles == null) continue;

                for (File logFile : logFiles) {
                    if (isNDaysBeforeFiles(days, logFile)) {
                        boolean deleted = logFile.delete();
                        Alog.i(TAG, "DeleteLogTask delete logFile=" + logFile+",deleted="+deleted);
                    }
                }
            }
        }

        private boolean isNDaysBeforeFiles(int days, File file) {
            return System.currentTimeMillis() - file.lastModified() > days * DAY_IN_MILLS;
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
