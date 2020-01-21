# ArrayBlockingQueue

## 简介

ArrayBlockingQueue是数组实现线程安全的有界的阻塞队列

线程安全是指ArrayblockingQueue内部通过"互斥锁"保护竞争资源，实现了多线程对竞争资源的互斥访问，而有界则是指ArrayBlockingQueue对应的数组是有界限的。阻塞队列，是指多线程访问资源时，当竞争资源已被某线程访问时，其他要获取资源的线程需要阻塞等待。而且，ArrayBlockingQueue是按FIFO原则对元素进行排序，元素都是从尾部插入到队列，从头部开始返回。

## 源码分析

### 主要属性

```java
//使用数组存储元素
final Object[] items;

//取元素的指针
int takeIndex;

//放元素的指针
int putIndex;

//元素数量
int count;

//保证并发访问的锁
final ReentrantLock lock;

//非空条件
private final Condition notEmpty;

//非空条件
private final Condition notFull;
```

### 构造方法



```java
public ArrayBlockingQueue(int capacity) {//默认构造传入容量采用非公平锁
    this(capacity, false);
}


public ArrayBlockingQueue(int capacity, boolean fair) {
    if (capacity <= 0)
        throw new IllegalArgumentException();
    this.items = new Object[capacity];
    lock = new ReentrantLock(fair);
    notEmpty = lock.newCondition();
    notFull =  lock.newCondition();
}


public ArrayBlockingQueue(int capacity, boolean fair,
                          Collection<? extends E> c) {
    this(capacity, fair);

    final ReentrantLock lock = this.lock;
    lock.lock(); // Lock only for visibility, not mutual exclusion
    try {
        int i = 0;
        try {
            for (E e : c) {
                checkNotNull(e);
                items[i++] = e;
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException();
        }
        count = i;
        putIndex = (i == capacity) ? 0 : i;
    } finally {
        lock.unlock();
    }
}
```

### 入队

```java
public boolean add(E e) {
    return super.add(e);//调用父类的方法
}
 public boolean add(E e) {
        if (offer(e))//子类实现了offer
            return true;
        else
            throw new IllegalStateException("Queue full");
    }

public boolean offer(E e) {
        checkNotNull(e);//检查元素不能为空
        final ReentrantLock lock = this.lock;
        lock.lock();//枷锁
        try {
            //数组满了返回false
            if (count == items.length)
                return false;
            else {
                //入队
                enqueue(e);
                return true;
            }
        } finally {
            lock.unlock();//解锁
        }
    }
//元素加入队列
private void enqueue(E x) {
       
        final Object[] items = this.items;
        items[putIndex] = x;//将元素放在put指针上
        if (++putIndex == items.length)
            putIndex = 0;
        count++;
        notEmpty.signal();
    }
```