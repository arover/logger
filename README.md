# File Logger for android

this is a file logger written in java without fancy features.

## Usage

```java

import com.arover.app.logger.Log;

public class App extends Application {
    static final String TAG = "App";
    @Override
    public void onCreate() {
        super.onCreate();
        
        String processName = getProcessName(this);
        //save logs of processes separately.
        //if your app is single process app, simply set processLogFolder as ""
        String folderName = Log.getLogFolderByProcess(this, processName,  "log_demo_");

        new LoggerManager.Builder(this)
                .enableLogcat(BuildConfig.DEBUG)
                .level(BuildConfig.DEBUG ? LoggerManager.Level.VERBOSE : LoggerManager.Level.DEBUG)
                // disable encryption in debug build.
                .encryptWithPublicKey(publicKey)
                // logs' root folder name, it's path is /sdcard/Android/data/[applicationId]/files/logs
                .rootFolder("logs")
                // process's log folder name(in root folder)
                //it's path is /sdcard/Android/data/[applicationId]/files/logs/log_demo_main/
                .processLogFolder(folderName)
                //use rxjava perform io tasks to avoid create new thread.
                .logTaskExecutor(new LogExecutor(){
                    @Override
                    public void execute(Runnable runnable) {
                        Schedulers.io().scheduleDirect(runnable);
                    }
                })
                .build()
                .deleteOldLogsDelayed(7);
        // use it like android.util.Log.
        Log.d(TAG,"app is launching...");
    }
}
```

