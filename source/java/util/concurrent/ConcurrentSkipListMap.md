# ConcurrentSkipListMap

## 简介

跳表是一个随机化的数据结构，实质就是一种可以进行**二分**查找的**有序链表**。

跳表在原有的有序链表上面增加了多级索引，通过索引来实现快速查找。

跳表不仅能提高搜索性能，同时也可以提高插入和删除操作的性能。

## 源码分析

### 主要内部类

```java
static final class Node<K,V> {//数据节点，单链表
    final K key;
    volatile Object value;
    volatile Node<K,V> next;
    Node(K key, Object value, Node<K,V> next) {
        this.key = key;
        this.value = value;
        this.next = next;
    }
    Node(Node<K,V> next) {
        this.key = null;
        this.value = this;
        this.next = next;
    }
}、
//索引节点，存储着对应的node值，及向下和向右的索引指针
 static class Index<K,V> {
        final Node<K,V> node;
        final Index<K,V> down;
        volatile Index<K,V> right;

      
        Index(Node<K,V> node, Index<K,V> down, Index<K,V> right) {
            this.node = node;
            this.down = down;
            this.right = right;
        }
     }
//HeadIndex，头索引节点，继承自Index，并扩展一个level字段，用于记录索引的层级
 static final class HeadIndex<K,V> extends Index<K,V> {
        final int level;
        HeadIndex(Node<K,V> node, Index<K,V> down, Index<K,V> right, int level) {
            super(node, down, right);
            this.level = level;
        }
    }
```
### 构造方法

```java
public ConcurrentSkipListMap() {
    this.comparator = null;
    initialize();
}


public ConcurrentSkipListMap(Comparator<? super K> comparator) {
    this.comparator = comparator;
    initialize();
}


public ConcurrentSkipListMap(Map<? extends K, ? extends V> m) {
    this.comparator = null;
    initialize();
    putAll(m);
}


public ConcurrentSkipListMap(SortedMap<K, ? extends V> m) {
    this.comparator = m.comparator();
    initialize();
    buildFromSorted(m);
}

private void initialize() {
        keySet = null;
        entrySet = null;
        values = null;
        descendingMap = null;
        head = new HeadIndex<K,V>(new Node<K,V>(null, BASE_HEADER, null),
                                  null, null, 1);
    }
}
```
### 添加元素

```java
public V put(K key, V value) {
    if (value == null)//value不能为空
        throw new NullPointerException();
    return doPut(key, value, false);
}
private V doPut(K key, V value, boolean onlyIfAbsent) {
        Node<K,V> z;             // 添加元素后存在z中
        if (key == null)//key也不能为空
            throw new NullPointerException();
        Comparator<? super K> cmp = comparator;//比较器
    // 找到目标节点的位置并插入,这里的目标节点是数据节点，也就是最底层的那条链
        outer: for (;;) {
            //寻找目标节点之前最近的一个索引对应的数据节点，存储在b中,并把b的下一个数据节点存储在n中
            for (Node<K,V> b = findPredecessor(key, cmp), n = b.next;;) {
                
                if (n != null) {
                    //如果下一节点不为空，就拿其key与目标节点的key比较，找到目标节点应该插入的位置
                    Object v; int c;
                    Node<K,V> f = n.next;//n的下一个数据节点，也就是b的下一个节点的下一个节点（孙子节点）
                    if (n != b.next)               // 如果n不为b的下一个节点,说明有其它线程修改了数据，则跳出内层循环
                        break;
                    //如果n的value值为空，说明该节点已删除，协助删除节点
                    if ((v = n.value) == null) {   // n is deleted
                        n.helpDelete(b, f);
                        break;
                    }
                    //如果b的值为空或者v等于n，说明b已被删除
                    if (b.value == null || v == n) // b is deleted
                        break;
                    //如果目标key与下一个节点的key大,说明目标元素所在的位置还在下一个节点的后面
                    if ((c = cpr(cmp, key, n.key)) > 0) {
                        //就把当前节点往后移一位, 同样的下一个节点也往后移一位,再重新检查新n是否为空，它与目标key的关系
                        b = n;
                        n = f;
                        continue;
                    }
                    //如果比较时发现下一个节点的key与目标key相同,说明链表中本身就存在目标节点
                    if (c == 0) {
                        //则用新值替换旧值，并返回旧值
                        if (onlyIfAbsent || n.casValue(v, value)) {
                            @SuppressWarnings("unchecked") V vv = (V)v;
                            return vv;
                        }
                        //如果替换旧值时失败，说明其它线程先一步修改了值，从头来过
                        break; // restart if lost race to replace value
                    }
                 //   如果c<0，就往下走，也就是找到了目标节点的位置
                    // else c < 0; fall through
                }
//有两种情况会到这里,一是到链表尾部了，也就是n为null了,二是找到了目标节点的位置，也就是上面的c<0
                z = new Node<K,V>(key, value, n);//新建目标节点，并赋值给z
                if (!b.casNext(n, z))
                    break;         // restart if lost race to append to b
                break outer;
            }
        }
    //目标节点已经插入到有序链表中了
//随机决定是否需要建立索引及其层次，如果需要则建立自上而下的索引
        int rnd = ThreadLocalRandom.nextSecondarySeed();
        if ((rnd & 0x80000001) == 0) { // 10000000000000000000000000000001=0x80000001
            //如果rnd是正偶数
                int level = 1, max;//默认level为1，也就是只要到这里了就会至少建立一层索引
            while (((rnd >>>= 1) & 1) != 0)
                ++level;
            //用于记录目标节点建立的最高的那层索引节点
            Index<K,V> idx = null;
            //头索引节点,这是最高层的头索引节点
            HeadIndex<K,V> h = head;
            //如果生成的层数小于等于当前最高层的层级,也就是跳表的高度不会超过现有高度
            if (level <= (max = h.level)) {
                //从第一层开始建立一条竖直的索引链表
                //条链表使用down指针连接起来
// 每个索引节点里面都存储着目标节点这个数据节点 
// 最后idx存储的是这条索引链表的最高层节点
                for (int i = 1; i <= level; ++i)
                    idx = new Index<K,V>(z, idx, null);
            }
            else { 
                //如果新的层数超过了现有跳表的高度,则最多只增加一层
                level = max + 1; // hold in array and later pick the one to use
                @SuppressWarnings("unchecked")Index<K,V>[] idxs =
                    (Index<K,V>[])new Index<?,?>[level+1];
                for (int i = 1; i <= level; ++i)
                    idxs[i] = idx = new Index<K,V>(z, idx, null);
                for (;;) {
                    h = head;
                    int oldLevel = h.level;
                    if (level <= oldLevel) // lost race to add level
                        break;
                    HeadIndex<K,V> newh = h;
                    Node<K,V> oldbase = h.node;
                    for (int j = oldLevel+1; j <= level; ++j)
                        newh = new HeadIndex<K,V>(oldbase, newh, idxs[j], j);
                    if (casHead(h, newh)) {
                        h = newh;
                        idx = idxs[level = oldLevel];
                        break;
                    }
                }
            }
            // find insertion points and splice in
            splice: for (int insertionLevel = level;;) {
                int j = h.level;
                for (Index<K,V> q = h, r = q.right, t = idx;;) {
                    if (q == null || t == null)
                        break splice;
                    if (r != null) {
                        Node<K,V> n = r.node;
                        // compare before deletion check avoids needing recheck
                        int c = cpr(cmp, key, n.key);
                        if (n.value == null) {
                            if (!q.unlink(r))
                                break;
                            r = q.right;
                            continue;
                        }
                        if (c > 0) {
                            q = r;
                            r = r.right;
                            continue;
                        }
                    }

                    if (j == insertionLevel) {
                        if (!q.link(r, t))
                            break; // restart
                        if (t.node.value == null) {
                            findNode(key);
                            break splice;
                        }
                        if (--insertionLevel == 0)
                            break splice;
                    }

                    if (--j >= insertionLevel && j < level)
                        t = t.down;
                    q = q.down;
                    r = q.right;
                }
            }
        }
        return null;
    }
```