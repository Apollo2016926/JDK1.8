# LinkedBlockingQueue

## 简介

LinkedBlockingQueue是由链表实现的阻塞队列，按照源码注释中的说法可以是“无界"的（如果一开始没有指定热容量大小，则为Integer的最大），也可以指定大小，元素按照FIFO的形式来访问，队列头部为待的时间最久的元素，尾部则最少，新元素插在尾部。大多数情况下，链表实现的阻塞队列比数组实现的队列具有更高的吞吐量，这是因为像ArrayBlockingQueue这样底层是由数组实现的阻塞队列在取值和插入的时候会锁住整个array，而LinkedBlockingQueue在实现时对于取和插这两个不同的操作采用了不同的锁进行，但是在多线程环境下也有可能产生各种不可预料的执行后果。

## 源码解析

### 主要属性

```java
private final int capacity;//容量

//元素的个数
private final AtomicInteger count = new AtomicInteger();

//链表头
transient Node<E> head;

//链表尾
private transient Node<E> last;

//取锁
private final ReentrantLock takeLock = new ReentrantLock();

/** Wait queue for waiting takes */
private final Condition notEmpty = takeLock.newCondition();

//放锁
private final ReentrantLock putLock = new ReentrantLock();

/** Wait queue for waiting puts */
private final Condition notFull = putLock.newCondition();
```

### 内部类

```java
static class Node<E> {
    E item;

 
    Node<E> next;

    Node(E x) { item = x; }
}
```

### 构造方法

```java
public LinkedBlockingQueue() {
    this(Integer.MAX_VALUE);//没传容量默认取Integer.MAX_VALUE
}


public LinkedBlockingQueue(int capacity) {
    if (capacity <= 0) throw new IllegalArgumentException();
    this.capacity = capacity;//传入初始容量
    last = head = new Node<E>(null);//初始化head，last节点
}

//
public LinkedBlockingQueue(Collection<? extends E> c) {
    this(Integer.MAX_VALUE);
    final ReentrantLock putLock = this.putLock;
    putLock.lock(); // Never contended, but necessary for visibility
    try {
        int n = 0;
        for (E e : c) {
            if (e == null)
                throw new NullPointerException();
            if (n == capacity)
                throw new IllegalStateException("Queue full");
            enqueue(new Node<E>(e));
            ++n;
        }
        count.set(n);
    } finally {
        putLock.unlock();
    }
}
```

### 入队

```java
public void put(E e) throws InterruptedException {
    if (e == null) throw new NullPointerException();
    // Note: convention in all put/take/etc is to preset local var
    // holding count negative to indicate failure unless set.
    int c = -1;
    Node<E> node = new Node<E>(e);
    final ReentrantLock putLock = this.putLock;
    final AtomicInteger count = this.count;
    putLock.lockInterruptibly();//put锁
    try {
        //
        while (count.get() == capacity) {
            notFull.await();//如果队列满了，就阻塞在notFull条件上，等待被其它线程唤醒
        }
        enqueue(node);//队列不满，入队
        c = count.getAndIncrement();//队列长度加1
        if (c + 1 < capacity)
            notFull.signal();
    } finally {
        putLock.unlock();
    }
    if (c == 0)
        signalNotEmpty();//如果原队列长度为0，现在加了一个元素后立即唤醒notEmpty条件
}
 private void enqueue(Node<E> node) {
        // 直接加到last后面
        last = last.next = node;
    }

```

### 出队

```java
public E take() throws InterruptedException {
    E x;
    int c = -1;
    final AtomicInteger count = this.count;
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lockInterruptibly();
    try {
        while (count.get() == 0) {
            notEmpty.await();//如果队列无元素，则阻塞在notEmpty条件上
        }
        x = dequeue();//出队
        c = count.getAndDecrement();// 获取出队前队列的长度
        if (c > 1)
            notEmpty.signal();//如果取之前队列长度大于1，则唤醒notEmpty
    } finally {
        takeLock.unlock();
    }
    if (c == capacity)//如果取之前队列长度等于容量,唤醒notFull
        signalNotFull();
    return x;
}

  private E dequeue() {
        // head节点本身是不存储任何元素的，这里把head删除，并把head下一个节点作为新的值，并把其值置空，返回原来的值
        Node<E> h = head;
        Node<E> first = h.next;
        h.next = h; // help GC
        head = first;
        E x = first.item;
        first.item = null;
        return x;
    }
```