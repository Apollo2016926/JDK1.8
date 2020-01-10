package com.eden.arrays;

import java.lang.reflect.Field;
import java.util.ArrayDeque;

/**
 * @Description: TODO
 * @Author gexx
 * @Date 2020/1/10
 * @Version V1.0
 **/
public class ArrayDequeTest {
    public static void main(String[] args) throws Exception {
        ArrayDeque arrayDeque=new ArrayDeque();
        arrayDeque.addFirst(3);
        arrayDeque.addFirst(4);
        arrayDeque.addLast(5);
        arrayDeque.addLast(6);

        arrayDeque.pollFirst();
        arrayDeque.pollLast();

        System.out.println(arrayDeque);
    }
}
