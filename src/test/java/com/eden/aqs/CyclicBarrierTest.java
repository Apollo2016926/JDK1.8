package com.eden.aqs;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * @Description: TODO
 * @Author gexx
 * @Date 2019/12/27
 * @Version V1.0
 **/
public class CyclicBarrierTest {


    public static void main(String[] args) {

        CyclicBarrier cyclicBarrier = new CyclicBarrier(3);

        for (int i = 0; i < 3; i++) {
            new Thread(() -> {

                try {
                    System.out.println("ready...");
                    cyclicBarrier.await();
                    System.out.println("go..");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                }

            }).start();
        }
    }
}
