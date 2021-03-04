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
        
        new LoggerManager.Builder(this)
                //enable logcat or not
                .enableLogcat(BuildConfig.DEBUG)
                // config level
                .level(BuildConfig.DEBUG ? LoggerManager.Level.VERBOSE: LoggerManager.Level.DEBUG)
                // set log folder name , log saved in external files dir, 
                // eg: /sdcard/Android/data/com.your.package.name/files/log/2021-02-02-0.log
                .folder("log")
                .build()
                // start old log delete task. 
                .deleteOldLogs(7);
        // use it like android.util.Log.
        Log.d(TAG,"app is launching...");
    }
}
```

