package com.eden.aqs;

import sun.awt.windows.ThemeReader;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Description: TODO
 * @Author gexx
 * @Date 2019/12/27
 * @Version V1.0
 **/
public class SemaphoreTest {
    private static final Semaphore semaphore = new Semaphore(100);
    private static final AtomicInteger fail = new AtomicInteger(0);
    private static final AtomicInteger success = new AtomicInteger(0);

    public static void main(String[] args) {
        for (int i = 0; i < 1000; i++) {
            new Thread(() -> seckill()).start();
        }
    }

    private static boolean seckill() {
        if (!semaphore.tryAcquire()) {
            System.out.println("no permits,count=" + fail.incrementAndGet());
            return false;
        }
        try {
            Thread.sleep(2000);
            System.out.println("success,count=" + success.incrementAndGet());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            semaphore.release();
        }


        return true;
    }
}
