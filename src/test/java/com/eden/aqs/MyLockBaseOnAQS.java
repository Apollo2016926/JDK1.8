package com.eden.aqs;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.stream.IntStream;

/**
 * @Description: TODO
 * @Author gexx
 * @Date 2019/12/26
 * @Version V1.0
 **/
public class MyLockBaseOnAQS {
    //定义一个同步器，实现AQS
    private static class Sysc extends AbstractQueuedSynchronizer {
        //实现tryAcquire(int arg)
        @Override
        protected boolean tryAcquire(int arg) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        //实现 tryRelease(int arg)
        @Override
        protected boolean tryRelease(int arg) {
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }
    }

    //声明同步器
    private final Sysc sysc = new Sysc();

    //加锁
    public void lock() {
        sysc.acquire(1);
    }

    //解锁
    public void unlock() {
        sysc.release(1);
    }

    private static int count = 0;


    public static void main(String[] args) throws InterruptedException {
        MyLockBaseOnAQS lock = new MyLockBaseOnAQS();
        CountDownLatch countDownLatch = new CountDownLatch(3);
        IntStream.range(0, 3).forEach(i -> new Thread(() ->
        {
            lock.lock();
            try {
                IntStream.range(0, 1000).forEach(j -> {
                    count++;
                });
            } finally {
                lock.unlock();
            }
            countDownLatch.countDown();
        }, "tt-" + i).start());
        countDownLatch.await();
        System.out.println(count);
    }
}
