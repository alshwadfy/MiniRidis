package com.miniRedis.store;

public class ExpirySweeper implements Runnable {
    
    private final Store store;
    private final int intervalSeconds;
    private volatile boolean running = false;
    private Thread thread;
    public ExpirySweeper(Store store, int intervalSeconds) {
        this.store = store;
        this.intervalSeconds = intervalSeconds;
    }
    @Override
    public void run() {
        while(running){
            try {
                Thread.sleep(intervalSeconds*1000);
                store.sweepExpired();
            } catch (InterruptedException e) {
                break;
            }
        }
    }


    public void stop(){
        running=false;
        if(thread != null)
            thread.interrupt();
    }

    public void start(){
        running=true;
        thread = Thread.ofVirtual().name("ExpirySweeper").start(this);
    }
}
