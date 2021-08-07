package com.arover.logger;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Process;

import com.arover.app.logger.Log;
import com.arover.app.logger.LogExecutor;
import com.arover.app.logger.LoggerManager;

import java.io.IOException;
import java.net.SocketException;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.exceptions.UndeliverableException;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * @author MZY
 * created at 2021/3/4 10:45
 */

public class App extends Application {
    private static final String TAG = "App";

    @Override
    public void onCreate() {
        super.onCreate();
        // change this to your public key, and save your private key
        // to Android/data/com.arover.logger/files/log_private.key
        // you can generate encryption keys by build this app.
        String publicKey = "30820122300d06092a864886f70d01010105000382010f003082010a0282010100cf9d8c3a47e7e6268e12d87f4eb09ff503fbb41dfa78e1e473e636967e3998dbb6e74e363f7a241d5b994359c3c134b2f1e4f9e6af197137e921b3870f9c0d798790d00f0b7e1eab6aa0965b971dca362de9b0d38d53cf78b6203a28210e00521c143fe230c387edb5a9868e58b60a871906793bc5cc288dbeb740963d844121e571622080ba6c40df1f9e22a8ccd76837e3e74d8f4c6a693b8200db29227268503071bc976f979bbec2c8666ef535d4b4a5eb479fc139e24c7046c75bff1a73eacf8d0c9136ad0afff73911f7049c1aa4b8af1867c1a2a45707e6f2c35e36ff955ee50d500e415854432e4960ca88a70111de72eb96848e7e452a07c45f17f50203010001";

        flushLogWhenUncaughtError();
        //flush log when an undeliverable error occurs
        addRxjavaDefaultErrHandler();
        //multi process support.
        String processName = getProcessName(this);
        //save logs of processes separately.
        String folderName = Log.getLogFolderByProcess(this, processName,  "log_demo_");

        new LoggerManager.Builder(this)
                //required.
                .enableLogcat(BuildConfig.DEBUG)
                //required.
                .level(BuildConfig.DEBUG ? LoggerManager.Level.VERBOSE : LoggerManager.Level.DEBUG)
                //required. log folder name of app scope storage.
                .rootFolder("logs")
                //optional. disable encryption in debug build.
                .encryptWithPublicKey(publicKey)
                //optional. if your app is single process app, simply set processLogFolder as ""
                .processLogFolder(folderName) //
                //optional.set rxjava io scheduler for perform background io tasks execution to
                // avoid new thread creation.
                .logTaskExecutor(new LogExecutor(){
                    @Override
                    public void execute(Runnable runnable) {
                        Schedulers.io().scheduleDirect(runnable);
                    }
                })
                .build()
                // delete old logs
                .deleteOldLogsDelayed(7);
        Log.d(TAG, "app is launching...");
    }

    private void flushLogWhenUncaughtError() {
        // flush file log buffer on caught UncaughtException.
        Thread.UncaughtExceptionHandler eh = Thread.getDefaultUncaughtExceptionHandler();
        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
            // flush log buffer on caught ab UncaughtException.
            Log.flush();
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
            Log.w(TAG,"rxjava: Undeliverable exception received, not sure what to do", e);
            //flush log buffer when
            Log.flush();
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
