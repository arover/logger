package com.arover.app.logger;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;

import com.arover.app.crypto.AesCbcCipher;
import com.arover.app.crypto.ChaCha20;
import com.arover.app.crypto.RsaCipher;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Calendar;

import static com.arover.app.Util.bytesToHexString;
import static com.arover.app.Util.hexStringToBytes;
import static com.arover.app.Util.intToBytes;


public class LogWriterThread extends HandlerThread {

    private static final String TAG = "LogWriterThread";

    private static final int MSG_FLUSH = 0;
    private static final int MSG_CLOSE = 1;
    private static final int MSG_LOG = 2;
    static final int MSG_COMPRESS_COMPLETED = 3;
    private static final int MSG_FORCE_FLUSH = 4;

    private static final long FLUSH_LOG_DELAY = 500;
    private static final long CLOSE_FILE_WRITER_DELAY = 5500;

    private static final int BUFFER_SIZE = 1024 * 20;
    private static final ByteBuffer sLogBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private static final long MAX_LOG_FILE_SIZE = 1024 * 1024 * 1024;
    private static final int FLUSH_THRESHOLD = BUFFER_SIZE/2;
    private static final byte ENCRYPT_LOG = 1;
    private static final byte PLAIN_LOG = 0;
    private final LoggerManager logManager;
    public static LogWriterThread instance;
    private byte[] publicKey;

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

        if(loggerManager.getPublicKey() != null){
            publicKey=hexStringToBytes(loggerManager.getPublicKey());
        }
        byte[] bytes = "private static final StringBuffer sLogBuffer = new StringBuffer();".getBytes();
        System.arraycopy(bytes, 0, key, 0, 32);

    }

    public void flushBuffer() {
        handler.sendEmptyMessage(MSG_FORCE_FLUSH);
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
                        android.util.Log.d(TAG, "put log" + log + "，bytes=" + bytesToHexString(bytes));

                        if(sLogBuffer.limit() - sLogBuffer.position() > bytes.length){
                            sLogBuffer.put(log.getBytes());
                            sendEmptyMessageDelayed(MSG_FLUSH, FLUSH_LOG_DELAY);
                        } else {
                            android.util.Log.e(TAG,"back pressure!!!  log discarded,  plz increase sLogBuffer");
                        }
                        break;

                    case MSG_FLUSH:
                        android.util.Log.d(TAG, "flush log begin");
                        removeMessages(MSG_FLUSH);
                        removeMessages(MSG_CLOSE);
                        writeLog(false);
                        android.util.Log.d(TAG, "flush log done");
                        break;
                    case MSG_FORCE_FLUSH:
                        android.util.Log.d(TAG, "flush log begin");
                        removeMessages(MSG_FLUSH);
                        removeMessages(MSG_FORCE_FLUSH);
                        removeMessages(MSG_CLOSE);
                        writeLog(true);
                        android.util.Log.d(TAG, "flush log done");
                        break;
                    case MSG_CLOSE:
                        android.util.Log.d(TAG, "close log write");
                        closeWriter();
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

    private void closeWriter() {
        try {
            if (fileLogWriter != null) {
                logFileNo--;
                fileLogWriter.close();
            }
        } catch (Exception ignored) {
        } finally {
            fileLogWriter = null;
        }
    }

    private void writeLog(boolean forceFlush) {
        if (Log.sLogDir == null) return;

        if (fileLogWriter == null || reachLogFileSizeLimit()) {
            createLogWriter();
        }

        if (fileLogWriter != null && (sLogBuffer.position() > FLUSH_THRESHOLD || forceFlush)) {
            android.util.Log.d(TAG, "write len hex= " + sLogBuffer.toString());
            int n = sLogBuffer.position();
            byte[] temp = new byte[n];
            sLogBuffer.flip();
            sLogBuffer.get(temp);
            sLogBuffer.clear();
            byte mode = (byte) (publicKey == null? 0:1);
            if(mode == ENCRYPT_LOG) {
                writeEncryptedLog(temp);
            } else {
                writePlainLog(temp);
            }
        }
    }

    private void writePlainLog(byte[] temp) {
        byte[] len = intToBytes(temp.length);
        android.util.Log.d(TAG, "write len hex= " + bytesToHexString(len)
                + ",len = " + temp.length + ",log hex=" + bytesToHexString(temp));
        try {
            fileLogWriter.write(len);
            fileLogWriter.write(PLAIN_LOG);
            fileLogWriter.write(temp);
            fileLogWriter.flush();
        } catch (Exception e) {
            android.util.Log.e(TAG, "writePlainLog", e);
            fileLogWriter = null;
        }
    }

    private void writeEncryptedLog(byte[] temp) {
        byte[] iv = AesCbcCipher.genRandomBytes(16);
        byte[] key = AesCbcCipher.genRandomBytes(16);
        byte[] encryptedLog = temp;
        byte mode = ENCRYPT_LOG;
        try {
            encryptedLog = AesCbcCipher.encrypt(temp, key, iv);
        } catch (Exception e) {
            android.util.Log.e(TAG, "encrypt log error", e);
            mode = 0;
        }
        byte[] len = intToBytes(encryptedLog.length);

        android.util.Log.v(TAG, "mode = " + mode);
        android.util.Log.v(TAG, "iv = " + bytesToHexString(iv));
        android.util.Log.v(TAG, "key = " + bytesToHexString(key));
        android.util.Log.d(TAG, "write len hex= " + bytesToHexString(len)
                + ",len = " + encryptedLog.length + ",log hex=" + bytesToHexString(encryptedLog));
        byte[] encryptKey = RsaCipher.encrypt(key, publicKey);
        android.util.Log.d(TAG, "encryptKey.length = " + encryptKey.length);
        byte[] encryptIv = RsaCipher.encrypt(iv, publicKey);
        android.util.Log.d(TAG, "encryptIv.length = " + encryptIv.length);

        try {
            fileLogWriter.write(len);
            fileLogWriter.write(mode);
            fileLogWriter.write(encryptIv);
            fileLogWriter.write(encryptKey);
            fileLogWriter.write(encryptedLog);
            fileLogWriter.flush();

        } catch (Exception e) {
            android.util.Log.e(TAG, "writeLog", e);
            fileLogWriter = null;
        }
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
        handler.sendEmptyMessage(MSG_FORCE_FLUSH);
        handler.sendEmptyMessage(MSG_CLOSE);
        return super.quitSafely();
    }

    public void decryptLog() {
        DataInputStream in = null;
        FileOutputStream out = null;
        android.util.Log.d(TAG, "read file = " + currentLogFile.getPath());
        try {
            File txtLogFile = new File(currentLogFile.getAbsolutePath() + ".txt");
            in = new DataInputStream(new FileInputStream(currentLogFile));
            out = new FileOutputStream(txtLogFile);
            byte[] buf;
            byte[] iv = new byte[256];
            byte[] key = new byte[256];
            int n;
            for (; ; ) {
                int len;
                try {
                    len = in.readInt();
                } catch (Exception e) {
                    if (!(e instanceof EOFException)) {
                        android.util.Log.d(TAG, "readInt = ", e);
                    } else {
                        android.util.Log.i(TAG, "parse log end.");
                    }
                    break;
                }
                android.util.Log.d(TAG, "size = " + len);
                byte mode = in.readByte();
                android.util.Log.v(TAG, "mode = " + mode);
                if(mode == ENCRYPT_LOG) {
                    n = in.read(iv);
                    android.util.Log.v(TAG, "iv = " + bytesToHexString(iv));
                    if (n <= 0) {
                        return;
                    }
                    n = in.read(key);
                    android.util.Log.v(TAG, "iv = " + bytesToHexString(key));
                    if (n <= 0) {
                        return;
                    }
                }
                buf = new byte[len];
                n = in.read(buf);
                if (n != len) {
                    android.util.Log.e(TAG, "encrypted log length is not correct, read len=" + n + ",len=" + len);
                    break;
                }
                if (mode == ENCRYPT_LOG) {
                    byte[] decryptLog;
                    try {
                        decryptLog = AesCbcCipher.decrypt(buf, key, iv);
                        android.util.Log.v(TAG, "write decryptLog = " + bytesToHexString(decryptLog));
                        out.write(decryptLog);
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "decrypt log error", e);
                        android.util.Log.i(TAG, "decryptLog failed = " + bytesToHexString(buf));
                        out.write(buf);
                    }
                } else {
                    android.util.Log.v(TAG, "write plain = " + bytesToHexString(buf));
                    out.write(buf);
                }
            }
            out.flush();
        } catch (Exception e) {
            android.util.Log.e(TAG, "parse encrypted log error:", e);
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
