# PriorityBlockingQueue 

## 简介

PriorityBlockingQueue是与类PriorityQueue使用相同排序规则并且提供阻塞检索操作的无界阻塞队列。虽然此队列在逻辑上是无界的，但尝试添加元素时可能会失败，由于资源耗尽（导致OutOfMemoryError）。此队列不允许空的元素。依赖自然顺序排序的优先级队列也不允许插入不可比较的对象（这样做会导致ClassCastException）

## 源码解析

### 主要属性

```java
private static final int DEFAULT_INITIAL_CAPACITY = 11;//默认容量

//最大数组大小
private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

//存储元素的数组
private transient Object[] queue;

//元素个数
private transient int size;

//比较器
private transient Comparator<? super E> comparator;

//重入锁
private final ReentrantLock lock;

//非空条件
private final Condition notEmpty;

//扩容时用的原子控制变量
private transient volatile int allocationSpinLock;

//不阻塞的优先级队列
private PriorityQueue<E> q;	
```

### 构造方法

```java
public PriorityBlockingQueue() {//默认容量11
    this(DEFAULT_INITIAL_CAPACITY, null);
}


public PriorityBlockingQueue(int initialCapacity) {
    this(initialCapacity, null);//传入容量
}


public PriorityBlockingQueue(int initialCapacity,
                             Comparator<? super E> comparator) {
    if (initialCapacity < 1)
        throw new IllegalArgumentException();
    //初始化各种变量
    this.lock = new ReentrantLock();
    this.notEmpty = lock.newCondition();
    this.comparator = comparator;
    this.queue = new Object[initialCapacity];
}


public PriorityBlockingQueue(Collection<? extends E> c) {
    this.lock = new ReentrantLock();
    this.notEmpty = lock.newCondition();
    boolean heapify = true; // true if not known to be in heap order
    boolean screen = true;  // true if must screen for nulls
    if (c instanceof SortedSet<?>) {
        SortedSet<? extends E> ss = (SortedSet<? extends E>) c;
        this.comparator = (Comparator<? super E>) ss.comparator();
        heapify = false;
    }
    else if (c instanceof PriorityBlockingQueue<?>) {
        PriorityBlockingQueue<? extends E> pq =
            (PriorityBlockingQueue<? extends E>) c;
        this.comparator = (Comparator<? super E>) pq.comparator();
        screen = false;
        if (pq.getClass() == PriorityBlockingQueue.class) // exact match
            heapify = false;
    }
    Object[] a = c.toArray();
    int n = a.length;
    // If c.toArray incorrectly doesn't return Object[], copy it.
    if (a.getClass() != Object[].class)
        a = Arrays.copyOf(a, n, Object[].class);
    if (screen && (n == 1 || this.comparator != null)) {
        for (int i = 0; i < n; ++i)
            if (a[i] == null)
                throw new NullPointerException();
    }
    this.queue = a;
    this.size = n;
    if (heapify)
        heapify();
}
```

### 入队

```java
public boolean offer(E e) {
    if (e == null)
        throw new NullPointerException();
    final ReentrantLock lock = this.lock;
    lock.lock();//枷锁
    int n, cap;
    Object[] array;
    while ((n = size) >= (cap = (array = queue).length))
        //扩容
        tryGrow(array, cap);
    try {
        Comparator<? super E> cmp = comparator;
        //根据是否传入比较器选择不同的方法放元素
        if (cmp == null)
            siftUpComparable(n, e, array);
        else
            siftUpUsingComparator(n, e, array, cmp);
        size = n + 1;//元素个数+1
        notEmpty.signal();//唤醒条件
    } finally {
        lock.unlock();//解锁
    }
    return true;
}


   private static <T> void siftUpComparable(int k, T x, Object[] array) {
        Comparable<? super T> key = (Comparable<? super T>) x;
        while (k > 0) {
            //取父节点位置
            int parent = (k - 1) >>> 1;
            //取父节点元素
            Object e = array[parent];
            //如果待插入的元素key大于父节点，不用堆化
            if (key.compareTo((T) e) >= 0)
                break;
            //父节点和待插入元素互换，进行下一次比较
            array[k] = e;
            k = parent;
        }
        array[k] = key;//放入元素
    }
```

### 扩容

```java
private void tryGrow(Object[] array, int oldCap) {
    lock.unlock(); // 释放锁
    Object[] newArray = null;
    if (allocationSpinLock == 0 &&
        UNSAFE.compareAndSwapInt(this, allocationSpinLockOffset,
                                 0, 1)) {
        //CAS更新allocationSpinLock变量为1的线程获得扩容资格
        try {
            //旧容量小于64则翻倍，旧容量大于64则增加一半
            int newCap = oldCap + ((oldCap < 64) ?
                                   (oldCap + 2) : // grow faster if small
                                   (oldCap >> 1));
            //判断新容量是否溢出
            if (newCap - MAX_ARRAY_SIZE > 0) {    // possible overflow
                int minCap = oldCap + 1;
                if (minCap < 0 || minCap > MAX_ARRAY_SIZE)
                    throw new OutOfMemoryError();
                newCap = MAX_ARRAY_SIZE;
            }
            if (newCap > oldCap && queue == array)
                newArray = new Object[newCap];//建新数组
        } finally {
            allocationSpinLock = 0;
        }
    }
    if (newArray == null)
        Thread.yield(); // 其他线程才会进入到这里面让出CPU
    lock.lock();//枷锁
    if (newArray != null && queue == array) {
        queue = newArray;
        //并拷贝旧数组元素到新数组中
        System.arraycopy(array, 0, newArray, 0, oldCap);
    }
}
```

### 出队

```java
public E take() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();//枷锁
    E result;
    try {
        //队列没有元素就阻塞在notEmpty条件上
        while ( (result = dequeue()) == null)
            notEmpty.await();
    } finally {
        lock.unlock();
    }
    return result;//返回出队的元素
}
private E dequeue() {
        int n = size - 1;//元素个数-1
        if (n < 0)
            return null;
        else {//没有元素返回null
            Object[] array = queue;
            //堆顶元素
            E result = (E) array[0];
            //队尾元素
            E x = (E) array[n];
            array[n] = null;
            Comparator<? super E> cmp = comparator;
            //自上而下的堆化
            if (cmp == null)
                siftDownComparable(0, x, array, n);
            else
                siftDownUsingComparator(0, x, array, n, cmp);
            size = n;
            return result;
        }
    }
```