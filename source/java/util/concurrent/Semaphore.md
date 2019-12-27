

# <center>Semaphore </center>

### 简介

Semaphore（信号量），内部维护一组许可证，通过acquire方法获取许可证，如果获取不到，则阻塞；
通过release释放许可，即添加许可证。
许可证其实是Semaphore中维护的一个volatile整型state变量，初始化的时候定义一个数量，获取时减少，
释放时增加，一直都是在操作state。
Semaphore内部基于AQS(同步框架)实现了公平或分公平两种方式获取资源。
Semaphore主要用于限制线程数量、一些公共资源的访问。

### 源码解析

#### 内部类

##### Sync

```java
 //Semaphore.Sync
abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1192457210091910933L;
//构造方法，传入许可次数，放入state中
        Sync(int permits) {
            setState(permits);
        }
//获取许可次数
        final int getPermits() {
            return getState();
        }
//非公平模式尝试获取许可
        final int nonfairTryAcquireShared(int acquires) {
            for (;;) {
                int available = getState();
                //剩余许可
                int remaining = available - acquires;
                if (remaining < 0 ||//如果剩余许可小于0了则直接返回
                    //如果剩余许可不小于0，则尝试原子更新state的值，成功了返回剩余许可
                    compareAndSetState(available, remaining))
                    return remaining;
            }
        }
//释放许可
        protected final boolean tryReleaseShared(int releases) {
            for (;;) {
                //剩余许可
                int current = getState();
                //剩余许壳+即将释放的许可
                int next = current + releases;
                if (next < current) // overflow
                    throw new Error("Maximum permit count exceeded");
             //如果原子更新state的值成功，就说明释放许可成功，则返回true
                if (compareAndSetState(current, next))
                    return true;
            }
        }
//减少许可
        final void reducePermits(int reductions) {
            for (;;) {
                int current = getState();
                //即将减少的许可
                int next = current - reductions;
                if (next > current) // underflow
                    
                    throw new Error("Permit count underflow");
                //原子更新state的值，成功了返回true
                if (compareAndSetState(current, next))
                    return;
            }
        }
//销毁许可
        final int drainPermits() {
            for (;;) {
                int current = getState();
               // 如果为0，直接返回 
// 如果不为0，把state原子更新为0
                if (current == 0 || compareAndSetState(current, 0))
                    return current;
            }
        }
    }
```

##### FairSync

```java
static final class FairSync extends Sync {
    private static final long serialVersionUID = 2014338818796000944L;

    FairSync(int permits) {
        super(permits);
    }
//尝试获取许可
    protected int tryAcquireShared(int acquires) {
        for (;;) {
            //公平模式需要检测是否前面有排队的
            //如果有返回失败
            if (hasQueuedPredecessors())
                return -1;
            //没有排队更新state
            int available = getState();
            int remaining = available - acquires;
            if (remaining < 0 ||
                compareAndSetState(available, remaining))
                return remaining;
        }
    }
}
```

##### NonfairSync

```java
static final class NonfairSync extends Sync {
    private static final long serialVersionUID = -2694183684443567898L;

    NonfairSync(int permits) {
        super(permits);
    }
// 尝试获取许可，调用父类的nonfairTryAcquireShared()方法
    protected int tryAcquireShared(int acquires) {
        return nonfairTryAcquireShared(acquires);
    }
}
```

#### 构造方法

```java
public Semaphore(int permits) {
    sync = new NonfairSync(permits);//创建时要传入许可次数，默认使用非公平模式
}

//传入许可次数，指定是否公平
public Semaphore(int permits, boolean fair) {
    sync = fair ? new FairSync(permits) : new NonfairSync(permits);
}
```

#### 其他方法

##### acquire

获取一个许可，默认采用的是可中断的方，果尝试获取许可失败，会进入AQS的队列中排队 

```
public void acquire() throws InterruptedException {
    sync.acquireSharedInterruptibly(1);
}
```

##### acquireUninterruptibly

获取一个许可，非中断方式，如果尝试获取许可失败，会进入AQS的队列中排 

```
public void acquireUninterruptibly() {
    sync.acquireShared(1);
}
```

##### tryAcquire

尝试获取一个许可，使用Sync的非公平模式尝试获取许可方法，不论是否获取到许可都返回，只尝试一次，不会进入队列排队。 

```

public boolean tryAcquire() {
    return sync.nonfairTryAcquireShared(1) >= 0;
}
```

##### tryAcquire

尝试获取一个许可，先尝试一次获取许可，如果失败则会等待timeout时间，这段时间内都没有获取到许可，则返回false，否则返回true； 

```
public boolean tryAcquire(long timeout, TimeUnit unit)
    throws InterruptedException {
    return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
}
```