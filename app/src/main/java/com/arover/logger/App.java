package com.arover.logger;

import android.app.Application;

import com.arover.app.logger.Log;
import com.arover.app.logger.LoggerManager;

import androidx.annotation.NonNull;

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
        String publicKey = "30820122300d06092a864886f70d01010105000382010f003082010a0282010100da93fa2b7c060f189a9d6c536a9b71c2197b0423f6a296757478fbdfc7a807fcd9a0b70c80124b559230721647acf5c32533de2b8f70fb03d8ba46f5718d241471938d338ef6b7cb6a2d342c23d959decfa0f7a1d4e5869962d41fc1831d5717396d51308048f822af2e42741238c9b9718c2d68039dea1f082ac688970bc2d6a3591a8a0ea3f28bc09ff53d8fc955e4bb0b34cb143d21612cfc7319e0995a6f419282f8db3fcbf7956a91d5191e13b242d1d96e1b7349acc0194cbee43378812d5f931f47197d2c153ccf46754e47fcb8ba7be26a6e0d83ffbe1ec8e9c29af5ea59fa26083ae2b41aa4c928e2a678d9f7ff4234a3d8c7b9ef62e0fe8ffde6590203010001";
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
                .build()
                .deleteOldLogsDelayed(7);
        Log.d(TAG, "app is launching...");
    }

}
