package com.arover.logger;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Process;
import android.support.annotation.NonNull;

import com.arover.app.logger.Log;
import com.arover.app.logger.LoggerManager;

import java.util.List;

/**
 * @author MZY
 * created at 2021/3/4 10:45
 */

public class App extends Application {
    private static final String TAG = "App";

    @Override
    public void onCreate() {
        super.onCreate();
        //============= test purpose =========
        // do not code private key
        String privateKey = "308204be020100300d06092a864886f70d0101010500048204a8308204a40201000282010100da93fa2b7c060f189a9d6c536a9b71c2197b0423f6a296757478fbdfc7a807fcd9a0b70c80124b559230721647acf5c32533de2b8f70fb03d8ba46f5718d241471938d338ef6b7cb6a2d342c23d959decfa0f7a1d4e5869962d41fc1831d5717396d51308048f822af2e42741238c9b9718c2d68039dea1f082ac688970bc2d6a3591a8a0ea3f28bc09ff53d8fc955e4bb0b34cb143d21612cfc7319e0995a6f419282f8db3fcbf7956a91d5191e13b242d1d96e1b7349acc0194cbee43378812d5f931f47197d2c153ccf46754e47fcb8ba7be26a6e0d83ffbe1ec8e9c29af5ea59fa26083ae2b41aa4c928e2a678d9f7ff4234a3d8c7b9ef62e0fe8ffde65902030100010282010004a11ecac4bb7bf3f368b5e52d8ef9aef3709f44493a6fe9708449a10cd6c930db65a10c33b00a2bd79702803db7f85e65421c2df696e7f41b5c74f95e14853ba5c7aaf563cd3929916295762c08b3e8f3e366d744e8706c3cd352bc5bc94216d37580f89560fb98c0212b510f713d36d9f9d9722dbb1947860f623427f9d215afb9b3832442f08dcfb900ff3d85592aa3623db1795831a20f1e65ee83ffea75c68d1fb59c6073aa6294d0b34ecd1f4ebbe2775c68ef375502a307b6b1ef468319012c9fc64373ac431040d22236b82817886e1a700a806a19125592da40b106350e2bca9e12dc8b1aaf72d0f6973927a088edce1cc265a957eae09d33f58c8102818100ff2de133efa3182fce8296f1f3997ee31bcecca8c1fd0454b54f1efcc3d240a1c7620882cc9a41968cd94385d01cac1fec583f4a466d9d44072d43d48c1101549d672f5af9e3b12a1c586ea383b789aa2978492873e5815f1d0b115df43e489b15a5d3bc54639d1b2ebbf3784344e855027157e62cc1479630d10dbb5eaf088102818100db47f59429b366fd1e96de6c57f41c531fcccd9fdfdb201d4608c06e0f694d1dcda20484f709109769304bd5e9b487aa96017dc29cc911a8bfcccf0bee03b8a1f1628f88699a0c365694bd45f1e679127907c8e6e44f0f0eb3d1e90ece39819c7b5a6dc7fae5bc64c10cca846e02d222c29055af49be227125382984157f31d9028181008819664597083bbc33be7ed768ff73279a0b4028b9de42d31328b1f44a54f757d1c9bd94559fe85f6d9beb61914fd995e52e64032f710331e74a032577d7120899331194ca36e0a4ff1d43553ec943495878e93c0424624de265a6cb9f9b208a668f5d50d309961f8b5b7f4da3433b4f2bd05bfb4ddf8058e08c7f7071dd95010281802423a2ecad46bcf580821adc3e4ea4106b1044df51747ac178565ee884afb51ef151ff6eae8c16e8ed54215b7aeddee21560df8a206edef331d11e5a77fab306359329d6b098cbe474b684cf2f43edb646ae2ac52ae180b8ca9810d1f8d8ef6c8bc3e68debe2cafecbf640b63caf4854a43e4770e1356dd5b9ed9ca2012cfa5902818100f242057f47e70f0d9e57be464863c5fd0c3ae83e99d2d3c4c7949fe189bede740f45e1a8410ba8f948ad70e1fa37dbfee5293da02a54e56dda7cc0720e54a0aeea8d2085236ee7e3fdcb1e7f1cc17f7acdf80e4ec768ba82e9e318e65e088ffaf7afa9543aa9edb3e6ee0d44a1a3beb59be0e1aed7b89f9793d2ab504c6dcb49";
        // here , just
        //============= test purpose =========
        String publicKey = "30820122300d06092a864886f70d01010105000382010f003082010a0282010100da93fa2b7c060f189a9d6c536a9b71c2197b0423f6a296757478fbdfc7a807fcd9a0b70c80124b559230721647acf5c32533de2b8f70fb03d8ba46f5718d241471938d338ef6b7cb6a2d342c23d959decfa0f7a1d4e5869962d41fc1831d5717396d51308048f822af2e42741238c9b9718c2d68039dea1f082ac688970bc2d6a3591a8a0ea3f28bc09ff53d8fc955e4bb0b34cb143d21612cfc7319e0995a6f419282f8db3fcbf7956a91d5191e13b242d1d96e1b7349acc0194cbee43378812d5f931f47197d2c153ccf46754e47fcb8ba7be26a6e0d83ffbe1ec8e9c29af5ea59fa26083ae2b41aa4c928e2a678d9f7ff4234a3d8c7b9ef62e0fe8ffde6590203010001";
        Thread.UncaughtExceptionHandler eh = Thread.getDefaultUncaughtExceptionHandler();
        Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler(){

            @Override
            public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
                Log.flush();
                if(eh != null) {
                    eh.uncaughtException(t, e);
                }
            }
        });


        String processName = getCurrentProcessName(this, Process.myPid());

        boolean isMainProcess = getPackageName().equals(processName);

        String folderName = null;

        if(isMainProcess){
            folderName = "logs/logger_app_main";
        } else {
            if(processName != null && processName.contains(":")){
                String[] names = processName.split(":");
                if(names.length >= 2){
                    folderName = "logs/logger_app_" +names[1];
                }
            }
            if(folderName ==  null){
                folderName = "logs/logger_app_" + Process.myPid();
            }
        }

        new LoggerManager.Builder(this)
                .enableLogcat(BuildConfig.DEBUG)
                .level(BuildConfig.DEBUG ? LoggerManager.Level.VERBOSE: LoggerManager.Level.DEBUG)
                .publicKey(publicKey)
                .folder(folderName)
                .build()
                .deleteOldLogsDelayed(7);
        Log.d(TAG,"app is launching...");
    }

    public String getCurrentProcessName(Context ctx, int pid){
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
        if(runningAppProcesses == null){
            return null;
        }

        for(ActivityManager.RunningAppProcessInfo info : runningAppProcesses){
            if(info.pid == pid){
                return info.processName;
            }
        }

        return null;
    }
}
