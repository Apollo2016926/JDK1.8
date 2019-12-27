# <center>CyclicBarrier</center>

### 简介

栅栏类似于闭锁，它能阻塞一组线程直到某个事件的发生。栅栏与闭锁的关键区别在于，所有的线程必须同时到达栅栏位置，才能继续执行。闭锁用于等待事件，而栅栏用于等待其他线程。
CyclicBarrier可以使一定数量的线程反复地在栅栏位置处汇集。当线程到达栅栏位置时将调用await方法，这个方法将阻塞直到所有线程都到达栅栏位置。如果所有线程都到达栅栏位置，那么栅栏将打开，此时所有的线程都将被释放，而栅栏将被重置以便下次使用。

### 源码分析

#### 主要属性

```java
//重入锁
private final ReentrantLock lock = new ReentrantLock();
//条件锁
private final Condition trip = lock.newCondition();
//需要等待的线程数量
private final int parties;
//唤醒是执行的命令
private final Runnable barrierCommand;
//代
private Generation generation = new Generation();
//当前这一代线程还需要等待的线程数
private int count;
```

#### 构造器

```java
public CyclicBarrier(int parties) {
    this(parties, null);
}
public CyclicBarrier(int parties, Runnable barrierAction) {
        if (parties <= 0) throw new IllegalArgumentException();
        this.parties = parties;
        this.count = parties;
        this.barrierCommand = barrierAction;
    }
```

#### 主要内部类

用于控制CyclicBarrier的循环使用 

```java
private static class Generation {
    boolean broken = false;
}
```

#### await

```java
public int await() throws InterruptedException, BrokenBarrierException {
    try {
        return dowait(false, 0L);
    } catch (TimeoutException toe) {
        throw new Error(toe); // cannot happen
    }
}

private int dowait(boolean timed, long nanos)
        throws InterruptedException, BrokenBarrierException,
               TimeoutException {
        final ReentrantLock lock = this.lock;
        lock.lock();//加锁
        try {
            //当前代 broken=false 
            final Generation g = generation;

            if (g.broken)
                throw new BrokenBarrierException();

            if (Thread.interrupted()) {
                breakBarrier();
                throw new InterruptedException();
            }

            int index = --count;
            if (index == 0) {  // 如果数量减到0了，走这段逻辑（最后一个线程走这里）
                boolean ranAction = false;
                try {
                    // 如果初始化的时候传了命令，这里执行
                    final Runnable command = barrierCommand;
                    if (command != null)
                        command.run();
                    ranAction = true;
                    nextGeneration();
                    return 0;
                } finally {
                    if (!ranAction)
                        breakBarrier();
                }
            }

            // 这个循环只有非最后一个线程可以走
            for (;;) {
                try {
                    if (!timed)
                        trip.await();//调用condition的await()方法
                    else if (nanos > 0L)
                        nanos = trip.awaitNanos(nanos);// 超时等待方法
                } catch (InterruptedException ie) {
                    if (g == generation && ! g.broken) {
                        breakBarrier();
                        throw ie;
                    } else {
                        // We're about to finish waiting even if we had not
                        // been interrupted, so this interrupt is deemed to
                        // "belong" to subsequent execution.
                        Thread.currentThread().interrupt();
                    }
                }

                if (g.broken)
                    throw new BrokenBarrierException();
// 正常来说这里肯定不相等,因为上面打破栅栏的时候调用nextGeneration()方法时generation的引用已经变化了
                if (g != generation)
                    return index;

                if (timed && nanos <= 0L) {
                    breakBarrier();
                    throw new TimeoutException();
                }
            }
        } finally {
            lock.unlock();
        }
    }
```