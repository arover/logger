package com.arover.logger;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Process;

import com.arover.app.logger.Alog;
import com.arover.app.logger.LoggerManager;

import java.io.IOException;
import java.util.List;

import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.exceptions.UndeliverableException;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * @author MZY
 * created at 2021/3/4 10:45
 */

public class App extends Application {
    private static final String TAG = "App";

    @Override
    public void onCreate() {
        super.onCreate();

        flushLogWhenUncaughtError();
        // process' logs will be saved separately. eg: process_main, process_push
        String publicKey = "30820122300d06092a864886f70d01010105000382010f003082010a0282010100f43a186ef8ab08593b868ea4ec62addfc8a5a8fe02a4410d0d5d7bb7979f98036feea9dcca2d972f27a0d174469b01c8aa876e32f4e1109abec4b996340bd69d017c83402dbff9e5328a24a0230f15d6455f09c60038dfd916b28da0c928ae19be031aff2d5becac7066451b4e3e525141558216c51a84c2a1fa64a5a2ba5314f0eafa7ea3a6407507eb939ec022318802f4716e34ce3651d35e30a771a20ce12ef80aaa1b62ef7f867a145aab296cc00df36efe2b4fc69bb70b8b77d7748051ec41f9ac9ea602539fbb8b1db5c8481ee6450a2aaa7e8f046f0cd8b200c3af3cc3272c5cf72e18fccd0af0bbac6adcb088d0eb13df3c537ce29e2ffc0dee64250203010001";

        // 默认支持多进程app，各个进程的日志分别存放至不同文件夹，主进程存放至process_main, 其他进程存放至process_xxx目录中。
        new LoggerManager.Builder(this)
                .enableLogcat(BuildConfig.DEBUG) //是否开启logcat输出
                .level(LoggerManager.Level.DEBUG) //日志级别
                .encryptWithPublicKey(publicKey) //可选参数，是否加密，密钥为空或者null则不开启加密。生成密钥请构建app然后生成。
//                .rootFolder("Log") //可选参数，默认日志文件夹名为logs。
//                .processLogFolder(folderName) //可选参数，自定义各个进程的日志文件夹名。
                .build()
                .deleteOldLogsDelayed(7);


        addRxjavaDefaultErrHandler();
        Alog.d(TAG, "app is launching...");
    }

    private void flushLogWhenUncaughtError() {
        // flush file log buffer on caught UncaughtException.
        Thread.UncaughtExceptionHandler eh = Thread.getDefaultUncaughtExceptionHandler();
        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
            // save error to log and flush log buffer to file.
            Alog.wtf(TAG,"uncaught Exception",e);
            Alog.flush();
            if (eh != null) {
                eh.uncaughtException(t, e);
            }
        });
    }

    private void addRxjavaDefaultErrHandler() {
        // if your app using rxjava , please
        RxJavaPlugins.setErrorHandler(e -> {
            if (e instanceof UndeliverableException) {
                e = e.getCause();
            }
            if ((e instanceof IOException) ) {//or  (e instanceof SocketException)
                // fine, irrelevant network problem or API that throws on cancellation
                return;
            }
            if (e instanceof InterruptedException) {
                // fine, some blocking code was interrupted by a dispose call
                return;
            }
            Thread.UncaughtExceptionHandler handler = Thread.currentThread()
                    .getUncaughtExceptionHandler();

            if (e instanceof NullPointerException
                    || e instanceof IllegalArgumentException
                    || e instanceof IllegalStateException) {
                // that's likely a bug in the application
                if(handler != null) {
                    handler.uncaughtException(Thread.currentThread(), e);
                }
                return;
            }
            Alog.w(TAG,"rxjava: Undeliverable exception received, not sure what to do", e);
            //flush log buffer when
            Alog.flush();
        });
    }

    public @Nullable String getProcessName(Context ctx) {
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
        if (runningAppProcesses == null) {
            return null;
        }

        for (ActivityManager.RunningAppProcessInfo info : runningAppProcesses) {
            if (info.pid == Process.myPid()) {
                return info.processName;
            }
        }

        return null;
    }
}
