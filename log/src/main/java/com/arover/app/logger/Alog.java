package com.arover.app.logger;

import static com.arover.app.logger.LogWriterThread.MODE_ENCRYPT_LOG;
import static com.arover.app.util.DataUtil.bytesToHexString;

import android.content.Context;
import android.net.Uri;
import android.os.Process;

import androidx.annotation.Nullable;

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

/**
 * File storage logger
 * logger wrapper File storage File logger.
 */
public class Alog {

    private static final String TAG = "Alog";

    public static String rootDir;


    static LogWriterThread logWriterThread;
    private static int sLogLvl = Level.DEBUG.code;

    public static boolean sLogcatEnabled;
    public static String sLogLvlName;
    static String sLogDir;
    static boolean sInitialized;

    volatile static OnLogCompressDoneListener onLogCompressListener;


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
    static void init(LoggerManager manager) {
        setLogDir(manager.getLogDirFullPath());
        setLogLevel(manager.getLevel());
        sLogcatEnabled = manager.enableLogcat();

        if (logWriterThread != null) {
            logWriterThread.quit();
        }

        logWriterThread = new LogWriterThread(manager);
        logWriterThread.start();

        android.util.Log.i(TAG, "initialized... level=" + sLogLvlName + ",lvl="
                + sLogLvl + ",Logcat Enabled=" + sLogcatEnabled + ",dir=" + manager
                .getLogDirFullPath());
    }

    public static String getRootDir() {
        return rootDir;
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
        String logmsg = msg == null ? "null":msg;
        if (sLogcatEnabled) android.util.Log.wtf(tag, logmsg);
        writeToFile(tag + " " + logmsg, Level.FATAL);
    }

    public static void wtf(String tag, String msg, Throwable t) {
        String trace = getTrace(t);
        if (msg == null)
            f(tag, trace);
        else
            f(tag, msg + "\n" + trace);
        flush();
    }

    public static void wtf(String tag, String msg) {
        String log = msg == null ? "null":msg;
        if (sLogcatEnabled) android.util.Log.wtf(tag, log);
        writeToFile(tag + " " + log, Level.FATAL);
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

        String logmsg = msg == null ? "null":msg;
        if (sLogcatEnabled) android.util.Log.e(tag, logmsg);

        writeToFile(tag + " " + logmsg, Level.ERROR);
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

        String logmsg = msg == null ? "null":msg;
        if (sLogcatEnabled) android.util.Log.w(tag, logmsg);

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
        String logmsg = msg == null ? "null":msg;
        if (sLogcatEnabled) android.util.Log.i(tag, logmsg);
        writeToFile(tag + " " + logmsg, Level.INFO);
    }

    public static void d(String tag, Throwable t) {
        if (sLogLvl < Level.DEBUG.code)
            return;
        d(tag, "", t);
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
        String logmsg = msg == null ? "null":msg;
        if (sLogcatEnabled) android.util.Log.d(tag, logmsg);
        writeToFile(tag + " " + logmsg, Level.DEBUG);
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
        String logmsg = msg == null ? "null":msg;
        if (sLogcatEnabled) android.util.Log.v(tag, logmsg);

        writeToFile(tag + " " + logmsg, Level.VERBOSE);
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
        sw.write(tr.getMessage()+"\n");
        tr.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    /**
     * 强制切换至新的日志文件， 以便将当前所有日志打包上传。
     * force logger switch to new file to write.
     * 此方法里面的逻辑将直接isCompressing状态置为true。
     */
    public static void switchToNewFile() {
        i(TAG,"switchToNewFile.");
        if(logWriterThread != null)
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
                if (mode == MODE_ENCRYPT_LOG) {
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
                if (mode == MODE_ENCRYPT_LOG) {
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
     *
     * @return previousFogFileName or NULL!!
     */
    public static String getPreviousLogFileName() {
        return logWriterThread.getPreviousLogFileName();
    }

    /**
     *  get log folder name by process name,
     * @param context
     * @param prefix optional, nullable, default is "app_" , main process log will
     *               saved in "app_main", process "xxx" 's log will saved in
     *               "app_xxx" etc.
     *
     */
    public static String getLogFolderByProcess(Context context,
                                               @Nullable String processName,
                                               @Nullable String prefix) {

        boolean isMainProcess = context.getPackageName().equals(processName) || processName == null;
        StringBuilder defaultPrefix = new StringBuilder();
        if(prefix != null){
            defaultPrefix.append(prefix);
        } else {
            defaultPrefix.append("app_");
        }
        StringBuilder folderName = null;
        if(isMainProcess){
            folderName = defaultPrefix.append("main");
        } else {
            if(processName.contains(":")){
                String[] names = processName.split(":");
                if(names.length >= 2){
                    folderName = defaultPrefix.append(names[1]);
                }
            }
            if(folderName ==  null){
                folderName = defaultPrefix.append(Process.myPid());
            }
        }
        return folderName.toString();
    }

    public static void setOnLogCompressListener(OnLogCompressDoneListener onLogCompressListener) {
        Alog.onLogCompressListener = onLogCompressListener;
    }

}
