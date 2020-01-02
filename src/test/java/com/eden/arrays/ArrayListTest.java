package com.eden.arrays;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description: TODO
 * @Author gexx
 * @Date 2019/12/31
 * @Version V1.0
 **/
public class ArrayListTest {
    public static void main(String[] args) {
        List list = new ArrayList<>();
        List list1 = new ArrayList();
        list1.add("sss");
        list.add(1);
        list.add(1, 2);
        list.addAll(list1);

        //get(int index)方法
        list.get(2);
//remove(int index)方法
        list.remove(0);
        //remove(Object o)
//        Object removeObejct=2;
//        list.remove(removeObejct);
//        list.removeAll(list1);
        list.retainAll(list1);

        System.out.println(list.size());
        System.out.println(list);


    }
}
