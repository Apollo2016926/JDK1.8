# ThreadLocal

## 简介

ThreadLocal是一个本地线程副本变量工具类。主要用于将私有线程和该线程存放的副本对象做一个映射，各个线程之间的变量互不干扰，在高并发场景下，可以实现无状态的调用，特别适用于各个线程依赖不通的变量值完成操作的场景。

## 源码解析

ThreadLocal类提供如下几个核心方法： 

 ### get

```java
public T get() {
    //获取当前线程的ThreadLocalMap对象threadLocals
    Thread t = Thread.currentThread();
    ThreadLocalMap map = getMap(t);
    if (map != null) {
        //从map中获取线程存储的K-V Entry节点
        ThreadLocalMap.Entry e = map.getEntry(this);
        if (e != null) {
            @SuppressWarnings("unchecked")
            T result = (T)e.value;
            //返回从Entry节点获取存储的Value副本值
            return result;
        }
    }
    //map为空的话返回初始值null，即线程变量副本为null
    return setInitialValue();
} 
private T setInitialValue() {
        T value = initialValue();
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
        return value;
    }
protected T initialValue() {
    return null;
}
```

 ### set

```java
public void set(T value) {
    //获取当前线程的成员变量map
    Thread t = Thread.currentThread();
    ThreadLocalMap map = getMap(t);
    if (map != null)
        //重新将ThreadLocal和新的value副本放入到map中
        map.set(this, value);
    else
        //对线程的成员变量ThreadLocalMap进行初始化创建，并将ThreadLocal和value副本放入map中
        createMap(t, value);
}
ThreadLocalMap getMap(Thread t) {
    return t.threadLocals;
}

void createMap(Thread t, T firstValue) {
    t.threadLocals = new ThreadLocalMap(this, firstValue);
}
```

 

 

 