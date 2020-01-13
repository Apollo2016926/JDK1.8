# ConcurrentHashMap

## 简介

ConcurrentHashMap是HashMap的线程安全版本，内部也是使用（数组 + 链表 + 红黑树）的结构来存储元素。

相比于同样线程安全的HashTable来说，效率等各方面都有极大地提高。

## 源码分析

### 属性

```java
private static final int MAXIMUM_CAPACITY = 1 << 30;
private static final int DEFAULT_CAPACITY = 16;
static final int TREEIFY_THRESHOLD = 8;
static final int UNTREEIFY_THRESHOLD = 6;
static final int MIN_TREEIFY_CAPACITY = 64;
static final int MOVED     = -1; // 表示正在转移
static final int TREEBIN   = -2; // 表示已经转换成树
static final int RESERVED  = -3; // hash for transient reservations
static final int HASH_BITS = 0x7fffffff; // usable bits of normal node hash
transient volatile Node<K,V>[] table;//默认没初始化的数组，用来保存元素
private transient volatile Node<K,V>[] nextTable;//转移的时候用的数组
/**
     * 用来控制表初始化和扩容的，默认值为0，当在初始化的时候指定了大小，这会将这个大小保存在sizeCtl中，大小为数组的0.75
     * 当为负的时候，说明表正在初始化或扩张，
     *     -1表示初始化
     *     -(1+n) n:表示活动的扩张线程
     */
    private transient volatile int sizeCtl;
```



### 构造方法

```java
public ConcurrentHashMap() {
}

public ConcurrentHashMap(int initialCapacity) {
    if (initialCapacity < 0)
        throw new IllegalArgumentException();
    int cap = ((initialCapacity >= (MAXIMUM_CAPACITY >>> 1)) ?
               MAXIMUM_CAPACITY :
               tableSizeFor(initialCapacity + (initialCapacity >>> 1) + 1));
    this.sizeCtl = cap;
}


public ConcurrentHashMap(Map<? extends K, ? extends V> m) {
    this.sizeCtl = DEFAULT_CAPACITY;
    putAll(m);
}

public ConcurrentHashMap(int initialCapacity, float loadFactor) {
    this(initialCapacity, loadFactor, 1);
}


public ConcurrentHashMap(int initialCapacity,
                         float loadFactor, int concurrencyLevel) {
    if (!(loadFactor > 0.0f) || initialCapacity < 0 || concurrencyLevel <= 0)
        throw new IllegalArgumentException();
    if (initialCapacity < concurrencyLevel)   // Use at least as many bins
        initialCapacity = concurrencyLevel;   // as estimated threads
    long size = (long)(1.0 + (long)initialCapacity / loadFactor);
    int cap = (size >= (long)MAXIMUM_CAPACITY) ?
        MAXIMUM_CAPACITY : tableSizeFor((int)size);
    this.sizeCtl = cap;
}
```

### 添加元素

```java
public V put(K key, V value) {
    return putVal(key, value, false);
}

/** Implementation for put and putIfAbsent */
final V putVal(K key, V value, boolean onlyIfAbsent) {
    //检查键值null
    if (key == null || value == null) throw new NullPointerException();
    //计算hash值
    int hash = spread(key.hashCode());
    //要插入的元素所在桶的元素个数
    int binCount = 0;
    //死循环，结合CAS使用（如果CAS失败，则会重新取整个桶进行下面的流程
    for (Node<K,V>[] tab = table;;) {
        Node<K,V> f; int n, i, fh;
        if (tab == null || (n = tab.length) == 0)
            //初始化桶
            tab = initTable();
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
            //如果要插入的元素所在的桶还没有元素，则把这个元素插入到这个桶中
            if (casTabAt(tab, i, null,
                         //如果使用CAS插入元素时，发现已经有元素了，则进入下一次循环，重新操作
                         new Node<K,V>(hash, key, value, null)))
                break;                   //如果使用CAS插入元素成功，则break跳出循环，流程结束
        }
        else if ((fh = f.hash) == MOVED)
            tab = helpTransfer(tab, f);//如果要插入的元素所在的桶的第一个元素的hash是MOVED，则当前线程帮忙一起迁移元素
        else {
            //如果这个桶不为空且不在迁移元素，则锁住这个桶（分段锁）
            //存在，则替换值（onlyIfAbsent=false）
            //不存在，则插入到链表结尾或插入树中
            V oldVal = null;
            synchronized (f) {
                //再次检测第一个元素是否有变化，如果有变化则进入下一次循环，从头来过
                if (tabAt(tab, i) == f) {
                    if (fh >= 0) {\
                        //第一个元素的hash值大于等于0
                        binCount = 1; //桶中元素个数赋值为1
                                  //遍历整个桶，每次结束binCount加1
                        for (Node<K,V> e = f;; ++binCount) {
                            K ek;
                            if (e.hash == hash &&
                                ((ek = e.key) == key ||
                                 (ek != null && key.equals(ek)))) {
                                //如果找到了这个元素，则赋值了新值
                                oldVal = e.val;
                                if (!onlyIfAbsent)
                                    e.val = value;
                                break;
                            }
                            Node<K,V> pred = e;
                            //如果到链表尾部还没有找到元素
                            if ((e = e.next) == null) {
                                //就把它插入到链表结尾并退出循环
                                pred.next = new Node<K,V>(hash, key,
                                                          value, null);
                                break;
                            }
                        }
                    }
                    else if (f instanceof TreeBin) {
                        //如果第一个元素是树节点
                        Node<K,V> p;
                        binCount = 2;//桶中元素个数赋值为2
                        //调用红黑树的插入方法插入元素,如果成功插入则返回null,否则返回寻找到的节点
                        if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                                       value)) != null) {
                             //如果找到了这个元素，则赋值了新值,并退出循环
                            oldVal = p.val;
                            if (!onlyIfAbsent)
                                p.val = value;
                        }
                    }
                }
            }
            //如果binCount不为0，说明成功插入了元素或者寻找到了元素
            if (binCount != 0) {
                //
                if (binCount >= TREEIFY_THRESHOLD)
                    //如果链表元素个数达到了8，则尝试树化
                    treeifyBin(tab, i);
                //如果要插入的元素已经存在，则返回旧值
                if (oldVal != null)
                    return oldVal;
                break;
            }
        }
    }
    //成功插入元素，元素个数加1
    addCount(1L, binCount);
    return null;
}
//初始化桶
 private final Node<K,V>[] initTable() {
        Node<K,V>[] tab; int sc;
        while ((tab = table) == null || tab.length == 0) {
            if ((sc = sizeCtl) < 0)
                Thread.yield(); // 如果sizeCtl<0说明正在初始化或者扩容，让出CPU
            else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                //如果把sizeCtl原子更新为-1成功，则当前线程进入初始化
                try {
                    //再次检查table是否为空，防止ABA问题
                    if ((tab = table) == null || tab.length == 0) {
                        // 如果sc为0则使用默认值16
                        int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                        @SuppressWarnings("unchecked")
                        //新建数组
                        Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                        table = tab = nt;
                        //设置sc为数组长度的0.75倍
                        sc = n - (n >>> 2);
                    }
                } finally {
                    sizeCtl = sc;//把sc赋值给sizeCtl，这时存储的是扩容门槛
                }
                break;
            }
        }
        return tab;
    }
//协助迁移元素
final Node<K,V>[] helpTransfer(Node<K,V>[] tab, Node<K,V> f) {
        Node<K,V>[] nextTab; int sc;
    //如果桶数组不为空且当前桶第一个元素为ForwardingNode类型
        if (tab != null && (f instanceof ForwardingNode) &&
            //nextTab不为空 ,说明当前桶已经迁移完毕了，才去帮忙迁移其它桶的元素 扩容时会把旧桶的第一个元素置为ForwardingNode，并让其nextTab指向新桶数组
            (nextTab = ((ForwardingNode<K,V>)f).nextTable) != null) {
            int rs = resizeStamp(tab.length);
            //izeCtl<0，说明正在扩容
            while (nextTab == nextTable && table == tab &&
                   (sc = sizeCtl) < 0) {
                if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                    sc == rs + MAX_RESIZERS || transferIndex <= 0)
                    break;
                //扩容线程数加1
                if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) {
                    // 当前线程帮忙迁移元素
                    transfer(tab, nextTab);
                    break;
                }
            }
            return nextTab;
        }
        return table;
    }

//迁移元素
private final void transfer(Node<K,V>[] tab, Node<K,V>[] nextTab) {
        int n = tab.length, stride;
        if ((stride = (NCPU > 1) ? (n >>> 3) / NCPU : n) < MIN_TRANSFER_STRIDE)
            stride = MIN_TRANSFER_STRIDE; // subdivide range
        if (nextTab == null) {            // initiating
            try {
                //如果nextTab为空，说明还没开始迁移,就新建一个新桶数组
                @SuppressWarnings("unchecked")
                Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n << 1];//新桶数组是原桶的两倍
                nextTab = nt;
            } catch (Throwable ex) {      // try to cope with OOME
                sizeCtl = Integer.MAX_VALUE;
                return;
            }
            nextTable = nextTab;
            transferIndex = n;
        }
    //新桶数组大小
        int nextn = nextTab.length;
    ///新建一个ForwardingNode类型的节点，并把新桶数组存储在里面
        ForwardingNode<K,V> fwd = new ForwardingNode<K,V>(nextTab);
        boolean advance = true;
        boolean finishing = false; // to ensure sweep before committing nextTab
        for (int i = 0, bound = 0;;) {
            Node<K,V> f; int fh;
            //整个while循环就是在算i的值
            while (advance) {
                int nextIndex, nextBound;
                if (--i >= bound || finishing)
                    advance = false;
                else if ((nextIndex = transferIndex) <= 0) {
                    i = -1;
                    advance = false;
                }
                else if (U.compareAndSwapInt
                         (this, TRANSFERINDEX, nextIndex,
                          nextBound = (nextIndex > stride ?
                                       nextIndex - stride : 0))) {
                    bound = nextBound;
                    i = nextIndex - 1;
                    advance = false;
                }
            }
            if (i < 0 || i >= n || i + n >= nextn) {
                int sc;\
                    // 如果一次遍历完成了,也就是整个map所有桶中的元素都迁移完成了
                if (finishing) {
                    //如果全部迁移完成了，则替换旧桶数组
                    nextTable = null;
                    table = nextTab;
                    //并设置下一次扩容门槛为新桶数组容量的0.75倍
                    sizeCtl = (n << 1) - (n >>> 1);
                    return;
                }
                if (U.compareAndSwapInt(this, SIZECTL, sc = sizeCtl, sc - 1)) {
                    //当前线程扩容完成，把扩容线程数-1
                    if ((sc - 2) != resizeStamp(n) << RESIZE_STAMP_SHIFT)
                        return;// 扩容完成两边肯定相等
                    finishing = advance = true;//把finishing设置为true
                    i = n; // recheck before commit
                }
            }
            else if ((f = tabAt(tab, i)) == null)
                //如果桶中无数据，直接放入ForwardingNode标记该桶已迁移
                advance = casTabAt(tab, i, null, fwd);
            else if ((fh = f.hash) == MOVED)
                advance = true; // 如果桶中第一个元素的hash值为MOVED,也就是该桶已迁移
            else {
                //锁定该桶并迁移元素
                synchronized (f) {
                    //再次判断当前桶第一个元素是否有修改,也就是可能其它线程先一步迁移了元素
                    if (tabAt(tab, i) == f) {
                        Node<K,V> ln, hn;
                        if (fh >= 0) {
                            int runBit = fh & n;
                            Node<K,V> lastRun = f;
                            for (Node<K,V> p = f.next; p != null; p = p.next) {
                                int b = p.hash & n;
                                if (b != runBit) {
                                    runBit = b;
                                    lastRun = p;
                                }
                            }
                            if (runBit == 0) {
                                ln = lastRun;
                                hn = null;
                            }
                            else {
                                hn = lastRun;
                                ln = null;
                            }
                            for (Node<K,V> p = f; p != lastRun; p = p.next) {
                                int ph = p.hash; K pk = p.key; V pv = p.val;
                                if ((ph & n) == 0)
                                    ln = new Node<K,V>(ph, pk, pv, ln);
                                else
                                    hn = new Node<K,V>(ph, pk, pv, hn);
                            }
                            setTabAt(nextTab, i, ln);
                            setTabAt(nextTab, i + n, hn);
                            setTabAt(tab, i, fwd);
                            advance = true;
                        }
                        else if (f instanceof TreeBin) {
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> lo = null, loTail = null;
                            TreeNode<K,V> hi = null, hiTail = null;
                            int lc = 0, hc = 0;
                            //遍历整颗树，根据hash&n是否为0分化成两颗树
                            for (Node<K,V> e = t.first; e != null; e = e.next) {
                                int h = e.hash;
                                TreeNode<K,V> p = new TreeNode<K,V>
                                    (h, e.key, e.val, null, null);
                                if ((h & n) == 0) {
                                    if ((p.prev = loTail) == null)
                                        lo = p;
                                    else
                                        loTail.next = p;
                                    loTail = p;
                                    ++lc;
                                }
                                else {
                                    if ((p.prev = hiTail) == null)
                                        hi = p;
                                    else
                                        hiTail.next = p;
                                    hiTail = p;
                                    ++hc;
                                }
                            }
                            ln = (lc <= UNTREEIFY_THRESHOLD) ? untreeify(lo) :
                                (hc != 0) ? new TreeBin<K,V>(lo) : t;
                            hn = (hc <= UNTREEIFY_THRESHOLD) ? untreeify(hi) :
                                (lc != 0) ? new TreeBin<K,V>(hi) : t;
                            setTabAt(nextTab, i, ln);
                            setTabAt(nextTab, i + n, hn);
                            setTabAt(tab, i, fwd);
                            advance = true;
                        }
                    }
                }
            }
        }
    }

```

### 删除元素

```java
public V remove(Object key) {
    return replaceNode(key, null, null);
}


final V replaceNode(Object key, V value, Object cv) {
    int hash = spread(key.hashCode());
    for (Node<K,V>[] tab = table;;) {//自旋
        Node<K,V> f; int n, i, fh;
        if (tab == null || (n = tab.length) == 0 ||
            (f = tabAt(tab, i = (n - 1) & hash)) == null)
            //如果目标key所在的桶不存在，跳出循环返回null
            break;
        else if ((fh = f.hash) == MOVED)
            //如果正在扩容中，协助扩容
            tab = helpTransfer(tab, f);
        else {
            V oldVal = null;
            boolean validated = false;//标记是否处理过
            synchronized (f) {
                if (tabAt(tab, i) == f) {
                    if (fh >= 0) {//fh>=0表示是链表节点
                        validated = true;
                        for (Node<K,V> e = f, pred = null;;) {//遍历链表寻找目标节点
                            K ek;
                            if (e.hash == hash &&
                                ((ek = e.key) == key ||
                                 (ek != null && key.equals(ek)))) {
                                V ev = e.val;//找到了目标节点
                                if (cv == null || cv == ev ||
                                    (ev != null && cv.equals(ev))) {\\检查目标节点旧value是否等于cv
                                    oldVal = ev;
                                    if (value != null)
                                        e.val = value;//如果value不为空则替换旧值
                                    else if (pred != null)
                                        pred.next = e.next;// 如果前置节点不为空删除当前节点
                                    else
                                        setTabAt(tab, i, e.next);//说明是桶中第一个元素，删除之
                                }
                                break;
                            }
                            pred = e;
                            if ((e = e.next) == null)//遍历到链表尾部还没找到元素，跳出循环
                                break;
                        }
                    }
                    else if (f instanceof TreeBin) {
                        validated = true;//如果是树节点
                        TreeBin<K,V> t = (TreeBin<K,V>)f;
                        TreeNode<K,V> r, p;
                        //遍历树找到了目标节点
                        if ((r = t.root) != null &&
                            (p = r.findTreeNode(hash, key, null)) != null) {
                            V pv = p.val;//检查目标节点旧value是否等于cv
                            if (cv == null || cv == pv ||
                                (pv != null && cv.equals(pv))) {
                                oldVal = pv;
                                if (value != null)
                                    p.val = value;// 如果value不为空则替换旧值
                                else if (t.removeTreeNode(p))// 如果value为空则删除元素
                                    // 如果删除后树的元素个数较少则退化成链表
                                    setTabAt(tab, i, untreeify(t.first));
                            }
                        }
                    }
                }
            }
            if (validated) {
                //如果处理过，不管有没有找到元素都返回
                if (oldVal != null) {
                    //如果找到了元素，返回其旧值
                    if (value == null)
                        // 如果要替换的值为空，元素个数减1
                        addCount(-1L, -1);
                    return oldVal;
                }
                break;
            }
        }
    }
    return null;
}
```