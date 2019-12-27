package com.eden.aqs;

import java.util.concurrent.Phaser;

/**
 * @Description: PhaserTest
 * @Author gexx
 * @Date 2019/12/27
 * @Version V1.0
 **/
public class PhaserTest {
    public static final int PARTIES = 3;
    public static final int PHASES = 4;

    public static void main(String[] args) {
        Phaser phaser = new Phaser(PARTIES) {
            @Override
            protected boolean onAdvance(int phase, int registeredParties) {
                System.out.println("======phase" + phase + "finished=======");
                return super.onAdvance(phase, registeredParties);
            }
        };

        for (int i = 0; i < PARTIES; i++) {
            new Thread(() -> {
                for (int j = 0; j < PARTIES; j++) {
                    System.out.println(String.format("%s: phase:%d", Thread.currentThread().getName(), j));
                    phaser.arriveAndAwaitAdvance();
                }

            }, "Thread " + i).start();

        }
    }

}
