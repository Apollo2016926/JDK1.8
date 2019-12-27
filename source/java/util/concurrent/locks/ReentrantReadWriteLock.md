# <center>ReentrantReadWriteLock</center>

### 简介

　ReentrantReadWriteLock是Lock的另一种实现方式，我们已经知道了ReentrantLock是一个排他锁，同一时间只允许一个线程访问，而ReentrantReadWriteLock允许多个读线程同时访问，但不允许写线程和读线程、写线程和写线程同时访问。相对于排他锁，提高了并发性。在实际应用中，大部分情况下对共享数据（如缓存）的访问都是读操作远多于写操作，这时ReentrantReadWriteLock能够提供比排他锁更好的并发性和吞吐量。 

读写锁内部维护了两个锁，一个用于读操作，一个用于写操作。所有 ReadWriteLock实现都必须保证 writeLock操作的内存同步效果也要保持与相关 readLock的联系。也就是说，成功获取读锁的线程会看到写入锁之前版本所做的所有更新。

　　ReentrantReadWriteLock支持以下功能：

　　　　1）支持公平和非公平的获取锁的方式；

　　　　2）支持可重入。读线程在获取了读锁后还可以获取读锁；写线程在获取了写锁之后既可以再次获取写锁又可以获取读锁；

　　　　3）还允许从写入锁降级为读取锁，其实现方式是：先获取写入锁，然后获取读取锁，最后释放写入锁。但是，从读取锁升级到写入锁是不允许的；

　　　　4）读取锁和写入锁都支持锁获取期间的中断；

　　　　5）Condition支持。仅写入锁提供了一个 Conditon 实现；读取锁不支持 Conditon ，readLock().newCondition() 会抛出 UnsupportedOperationException。 

### 源码分析

##### 主要属性

```java

private final ReentrantReadWriteLock.ReadLock readerLock;//读锁
//写锁
private final ReentrantReadWriteLock.WriteLock writerLock;
//实现AbstractQueuedSynchronizer同步器
final Sync sync;
```
##### 构造器

```java
public ReentrantReadWriteLock() {//默认使用非公平锁
    this(false);
}

public ReentrantReadWriteLock(boolean fair) {
    sync = fair ? new FairSync() : new NonfairSync();
    readerLock = new ReadLock(this);
    writerLock = new WriteLock(this);
}
```

##### 获取读写锁

```java
public ReentrantReadWriteLock.WriteLock writeLock() { return writerLock; }
public ReentrantReadWriteLock.ReadLock  readLock()  { return readerLock; }
```

##### 基于非公平锁的加解锁

###### ReadLock.lock

```java
//ReentrantReadWriteLock.ReadLock#lock
public void lock() {
    sync.acquireShared(1);
}
//.AbstractQueuedSynchronizer#acquireShared
 public final void acquireShared(int arg) {
        if (tryAcquireShared(arg) < 0)//尝试获取共享锁 1成功 -1失败
            //失败就可能排队
            doAcquireShared(arg);
    }
 protected final int tryAcquireShared(int unused) {
        
            Thread current = Thread.currentThread();
            int c = getState();
     //互斥锁的次数， 如果其它线程获得了写锁，直接返回-1
            if (exclusiveCount(c) != 0 &&
                getExclusiveOwnerThread() != current)
                return -1;
     //读锁被获取的次数
            int r = sharedCount(c);
     //下面说明此时还没有写锁，尝试去更新state的值获取读锁
     //读者是否需要排队（是否是公平模式）
            if (!readerShouldBlock() &&
                r < MAX_COUNT &&
                compareAndSetState(c, c + SHARED_UNIT)) {
                // 获取读锁成功
                if (r == 0) {
                    firstReader = current;//第一读线程
                    firstReaderHoldCount = 1;//重入次数1
                } else if (firstReader == current) {
                    //如果有线程获取了读锁且是当前线程是第一个读者,重入次数加1
                    firstReaderHoldCount++;
                } else {
                     //如果有线程获取了读锁且当前线程不是第一个读者
				// 则从缓存中获取重入次数保存器
                    HoldCounter rh = cachedHoldCounter;
                   // 如果缓存不属性当前线程 再从ThreadLocal中获取
// readHolds本身是一个ThreadLocal，里面存储的是HoldCounter
                    if (rh == null || rh.tid != getThreadId(current))\              
// get()的时候会初始化rh
                        cachedHoldCounter = rh = readHolds.get();
                    else if (rh.count == 0)//// 如果rh的次数为0，把它放到ThreadLocal中去
                        readHolds.set(rh);
                    rh.count++; 重入的次数加1（
                }
                return 1;
            }
      
// 通过这个方法再去尝试获取读锁（如果之前其它线程获取了写锁，一样返回-1表示失败）
            return fullTryAcquireShared(current);
        }
// AbstractQueuedSynchronizer.doAcquireShared()
private void doAcquireShared(int arg) {
// 进入AQS的队列中
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {
                final Node p = node.predecessor();
               // 如果前一个节点是头节点（说明是第一个排队的节点）
                if (p == head) {
                    //再次尝试获取读锁
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        // 头节点后移并传播,唤醒后面连续的读节点
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        if (interrupted)
                            selfInterrupt();
                        failed = false;
                        return;
                    }
                }
                //没获取到读锁，阻塞并等待被唤醒
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }


  private void setHeadAndPropagate(Node node, int propagate) {
        Node h = head; // Record old head for check below
        setHead(node);//设置当前节点为新的头结点
       //如果旧的头节点或新的头节点为空或者其等待状态小于0（表示状态为SIGNAL/PROPAGATE）
        if (propagate > 0 || h == null || h.waitStatus < 0 ||
            (h = head) == null || h.waitStatus < 0) {     
      // 需要传播取下一个节点
            Node s = node.next;
            if (s == null || s.isShared()) //如果下一个节点为空，或者是需要获取读锁的节点
                
                doReleaseShared();//唤醒下一个节点
        }
    }

private void doReleaseShared() {
   
        for (;;) {
            Node h = head;
            if (h != null && h != tail) {
                int ws = h.waitStatus;
                if (ws == Node.SIGNAL) {//如果头节点状态为SIGNAL，说明要唤醒下一个节点
                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                        continue;            // loop to recheck cases
                    unparkSuccessor(h);//唤醒
                }
                else if (ws == 0 &&
                         !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))//把头节点的状态改为PROPAGATE成功才会跳到下面的if
                    continue;                // loop on failed CAS
            }
            if (h == head)                   //如果唤醒后head没变，则跳出循环
                break;
        }
    }
```

###### ReadLock.unlock

```java
//ReentrantReadWriteLock.ReadLock.unlock
public void unlock() {
    sync.releaseShared(1);
}
//AbstractQueuedSynchronizer.releaseShared
 public final boolean releaseShared(int arg) {
        if (tryReleaseShared(arg)) {
// 如果尝试释放成功了，就唤醒下一个节点
            doReleaseShared();
            return true;
        }
        return false;
    }
//ReentrantReadWriteLock.Sync.tryReleaseShared
protected final boolean tryReleaseShared(int unused) {
            Thread current = Thread.currentThread();
            if (firstReader == current) {
                //  如果第一个读者（读线程）是当前线程 就把它重入的次数减1，如果减到0了就把第一个读者置为空
                if (firstReaderHoldCount == 1)
                    firstReader = null;
                else
                    firstReaderHoldCount--;
            } else {
              //  如果第一个读线程不是当前线程，把它重入的次数减1
                HoldCounter rh = cachedHoldCounter;
                if (rh == null || rh.tid != getThreadId(current))
                    rh = readHolds.get();
                int count = rh.count;
                if (count <= 1) {
                    readHolds.remove();
                    if (count <= 0)
                        throw unmatchedUnlockException();
                }
                --rh.count;
            }
            for (;;) {
                      
// 共享锁获取的次数减1  如果减为0了说明完全释放了，才返回true
                int c = getState();
                int nextc = c - SHARED_UNIT;
                if (compareAndSetState(c, nextc))
                    // Releasing the read lock has no effect on readers,
                    // but it may allow waiting writers to proceed if
                    // both read and write locks are now free.
                    return nextc == 0;
            }
        }
```





