# LinkedTransferQueue 

## 简介

LinkedTransferQueue是一个由链表结构组成的无界阻塞TransferQueue队列。相对于其他阻塞队列，LinkedTransferQueue多了tryTransfer和transfer方法。

LinkedTransferQueue采用一种预占模式。意思就是消费者线程取元素时，如果队列不为空，则直接取走数据，若队列为空，那就生成一个节点（节点元素为null）入队，然后消费者线程被等待在这个节点上，后面生产者线程入队时发现有一个元素为null的节点，生产者线程就不入队了，直接就将元素填充到该节点，并唤醒该节点等待的线程，被唤醒的消费者线程取走元素，从调用的方法返回。我们称这种节点操作为“匹配”方式。

LinkedTransferQueue是ConcurrentLinkedQueue、SynchronousQueue（公平模式下转交元素）、LinkedBlockingQueue（阻塞Queue的基本方法）的超集。而且LinkedTransferQueue更好用，因为它不仅仅综合了这几个类的功能，同时也提供了更高效的实现。

## 源码分析

### 属性

```java
//放元素的几种方式
private static final int NOW   = 0; // 立即返回
private static final int ASYNC = 1; // 异步
private static final int SYNC  = 2; // 同步
private static final int TIMED = 3; // 超时
  transient volatile Node head;//头结点
    private transient volatile Node tail;//尾节点
```

### 内部类

```java
static final class Node {
    final boolean isData;   // 是否是数据节点
    volatile Object item;   // i元素值h
    volatile Node next;//下一节点
    volatile Thread waiter; // 持有元素的线程
    }
```

### 构造方法

```java

public LinkedTransferQueue() {
}

public LinkedTransferQueue(Collection<? extends E> c) {
    this();
    addAll(c);
}
```

### 入队

四个方法都是一样的，使用异步的方式调用xfer()方法，传入的参数都一模一样 

```java
public void put(E e) {
    xfer(e, true, ASYNC, 0);
}
public boolean offer(E e, long timeout, TimeUnit unit) {
        xfer(e, true, ASYNC, 0);
        return true;
    }
public boolean offer(E e) {
        xfer(e, true, ASYNC, 0);
        return true;
    }

    public boolean add(E e) {
        xfer(e, true, ASYNC, 0);
        return true;
    }
```

### 出队

出队的四个方法也是直接或间接的调用xfer()方法，放取元素的方式和超时规则略微不同，本质没有大的区别。 

```java
public E remove() {
    E x = poll();
    if (x != null)
        return x;
    else
        throw new NoSuchElementException();
}
 public E take() throws InterruptedException {
        E e = xfer(null, false, SYNC, 0);//同步
        if (e != null)
            return e;
        Thread.interrupted();
        throw new InterruptedException();
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        //有超时时间
        E e = xfer(null, false, TIMED, unit.toNanos(timeout));
        if (e != null || !Thread.interrupted())
            return e;
        throw new InterruptedException();
    }

    public E poll() {//立即返回
        
        return xfer(null, false, NOW, 0);
    }

```

### \*transfer*

```java
public boolean tryTransfer(E e) {
    return xfer(e, true, NOW, 0) == null;
}

/**
 * Transfers the element to a consumer, waiting if necessary to do so.
 *
 * <p>More precisely, transfers the specified element immediately
 * if there exists a consumer already waiting to receive it (in
 * {@link #take} or timed {@link #poll(long,TimeUnit) poll}),
 * else inserts the specified element at the tail of this queue
 * and waits until the element is received by a consumer.
 *
 * @throws NullPointerException if the specified element is null
 */
public void transfer(E e) throws InterruptedException {
    if (xfer(e, true, SYNC, 0) != null) {
        Thread.interrupted(); // failure possible only due to interrupt
        throw new InterruptedException();
    }
}

/**
 * Transfers the element to a consumer if it is possible to do so
 * before the timeout elapses.
 *
 * <p>More precisely, transfers the specified element immediately
 * if there exists a consumer already waiting to receive it (in
 * {@link #take} or timed {@link #poll(long,TimeUnit) poll}),
 * else inserts the specified element at the tail of this queue
 * and waits until the element is received by a consumer,
 * returning {@code false} if the specified wait time elapses
 * before the element can be transferred.
 *
 * @throws NullPointerException if the specified element is null
 */
public boolean tryTransfer(E e, long timeout, TimeUnit unit)
    throws InterruptedException {
    if (xfer(e, true, TIMED, unit.toNanos(timeout)) == null)
        return true;
    if (!Thread.interrupted())
        return false;
    throw new InterruptedException();
}
```

### xfer

```java
private E xfer(E e, boolean haveData, int how, long nanos) {
    if (haveData && (e == null))
        throw new NullPointerException();
    Node s = null;                        // the node to append, if needed
// 外层循环，自旋，失败就重试
    retry:
    for (;;) {                            
//从头节点开始尝试匹配，如果头节点被其它线程先一步匹配了，就再尝试其下一个，直到匹配到为止，或者到队列中没有元素为止
        for (Node h = head, p = h; p != null;) { // find & match first node
            boolean isData = p.isData;
            Object item = p.item;
            if (item != p && (item != null) == isData) { // p没有被匹配到
                if (isData == haveData) *// 如果两者模式一样，则不能匹配，跳出循环后尝试入队
                    break;
                if (p.casItem(item, e)) { //如果两者模式不一样，则尝试匹配,把p的值设置为e（如果是取元素则e是null，如果是放元素则e是元素值）
                    for (Node q = p; q != h;) {//匹配成功
                        Node n = q.next;  // update by 2 unless singleton
                        if (head == h && casHead(h, n == null ? q : n)) {
                            h.forgetNext();
                            break;
                        }                 // advance and retry
                        if ((h = head)   == null ||
                            (q = h.next) == null || !q.isMatched())
                            break;        // unless slack < 2
                    }
                    LockSupport.unpark(p.waiter);
                    return LinkedTransferQueue.<E>cast(item);
                }
            }
            Node n = p.next;
            p = (p != n) ? n : (h = head); // Use head if p offlist
        }
//就入队（不管放元素还是取元素都得入队
        if (how != NOW) {                 // No matches available
            if (s == null)
                //新建节点
                s = new Node(e, haveData);
            //尝试入队
            Node pred = tryAppend(s, haveData);
            if (pred == null)
                continue retry;           // 入队失败，重试
            if (how != ASYNC)//如果不是异步（同步或者有超时）
                return awaitMatch(s, pred, e, (how == TIMED), nanos);
        }
        return e; // not waiting
    }
}
 private Node tryAppend(Node s, boolean haveData) {
     //从尾节点开始遍历
        for (Node t = tail, p = t;;) {        // move p to last node and append
            Node n, u;                        // temps for reads of next & tail
            if (p == null && (p = head) == null) {//如果首尾都是null，说明链表中还没有元素
                if (casHead(null, s))//就让首节点指向s
                    return s;                 // initialize
            }
            else if (p.cannotPrecede(haveData))
                return null;                  // lost race vs opposite mode
            else if ((n = p.next) != null)    // not last; keep traversing
                p = p != t && t != (u = tail) ? (t = u) : // stale tail
                    (p != n) ? n : null;      // restart if off list
            else if (!p.casNext(null, s))
                p = p.next;                   // re-read on CAS failure
            else {
                if (p != t) {                 // update if slack now >= 2
                    while ((tail != t || !casTail(t, s)) &&
                           (t = tail)   != null &&
                           (s = t.next) != null && // advance and retry
                           (s = s.next) != null && s != t);
                }
                return p;
            }
        }
    }


    private E awaitMatch(Node s, Node pred, E e, boolean timed, long nanos) {
        final long deadline = timed ? System.nanoTime() + nanos : 0L;
        Thread w = Thread.currentThread();
        int spins = -1; // initialized after first item and cancel checks
        ThreadLocalRandom randomYields = null; // bound if needed

        for (;;) {
            Object item = s.item;
            if (item != e) {                  // matched
                // assert item != s;
                s.forgetContents();           // avoid garbage
                return LinkedTransferQueue.<E>cast(item);
            }
            if ((w.isInterrupted() || (timed && nanos <= 0)) &&
                    s.casItem(e, s)) {        // cancel
                unsplice(pred, s);
                return e;
            }

            if (spins < 0) {                  // establish spins at/near front
                if ((spins = spinsFor(pred, s.isData)) > 0)
                    randomYields = ThreadLocalRandom.current();
            }
            else if (spins > 0) {             // spin
                --spins;
                if (randomYields.nextInt(CHAINED_SPINS) == 0)
                    Thread.yield();           // occasionally yield
            }
            else if (s.waiter == null) {
                s.waiter = w;                 // request unpark then recheck
            }
            else if (timed) {
                nanos = deadline - System.nanoTime();
                if (nanos > 0L)
                    LockSupport.parkNanos(this, nanos);
            }
            else {
                LockSupport.park(this);
            }
        }
    }

```