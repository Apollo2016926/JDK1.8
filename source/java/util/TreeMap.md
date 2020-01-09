#  <center></center>

## 简介

TreeMap实现了SotredMap接口，它是有序的集合。而且是一个红黑树结构，每个key-value都作为一个红黑树的节点。如果在调用TreeMap的构造函数时没有指定比较器，则根据key执行自然排序。这点会在接下来的代码中做说明，如果指定了比较器则按照比较器来进行排序。

 ## 继承体系

![](./TreeMap.png)

 ## SortedMap

SortedMap规定了元素可以按key的大小来遍历，它定义了一些返回部分map的方法。 

 ```java
public interface SortedMap<K,V> extends Map<K,V> {
    //key的比较器
    Comparator<? super K> comparator();
    //返回formKey（包含）到ToKey（不包含）的子map
    SortedMap<K,V> subMap(K fromKey, K toKey);
// 返回小于toKey（不包含）的子map
    SortedMap<K,V> headMap(K toKey);
  // 返回小于等于formKey（包含）的子map
    SortedMap<K,V> tailMap(K fromKey);
//返回最小的Key
    K firstKey();
//返回最大的Key
    K lastKey();
//返回key集合
    Set<K> keySet();
//返回value集合
    Collection<V> values();
// 返回节点集合
    Set<Map.Entry<K, V>> entrySet();
}
 ```

## NavigableMap

NavigableMap是对SortedMap的增强，定义了一些返回离目标key最近的元素的方法。

```java
// 小于给定的Key的最大节点
Map.Entry<K,V> lowerEntry(K key);
//小于给定的Key的最大Key
    K lowerKey(K key);
//小于等于给定的Key的最大节点
Map.Entry<K,V> floorEntry(K key);
//小于等于给定的Key的最大Key
  K floorKey(K key);
//大于等于给定key的最小节点
 Map.Entry<K,V> ceilingEntry(K key);
//大于等于给定key的最小key
 K ceilingKey(K key);
// 大于给定的最小节点
 Map.Entry<K,V> higherEntry(K key);
//大于给定的最小key
  K higherKey(K key);
// 最小节点
Map.Entry<K,V> firstEntry();
//最大的节点
    Map.Entry<K,V> lastEntry();
//弹出最小的节点
 Map.Entry<K,V> pollFirstEntry();
//弹出最大的节点
    Map.Entry<K,V> pollLastEntry();
//返回倒叙的map
NavigableMap<K,V> descendingMap();
//返回有序的key集合
 NavigableSet<K> navigableKeySet();
//返回倒序的key集合
NavigableSet<K> descendingKeySet();
//返回小于toKey的子map，是否包含toKey自己决定
    NavigableMap<K,V> headMap(K toKey, boolean inclusive);
//返回从fromKey到toKey的子map，是否包含起止元素可以自己决定
 NavigableMap<K,V> subMap(K fromKey, boolean fromInclusive,
                             K toKey,   boolean toInclusive);
//返回大于fromKey的子map，是否包含fromKey自己决定
  NavigableMap<K,V> tailMap(K fromKey, boolean inclusive);
//等价于subMap(fromKey, true, toKey, false)
 SortedMap<K,V> subMap(K fromKey, K toKey);
//等价于headMap(toKey, false)
    SortedMap<K,V> headMap(K toKey); 
// 等价于tailMap(fromKey, true)
    SortedMap<K,V> tailMap(K fromKey);
```

 ## 源码分析

### 属性

```java
//比较器 没传则key需要实现Comparator接口
private final Comparator接口<? super K> comparator;
//根节点
private transient Entry<K,V> root;

//元素个数
private transient int size =   0;

//修改次数
private transient int modCount = 0;
```

### 内部类

存储节点

```java
static final class Entry<K,V> implements Map.Entry<K,V> {
    K key;
    V value;
    Entry<K,V> left;
    Entry<K,V> right;
    Entry<K,V> parent;
    boolean color = BLACK;
}
```

### 构造方法

```java
public TreeMap() {
    comparator = null;//默认构造器 key必须实现comparable接口
}


public TreeMap(Comparator<? super K> comparator) {
    this.comparator = comparator;//根据传入的比较器比较key的大小
}

//key必须实现Comparable接口，把传入map中的所有元素保存到新的TreeMap中 
public TreeMap(Map<? extends K, ? extends V> m) {
    comparator = null;
    putAll(m);
}

//使用传入map的比较器，并把传入map中的所有元素保存到新的TreeMap中 
public TreeMap(SortedMap<K, ? extends V> m) {
    comparator = m.comparator();
    try {
        buildFromSorted(m.size(), m.entrySet().iterator(), null, null);
    } catch (java.io.IOException cannotHappen) {
    } catch (ClassNotFoundException cannotHappen) {
    }
}
```

### get(Object key)



```java
public V get(Object key) {
    Entry<K,V> p = getEntry(key);
    return (p==null ? null : p.value);
}
final Entry<K,V> getEntry(Object key) {
        // 如果comparator不为空，使用comparator的版本获取元素
        if (comparator != null)
            return getEntryUsingComparator(key);
        if (key == null)
            throw new NullPointerException();
        @SuppressWarnings("unchecked")
    //将key转为Comparable
            Comparable<? super K> k = (Comparable<? super K>) key;
        Entry<K,V> p = root;
        while (p != null) {
            //遍历
            int cmp = k.compareTo(p.key);
            if (cmp < 0)
                p = p.left;
            else if (cmp > 0)
                p = p.right;
            else
                return p;
        }
        return null;
    }
 final Entry<K,V> getEntryUsingComparator(Object key) {
        @SuppressWarnings("unchecked")
            K k = (K) key;
        Comparator<? super K> cpr = comparator;
        if (cpr != null) {
            Entry<K,V> p = root;
            while (p != null) {//从根元素开始遍历
                int cmp = cpr.compare(k, p.key);
                if (cmp < 0)
                    //小于0从左子树查找
                    p = p.left;
                else if (cmp > 0)
                    //大于0则从柚子树查找
                    p = p.right;
                else
                    //查到返回
                    return p;
            }
        }
        return null;
    }
```

### rotateLeft(Entry<K,V> p)

左旋

```java
private void rotateLeft(Entry<K,V> p) {//以P节点为支点左旋
    if (p != null) {
        //p的右节点 r
        Entry<K,V> r = p.right;
        //将r的左节点设置为p的右节点
        p.right = r.left;
        
        if (r.left != null)
            //如果r的左节点存在，将p节点设置为r左节点的父节点
            r.left.parent = p;
        //将P节点设置为R节点的父节点
        r.parent = p.parent;
        if (p.parent == null)
          //  如果p节点的父亲为空，则将r节点设置为根节点
            root = r;
        else if (p.parent.left == p)
            //如果p节点是他父亲的做节点，则将r节点设置为p节点父亲的左节点
            p.parent.left = r;
        else
            //如果p节点是他父亲的右节点，则将r节点设置为p节点父亲的右节点
            p.parent.right = r;
        r.left = p;//将p节点设置为r的左节点
        p.parent = r;将r设置为p的父节点
    }
}
```

### rotateRight(Entry<K,V> p)

右旋

```java
private void rotateRight(Entry<K,V> p) {
    if (p != null) {
    //p的左节点l
        Entry<K,V> l = p.left;
        //将l的右节点设置为p的左节点
        p.left = l.right;
        //如果l的右节点不为空，则将p节点设置l右节点的父节点
        if (l.right != null) l.right.parent = p;
        //p的父节点设置为l的父节点
        l.parent = p.parent;
       
        if (p.parent == null)
             //如果p的父节点为空，则将l节点设置为根节点
            root = l;
        else if (p.parent.right == p)
            //如果p节点是p父节点的右右节点则将l节点设置p父节点的右节点
            p.parent.right = l;
        //如果p节点是p父节点的左节点设置p父节点的左节点
        else p.parent.left = l;
        //将p节点设置为l的的右节点
        l.right = p;
        //将l设置为p的父节点
        p.parent = l;
    }
}
```
### put(K key, V value)

```java
public V put(K key, V value) {
    Entry<K,V> t = root;
    if (t == null) {//判断根节点
        //检查是否为空
        compare(key, key); // type (and possibly null) check
//插入根节点
        root = new Entry<>(key, value, null);
        size = 1;
        modCount++;
        return null;
    }
    int cmp;//存储key的比较结果
    Entry<K,V> parent;
    // split comparator and comparable paths
    Comparator<? super K> cpr = comparator;
    if (cpr != null) {
        //比较器不为空
        do {
            parent = t;//从根节点开始遍历
            cmp = cpr.compare(key, t.key);
            if (cmp < 0)
                //传入的key比父节点小从左子树查
                t = t.left;
            else if (cmp > 0)
 //传入的key比父节点大从右子树查
                t = t.right;
            else
                //找到了就将值设置
                return t.setValue(value);
        } while (t != null);
    }
    else {
        //比较器为空 key为空则抛出异常
        if (key == null)
            throw new NullPointerException();
        @SuppressWarnings("unchecked")
        //将key强转为比较器
            Comparable<? super K> k = (Comparable<? super K>) key;
        do {
            //从根节点开始遍历
            parent = t;
            cmp = k.compareTo(t.key);
            if (cmp < 0)
                t = t.left;
            else if (cmp > 0)
                t = t.right;
            else
                return t.setValue(value);
        } while (t != null);
    }
    // 如果没找到，那么新建一个节点，并插入到树中
    Entry<K,V> e = new Entry<>(key, value, parent);
    if (cmp < 0)
        //小于插入到左子节点
        parent.left = e;
    else
        //大于插入到右子节点
        parent.right = e;
    //平衡
    fixAfterInsertion(e);
    size++;
    modCount++;
    return null;
}
```

### fixAfterInsertion(Entry<K,V> x) 

插入再平衡

插入的元素默认都是红色，因为插入红色元素只违背了第4条特性，那么我们只要根据这个特性来平衡就容易多了。

根据不同的情况有以下几种处理方式：

1. 插入的元素如果是根节点，则直接涂成黑色即可，不用平衡；
2. 插入的元素的父节点如果为黑色，不需要平衡；
3. 插入的元素的父节点如果为红色，则违背了特性4，需要平衡，平衡时又分成下面三种情况：

**（如果父节点是祖父节点的左节点）**

| 情况                                                         | 策略                                                         |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| 1）父节点为红色，叔叔节点也为红色                            | （1）将父节点设为黑色； （2）将叔叔节点设为黑色； （3）将祖父节点设为红色； （4）将祖父节点设为新的当前节点，进入下一次循环判断； |
| 2）父节点为红色，叔叔节点为黑色，且当前节点是其父节点的右节点 | （1）将父节点作为新的当前节点； （2）以新当节点为支点进行左旋，进入情况3）； |
| 3）父节点为红色，叔叔节点为黑色，且当前节点是其父节点的左节点 | （1）将父节点设为黑色； （2）将祖父节点设为红色； （3）以祖父节点为支点进行右旋，进入下一次循环判断； |

**（如果父节点是祖父节点的右节点，则正好与上面反过来）**

| 情况                                                         | 策略                                                         |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| 1）父节点为红色，叔叔节点也为红色                            | （1）将父节点设为黑色； （2）将叔叔节点设为黑色； （3）将祖父节点设为红色； （4）将祖父节点设为新的当前节点，进入下一次循环判断； |
| 2）父节点为红色，叔叔节点为黑色，且当前节点是其父节点的左节点 | （1）将父节点作为新的当前节点； （2）以新当节点为支点进行右旋； |
| 3）父节点为红色，叔叔节点为黑色，且当前节点是其父节点的右节点 | （1）将父节点设为黑色； （2）将祖父节点设为红色； （3）以祖父节点为支点进行左旋，进入下一次循环判断； |

```java
/**
 * 插入再平衡
 *（1）每个节点或者是黑色，或者是红色。
 *（2）根节点是黑色。
 *（3）每个叶子节点（NIL）是黑色。（注意：这里叶子节点，是指为空(NIL或NULL)的叶子节点！）
 *（4）如果一个节点是红色的，则它的子节点必须是黑色的。
 *（5）从一个节点到该节点的子孙节点的所有路径上包含相同数目的黑节点
 */
private void fixAfterInsertion(Entry<K,V> x) {
    x.color = RED;//插入的节点设置为 红色
//只有当插入的节点不是根节点且其父节点为红色才需要平衡
    while (x != null && x != root && x.parent.color == RED) {
        if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {
            //如果父节点是祖父节点的左节点
            //y为叔叔叔节点
            Entry<K,V> y = rightOf(parentOf(parentOf(x)));
            if (colorOf(y) == RED) {//叔叔节点也是红色时
                //将父几点设为黑色
                setColor(parentOf(x), BLACK);
                //叔叔节点也设为黑色
                setColor(y, BLACK);
                //祖父节点设为红色
                setColor(parentOf(parentOf(x)), RED);
                //将祖父节点设置为当前节点继续下一次循环
                x = parentOf(parentOf(x));
            } else {
                //如果叔叔节点为黑色
                //如果当前节点是父节点的右节点
                if (x == rightOf(parentOf(x))) {
                   // 将父节点设置为当前节点
                    x = parentOf(x);
                    //以当前节点左旋
                    rotateLeft(x);
                }
                //将父节点设为黑色
                setColor(parentOf(x), BLACK);
                //将祖父节点设为红色
                setColor(parentOf(parentOf(x)), RED);
                //（3）以祖父节点为支点进行右旋
                rotateRight(parentOf(parentOf(x)));
            }
        } else {
            //如果父节点是祖父节点的右节点
            //y是叔叔节点
            Entry<K,V> y = leftOf(parentOf(parentOf(x)));
       	 
            if (colorOf(y) == RED) {
                    //情况1）如果叔叔节点为红色         
				// （1）将父节点设为黑色
                setColor(parentOf(x), BLACK);
                //将叔叔节点设为黑色
                setColor(y, BLACK);.
                    //将祖父节点设置为红色
                setColor(parentOf(parentOf(x)), RED);
              // 将祖父节点设为新的当前节点
                x = parentOf(parentOf(x));
            } else {
                //如果叔叔节点为黑色
                //如果当前节点为其父节点的左节点
                if (x == leftOf(parentOf(x))) {
                    //将父节点设为当前节点
                    x = parentOf(x);
                    //以新当前节点右旋
                    rotateRight(x);
                }
                setColor(parentOf(x), BLACK);
                //祖父节点设为红色
                setColor(parentOf(parentOf(x)), RED);
                //以祖父节点为支点进行左旋
                rotateLeft(parentOf(parentOf(x)));
            }
        }
    }
    root.color = BLACK;
}
```

### remove(Object key)

删除元素本身比较简单，就是采用二叉树的删除规则。

（1）如果删除的位置有两个叶子节点，则从其右子树中取最小的元素放到删除的位置，然后把删除位置移到替代元素的位置，进入下一步。

（2）如果删除的位置只有一个叶子节点（有可能是经过第一步转换后的删除位置），则把那个叶子节点作为替代元素，放到删除的位置，然后把这个叶子节点删除。

（3）如果删除的位置没有叶子节点，则直接把这个删除位置的元素删除即可。

（4）针对红黑树，如果删除位置是黑色节点，还需要做再平衡。

（5）如果有替代元素，则以替代元素作为当前节点进入再平衡。

（6）如果没有替代元素，则以删除的位置的元素作为当前节点进入再平衡，平衡之后再删除这个节点。

```java
public V remove(Object key) {
    //获取要删除的节点
    Entry<K,V> p = getEntry(key);
    if (p == null)
        return null;

    V oldValue = p.value;
    //删除节点
    deleteEntry(p);
    //返回旧值
    return oldValue;
}


private void deleteEntry(Entry<K,V> p) {
    //修改次数+1
        modCount++;
        size--;//元素个数-1
        if (p.left != null && p.right != null) {
            //如果要删除的节点有两个节点
            //取右子树最小节点
            Entry<K,V> s = successor(p);
            //用右子树最小节点的值替换当前节点的值
            p.key = s.key;
            p.value = s.value;
            p = s;
        } // p has 2 children

        // Start fixup at replacement node, if it exists.
    //如果当前节点有子节点，则用子节点替换当前节点
        Entry<K,V> replacement = (p.left != null ? p.left : p.right);

        if (replacement != null) {
            // 把替换节点直接放到当前节点的位置上（相当于删除了p，并把替换节点移动过来了）
            replacement.parent = p.parent;
            if (p.parent == null)
                root = replacement;
            else if (p == p.parent.left)
                p.parent.left  = replacement;
            else
                p.parent.right = replacement;

            // N将p的各项属性都设为空.
            p.left = p.right = p.parent = null;

            // 如果p是黑节点，则需要再平衡
            if (p.color == BLACK)
                fixAfterDeletion(replacement);
        } else if (p.parent == null) { // return if we are the only node.
            root = null;如果当前节点就是根节点，则直接将根节点设为空即可
        } else { //  No children. Use self as phantom replacement and unlink.
            //如果当前节点没有子节点且其为黑节点，则把自己当作虚拟的替换节点进行再平衡
            if (p.color == BLACK)//
                fixAfterDeletion(p);
			//平衡完成后删除当前节点（与父节点断绝关系
            if (p.parent != null) {
                if (p == p.parent.left)
                    p.parent.left = null;
                else if (p == p.parent.right)
                    p.parent.right = null;
                p.parent = null;
            }
        }
    }
```
删除再平衡

经过上面的处理，真正删除的肯定是黑色节点才会进入到再平衡阶段。

因为删除的是黑色节点，导致整颗树不平衡了，所以这里我们假设把删除的黑色赋予当前节点，这样当前节点除了它自已的颜色还多了一个黑色，那么：

（1）如果当前节点是根节点，则直接涂黑即可，不需要再平衡；

（2）如果当前节点是红+黑节点，则直接涂黑即可，不需要平衡；

（3）如果当前节点是黑+黑节点，则我们只要通过旋转把这个多出来的黑色不断的向上传递到一个红色节点即可，这又可能会出现以下四种情况：

**（假设当前节点为父节点的左子节点）**

| 情况                                                         | 策略                                                         |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| 1）x是黑+黑节点，x的兄弟是红节点                             | （1）将兄弟节点设为黑色； （2）将父节点设为红色； （3）以父节点为支点进行左旋； （4）重新设置x的兄弟节点，进入下一步； |
| 2）x是黑+黑节点，x的兄弟是黑节点，且兄弟节点的两个子节点都是黑色 | （1）将兄弟节点设置为红色； （2）将x的父节点作为新的当前节点，进入下一次循环； |
| 3）x是黑+黑节点，x的兄弟是黑节点，且兄弟节点的右子节点为黑色，左子节点为红色 | （1）将兄弟节点的左子节点设为黑色； （2）将兄弟节点设为红色； （3）以兄弟节点为支点进行右旋； （4）重新设置x的兄弟节点，进入下一步； |
| 3）x是黑+黑节点，x的兄弟是黑节点，且兄弟节点的右子节点为红色，左子节点任意颜色 | （1）将兄弟节点的颜色设为父节点的颜色； （2）将父节点设为黑色； （3）将兄弟节点的右子节点设为黑色； （4）以父节点为支点进行左旋； （5）将root作为新的当前节点（退出循环）； |

**（假设当前节点为父节点的右子节点，正好反过来）**

| 情况                                                         | 策略                                                         |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| 1）x是黑+黑节点，x的兄弟是红节点                             | （1）将兄弟节点设为黑色； （2）将父节点设为红色； （3）以父节点为支点进行右旋； （4）重新设置x的兄弟节点，进入下一步； |
| 2）x是黑+黑节点，x的兄弟是黑节点，且兄弟节点的两个子节点都是黑色 | （1）将兄弟节点设置为红色； （2）将x的父节点作为新的当前节点，进入下一次循环； |
| 3）x是黑+黑节点，x的兄弟是黑节点，且兄弟节点的左子节点为黑色，右子节点为红色 | （1）将兄弟节点的右子节点设为黑色； （2）将兄弟节点设为红色； （3）以兄弟节点为支点进行左旋； （4）重新设置x的兄弟节点，进入下一步； |
| 3）x是黑+黑节点，x的兄弟是黑节点，且兄弟节点的左子节点为红色，右子节点任意颜色 | （1）将兄弟节点的颜色设为父节点的颜色； （2）将父节点设为黑色； （3）将兄弟节点的左子节点设为黑色； （4）以父节点为支点进行右旋； （5）将root作为新的当前节点（退出循环）； |

### fixAfterDeletion(Entry<K,V> x)

```java
private void fixAfterDeletion(Entry<K,V> x) {
    // 只有当前节点不是根节点且当前节点是黑色时才进入循环
    while (x != root && colorOf(x) == BLACK) {
        //当前节点是父节点的左子节点
        if (x == leftOf(parentOf(x))) {
            //sib是当前节点的右兄弟节点
            Entry<K,V> sib = rightOf(parentOf(x));
			//	如果兄弟节点是红色
            if (colorOf(sib) == RED) {
                //染黑兄弟
                setColor(sib, BLACK);
                //染红父亲
                setColor(parentOf(x), RED);
                //以父节点左旋
                rotateLeft(parentOf(x));
                、//重新设置x的兄弟节点，进入下一步
                sib = rightOf(parentOf(x));
            }

            if (colorOf(leftOf(sib))  == BLACK &&
                colorOf(rightOf(sib)) == BLACK) {
                //如果兄弟节点的两个子节点都是黑色
                setColor(sib, RED);//兄弟节点设置为红色
                x = parentOf(x);//将父节点设为当前节点
            } else {
                
                if (colorOf(rightOf(sib)) == BLACK) {
                    //兄弟节点的右节点是黑色
                    //兄弟左子节点染黑
                    setColor(leftOf(sib), BLACK);
                    //兄弟染红
                    setColor(sib, RED);
                    //右旋兄弟
                    rotateRight(sib);
                    //重新设置x的兄弟节点
                    sib = rightOf(parentOf(x));
                }
                //将兄弟节点的颜色设为父节点的颜色
                setColor(sib, colorOf(parentOf(x)));
                //将父节点设为黑色
                setColor(parentOf(x), BLACK);
                //将兄弟节点的右节点染黑
                setColor(rightOf(sib), BLACK);
                rotateLeft(parentOf(x));//左旋父亲
                x = root;//跟节点设置为当前节点
                
            }
        } else { 
              //当前节点是父节点的y右子节点
            Entry<K,V> sib = leftOf(parentOf(x));

            if (colorOf(sib) == RED) {
                setColor(sib, BLACK);
                setColor(parentOf(x), RED);
                rotateRight(parentOf(x));
                sib = leftOf(parentOf(x));
            }

            if (colorOf(rightOf(sib)) == BLACK &&
                colorOf(leftOf(sib)) == BLACK) {
                setColor(sib, RED);
                x = parentOf(x);
            } else {
                if (colorOf(leftOf(sib)) == BLACK) {
                    setColor(rightOf(sib), BLACK);
                    setColor(sib, RED);
                    rotateLeft(sib);
                    sib = leftOf(parentOf(x));
                }
                setColor(sib, colorOf(parentOf(x)));
                setColor(parentOf(x), BLACK);
                setColor(leftOf(sib), BLACK);
                rotateRight(parentOf(x));
                x = root;
            }
        }
    }
//退出条件为多出来的黑色向上传递到了根节点或者红节点
    //则将x设为黑色即可满足红黑树规则
    setColor(x, BLACK);
}
```