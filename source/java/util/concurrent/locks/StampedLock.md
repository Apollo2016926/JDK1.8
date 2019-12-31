# <center>StampedLock</center>

StampedLock类，在JDK1.8时引入，是对读写锁ReentrantReadWriteLock的增强，该类提供了一些功能，优化了读锁、写锁的访问，同时使读写锁之间可以互相转换，更细粒度控制并发。
首先明确下，该类的设计初衷是作为一个内部工具类，用于辅助开发其它线程安全组件，用得好，该类可以提升系统性能，用不好，容易产生死锁和其它莫名其妙的问题

### 源码分析

#### 内部类

```java
static final class WNode {
    volatile WNode prev;
    volatile WNode next;
    volatile WNode cowait;    // 读线程用的链表
    volatile Thread thread;   // 阻塞的线程
    volatile int status;      // 0, WAITING, or CANCELLED
    final int mode;           // RMODE or WMODE
    WNode(int m, WNode p) { mode = m; prev = p; }
}
```

#### 属性

```java
private static final int LG_READERS = 7;

//用于计算state值的位常量

private static final long RUNIT = 1L;              //一单位读锁  0000 0001
private static final long WBIT  = 1L << LG_READERS;//写锁标识位  1000 0000 128
private static final long RBITS = WBIT - 1L;//读状态标识         0111 1111 127
private static final long RFULL = RBITS - 1L;//读锁的最大数量    0111 1110 126
private static final long ABITS = RBITS | WBIT;//获取读写状态    1111 1111 255
// -128 = 1111 1111 1111 1111 1111 1111 1111 1111 1111 1111 1111 1111 1111 1111 1000 0000
private static final long SBITS = ~RBITS; // 读线程个数的反数，高25位全部为1

// Initial value for lock state; avoid failure value zero
private static final long ORIGIN = WBIT << 1;//256 = 1 0000 0000  state的初始值
//CPU核数，用于控制自旋次数
    private static final int NCPU = Runtime.getRuntime().availableProcessors();

    /** 尝试获取锁时，如果超过该值仍未获取锁，则加入等待队列 */
    private static final int SPINS = (NCPU > 1) ? 1 << 6 : 0;

    /** 等待队列的首节点 自旋获取锁失败炒货该值 会继续阻塞 */
    private static final int HEAD_SPINS = (NCPU > 1) ? 1 << 10 : 0;

    /** 再次进入阻塞之前的最大重试次数 */
    private static final int MAX_HEAD_SPINS = (NCPU > 1) ? 1 << 16 : 0;
```

#### 构造

```java
public StampedLock() {
    state = ORIGIN;
}
```
#### 其他方法



....

