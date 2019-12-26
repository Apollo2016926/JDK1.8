
package com.eden.aqs;

/**
 * @Description: TODO
 * @Author gexx
 * @Date 2019/12/24
 * @Version V1.0
 **/
public class ValtielTest {

    public static  int finished = 0;

    private static void checkFined() {
        while (0 == finished) {
            System.out.println();
        }
        System.out.println("finished1111");
    }

    private static void finish() {
        finished = 1;
    }

    public static void main(String[] args)
            throws

            InterruptedException {
        new Thread(() -> checkFined()).start();
        Thread.sleep(1000);
        finish();
        System.out.println("main finished");

    }
}

