# <center>AtomicStampedReference</center>

本文基于jdk1.8

### 简介

AtomicStampedReference是java并发包下提供的一个原子类，它能解决其它原子类无法解决的ABA问题。 

### 源码解析

```java
// 将值和版本号封装到Pair中 通过比较Pair对象中的renference 和stamp来解决ABA问题
private static class Pair<T> {
    final T reference;
    final int stamp;
    private Pair(T reference, int stamp) {
        this.reference = reference;
        this.stamp = stamp;
    }
    static <T> Pair<T> of(T reference, int stamp) {
        return new Pair<T>(reference, stamp);
    }
}

private volatile Pair<V> pair;//多个线程同时修改这个pair要可见。

// 构造方法即显示存储的值和版本号既为Pair对象的两个属性
 public AtomicStampedReference(V initialRef, int initialStamp) {
        pair = Pair.of(initialRef, initialStamp);
    }
// 传入版本号数组对象 获取当前对象的实际值 无论传入的数组值为何
//总是会返回当前对象的值并且版本号数组的第一个值为当前对象的版本号
public V get(int[] stampHolder) {
        Pair<V> pair = this.pair;
        stampHolder[0] = pair.stamp;
        return pair.reference;
    }
// 实际调用的还是compareAndSet方法和之前分析UNsafe类相似，应该还是在jdk1..8后才会有实际的区别把
public boolean weakCompareAndSet(V   expectedReference,
                                     V   newReference,
                                     int expectedStamp,
                                     int newStamp) {
        return compareAndSet(expectedReference, newReference,
                             expectedStamp, newStamp);
    }

public boolean compareAndSet(V   expectedReference,
                                 V   newReference,
                                 int expectedStamp,
                                 int newStamp) {
        Pair<V> current = pair;
        return
            //只有当期望值版本号等于当前值和版本号时 才会执行更新
            expectedReference == current.reference &&
            expectedStamp == current.stamp &&
            //当当前版本和值等于要修改的版本和值时虽然返回true 但实际并未作更新 版本号也不会变化
            ((newReference == current.reference &&
              newStamp == current.stamp) ||
             //反之用新值及版本替换旧值和版本
             casPair(current, Pair.of(newReference, newStamp)));
    }

// 当版本号不一致时 new一个新的对将当前对象替换
 public boolean attemptStamp(V expectedReference, int newStamp) {
        Pair<V> current = pair;
        return
            expectedReference == current.reference &&
            (newStamp == current.stamp ||
             casPair(current, Pair.of(expectedReference, newStamp)));
    }



```