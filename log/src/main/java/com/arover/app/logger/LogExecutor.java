package com.arover.app.logger;

/**
 * executor for large list object log string generation.
 * @author minstrel
 * created at 2020/12/16 10:39
 */
public abstract class LogExecutor {
    /**
     *
     * @param runnable log generator
     */
    public abstract void execute(Runnable runnable);

    /**
     * clean up action
     */
    public void tearDown(){}
}
