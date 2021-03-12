package com.arover.logger;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Process;

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
