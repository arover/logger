package com.arover.app.logger;

import android.content.Context;
import android.net.Uri;
import android.os.Process;

import com.arover.app.util.DataUtil;
import com.arover.app.crypto.AesCbcCipher;
import com.arover.app.crypto.RsaCipher;
import com.arover.app.logger.LoggerManager.Level;
import com.arover.app.util.IoUtil;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;

import static com.arover.app.util.DataUtil.bytesToHexString;
import static com.arover.app.logger.LogWriterThread.ENCRYPT_LOG;

/**
 * File storage logger
 * logger wrapper File storage File logger.
 */
public class Log {

    private static final String TAG = "Log";

    public static File rootDir;
    public static String crashLogDir;

    static LogWriterThread logWriterThread;
    private static int sLogLvl = Level.DEBUG.code;

    public static boolean sLogcatEnabled;
    public static String sLogLvlName;
    static String sLogDir;
    static boolean sInitialized;
    private static LogExecutor logGenerator;


    public static void flush() {
        if (logWriterThread != null) {
            logWriterThread.flushBuffer();
        }
    }

    private static void setLogLevel(Level level) {
        sLogLvlName = level.name();
        sLogLvl = level.code;
    }

    private static void setLogDir(String dir) {
        sLogDir = dir;
    }

    /**
     * init file logger with level and debug mode.
     */
    public static void init(LoggerManager manager) {
        setLogDir(manager.getLogDirFullPath());
        setLogLevel(manager.getLevel());
        sLogcatEnabled = manager.enableLogcat();
        logGenerator = manager.logExecutor;

        if (logWriterThread != null) {
            logWriterThread.quit();
        }

        logWriterThread = new LogWriterThread(manager);
        logWriterThread.start();

        android.util.Log.i(TAG, "initialized... level=" + sLogLvlName + ",lvl="
                + sLogLvl + ",Logcat Enabled=" + sLogcatEnabled + ",dir=" + manager
                .getLogDirFullPath());
    }

    public static File getRootDir() {
        return rootDir;
    }

    public static String getCrashLogDir() {
        return crashLogDir;
    }

    public static boolean isInitialized() {
        return sInitialized;
    }

    public static void f(String tag, Throwable t) {
        f(tag, "", t);
    }

    public static void f(String tag, String msg, Throwable t) {
        String trace = getTrace(t);
        if (msg == null)
            f(tag, trace);
        else
            f(tag, msg + "\n" + trace);
    }

    public static void f(String tag, String msg) {

        if (sLogcatEnabled) android.util.Log.wtf(tag, msg);
        writeToFile(tag + " " + msg, Level.FATAL);
    }

    public static void e(String tag, Throwable t) {
        if (sLogLvl < Level.ERROR.code)
            return;
        e(tag, "", t);
    }

    public static void e(String tag, String msg, Throwable t) {
        if (sLogLvl < Level.ERROR.code) return;

        String trace = getTrace(t);

        if (msg == null)
            e(tag, trace);
        else
            e(tag, msg + "\n" + trace);
    }

    public static void e(String tag, String msg) {
        if (sLogLvl < Level.ERROR.code) return;

        if (sLogcatEnabled) android.util.Log.e(tag, msg);

        writeToFile(tag + " " + msg, Level.ERROR);
    }

    public static void w(String tag, Throwable t) {
        w(tag, "", t);
    }

    public static void w(String tag, String msg, Throwable t) {
        if (sLogLvl < Level.WARN.code) return;

        String trace = getTrace(t);
        if (msg == null)
            w(tag, trace);
        else
            w(tag, msg + "\n" + trace);
    }

    public static void w(String tag, String msg) {
        if (sLogLvl < Level.WARN.code) return;

        if (sLogcatEnabled) android.util.Log.w(tag, msg);

        writeToFile(tag + " " + msg, Level.WARN);
    }

    public static void i(String tag, Throwable t) {
        if (sLogLvl < Level.INFO.code)
            return;
        i(tag, "", t);
    }

    public static void i(String tag, String msg, Throwable t) {
        if (sLogLvl < Level.INFO.code)
            return;

        String trace = getTrace(t);
        if (msg == null)
            i(tag, trace);
        else
            i(tag, msg + "\n" + trace);
    }

    public static void i(String tag, String msg) {
        if (sLogLvl < Level.INFO.code) {
            return;
        }

        if (sLogcatEnabled) android.util.Log.i(tag, msg);
        writeToFile(tag + " " + msg, Level.INFO);
    }

    public static void d(String tag, Throwable t) {
        if (sLogLvl < Level.DEBUG.code)
            return;
        d(tag, "", t);
    }

    public static void dd(String tag, String format, Object... args) {
        if (sLogLvl < Level.DEBUG.code)
            return;

        if (args == null || args.length == 0) {
            d(tag, format);
            return;
        }

        LogStringGenerator runnable = new LogStringGenerator(Level.DEBUG, tag, format, args);
        logGenerator.execute(runnable);
    }

    public static void d(String tag, String msg, Throwable t) {
        if (sLogLvl < Level.DEBUG.code) return;

        String trace = getTrace(t);
        if (msg == null)
            d(tag, trace);
        else
            d(tag, msg + "\n" + trace);
    }

    public static void d(String tag, String msg) {
        if (sLogLvl < Level.DEBUG.code) return;
        if (sLogcatEnabled) android.util.Log.d(tag, msg);
        writeToFile(tag + " " + msg, Level.DEBUG);
    }

    public static void v(String tag, Throwable t) {
        if (sLogLvl < Level.VERBOSE.code) return;
        v(tag, "", t);
    }

    public static void v(String tag, String msg, Throwable t) {
        if (sLogLvl < Level.VERBOSE.code)
            return;

        String trace = getTrace(t);
        if (msg == null)
            v(tag, trace);
        else
            v(tag, msg + "\n" + trace);
    }

    public static void v(String tag, String msg) {
        if (sLogLvl < Level.VERBOSE.code) return;
        if (sLogcatEnabled) android.util.Log.v(tag, msg);

        writeToFile(tag + " " + msg, Level.VERBOSE);
    }


    private static void writeToFile(String log, Level level) {
        if (logWriterThread != null) {
            logWriterThread.writeLog(level, log);
        } else {
            android.util.Log.i(TAG,
                    "writeToFile: logWriterThread not init yet, messages are discard.");
        }
    }

    private static String getTrace(Throwable tr) {
        if (tr == null) return "";

        // This is to reduce the amount of log spew that apps do in the non-error
        // condition of the network being unavailable.
        Throwable t = tr;
        while (t != null) {
            if (t instanceof UnknownHostException) {
                return "";
            }
            t = t.getCause();
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        tr.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    public static void compressAllLogs() {
        logWriterThread.flushAndStartCompressTask();
    }

    public static void decryptLogFile(Context context, Uri uri, byte[] privateKey) throws Exception {
        DataInputStream in = null;
        FileOutputStream out = null;
        android.util.Log.d(TAG, "read file = " + uri.getPath());
        try {
            File txtLogFile = new File(context.getExternalFilesDir(null),"/output.log");
//            File txtLogFile = new File(uri.getAbsolutePath() + ".txt");
            in = new DataInputStream(context.getContentResolver().openInputStream(uri));
            out = new FileOutputStream(txtLogFile);
            byte[] buf;
            byte[] iv = new byte[LogWriterThread.ENCRYPT_IV_LEN];
            byte[] key = new byte[LogWriterThread.ENCRYPT_KEY_LEN];
            int n;
            for (; ; ) {
                int len;
                try {
                    len = in.readInt();
                } catch (Exception e) {
                    if (!(e instanceof EOFException)) {
                        android.util.Log.d(TAG, "readInt = ", e);
                        throw new IllegalStateException("read log length data error.",e);
                    } else {
                        android.util.Log.i(TAG, "parse log end.");
                    }
                    break;
                }
                android.util.Log.d(TAG, "size = " + len);
                if(len > LogWriterThread.BUFFER_SIZE){
                    android.util.Log.e(TAG, "invalid log length, are you sure log file is right??? = " + len);
                    return;
                }
                byte mode = in.readByte();
                android.util.Log.v(TAG, "mode = " + mode);
                if (mode == ENCRYPT_LOG) {
                    n = in.read(iv);
                    android.util.Log.v(TAG, "iv = " + bytesToHexString(iv));
                    if (n != iv.length) {
                        throw new IllegalStateException("read iv data error n != iv.length.");
                    }
                    n = in.read(key);
                    android.util.Log.v(TAG, "iv = " + bytesToHexString(key));
                    if (n != key.length) {
                        throw new IllegalStateException("read key data error n != key.length.");
                    }
                }
                buf = new byte[len];
                n = in.read(buf);
                if (n != len) {
                    android.util.Log.e(TAG, "encrypted log length is not correct, read len=" + n + ",len=" + len);
                    throw new IllegalStateException("read log data error, read log length is not equals len.");
                }
                if (mode == ENCRYPT_LOG) {
                    byte[] decryptLog;
                    try {
                        byte[] decryptKey = RsaCipher.decrypt(key, privateKey);
                        byte[] decryptIv = RsaCipher.decrypt(iv, privateKey);

//                        android.util.Log.d(TAG, "decryptKey" + bytesToHexString(decryptKey));
//                        android.util.Log.d(TAG, "decryptKey" + bytesToHexString(decryptIv));

                        decryptLog = AesCbcCipher.decrypt(buf, decryptKey, decryptIv);

//                        android.util.Log.v(TAG, "write decryptLog = " + bytesToHexString(decryptLog));

                        out.write(decryptLog);
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "decrypt log error", e);
                        android.util.Log.i(TAG, "decryptLog failed = " + bytesToHexString(buf));
                        out.write(buf);
                    }
                } else {
//                    android.util.Log.v(TAG, "write plain = " + bytesToHexString(buf));
                    out.write(buf);
                }
            }
            out.flush();

        } catch (Exception e) {
            android.util.Log.e(TAG, "parse encrypted log error:", e);
            throw e;
        } finally {
            IoUtil.closeQuietly(in);
            IoUtil.closeQuietly(out);
        }

    }

    public static File getCurrentLogFile() {
        return logWriterThread.getCurrentFile();
    }

    /**
     *  get log folder name by process name,
     * @param context
     * @param prefix optional, nullable, default is "logs/app_" , main process log will
     *               saved in "logs/app_main", process "xxx" 's log will saved in
     *               "logs/app_xxx" etc.
     *               you can set it like "Log/app", Log is parent folder name , app is process log
     *               folder prefix
     *
     */
    public static String getLogFolderByProcess(Context context, String prefix) {
        String processName = DataUtil.getCurrentProcessName(context, Process.myPid());
        boolean isMainProcess = context.getPackageName().equals(processName);
        String defaultPrefix = "logs/app_";
        if(prefix != null){
            defaultPrefix = prefix;
        }
        String folderName = null;
        if(isMainProcess){
            folderName = defaultPrefix + "main";
        } else {
            if(processName != null && processName.contains(":")){
                String[] names = processName.split(":");
                if(names.length >= 2){
                    folderName = defaultPrefix +names[1];
                }
            }
            if(folderName ==  null){
                folderName = defaultPrefix + Process.myPid();
            }
        }
        return folderName;
    }
}
