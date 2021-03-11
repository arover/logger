package com.arover.app.logger;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.util.Calendar;


public class LogWriterThread extends HandlerThread {

    private static final String TAG = "LogWriterThread";

    private static final int MSG_FLUSH = 0;
    private static final int MSG_CLOSE = 1;
    private static final int MSG_LOG = 2;
    static final int MSG_COMPRESS_COMPLETED = 3;

    private static final long FLUSH_LOG_DELAY = 120;
    private static final long CLOSE_FILE_WRITER_DELAY = 1500;

    private static final int BUFFER_SIZE = 4096;
    private static final StringBuffer sLogBuffer = new StringBuffer();
    private static final long MAX_LOG_FILE_SIZE = 1024 * 1024 * 1024;
    private final LoggerManager logManager;

    private boolean isCompressing;
    private BufferedWriter fileLogWriter;
    private Handler handler;
    private LogCompressor logCompressor;
    private boolean doCheckUncompressedLogs = true;
    private File currentLogFile;
    private int logFileNo;
    private String currentLogFileName;

    LogWriterThread(LoggerManager loggerManager) {
        super("LogWriter", Process.THREAD_PRIORITY_BACKGROUND);
        this.logManager = loggerManager;
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();

        handler = new Handler(getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    //首先写入内存;发送延迟写入命令。
                    case MSG_LOG:
                        String log = (String) msg.obj;
                        sLogBuffer.append(log);
                        sendEmptyMessageDelayed(MSG_FLUSH, FLUSH_LOG_DELAY);
                        break;
                    //清除队列中的所有写入命令, 写log至文件。
                    case MSG_FLUSH:
                        removeMessages(MSG_FLUSH);
                        writeLog();
                        removeMessages(MSG_CLOSE);
                        sendEmptyMessageDelayed(MSG_CLOSE, CLOSE_FILE_WRITER_DELAY);
                        break;
                    case MSG_CLOSE:
                        try {
                            if (fileLogWriter != null) {
                                logFileNo--;
                                fileLogWriter.close();
                            }
                        } catch (Exception ignored) {
                        } finally {
                            fileLogWriter = null;
                        }
                        break;
                    case MSG_COMPRESS_COMPLETED:
                        isCompressing = false;
                        logCompressor = null;
                        break;
                }
            }
        };
        Log.i(TAG, "LogWriterThread started level=" + Log.sLogLvlName + ",locat enable=" + Log.sLogcatEnabled);
        Log.sInitialized = true;
    }

    private void writeLog() {
        if (Log.sLogDir == null) return;

        try {
            if (fileLogWriter == null || reachLogFileSizeLimit()) {
                createLogWriter();
            }

            if (fileLogWriter != null && (sLogBuffer.length() > 0)) {
                fileLogWriter.write(sLogBuffer.toString());
                fileLogWriter.flush();
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "writeLog", e);
            fileLogWriter = null;
        } finally {
            sLogBuffer.setLength(0);
        }
    }

    private boolean reachLogFileSizeLimit() {
        return currentLogFile.length() > MAX_LOG_FILE_SIZE;

    }

    private void createLogWriter() {
        if(fileLogWriter!=null){
            close(fileLogWriter);
        }

        File folder = new File(Log.sLogDir);
        if (!folder.exists() && !folder.mkdirs()) {
            android.util.Log.e(TAG, "log dir create failed.dir=" + Log.sLogDir);
        }

        try {
            currentLogFileName = genLogFileName();
            currentLogFile = new File(Log.sLogDir, currentLogFileName);
            if (!currentLogFile.exists()) {
                boolean created = currentLogFile.createNewFile();
                if (!created) {
                    android.util.Log.e(TAG, "failed to create new log file  path="
                            + currentLogFile.getAbsolutePath());
                }
                //check uncompressed logs after create new log file.
                doCheckUncompressedLogs = true;
            }
            //todo check existing logfile size and update logfile No.

            fileLogWriter = new BufferedWriter(new FileWriter(currentLogFile, true));

            if (doCheckUncompressedLogs &&  !isCompressing) {
                findAllOldLogsAndCompress();
                doCheckUncompressedLogs = false;
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "writeLog", e);
        }
    }

    private void close(Closeable closeable) {
        if(closeable==null) return;
        try{
            closeable.close();
        }catch (Exception ignored){}
    }

    private String genLogFileName() {
        Calendar cal = Calendar.getInstance();
        String filename  = cal.get(Calendar.YEAR)
                + "-" + (cal.get(Calendar.MONTH) + 1)
                + ("-") + cal.get(Calendar.DAY_OF_MONTH)
                + "-"+logFileNo+".log";
        logFileNo ++ ;

        return filename;
    }

    private void findAllOldLogsAndCompress() {
        if (isCompressing) return;
        android.util.Log.d(TAG, "findAllOldLogsAndCompress currentLogFileName=" + currentLogFileName);

        logCompressor = new LogCompressor(handler,logManager.getLogDirFullPath(), currentLogFileName);
        logCompressor.start();
    }

    private String getLogTime() {
        Calendar calendar = Calendar.getInstance();
        return (calendar.get(Calendar.MONTH) + 1)
                + "-" + calendar.get(Calendar.DAY_OF_MONTH)
                + " " + calendar.get(Calendar.HOUR_OF_DAY)
                + ":" + calendar.get(Calendar.MINUTE)
                + ":" + calendar.get(Calendar.SECOND)
                + "." + calendar.get(Calendar.MILLISECOND);
    }

    void writeLog(LoggerManager.Level level, String log) {

        String logStr = getLogTime()+ level.prefix + log + "\n";
        if (handler == null) {
            sLogBuffer.append(logStr);
            android.util.Log.d(TAG, "looper not prepared, log put in buffer.");
            return;
        }
        Message msg = Message.obtain(handler, MSG_LOG, logStr);
        msg.sendToTarget();
    }

    @Override
    public boolean quit() {
        android.util.Log.d(TAG,"quit");
        writeLog();

        close(fileLogWriter);

        return super.quit();
    }


}
