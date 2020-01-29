# DelayQueue

## 简介

一个实现PriorityBlockingQueue实现延迟获取的无界队列，在创建元素时，可以指定多久才能从队列中获取当前元素。只有延时期满后才能从队列中获取元素。（DelayQueue可以运用在以下应用场景：1.缓存系统的设计：可以用DelayQueue保存缓存元素的有效期，使用一个线程循环查询DelayQueue，一旦能从DelayQueue中获取元素时，表示缓存有效期到了。2.定时任务调度。使用DelayQueue保存当天将会执行的任务和执行时间，一旦从DelayQueue中获取到任务就开始执行，从比如TimerQueue就是使用DelayQueue实现的。

## 源码解析

### 主要属性

```java
private final transient ReentrantLock lock = new ReentrantLock();
private final PriorityQueue<E> q = new PriorityQueue<E>();//优先级队列
//用于标记当前是否有线程在排队
private Thread leader = null;
//条件
private final Condition available = lock.newCondition();
```

### 入队

```java
 
public boolean offer(E e) {
    final ReentrantLock lock = this.lock;
    lock.lock();//枷锁
    try {
        q.offer(e);//添加元素到优先级队列中
        if (q.peek() == e) {
            //如果添加的元素是堆顶元素
            //就把leader置为空，并唤醒等待在条件available上的线程
            leader = null;
            available.signal();
        }
        return true;
    } finally {
        lock.unlock();
    }
}
```

### 出队

```java
public E poll() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        E first = q.peek();
        //检查第一个元素，如果为空或者还没到期，就返回null
        if (first == null || first.getDelay(NANOSECONDS) > 0)
            return null;
        else
            return q.poll();//如果第一个元素到期了就调用优先级队列的poll()弹出第一个元素
    } finally {
        lock.unlock();
    }
}
```