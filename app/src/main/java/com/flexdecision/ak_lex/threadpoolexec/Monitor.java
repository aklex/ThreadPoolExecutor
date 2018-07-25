package com.flexdecision.ak_lex.threadpoolexec;

import java.util.concurrent.ThreadPoolExecutor;

public class Monitor implements Runnable {
    private CustomThreadPoolManager manager;
    private int seconds;
    private boolean run = true;

    public Monitor(CustomThreadPoolManager manager, int seconds) {
        this.manager = manager;
        this.seconds = seconds;
    }

    public boolean getStatus(){
        return run;
    }
    public void shutdown(){
        run = false;
    }

    @Override
    public void run() {
        while (run){
            ThreadPoolExecutor executor = (ThreadPoolExecutor) manager.getmExecutorService();
            System.out.println(
                    String.format("[monitor] [%d/%d] Active: %d, Completed: %d, Task: %d, isShutdown: %s, isTerminated: %s",
                            executor.getPoolSize(),
                            executor.getCorePoolSize(),
                            executor.getActiveCount(),
                            executor.getCompletedTaskCount(),
                            executor.getTaskCount(),
                            executor.isShutdown(),
                            executor.isTerminated()));
            try {
                Thread.sleep(seconds*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if( executor.getActiveCount() == 0){
                shutdown();
                manager.finished();
            }

        }
    }
}
