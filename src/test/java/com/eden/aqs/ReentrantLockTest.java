package com.eden.aqs;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Description: TODO
 * @Author gexx
 * @Date 2019/12/26
 * @Version V1.0
 **/
public class ReentrantLockTest {
    public static void main(String[] args) throws Exception {
        //声明一个重入锁
        ReentrantLock lock = new ReentrantLock();
        //声明一个条件锁
        Condition condition = lock.newCondition();

        new Thread(() -> {
            lock.lock();//1

            System.out.println("before await");//2

            try {
                condition.await();//3
                System.out.println("after await");//10
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();//11
            }


        }).start();


        try {
            Thread.sleep(1000);
            lock.lock();//4
            Thread.sleep(2000);//5

            System.out.println("before sigal ");//6

// 通知条件已成立
            condition.signal();//7
            System.out.println("after sigal");//8
        } finally {
            lock.unlock();
        }
    }
}
