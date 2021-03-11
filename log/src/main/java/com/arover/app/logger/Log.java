package com.arover.app.logger;

import com.arover.app.logger.LoggerManager.Level;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;

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

    public static void dd(String tag, String format, Object ...args) {
        if (sLogLvl < Level.DEBUG.code)
            return;

        if(args == null || args.length == 0){
            d(tag,format);
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


}
