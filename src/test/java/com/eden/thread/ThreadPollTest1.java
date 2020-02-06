package com.eden.thread;

import java.util.concurrent.*;

public class ThreadPollTest1 {
    public static void main(String[] args) {
        ExecutorService threadPoll=new ThreadPoolExecutor(5,10,1, TimeUnit.SECONDS,new ArrayBlockingQueue<>(5),
                Executors.defaultThreadFactory(),new RejectedExecutionHandler(){

            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                System.out.println(currentThreadName()+", 丢弃");
            }
        }
        );

        for(int i=0;i<20;i++){
            int num=i;
            threadPoll.execute(()->{
                try {
                    System.out.println(currentThreadName()+", "+num+"running, "+System.currentTimeMillis());
                            Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

    }
    private  static  String currentThreadName(){
        return    Thread.currentThread().getName();
    }
}
