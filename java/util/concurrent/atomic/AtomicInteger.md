# < center>AtomicInteger源码分析< /center>

本文基于jdk1.8

### 简介

Atomic包是[Java](http://lib.csdn.net/base/17).util.concurrent下的另一个专门为线程安全设计的Java包，包含多个原子操作类。这个包里面提供了一组原子变量类。其基本的特性就是在多线程环境下，当有多个线程同时执行这些类的实例包含的方法时，具有排他性，即当某个线程进入方法，执行其中的指令时，不会被其他线程打断，而别的线程就像自旋锁一样，一直等到该方法执行完成，才由JVM从等待队列中选择一个另一个线程进入，这只是一种逻辑上的理解。实际上是借助硬件的相关指令来实现的，不会阻塞线程(或者说只是在硬件级别上阻塞了)。可以对基本数据、数组中的基本数据、对类中的基本数据进行操作。原子变量类相当于一种泛化的volatile变量，能够支持原子的和有条件的读-改-写操作 

### 源码解析

+ 初始化

```java
// 拿到Unsafe实例
private static final Unsafe unsafe = Unsafe.getUnsafe();
private static final long valueOffset;//变量value的内存偏移量
//通过unsafe获取AtomicInteger类下value字段的内存偏移量
static {
    try {
        valueOffset = unsafe.objectFieldOffset
            (AtomicInteger.class.getDeclaredField("value"));
    } catch (Exception ex) { throw new Error(ex); }
}

private volatile int value;// volatile修饰的变量value 保证内存可见、防止指令重排
```

+ 构造函数

```java
// value=initialValue
public AtomicInteger(int initialValue) {
    value = initialValue;
}

//无参构造默认value=0
public AtomicInteger() {
}
```



+ set() get()

```java
// 返回value
public final int get() {
    return value;
}

// 将传入的值替换value
public final void set(int newValue) {
    value = newValue;
}
```

```java
//最终把值设置为newValue，使用该方法后，其他线程在一段时间内还会获取到旧值
public final void lazySet(int newValue) {
    unsafe.putOrderedInt(this, valueOffset, newValue);
}
//设置为newValue 并返回旧值
public final int getAndSet(int newValue) {
    return unsafe.getAndSetInt(this, valueOffset, newValue);
}
```

```java
//jdk1.8时这两个方法底层调用的是同一个方法 所以认为并无区别  查阅资料到了1.9 weakCompareAndSet才有特殊实现
//如果调用此方法对象的值等于expect，则设置值为update并返回true;反之不修改返回false
public final boolean compareAndSet(int expect, int update) {
    return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
}

public final boolean weakCompareAndSet(int expect, int update) {
    return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
}
```

```java
// 返回原值并将当前对象的value+1
public final int getAndIncrement() {
        return unsafe.getAndAddInt(this, valueOffset, 1);
    }
// 返回原值并将当前对象的value+ -1
    public final int getAndDecrement() {
        return unsafe.getAndAddInt(this, valueOffset, -1);
    }

  // 返回原值并将当前对象的value+delta
    public final int getAndAdd(int delta) {
        return unsafe.getAndAddInt(this, valueOffset, delta);
    }

   // 将当前对象的value+1 并返回计算后的结果
    public final int incrementAndGet() {
        return unsafe.getAndAddInt(this, valueOffset, 1) + 1;
    }
// 将当前对象的value+-1 并返回计算后的结果
    public final int decrementAndGet() {
        return unsafe.getAndAddInt(this, valueOffset, -1) - 1;
    }

   // 将当前对象的value+delta 并返回计算后的结果
    public final int addAndGet(int delta) {
        return unsafe.getAndAddInt(this, valueOffset, delta) + delta;
    }

// 1.8新增方法，更新当前值，返回以前的值
    public final int getAndUpdate(IntUnaryOperator updateFunction) {
        int prev, next;
        do {
            prev = get();
            next = updateFunction.applyAsInt(prev);
        } while (!compareAndSet(prev, next));
        return prev;
    }

// 1.8新增方法，更新当前值，返回更新后的值
    public final int updateAndGet(IntUnaryOperator updateFunction) {
        int prev, next;
        do {
            prev = get();
            next = updateFunction.applyAsInt(prev);
        } while (!compareAndSet(prev, next));
        return next;
    }

// 1.8新增方法，更新当前值，返回以前的值
    public final int getAndAccumulate(int x,
                                      IntBinaryOperator accumulatorFunction) {
        int prev, next;
        do {
            prev = get();
            next = accumulatorFunction.applyAsInt(prev, x);
        } while (!compareAndSet(prev, next));
        return prev;
    }

  // 1.8新增方法，更新当前值，返回更新后的值
    public final int accumulateAndGet(int x,
                                      IntBinaryOperator accumulatorFunction) {
        int prev, next;
        do {
            prev = get();
            next = accumulatorFunction.applyAsInt(prev, x);
        } while (!compareAndSet(prev, next));
        return next;
    }

```