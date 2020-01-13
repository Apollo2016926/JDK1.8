# CopyOnWriteArraySet

## 简介

CopyOnWriteArraySet底层是使用CopyOnWriteArrayList存储元素的，所以它并不是使用Map来存储元素的。
但是，我们知道CopyOnWriteArrayList底层其实是一个数组，它是允许元素重复的，那么用它来实现CopyOnWriteArraySet怎么保证元素不重复呢？

## 源码分析

### 属性

```java
private final CopyOnWriteArrayList<E> al;//内部用CopyOnWriteArrayList存储数据
```

### 构造方法

```java
public CopyOnWriteArraySet() {
    al = new CopyOnWriteArrayList<E>();
}


public CopyOnWriteArraySet(Collection<? extends E> c) {
    if (c.getClass() == CopyOnWriteArraySet.class) {
        @SuppressWarnings("unchecked") CopyOnWriteArraySet<E> cc =
            (CopyOnWriteArraySet<E>)c;
        //如果是CopyOnWriteArraySet类型，说明没有重复元素
        //直接用CopyOnWriteArrayList的构造方法初始化
        al = new CopyOnWriteArrayList<E>(cc.al);
    }
    else {
        //如果不是则有可能存在重复数据，调用addAllAbsent排除
        al = new CopyOnWriteArrayList<E>();
        al.addAllAbsent(c);
    }
}
```

### equals(Object o)

```java
public boolean equals(Object o) {
    //如果两者是同一个对象，返回true
    if (o == this)
        return true;
    //如果o不是set对象，返回false
    if (!(o instanceof Set))
        return false;
    Set<?> set = (Set<?>)(o);
    Iterator<?> it = set.iterator();

    //集合元素数组的快照
    Object[] elements = al.getArray();
    int len = elements.length;
    
    boolean[] matched = new boolean[len];
    int k = 0;
    //从o这个集合开始遍历
    outer: while (it.hasNext()) {
        if (++k > len)
            //如果k>len了，说明o中元素多了
            return false;
        Object x = it.next();
        //遍历检查是否在当前集合中
        for (int i = 0; i < len; ++i) {
            if (!matched[i] && eq(x, elements[i])) {
                matched[i] = true;
                continue outer;
            }
        }
        return false;
    }
    return k == len;
}
```