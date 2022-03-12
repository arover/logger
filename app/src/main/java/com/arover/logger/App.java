package com.arover.logger;

import android.app.Application;

import com.arover.app.logger.Alog;
import com.arover.app.logger.LoggerManager;
import com.arover.app.util.ProcessUtil;

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
        // 默认支持多进程app，各个进程的日志分别存放至不同文件夹，主进程存放至process_main, 其他进程存放至process_xxx目录中。
        new LoggerManager.Builder(this)
                .enableLogcat(BuildConfig.DEBUG) //是否开启logcat输出
                .level(LoggerManager.Level.DEBUG) //日志级别
//                .encryptWithPublicKey(publicKey) //可选参数，是否加密，密钥为空或者null则不开启加密。生成密钥请构建app然后生成。
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
            //
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

}
