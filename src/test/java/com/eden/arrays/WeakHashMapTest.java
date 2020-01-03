package com.eden.arrays;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * @Description: TODO
 * @Author gexx
 * @Date 2020/1/3
 * @Version V1.0
 **/
public class WeakHashMapTest {
    public static void main(String[] args) {
        Map<String, Integer> map = new WeakHashMap<>();

        map.put(new String("1"), 1);
        map.put(new String("2"), 2);
        map.put(new String("3"), 3);

        map.put("4", 4);

        String key = null;
        for (String s : map.keySet()) {
            if (s.equals("3")) {
                key = s;
            }
        }

        System.out.println(map);

        System.gc();

        map.put(new String("5"),5);

        System.out.println(map);

        key=null;
        System.gc();

        System.out.println(map);
    }


}
