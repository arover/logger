package com.arover.logger;

import android.app.Application;

import com.arover.app.logger.Log;
import com.arover.app.logger.LogExecutor;
import com.arover.app.logger.LoggerManager;

import androidx.annotation.NonNull;
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
        String publicKey = "30820122300d06092a864886f70d01010105000382010f003082010a0282010100cf9d8c3a47e7e6268e12d87f4eb09ff503fbb41dfa78e1e473e636967e3998dbb6e74e363f7a241d5b994359c3c134b2f1e4f9e6af197137e921b3870f9c0d798790d00f0b7e1eab6aa0965b971dca362de9b0d38d53cf78b6203a28210e00521c143fe230c387edb5a9868e58b60a871906793bc5cc288dbeb740963d844121e571622080ba6c40df1f9e22a8ccd76837e3e74d8f4c6a693b8200db29227268503071bc976f979bbec2c8666ef535d4b4a5eb479fc139e24c7046c75bff1a73eacf8d0c9136ad0afff73911f7049c1aa4b8af1867c1a2a45707e6f2c35e36ff955ee50d500e415854432e4960ca88a70111de72eb96848e7e452a07c45f17f50203010001";
        Thread.UncaughtExceptionHandler eh = Thread.getDefaultUncaughtExceptionHandler();
        Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
                Log.flush();
                if (eh != null) {
                    eh.uncaughtException(t, e);
                }
            }
        });


        //save logs of processes separately.
        String folderName = Log.getLogFolderByProcess(this, "logs/log_demo_");

        new LoggerManager.Builder(this)
                .enableLogcat(BuildConfig.DEBUG)
                .level(BuildConfig.DEBUG ? LoggerManager.Level.VERBOSE : LoggerManager.Level.DEBUG)
                // disable encryption in debug build.
                .encryptWithPublicKey(publicKey)
                .folder(folderName)
                //use rxjava perform io tasks to avoid create new thread.
                .logTaskExecutor(new LogExecutor(){
                    @Override
                    public void execute(Runnable runnable) {
                        Schedulers.io().scheduleDirect(runnable);
                    }
                })
                .build()
                .deleteOldLogsDelayed(7);
        Log.d(TAG, "app is launching...");
    }

}
