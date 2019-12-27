package com.eden.aqs;

import java.util.concurrent.CountDownLatch;

/**
 * @Description: TODO
 * @Author gexx
 * @Date 2019/12/27
 * @Version V1.0
 **/
public class CountDownLatchTest {

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch statrtSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(5);

        for (int i = 0; i < 5; i++) {
            new Thread(() -> {

                try {
                    System.out.println("Aid thread is waiting for starting..");
                    statrtSignal.await();
                    System.out.println("Aid thread is doing something.");
                    doneSignal.countDown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }).start();
        }
        Thread.sleep(2000);
        System.out.println("Main thread is doing something...");
        statrtSignal.countDown();
        System.out.println("Main thread is waiting for aid threads finishing...");
        doneSignal.await();
        System.out.println("Main thread is doing something after all threads have finished...");

    }
}
