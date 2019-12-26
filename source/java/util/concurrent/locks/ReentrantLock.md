# <center>ReentrantLock</center>

### 简介

jdk中独占锁的实现除了使用关键字synchronized外,还可以使用ReentrantLock。虽然在性能上ReentrantLock和synchronized没有什么区别，但ReentrantLock相比synchronized而言功能更加丰富，使用起来更为灵活，也更适合复杂的并发场景。

### 源码分析

#### 内部类

```java
abstract static class Sync extends AbstractQueuedSynchronizer {}//抽象类Sync实现了AQS的部分方法
static final class FairSync extends Sync {}//FairSync实现了Sync，主要用于公平锁的获取
static final class NonfairSync extends Sync {}//NonfairSync实现了Sync，主要用于非公平锁的获取；
```

#### 主要属性及构造方法

```java
//它在构造方法中初始化，决定使用公平锁还是非公平锁的方式获取锁
private final Sync sync;
// 默认构造方法 非公平锁
 public ReentrantLock() {
        sync = new NonfairSync();
    }

    //可选择使用公平锁还是非公平锁
    public ReentrantLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
    }
```

##### lock

+ 公平锁

  ReentrantLock reentrantLock = new ReentrantLock(true); 

  reentrantLock 为new FairSync()实例

  ```java
  //ReentrantLock.lock()
  public void lock() {
      sync.lock();/
  }
  //ReentrantLock.FairSync.lock()
  final void lock() {
             acquire(1);//调用AbstractQueuedSynchronizer.acquire获取锁
          }
  //AbstractQueuedSynchronizer.acquire
  public final void acquire(int arg) {
      //尝试获取锁
          if (!tryAcquire(arg) &&
              //获取锁失败，加入失败队列
              acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
              selfInterrupt();
      }
  //ReentrantLock.FairSync.tryAcquire()
   protected final boolean tryAcquire(int acquires) {
       //当前线程
              final Thread current = Thread.currentThread();
       //获取当前状态变量的值
              int c = getState();
       // 如果状态变量的值为0，说明暂时还没有人占有锁
              if (c == 0) {
                  //h如果没有其他线程在排队
                  if (!hasQueuedPredecessors() &&
                      //尝试更新state的值为1
                      compareAndSetState(0, acquires)) {
                      //获取到了锁，将AbstractOwnableSynchronizer.exclusiveOwnerThread设置为当前线程
                      setExclusiveOwnerThread(current);
                      return true;
                  }
              }
       //如果当前线程本身占着锁，现在有来获取锁直接让他获取锁并返回
              else if (current == getExclusiveOwnerThread()) {
                  //状态变量state+1
                  int nextc = c + acquires;
                  if (nextc < 0)//
                      throw new Error("Maximum lock count exceeded");
                  setState(nextc);
                  return true;
              }
              return false;
          }
  //AbstractQueuedSynchronizer#addWaiter
  //尝试获取锁失败时调用此方法
  private Node addWaiter(Node mode) {
      //新建一个节点 mode为独占模式 addWaiter(Node.EXCLUSIVE)
          Node node = new Node(Thread.currentThread(), mode);
          //先尝试将新节点加到尾节点后面，成功则返回新节点，失败调用enq（）不断尝试
          Node pred = tail;
          if (pred != null) {
              //新节点的前置节点设置为当前尾节点
              node.prev = pred;
              //CAS更新尾节点为新节点
              if (compareAndSetTail(pred, node)) {
                  pred.next = node;
                  return node;
              }
          }
      //新节点入队失败调用enq()
          enq(node);
          return node;
      }
  //AbstractQueuedSynchronizer.enq()
      private Node enq(final Node node) {
          //自旋
          for (;;) {
              Node t = tail;
              //尾节点为空，说明还没初始化
              if (t == null) { // Must initialize
                  //初始化
                  if (compareAndSetHead(new Node()))
                      tail = head;
              } else {
                  //如果尾节点不为空，设置新节点的前驱节点为当前的尾节点
                  node.prev = t;
  // CAS更新尾节点为新节点
                  if (compareAndSetTail(t, node)) {
                      成功设置旧的尾节点的下一个节点为新节点
                      t.next = node;
                      return t;
                  }
              }
          }
      }
  
  //AbstractQueuedSynchronizer.acquireQueued(）
  //调用上面的addWaiter()方法使得新节点已经成功入队了
  // 这个方法是尝试让当前节点来获取锁的
  final boolean acquireQueued(final Node node, int arg) {
          boolean failed = true;//失败标记
          try {
              boolean interrupted = false;//中断标记
              //自旋
              for (;;) {
                  //获取当前节点的前一个节点
                  final Node p = node.predecessor();
                  //如果当前节点的前一个节点为head节点，则说明轮到自己获取锁了
  			// 调用ReentrantLock.FairSync.tryAcquire()方法再次尝试获取锁
                  if (p == head && tryAcquire(arg)) {
                      //获取到锁，将当前节点设置为新的头节点，这里同时只会有一个线程在运行所以不用CAS
                      setHead(node);
                      p.next = null; // help GC
                      failed = false;//未失败
                      return interrupted;
                  }
                  //是否需要阻塞
                  if (shouldParkAfterFailedAcquire(p, node) &&
                      //真正阻塞的方法
                      parkAndCheckInterrupt())
                      //如果中断了
                      interrupted = true;
              }
          } finally {
              //如果失败了
              if (failed)
                  //取消获取锁
                  cancelAcquire(node);
          }
      }
  //.AbstractQueuedSynchronizr#shouldParkAfterFailedAcquire（）
  // 这个方法是在上面的for()循环里面调用的
  // 第一次调用会把前一个节点的等待状态设置为SIGNAL，并返回false
  // 第二次调用才会返回true
  private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        //上一节点的等待状态
     // 注意Node的waitStatus字段我们在上面创建Node的时候并没有指定 ,所以为0 
      int ws = pred.waitStatus;
  // 如果等待状态为SIGNAL(等待唤醒)，直接返回true
          if (ws == Node.SIGNAL)
              return true;
     // 如果前一个节点的状态大于0，也就是已取消状态
          if (ws > 0) {
             // 把前面所有取消状态的节点都从链表中删除
              do {
                  node.prev = pred = pred.prev;
              } while (pred.waitStatus > 0);
              pred.next = node;
          } else {
           /*
           如过前一个节点的状态小于等于0，则它状态设置为等待唤醒
           这里可以简单地理解为把初始状态0设置为SIGNAL
           CONDITION是条件锁的时候使用的 
           PROPAGATE是共享锁使用的
           */
              compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
          }
          return false;
      }
  // AbstractQueuedSynchronizer.parkAndCheckInterrupt 阻塞
     private final boolean parkAndCheckInterrupt() {
         // 阻塞当前线程
  // 底层调用的是Unsafe的park()方法
          LockSupport.park(this);
         // 返回是否已中断
          return Thread.interrupted();
      }
  ```

获取锁的主要过程大致如下：

（1）尝试获取锁，如果获取到了就直接返回了；

（2）尝试获取锁失败，再调用addWaiter()构建新节点并把新节点入队；

（3）然后调用acquireQueued()再次尝试获取锁，如果成功了，直接返回；

（4）如果再次失败，再调用shouldParkAfterFailedAcquire()将节点的等待状态置为等待唤醒（SIGNAL）；

（5）调用parkAndCheckInterrupt()阻塞当前线程；

（6）如果被唤醒了，会继续在acquireQueued()的for()循环再次尝试获取锁，如果成功了就返回；

（7）如果不成功，再次阻塞，重复（3）（4）（5）直到成功获取到锁。

+ 非公平锁

  ReentrantLock reentrantLock = new ReentrantLock(false); 

  reentrantLock 为new NonfairSync()实例



```java
//ReentrantLosck.lock
public void lock() {
    sync.lock();
}
// ReentrantLock.NonfairSync#lock
//公平锁是直接掉acquire（1）
   final void lock() {
       //直接尝试CAS更新状态变量
            if (compareAndSetState(0, 1))
                // 如果更新成功，说明获取到锁，把当前线程设为独占线程
                setExclusiveOwnerThread(Thread.currentThread());
            else
                acquire(1);
        }
//AbstractQueuedSynchronizer.acquire
public final void acquire(int arg) {
    //尝试获取锁
        if (!tryAcquire(arg) &&
            //获取锁失败，加入失败队列
            acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            selfInterrupt();
    }
//// ReentrantLock.NonfairSync.tryAcquire()
 protected final boolean tryAcquire(int acquires) {
// 调用父类的方法
            return nonfairTryAcquire(acquires);
        }

//ReentrantLock.Sync#nonfairTryAcquire
final boolean nonfairTryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                //如果状态变量的值为0，再次尝试CAS更新状态变量的值
                 //相对于公平锁模式少了!hasQueuedPredecessors()条件
                if (compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0) // overflow
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }
```
相对于公平锁，非公平锁加锁的过程主要有两点不同：

（1）一开始就尝试CAS更新状态变量state的值，如果成功了就获取到锁了；

（2）在tryAcquire()的时候没有检查是否前面有排队的线程，直接上去获取锁才不管别人有没有排队呢；

总的来说，相对于公平锁，非公平锁在一开始就多了两次直接尝试获取锁的过程

##### tryLock

```java
//尝试获取一次锁，成功了就返回true，没成功就返回false，不会继续尝试 
public boolean tryLock() {
    return sync.nonfairTryAcquire(1);//调用非公平锁的获取锁
}
//尝试获取锁，并等待一段时间，如果在这段时间内都没有获取到锁，就返回false
public boolean tryLock(long timeout, TimeUnit unit)
        throws InterruptedException {
    return sync.tryAcquireNanos(1, unit.toNanos(timeout));
}
// AbstractQueuedSynchronizer.tryAcquireNanos()
public final boolean tryAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted())//线程中断抛出异常
            throw new InterruptedException();
        return tryAcquire(arg) || // 先尝试获取一次锁  不同的锁调不同的方法
            doAcquireNanos(arg, nanosTimeout);
    }
// AbstractQueuedSynchronizer.doAcquireNanos()
private boolean doAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (nanosTimeout <= 0L)
// 如果时间已经到期了，直接返回false
            return false;
    //计算到期时间
        final long deadline = System.nanoTime() + nanosTimeout;
        final Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return true;
                }
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L)
                    //到期
                    return false;
                if (shouldParkAfterFailedAcquire(p, node) &&
                    // 只有到期时间大于1000纳秒，才阻塞
                    nanosTimeout > spinForTimeoutThreshold)//spinForTimeoutThreshold = 1000L
                    LockSupport.parkNanos(this, nanosTimeout);//阻塞
                if (Thread.interrupted())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }
```

##### unlock

```java
ReentrantLock.unlock
public void unlock() {
    sync.release(1);//调用父类的AbstractQueuedSynchronizer.release
}
//AbstractQueuedSynchronizer.release
  public final boolean release(int arg) {
  //调用实现类的tryRelease()方法释放锁
        if (tryRelease(arg)) {
            Node h = head;
            //如果头节点不为空，且等待状态不是0，就唤醒下一个节点
           // 在每个节点阻塞之前会把其上一个节点的等待状态设为SIGNAL（-1） 
// 所以，SIGNAL的准确理解应该是唤醒下一个等待的线程
            if (h != null && h.waitStatus != 0)
                unparkSuccessor(h);
            return true;
        }
        return false;
    }
    
//ReentrantLock.Sync#tryRelease
        protected final boolean tryRelease(int releases) {//releases=1
            int c = getState() - releases;
// 如果当前线程不是占有着锁的线程，抛出异常
            if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException();
            boolean free = false;
            // 如果状态变量的值为0了，说明完全释放了锁
// 这也就是为什么重入锁调用了多少次lock()就要调用多少次unlock()的原因
// 如果不这样做，会导致锁不会完全释放，别的线程永远无法获取到锁
            if (c == 0) {
                free = true;
                setExclusiveOwnerThread(null);
            }    
// 设置状态变量的值
            setState(c);
            return free;
        }
//AbstractQueuedSynchronizer#unparkSuccessor
private void unparkSuccessor(Node node) {//node头节点
       
        int ws = node.waitStatus;
        if (ws < 0)
            // 如果头节点的等待状态小于0，就把它设置为0
            compareAndSetWaitStatus(node, ws, 0);
//头结点的下一节点
        Node s = node.next;
    // 如果下一个节点为空，或者其等待状态大于0 即waitStatus=1 取消状态
        if (s == null || s.waitStatus > 0) {
            s = null;
// 从尾节点向前遍历取到队列最前面的那个状态不是已取消状态的节点
            for (Node t = tail; t != null && t != node; t = t.prev)
                if (t.waitStatus <= 0)
                    s = t;
        }
     
// 如果下一个节点不为空，则唤醒它
        if (s != null)
            LockSupport.unpark(s.thread);
    }
```
释放锁的过程大致为：

（1）将state的值减1；

（2）如果state减到了0，说明已经完全释放锁了，唤醒下一个等待着的节点；