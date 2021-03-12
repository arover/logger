package com.arover.app.logger;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;

import com.arover.app.crypto.ChaCha20;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;


public class LogWriterThread extends HandlerThread {

    private static final String TAG = "LogWriterThread";

    private static final int MSG_FLUSH = 0;
    private static final int MSG_CLOSE = 1;
    private static final int MSG_LOG = 2;
    static final int MSG_COMPRESS_COMPLETED = 3;

    private static final long FLUSH_LOG_DELAY = 500;
    private static final long CLOSE_FILE_WRITER_DELAY = 5500;


    private static final ByteBuffer sLogBuffer = ByteBuffer.allocateDirect(1024 * 200);
    private static final long MAX_LOG_FILE_SIZE = 1024 * 1024 * 1024;
    private final LoggerManager logManager;
    public static LogWriterThread instance;

    private boolean isCompressing;
    private DataOutputStream fileLogWriter;
    private Handler handler;
    private LogCompressor logCompressor;
    private boolean doCheckUncompressedLogs = true;
    private File currentLogFile;
    private int logFileNo;
    private String currentLogFileName;
    private byte[] key = new byte[32];
    private byte[] nonce = "12345678".getBytes();


    LogWriterThread(LoggerManager loggerManager) {
        super("LogWriter", Process.THREAD_PRIORITY_BACKGROUND);
        this.logManager = loggerManager;
        instance = this;
        byte[] bytes = "private static final StringBuffer sLogBuffer = new StringBuffer();".getBytes();
        System.arraycopy(bytes, 0, key, 0, 32);

    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();

        handler = new Handler(getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_LOG:
                        String log = (String) msg.obj;
                        byte[] bytes = log.getBytes();
                        android.util.Log.d(TAG,"put log"+log+"，bytes="+bytesToHexString(bytes));
                        sLogBuffer.put(log.getBytes());
                        sendEmptyMessageDelayed(MSG_FLUSH, FLUSH_LOG_DELAY);
                        break;

                    case MSG_FLUSH:
                        android.util.Log.d(TAG,"flush log begin");
                        removeMessages(MSG_FLUSH);
                        writeLog();
                        removeMessages(MSG_CLOSE);
                        sendEmptyMessageDelayed(MSG_CLOSE, CLOSE_FILE_WRITER_DELAY);
                        android.util.Log.d(TAG,"flush log done");
                        break;
                    case MSG_CLOSE:
                        android.util.Log.d(TAG,"close log write");
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
        Log.i(TAG, "LogWriterThread started level=" + Log.sLogLvlName + ",logcat enable=" + Log.sLogcatEnabled);
        Log.sInitialized = true;
    }

    private void writeLog() {
        if (Log.sLogDir == null) return;

        if (fileLogWriter == null || reachLogFileSizeLimit()) {
            createLogWriter();
        }

        try {
            if (fileLogWriter != null && (sLogBuffer.position() > 0)) {
                int n  = sLogBuffer.position();
                byte[] temp = new byte[n];
                sLogBuffer.flip();
                sLogBuffer.get(temp);
                sLogBuffer.clear();
                byte[] logData = encryptLogs(temp);
                byte[] len = intToBytes(logData.length);
                android.util.Log.d(TAG,"write len hex= "+bytesToHexString(len)
                        +",len = "+logData.length+",log hex="+bytesToHexString(logData));
                fileLogWriter.write(len);
                fileLogWriter.write(logData);
                fileLogWriter.flush();
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "writeLog", e);
            fileLogWriter = null;
        }
    }

    public static String bytesToHexString(byte[] data) {
        if (data == null) return "";
        StringBuilder s = new StringBuilder(data.length * 2);
        for (byte b : data) {
            s.append(String.format("%02x", b & 0xff));
        }
        return s.toString();
    }

    public static byte[] hexStringToBytes(String s) {
        if(s.length()==0){
            return new byte[0];
        }

        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
    public static byte[] intToBytes(int value){
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
    }
    private byte[] encryptLogs(byte[] logs) {
        return encrypt(logs, key, nonce);
    }

    byte[] encrypt(byte[] plain, byte[] key, byte[] nonce) {
        byte[] result = new byte[plain.length];

//        try {
//            ChaCha20 cipher = new ChaCha20(key, nonce, 0);
//            cipher.encrypt(result, result, result.length);
//        } catch (Exception e) {
//            android.util.Log.d(TAG,"encrypt",e);
//        }
        return result;
    }

    private boolean reachLogFileSizeLimit() {
        return currentLogFile.length() > MAX_LOG_FILE_SIZE;

    }

    private void createLogWriter() {
        if (fileLogWriter != null) {
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

            fileLogWriter = new DataOutputStream(new FileOutputStream(currentLogFile, true));

            if (doCheckUncompressedLogs && !isCompressing) {
                findAllOldLogsAndCompress();
                doCheckUncompressedLogs = false;
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "writeLog", e);
        }
    }

    private void close(Closeable closeable) {
        if (closeable == null) return;
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }

    private String genLogFileName() {
        Calendar cal = Calendar.getInstance();
        String filename = cal.get(Calendar.YEAR)
                + "-" + (cal.get(Calendar.MONTH) + 1)
                + ("-") + cal.get(Calendar.DAY_OF_MONTH)
                + "-" + logFileNo + ".log";
        logFileNo++;

        return filename;
    }

    private void findAllOldLogsAndCompress() {
        if (isCompressing) return;
        android.util.Log.d(TAG, "findAllOldLogsAndCompress currentLogFileName=" + currentLogFileName);

        logCompressor = new LogCompressor(handler, logManager.getLogDirFullPath(), currentLogFileName);
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

        String logStr = getLogTime() + level.prefix + log + "\n";
        if (handler == null) {
            sLogBuffer.put(logStr.getBytes());
            android.util.Log.d(TAG, "looper not prepared, log put in buffer.");
            return;
        }
        Message msg = Message.obtain(handler, MSG_LOG, logStr);
        msg.sendToTarget();
    }

    @Override
    public boolean quit() {
        android.util.Log.d(TAG, "quit");
        writeLog();

        close(fileLogWriter);

        return super.quit();
    }

    public static int byteArrayToInt(byte[] b) {
        return   b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    public void decryptLog() {
        DataInputStream in = null;
        FileOutputStream out = null;
        android.util.Log.d(TAG,"read file = "+currentLogFile.getPath());
        try {
            File txtLogFile = new File(currentLogFile.getAbsolutePath() + ".txt");
            in = new DataInputStream(new FileInputStream(currentLogFile));
            out = new FileOutputStream(txtLogFile);
            byte[] buf = new byte[1024 * 300];
            byte[] sizeBuf = new byte[4];

            int n = 0;
            for(;;) {

                int len;
                try {
                    len = in.readInt();
                }catch (Exception e){
                    android.util.Log.e(TAG,"readInt = ",e);
                    break;
                }
//                 = byteArrayToInt(sizeBuf);
                android.util.Log.d(TAG,"size = "+len);
                if(len > 1024 * 200){
                    android.util.Log.e(TAG,"consider increase buf size = "+len);
                    return;
                }
                n = in.read(buf, 0, len);
                if (n <= 0) {
                    break;
                }

                out.write(encrypt(buf, key, nonce));
            }
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
