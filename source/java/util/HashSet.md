# <center>HashSet</center>

## 简介

①：实现了Serializable接口，表明它支持序列化。 
②：实现了Cloneable接口，表明它支持克隆，可以调用超类的clone（）方法进行浅拷贝。 
③：继承了AbstractSet抽象类，和ArrayList和LinkedList一样，在他们的抽象父类中，都提供了equals（）方法和hashCode（）方法。它们自身并不实现这两个方法，（但是ArrayList和LinkedList的equals（）实现不同。你可以看我的关于ArrayList这一块的源码解析）这就意味着诸如和HashSet一样继承自AbstractSet抽象类的TreeSet、LinkedHashSet等，他们只要元素的个数和集合中元素相同，即使他们是AbstractSet不同的子类，他们equals（）相互比较的后的结果仍然是true

![继承图](./HashSet.png)

## 源码解析

### 属性

```java
private transient HashMap<E,Object> map;//内部使用Hashmap
//虚拟对象，用来作为value放入map中 没有实际意义
private static final Object PRESENT = new Object();
```

### 构造方法

主要调用的都是HashMap的构造方法

```java
public HashSet() {//空构造
    map = new HashMap<>();
}


public HashSet(Collection<? extends E> c) {
    map = new HashMap<>(Math.max((int) (c.size()/.75f) + 1, 16));
    addAll(c);
}


public HashSet(int initialCapacity, float loadFactor) {
    map = new HashMap<>(initialCapacity, loadFactor);
}


public HashSet(int initialCapacity) {
    map = new HashMap<>(initialCapacity);
}

//只能被同一个包调用
HashSet(int initialCapacity, float loadFactor, boolean dummy) {
    map = new LinkedHashMap<>(initialCapacity, loadFactor);
}
```

### 其他方法

直接调用HashMap的put()方法，把元素本身作为key，把PRESENT作为value，也就是这个map中所有的value都是一样的。 

```
public boolean add(E e) {
    return map.put(e, PRESENT)==null;
}
```
直接调用HashMap的remove()方法，注意map的remove返回是删除元素的value，而Set的remov返回的是boolean类型。

这里要检查一下，如果是null的话说明没有该元素，如果不是null肯定等于PRESENT。

```
public boolean remove(Object o) {
    return map.remove(o)==PRESENT;
}
```
### fail-fast 

fail-fast机制是java集合中的一种错误机制。

当使用迭代器迭代时，如果发现集合有修改，则快速失败做出响应，抛出ConcurrentModificationException异常。

这种修改有可能是其它线程的修改，也有可能是当前线程自己的修改导致的，比如迭代的过程中直接调用remove()删除元素等。

另外，并不是java中所有的集合都有fail-fast的机制。比如，像最终一致性的ConcurrentHashMap、CopyOnWriterArrayList等都是没有fast-fail的。

那么，fail-fast是怎么实现的呢？

细心的同学可能会发现，像ArrayList、HashMap中都有一个属性叫 `modCount`，每次对集合的修改这个值都会加1，在遍历前记录这个值到 `expectedModCount`中，遍历中检查两者是否一致，如果出现不一致就说明有修改，则抛出ConcurrentModificationException异常。

