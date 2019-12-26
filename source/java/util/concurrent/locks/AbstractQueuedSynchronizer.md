# <center>AbstractQueuedSynchronizer </center>

### 简介

AQS的主要使用方式是继承它作为一个内部辅助类实现同步原语，它可以简化你的并发工具的内部实现，屏蔽同步状态管理、线程的排队、等待与唤醒等底层操作。
AQS设计基于模板方法模式，开发者需要继承同步器并且重写指定的方法，将其组合在并发组件的实现中，调用同步器的模板方法，模板方法会调用使用者重写的方法。

### 源码解析

#### 主要内部类

```java
static final class Node {
  //共享模式的标记
    static final Node SHARED = new Node();
  //标识一个节点是独占模式
    static final Node EXCLUSIVE = null;
 // waitStatus变量的值，标志着线程被取消
    static final int CANCELLED =  1;
   // waitStatus变量的值，标志着后继线程(即队列中此节点之后的节点)需要被阻塞.(用于独占锁)
    static final int SIGNAL    = -1;
// waitStatus变量的值，标志着线程在Condition条件上等待阻塞.(用于Condition的await等待)
    static final int CONDITION = -2;
     // waitStatus变量的值，标志着下一个acquireShared方法线程应该被允许。(用于共享锁)
    static final int PROPAGATE = -3;

    // 标记着当前节点的状态，默认状态是0, 小于0的状态都是有特殊作用，大于0的状态表示已取消
    volatile int waitStatus;

    // prev和next实现一个双向链表
    volatile Node prev;
    volatile Node next;
 
// 当前节点保存的线程
    volatile Thread thread;

     // 可能有两种作用：1. 表示下一个在Condition条件上等待的节点
        // 2. 表示是共享模式或者独占模式，注意第一种情况节点一定是共享模式
    Node nextWaiter;
     
// 是否是共享模式
    final boolean isShared() {
            return nextWaiter == SHARED;
        }
    //返回前一个节点prev，如果为null，则抛出NullPointerException异常
       final Node predecessor() throws NullPointerException {
            Node p = prev;
            if (p == null)
                throw new NullPointerException();
            else
                return p;
        }
//// 用于创建链表头head，或者共享模式SHARED
        Node() {    // Used to establish initial head or SHARED marker
        }
 
// 把共享模式还是互斥模式存储到nextWaiter这个字段里面了
        Node(Thread thread, Node mode) {     // Used by addWaiter
            this.nextWaiter = mode;
            this.thread = thread;
        }
   
// 等待的状态，在Condition中使用
        Node(Thread thread, int waitStatus) { // Used by Condition
            this.waitStatus = waitStatus;
            this.thread = thread;
        }
```

#### 主要属性

```java
//双向链表头、尾
private transient volatile Node head;
private transient volatile Node tail;
// 控制加锁解锁的状态变量
private volatile int state;
 private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long stateOffset;//状态变量state的偏移量
    private static final long headOffset;
    private static final long tailOffset;
    private static final long waitStatusOffset;// 等待状态的偏移量（Node的属性）
    private static final long nextOffset;// 下一个节点的偏移量（Node的属性）

    static {
        try {
            stateOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("state"));
            headOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("head"));
            tailOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("tail"));
            waitStatusOffset = unsafe.objectFieldOffset
                (Node.class.getDeclaredField("waitStatus"));
            nextOffset = unsafe.objectFieldOffset
                (Node.class.getDeclaredField("next"));

        } catch (Exception ex) { throw new Error(ex); }
    }
/**
     * 通过CAS函数设置head值，仅仅在enq方法中调用
     */
    private final boolean compareAndSetHead(Node update) {
        return unsafe.compareAndSwapObject(this, headOffset, null, update);
    }
// 互斥模式下使用：尝试获取锁
 protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }

    // 互斥模式下使用：尝试释放锁
    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }

// 共享模式下使用：尝试获取锁
    protected int tryAcquireShared(int arg) {
        throw new UnsupportedOperationException();
    }

    // 共享模式下使用：尝试释放锁
    protected boolean tryReleaseShared(int arg) {
        throw new UnsupportedOperationException();
    }
// 如果当前线程独占着锁，返回true
    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }

 
```