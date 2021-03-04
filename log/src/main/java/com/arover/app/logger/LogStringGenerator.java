package com.arover.app.logger;

import com.arover.app.logger.Log;
import com.arover.app.logger.LoggerManager;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author minstrel
 * created at 2020/12/16 10:49
 */

public class LogStringGenerator implements Runnable{
    private final String format;
    private final Object[] args;
    private final String tag;
    private final LoggerManager.Level level;

    @SuppressWarnings("rawtypes")
    public LogStringGenerator(LoggerManager.Level level, String tag, String format, Object[] originArgs) {
        this.level = level;
        this.tag = tag;
        this.format = format;
        args = new Object[originArgs.length];
        System.arraycopy(originArgs,0, args,0, args.length);
        for(int i = args.length -1; i>=0; i--){
            if(args[i] instanceof Collection){
                args[i] = Arrays.asList(((Collection)args[i]).toArray());
            }
        }
    }

    @Override
    public void run() {
        String logString = tag + " " +String.format(format, args);
        Log.logWriterThread.writeLog(level, logString);
        if(Log.sLogcatEnabled)
            android.util.Log.d(tag,logString);
    }
}
