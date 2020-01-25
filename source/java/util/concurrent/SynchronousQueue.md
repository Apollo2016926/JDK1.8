#  

## 简介

SynchronousQueue是一个比较特殊的阻塞队列实现类，它实际上并不会给元素维护一系列存储空间，它维护了一组线程，当尝试调用添加元素的方法时，必须要有另外一个线程尝试取出这些元素，否则就无法将元素"添加"进去。在生产-消费者模型中，这样做的好处是可以降低从生产者到消费者中间的延迟，而在LinkedBlockingQueue或者ArrayBlockingQueue中，生产者必须将元素插入到其中存储元素的结构中然后才能转交给消费者。这个类在Java并发工具包中用作CacheThreadPool的任务队列：

```java
public static ExecutorService newCachedThreadPool() {
    return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                  60L, TimeUnit.SECONDS,
                                  new SynchronousQueue<Runnable>());
}
```

简单地来说，SynchronousQueue有以下几大特性：
1、SynchronousQueue和传统的集合类不同，它的内部没有恒定的存储元素的空间。每一个添加元素的操作都必须要有另外一个线程进行取出操作，否则添加元素操作不会成功，反之亦然。
2、因为内部没有恒定的存储元素的空间，所以SynchronousQueue不支持调用isEmpty、size、remainingCapacity、clear、contains、remove、toArray等方法，也不支持元素的迭代操作。
3、和ReentrantLock类似，它支持公平和非公平模式

## 源码分析

### 主要属性

```java
static final int NCPUS = Runtime.getRuntime().availableProcessors();//CPU数量

//有超时的情况自旋32次 CPU小于2颗不自旋
static final int maxTimedSpins = (NCPUS < 2) ? 0 : 32;

//没有超时的情况自旋16次
static final int maxUntimedSpins = maxTimedSpins * 16;

//针对有超时的情况，自旋了多少次后，如果剩余时间大于1000纳秒就使用带时间的LockSupport.parkNanos这个方法
static final long spinForTimeoutThreshold = 1000L;
//传输器，两个线程交换元素使用的工具
private transient volatile Transferer<E> transferer;
```

### 主要内部类

传输器

```java
abstract static class Transferer<E> {//
  
    abstract E transfer(E e, boolean timed, long nanos);
}
```

传输器的栈实现

```java
static final class TransferStack<E> extends Transferer<E> {
    //消费者
    static final int REQUEST    = 0;
    //生产者
    static final int DATA       = 1;
    //二者正在匹配中
    static final int FULFILLING = 2;
	volatile SNode head;//栈的头结点
    //栈中的节点
    static final class SNode {
        volatile SNode next;        // 下一个节点
        volatile SNode match;       // 匹配者
        volatile Thread waiter;     // 等待的线程
        Object item;                // 元素s
        int mode;//模式，也就是节点的类型，是消费者，是生产者，还是正在匹配中
    }
}
```

传输器的队列实现

```java
static final class TransferQueue<E> extends Transferer<E> {
   //队列头
        transient volatile QNode head;
        //队列尾
        transient volatile QNode tail;
 

    /** Node class for TransferQueue. */
    static final class QNode {
        volatile QNode next;          // 下一个节点
        volatile Object item;         // 存储的元素
        volatile Thread waiter;       // t等待的线程
        final boolean isData;//是否是数据节点
    }
```
### 构造方法

```java
public SynchronousQueue() {
    this(false);//默认非公平模式即使用栈
}

//公平模式使用队列，反之使用栈
public SynchronousQueue(boolean fair) {
    transferer = fair ? new TransferQueue<E>() : new TransferStack<E>();
}
```

### 入队

```java
public void put(E e) throws InterruptedException {
    if (e == null) throw new NullPointerException();//非空
    if (transferer.transfer(e, false, 0) == null) {
        Thread.interrupted();
        throw new InterruptedException();
    }
}
```

### 出队

```java
public E take() throws InterruptedException {
    E e = transferer.transfer(null, false, 0);
    if (e != null)
        return e;
    Thread.interrupted();
    throw new InterruptedException();
}

```

基于栈的传输

```java
E transfer(E e, boolean timed, long nanos) {
   

    SNode s = null; // constructed/reused as needed
    int mode = (e == null) ? REQUEST : DATA;

    for (;;) {
        SNode h = head;
        if (h == null || h.mode == mode) {  // empty or same-mode
            if (timed && nanos <= 0) {      // can't wait
                if (h != null && h.isCancelled())
                    casHead(h, h.next);     // pop cancelled node
                else
                    return null;
            } else if (casHead(h, s = snode(s, e, h, mode))) {
                SNode m = awaitFulfill(s, timed, nanos);
                if (m == s) {               // wait was cancelled
                    clean(s);
                    return null;
                }
                if ((h = head) != null && h.next == s)
                    casHead(h, s.next);     // help s's fulfiller
                return (E) ((mode == REQUEST) ? m.item : s.item);
            }
        } else if (!isFulfilling(h.mode)) { // try to fulfill
            if (h.isCancelled())            // already cancelled
                casHead(h, h.next);         // pop and retry
            else if (casHead(h, s=snode(s, e, h, FULFILLING|mode))) {
                for (;;) { // loop until matched or waiters disappear
                    SNode m = s.next;       // m is s's match
                    if (m == null) {        // all waiters are gone
                        casHead(s, null);   // pop fulfill node
                        s = null;           // use new node next time
                        break;              // restart main loop
                    }
                    SNode mn = m.next;
                    if (m.tryMatch(s)) {
                        casHead(s, mn);     // pop both s and m
                        return (E) ((mode == REQUEST) ? m.item : s.item);
                    } else                  // lost match
                        s.casNext(m, mn);   // help unlink
                }
            }
        } else {                            // help a fulfiller
            SNode m = h.next;               // m is h's match
            if (m == null)                  // waiter is gone
                casHead(h, null);           // pop fulfilling node
            else {
                SNode mn = m.next;
                if (m.tryMatch(h))          // help match
                    casHead(h, mn);         // pop both h and m
                else                        // lost match
                    h.casNext(m, mn);       // help unlink
            }
        }
    }
}
```