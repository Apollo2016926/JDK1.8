

# <center>CountDownLatch </center>

### 简介

countDownLatch这个类使一个线程等待其他线程各自执行完毕后再执行。

是通过一个计数器来实现的，计数器的初始值是线程的数量。每当一个线程执行完毕后，计数器的值就-1，当计数器的值为0时，表示所有线程都执行完毕，然后在闭锁上等待的线程就可以恢复工作了

### 源码解析

#### 构造方法

```java
 public CountDownLatch(int count) {
        if (count < 0) throw new IllegalArgumentException("count < 0");
        this.sync = new Sync(count);
    }
```

#### 内部类

##### Sync

```java
 private static final class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 4982264981922014374L;
//传入初始次数
        Sync(int count) {
            setState(count);
        }
//获取剩余次数
        int getCount() {
            return getState();
        }
//尝试获取共享次数
        protected int tryAcquireShared(int acquires) {
            return (getState() == 0) ? 1 : -1;
        }
//尝试释放锁
        protected boolean tryReleaseShared(int releases) {
            
            for (;;) {
                int c = getState();
                if (c == 0)
                    //state=0，无法再释放
                    return false;
                int nextc = c-1;
                //state-1更新
                if (compareAndSetState(c, nextc))
                    return nextc == 0;
            }
        }
    }

```

#### 其他方法

##### await

```java
//CountDownLatch.await()
public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }
//AbstractQueuedSynchronizer.acquireSharedInterruptibly()
 public final void acquireSharedInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
     // 尝试获取锁，state 等于0总是成功 不等于0就排队
        if (tryAcquireShared(arg) < 0)
		//如果失败则排队 
            doAcquireSharedInterruptibly(arg);
    }
```

##### countDown

```java

public void countDown() {
        sync.releaseShared(1);
    }

  public final boolean releaseShared(int arg) {
        if (tryReleaseShared(arg)) {
            // 尝试释放共享锁，如果成功了，就唤醒排队的线程
            doReleaseShared();
            return true;
        }
        return false;
    }
```


