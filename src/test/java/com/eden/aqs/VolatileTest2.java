package com.eden.aqs;


import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @Description: TODO
 * @Author gexx
 * @Date 2019/12/24
 * @Version V1.0
 **/
public class VolatileTest2 {
    private static Config config = null;
    private static volatile boolean inistialized = false;

    public static void main(String[] args) {
        new Thread(() -> {
            config = new Config();
            config.name = "config";
            inistialized = true;
        }).start();

        new Thread(() -> {
            while (!inistialized) {
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));
            }
            String name = config.name;
        }).start();
    }


    static class Config {

        String name;
    }
}
