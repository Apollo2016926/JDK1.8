# ThreadPoolExecutor 

## 简介

使用线程池主要为了解决一下几个问题：

- 通过重用线程池中的线程，来减少每个线程创建和销毁的性能开销。
- 对线程进行一些维护和管理，比如定时开始，周期执行，并发数控制等等。

## 源码解析

### 主要属性

```java
private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
private static final int COUNT_BITS = Integer.SIZE - 3;
private static final int CAPACITY   = (1 << COUNT_BITS) - 1;

// runState is stored in the high-order bits
private static final int RUNNING    = -1 << COUNT_BITS;
private static final int SHUTDOWN   =  0 << COUNT_BITS;
private static final int STOP       =  1 << COUNT_BITS;
private static final int TIDYING    =  2 << COUNT_BITS;
private static final int TERMINATED =  3 << COUNT_BITS;

// 线程池的转台
private static int runStateOf(int c)     { return c & ~CAPACITY; }
//线程池中工作线程的数量
private static int workerCountOf(int c)  { return c & CAPACITY; }
//计算tcl的值，等于运行状态加上线程数量
private static int ctlOf(int rs, int wc) { return rs | wc; }
```

### 构造函数

```java
public ThreadPoolExecutor(int corePoolSize,
                          int maximumPoolSize,
                          long keepAliveTime,
                          TimeUnit unit,
                          BlockingQueue<Runnable> workQueue) {
    this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
         Executors.defaultThreadFactory(), defaultHandler);
}


public ThreadPoolExecutor(int corePoolSize,
                          int maximumPoolSize,
                          long keepAliveTime,
                          TimeUnit unit,
                          BlockingQueue<Runnable> workQueue,
                          ThreadFactory threadFactory) {
    this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
         threadFactory, defaultHandler);
}


public ThreadPoolExecutor(int corePoolSize,
                          int maximumPoolSize,
                          long keepAliveTime,
                          TimeUnit unit,
                          BlockingQueue<Runnable> workQueue,
                          RejectedExecutionHandler handler) {
    this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
         Executors.defaultThreadFactory(), handler);
}

public ThreadPoolExecutor(int corePoolSize,//核心线程数
                          int maximumPoolSize,//最大线程数
                          long keepAliveTime,//线程保持空闲时间
                          TimeUnit unit,//单位
                          BlockingQueue<Runnable> workQueue,//任务队列
                          ThreadFactory threadFactory,//线程工厂
                          RejectedExecutionHandler handler //拒绝策略) {
    if (corePoolSize < 0 ||
        maximumPoolSize <= 0 ||
        maximumPoolSize < corePoolSize ||
        keepAliveTime < 0)
        throw new IllegalArgumentException();
    if (workQueue == null || threadFactory == null || handler == null)
        throw new NullPointerException();
    this.corePoolSize = corePoolSize;
    this.maximumPoolSize = maximumPoolSize;
    this.workQueue = workQueue;
    this.keepAliveTime = unit.toNanos(keepAliveTime);
    this.threadFactory = threadFactory;
    this.handler = handler;
}
```

### 线程池状态

#### RUNNING 

创建线程池的时候会初始化ctl，初始化为RUNNING状态

```java
private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
```

#### SHUTDOWN 

```java
public void shutdown() {
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        checkShutdownAccess();
        //修改为SHUTDOWN
        advanceRunState(SHUTDOWN);
        //标记线程为中断状态
        interruptIdleWorkers();
        onShutdown(); // hook for ScheduledThreadPoolExecutor
    } finally {
        mainLock.unlock();
    }
    tryTerminate();
}
 
    private void advanceRunState(int targetState) {
        for (;;) {
            int c = ctl.get();
            //如果状态大于SHUTDOWN，或者修改为SHUTDOWN成功了，才会break跳出自旋
            if (runStateAtLeast(c, targetState) ||
                ctl.compareAndSet(c, ctlOf(targetState, workerCountOf(c))))
                break;
        }
    }
```

#### STOP

```java
public List<Runnable> shutdownNow() {
    List<Runnable> tasks;
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        checkShutdownAccess();
        //修改为STOP状态
        advanceRunState(STOP);// 
// 标记所有线程为中断状态
        interruptWorkers();
        tasks = drainQueue();
    } finally {
        mainLock.unlock();
    }
    tryTerminate();
    return tasks;
}
```

#### TIDYING 

```java
final void tryTerminate() {
    for (;;) {
        int c = ctl.get();
        if (isRunning(c) ||//运行中
            runStateAtLeast(c, TIDYING) ||//状态的值比TIDYING还大，也就是TERMINATED
            (runStateOf(c) == SHUTDOWN && ! workQueue.isEmpty()))//HUTDOWN状态且任务队列不为空
            return;
        if (workerCountOf(c) != 0) { //工作线程数量不为0，也不会执行后续代码
            interruptIdleWorkers(ONLY_ONE);
            return;
        }

        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // CAS修改状态为TIDYING状态
            if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {
                try {
                    terminated(); // 更新成功，执行terminated钩子方法

                } finally {
                    ctl.set(ctlOf(TERMINATED, 0));// 强制更新状态为TERMINATED，这里不需要CAS了
                    termination.signalAll();
                }
                return;
            }
        } finally {
            mainLock.unlock();
        }
        // else retry on failed CAS
    }
}
```

### execute

```java
public void execute(Runnable command) {
    if (command == null)//任务不能为空
        throw new NullPointerException();
 
    int c = ctl.get();// 控制变量（高3位存储状态，低29位存储工作线程的数量）
    if (workerCountOf(c) < corePoolSize) {// 控制变量（高3位存储状态，低29位存储工作线程的数量）
        if (addWorker(command, true))//就添加一个工作线程（核心）
            return;
        c = ctl.get();//重新获取下控制变量
    }
    if (isRunning(c) && workQueue.offer(command)) {//如果达到了核心数量且线程池是运行状态，任务入队列
        int recheck = ctl.get();
        //再次检查线程池状态，如果不是运行状态，就移除任务并执行拒绝策略
        if (! isRunning(recheck) && remove(command))
            reject(command);
        //容错检查工作线程数量是否为0，如果为0就创建一个
        else if (workerCountOf(recheck) == 0)
            addWorker(null, false);
    }
    else if (!addWorker(command, false))//任务入队列失败，尝试创建非核心工作线程
        reject(command);//非核心工作线程创建失败，执行拒绝策略
}
```

### addWorker

```java
private boolean addWorker(Runnable firstTask, boolean core) {
    //判断有没有资格创建新的工作线程
    retry:
    for (;;) {
        int c = ctl.get();
        int rs = runStateOf(c);

        // 线程池状态检查
        if (rs >= SHUTDOWN &&
            ! (rs == SHUTDOWN &&
               firstTask == null &&
               ! workQueue.isEmpty()))
            return false;
//工作线程数量检查
        for (;;) {
            int wc = workerCountOf(c);
            if (wc >= CAPACITY ||
                wc >= (core ? corePoolSize : maximumPoolSize))
                return false;
            //数量加1并跳出循环
            if (compareAndIncrementWorkerCount(c))
                break retry;
            c = ctl.get();  // Re-read ctl
            if (runStateOf(c) != rs)
                continue retry;
            // else CAS failed due to workerCount change; retry inner loop
        }
    }
//如果上面的条件满足，则会把工作线程数量加1，然后执行下面创建线程的动作
    boolean workerStarted = false;
    boolean workerAdded = false;
    Worker w = null;
    try {
        // 创建工作线程
        w = new Worker(firstTask);
        final Thread t = w.thread;
        if (t != null) {
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                  
// 再次检查线程池的状态
                int rs = runStateOf(ctl.get());

                if (rs < SHUTDOWN ||
                    (rs == SHUTDOWN && firstTask == null)) {
                    if (t.isAlive()) // precheck that t is startable
                        throw new IllegalThreadStateException();
                    workers.add(w);// 添加到工作线程队列
                    int s = workers.size();//还在池子中的线程数量（只能在mainLock中使用）
                    if (s > largestPoolSize)
                        largestPoolSize = s;
                    workerAdded = true;// 标记线程添加成功
                }
            } finally {
                mainLock.unlock();
            }
            if (workerAdded) {
                t.start();// 线程添加成功之后启动线程
                workerStarted = true;
            }
        }
    } finally {
    // 线程启动失败，执行失败方法（线程数量减1，执行tryTerminate()方法等）
        if (! workerStarted)
            addWorkerFailed(w);
    }
    return workerStarted;
}
```

### runWorker

```java
final void runWorker(Worker w) {
        Thread wt = Thread.currentThread();//工作线程
        Runnable task = w.firstTask;
        w.firstTask = null;
        w.unlock(); // //强制释放锁(shutdown()里面有加锁)
        boolean completedAbruptly = true;
        try {
            //取任务，如果有第一个任务，这里先执行第一个任务
            while (task != null || (task = getTask()) != null) {
                w.lock();
                //查线程池的状态
                if ((runStateAtLeast(ctl.get(), STOP) ||
                     (Thread.interrupted() &&
                      runStateAtLeast(ctl.get(), STOP))) &&
                    !wt.isInterrupted())
                    wt.interrupt();
                try {
                    //钩子方法，方便子类在任务执行前做一些处理
                    beforeExecute(wt, task);
                    Throwable thrown = null;
                    try {
                        task.run();//真正任务执行的地方
                    } catch (RuntimeException x) {
                        thrown = x; throw x;
                    } catch (Error x) {
                        thrown = x; throw x;
                    } catch (Throwable x) {
                        thrown = x; throw new Error(x);
                    } finally {
                        afterExecute(task, thrown);
                    }
                } finally {
                    task = null;//task置为空，重新从队列中取
                    w.completedTasks++;//完成任务数加1
                    w.unlock();
                }
            }
            completedAbruptly = false;
        } finally {
            processWorkerExit(w, completedAbruptly);//到这里肯定是上面的while循环退出了
        }
    }
```

### getTask

```java
private Runnable getTask() {
    boolean timedOut = false; // 是否超时

    for (;;) {
        int c = ctl.get();
        int rs = runStateOf(c);

        // 线程池状态是SHUTDOWN的时候会把队列中的任务执行完直到队列为空线程池状态是STOP时立即退出
        if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
            decrementWorkerCount();
            return null;
        }
   // 工作线程数量
        int wc = workerCountOf(c);

     
        boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;
//是允许核心线程数超时，这种就是说所有的线程都可能，超时是工作线程数大于了核心数量，这种肯定是允许超时的
        if ((wc > maximumPoolSize || (timed && timedOut))
            && (wc > 1 || workQueue.isEmpty())) {
            if (compareAndDecrementWorkerCount(c))
                return null;
            continue;
        }

        try {
            Runnable r = timed ?
                workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :
                workQueue.take();
            if (r != null)
                return r;
            timedOut = true;
        } catch (InterruptedException retry) {
            timedOut = false;
        }
    }
}
```