# Thread 

## 线程状态

```java
public enum State {
    //线程刚创建，尚未启动，还未调用start
    NEW,
  //可运行线程的线程状态。调用了 start() 方法，此时线程已经准备好被执行，处于就绪队列中。
       //处于此状态的线程是正在JVM中运行的，但可能在等待操作系统中的其他资源，如CPU时间片。
    RUNNABLE,
//阻塞等待监视器锁的状态。处于此状态的线程正在阻塞等待监视器锁，以进入一个同步块/方法，
     //或者在执行完wait()方法后重入同步块/方法
     BLOCKED,
//等待状态。执行完Object.wait无超时参数操作，或者 Thread.join无超时参数操作
   // 或者 LockSupport.park操作后，线程进入等待状态。
       // 一般在等待状态的线程在等待其它线程执行特殊操作，例如：
        //等待其它线程调用Object.notify()唤醒或者Object.notifyAll()唤醒所有。
    WAITING,

//计时等待状态。Thread.sleep、Object.wait带超时时间、Thread.join带超时时间、LockSupport.parkNanos、LockSupport.parkUntil这些操作会使线程进入计时等待状态
    TIMED_WAITING
  //终止状态，线程执行完毕。
    TERMINATED;
}
```

## 主要属性

```java
// 类加载的时候，调用静态的registerNatives()方法， 这个方法是本地方法
private static native void registerNatives();
 static {
     registerNatives();
 }
//线程名字
 private volatile String name;
 private int            priority;//线程优先级
 private Thread         threadQ;
 private long           eetop;
 private boolean     single_step;//是否是单步执行
 //是否是守护线程
 private boolean     daemon = false;
//JVM状态
 private boolean     stillborn = false;
//从构造方法传过来的Runnable，实际要执行的线程任务
 private Runnable target;
 //当前线程所在的线程组
 private ThreadGroup group;
//当前线程的上下文类加载器
 private ClassLoader contextClassLoader;

 //当前线程继承的访问控制上下文
 private AccessControlContext inheritedAccessControlContext;

 //线程的默认编号，用于生成线程的默认名字
 private static int threadInitNumber;
 private static synchronized int nextThreadNum() {
     return threadInitNumber++;
 }

 //当前线程维护的ThreadLocal值，ThreadLocalMap会被ThreadLocal类维护
 ThreadLocal.ThreadLocalMap threadLocals = null;
 //当前线程维护的从父线程那里继承的ThreadLocal值
 ThreadLocal.ThreadLocalMap inheritableThreadLocals = null;

//给这个线程设置的栈的大小,默认为0
 private long stackSize;

 /*
  * JVM-private state that persists after native thread termination.
  */
 private long nativeParkEventPointer;

 //线程id
 private long tid;

//用于生成线程id
 private static long threadSeqNumber;

 //标识线程状态，默认是线程未启动
 private volatile int threadStatus = 0;


 private static synchronized long nextThreadID() {
     return ++threadSeqNumber;
 }

 /**
  * The argument supplied to the current call to
  * java.util.concurrent.locks.LockSupport.park.
  * Set by (private) java.util.concurrent.locks.LockSupport.setBlocker
  * Accessed using java.util.concurrent.locks.LockSupport.getBlocker
  */
 volatile Object parkBlocker;

 /* The object in which this thread is blocked in an interruptible I/O
  * operation, if any.  The blocker's interrupt method should be invoked
  * after setting this thread's interrupt status.
  */
 private volatile Interruptible blocker;
 private final Object blockerLock = new Object();

 //设置blocker字段
 void blockedOn(Interruptible b) {
     synchronized (blockerLock) {
         blocker = b;
     }
 }

//线程执行的最低优先级
 public final static int MIN_PRIORITY = 1;

//线程执行的默认优先级
 public final static int NORM_PRIORITY = 5;

 //线程执行的最高的优先级
 public final static int MAX_PRIORITY = 10;
```

## 构造方法

```java
public Thread() {
    init(null, null, "Thread-" + nextThreadNum(), 0);
}


public Thread(Runnable target) {
    init(null, target, "Thread-" + nextThreadNum(), 0);
}


Thread(Runnable target, AccessControlContext acc) {
    init(null, target, "Thread-" + nextThreadNum(), 0, acc);
}


public Thread(ThreadGroup group, Runnable target) {
    init(group, target, "Thread-" + nextThreadNum(), 0);
}


public Thread(String name) {
    init(null, null, name, 0);
}


public Thread(ThreadGroup group, String name) {
    init(group, null, name, 0);
}


public Thread(Runnable target, String name) {
    init(null, target, name, 0);
}


public Thread(ThreadGroup group, Runnable target, String name) {
    init(group, target, name, 0);
}


public Thread(ThreadGroup group, Runnable target, String name,
              long stackSize) {
    init(group, target, name, stackSize);
}

   private void init(ThreadGroup g, Runnable target, String name,
                      long stackSize) {
        init(g, target, name, stackSize, null);
    }

private void init(ThreadGroup g, Runnable target, String name,
                      long stackSize, AccessControlContext acc) {
        if (name == null) {
            throw new NullPointerException("name cannot be null");
        }

        this.name = name;

        Thread parent = currentThread();//获取父线程
        SecurityManager security = System.getSecurityManager();
        if (g == null) {//判断线程组参数是否为空
            //如果没有传入线程组的话， 首先使用SecurityManager中的ThreadGroup
            if (security != null) {
                g = security.getThreadGroup();
            }
           //如果从SecurityManager中获取不到ThreadGroup， 那么就从父线程中获取线程组
            if (g == null) {
                g = parent.getThreadGroup();
            }
        }
        g.checkAccess();
        if (security != null) {
            if (isCCLOverridden(getClass())) {
                security.checkPermission(SUBCLASS_IMPLEMENTATION_PERMISSION);
            }
        }

        g.addUnstarted();
//初始化线程组
        this.group = g;
            //子线程继承父线程的优先级和守护属性
        this.daemon = parent.isDaemon();
        this.priority = parent.getPriority();
        if (security == null || isCCLOverridden(parent.getClass()))
            this.contextClassLoader = parent.getContextClassLoader();
        else
            this.contextClassLoader = parent.contextClassLoader;
        this.inheritedAccessControlContext =
                acc != null ? acc : AccessController.getContext();
        this.target = target;  //初始化target
        setPriority(priority);//设置优先级
        if (parent.inheritableThreadLocals != null)
            this.inheritableThreadLocals =
                ThreadLocal.createInheritedMap(parent.inheritableThreadLocals);
        /* Stash the specified stack size in case the VM cares */
        this.stackSize = stackSize;//设置栈深度

        /* Set thread ID */
        tid = nextThreadID();
    }
```

## native

```java
    //获得当前正在执行的线程的引用
    public static native Thread currentThread();

   //使当前线程从运行状态（Running）变为可运行状态（Runnable）
    public static native void yield();

    //强制当前正在执行的线程休眠（暂停执行），休眠结束后，线程返回到可运行状态
    public static native void sleep(long millis) throws InterruptedException;

    //启动线程，为线程分配对应的资源
    private native void start0();

    //查看当前线程是否被中断
    private native boolean isInterrupted(boolean ClearInterrupted);

    //查看当前线程是否存活
    public final native boolean isAlive();

    //获取当前线程栈帧的数量
    public native int countStackFrames();

     //当且仅当当前线程在指定的对象上持有监视器锁时，才返回 true
    public static native boolean holdsLock(Object obj);

    private native static StackTraceElement[][] dumpThreads(Thread[] threads);
    private native static Thread[] getThreads();

   //设置线程优先级
    private native void setPriority0(int newPriority);
    //停止线程
    private native void stop0(Object o);
    //挂起线程
    private native void suspend0();
    //将一个挂起线程复活继续执行
    private native void resume0();
    //设置该线程的中断状态
    private native void interrupt0();
    private native void setNativeName(String name);
```

## start

```java
public synchronized void start() {
 //线程只能被启动一次，不能被重复启动，如果线程已启动则抛出异常
    if (threadStatus != 0)
        throw new IllegalThreadStateException();

   //向线程组中添加此线程
    group.add(this);

    boolean started = false;
    try {
        start0();
        started = true;
    } finally {
        try {
            if (!started) {
                //// 如果线程启动失败，从线程组里面移除该线程
                group.threadStartFailed(this);
            }
        } catch (Throwable ignore) {
            /* do nothing. If start0 threw a Throwable then
              it will be passed up the call stack */
        }
    }
}
```