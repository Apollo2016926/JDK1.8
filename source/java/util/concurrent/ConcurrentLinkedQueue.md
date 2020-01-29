# ConcurrentLinkedQueue

## 简介

在并发编程中我们有时候需要使用线程安全的队列。如果我们要实现一个线程安全的队列有两种实现方式一种是使用阻塞算法，另一种是使用非阻塞算法。使用阻塞算法的队列可以用一个锁（入队和出队用同一把锁）或两个锁（入队和出队用不同的锁）等方式来实现，而非阻塞的实现方式则可以使用循环CAS的方式来实现，下面我们一起来研究下Doug Lea是如何使用非阻塞的方式来实现线程安全队列ConcurrentLinkedQueue的。

ConcurrentLinkedQueue是一个基于链接节点的无界线程安全队列，它采用先进先出的规则对节点进行排序，当我们添加一个元素的时候，它会添加到队列的尾部，当我们获取一个元素时，它会返回队列头部的元素。它采用了“wait－free”算法来实现，该算法在Michael & Scott算法上进行了一些修改。

## 源码分析

### 主要属性

```java
private transient volatile Node<E> head;//头结点

private transient volatile Node<E> tail;//尾
```

### 内部类

```java
private static class Node<E> {
    volatile E item;
    volatile Node<E> next;
    }
```

### 构造方法

```java
public ConcurrentLinkedQueue() {
    head = tail = new Node<E>(null);
}

/**
 * Creates a {@code ConcurrentLinkedQueue}
 * initially containing the elements of the given collection,
 * added in traversal order of the collection's iterator.
 *
 * @param c the collection of elements to initially contain
 * @throws NullPointerException if the specified collection or any
 *         of its elements are null
 */
public ConcurrentLinkedQueue(Collection<? extends E> c) {
    Node<E> h = null, t = null;
    for (E e : c) {
        checkNotNull(e);
        Node<E> newNode = new Node<E>(e);
        if (h == null)
            h = t = newNode;
        else {
            t.lazySetNext(newNode);
            t = newNode;
        }
    }
    if (h == null)
        h = t = new Node<E>(null);
    head = h;
    tail = t;
}
```

### 入队

```java
public boolean offer(E e) {
    checkNotNull(e);//元素不为空
    final Node<E> newNode = new Node<E>(e);

    for (Node<E> t = tail, p = t;;) {
        Node<E> q = p.next;
        if (q == null) {//如果尾节点后没有节点到达链表尾部，入队尾
            // p is last node
            if (p.casNext(null, newNode)) {//cas更新尾几点
                //如果p不等于t，说明有其它线程先一步更新tail，把tail原子更新为新节点
                if (p != t) // hop two nodes at a time
                    casTail(t, newNode);  // Failure is OK.
                return true;
            }
            // Lost CAS race to another thread; re-read next
        }
        else if (p == q)
            //如果p的next等于p，说明p已经被删除了（已经出队了）
            p = (t != (t = tail)) ? t : head;
        else
            // t后面还有值，重新设置p的值
            p = (p != t && t != (t = tail)) ? t : q;
    }
}
```

### 出队

```java
public E poll() {
    restartFromHead:
    for (;;) {
        for (Node<E> h = head, p = h, q;;) {//从头结点开始
            E item = p.item;

            if (item != null && p.casItem(item, null)) {
                //节点的值不为null。且将节点的值更新为null成功
                // 如果头节点变了，则不会走到这个分支，会先走下面的分支拿到新的头节点这时候p就不等于h了，就更新头节点在updateHead()中会把head更新为新节点
                if (p != h) // hop two nodes at a time
                    updateHead(h, ((q = p.next) != null) ? q : p);
                return item;
            }
            // 下面三个分支说明头节点变了且p的item肯定为null
            else if ((q = p.next) == null) {
                // 如果p的next为空，说明队列中没有元素了更新h为p，也就是空元素的节点
                updateHead(h, p);
                return null;
            }
            else if (p == q)//如果p等于p的next，说明p已经出队了，重试
                continue restartFromHead;
            else/// 将p设置为p的next
                p = q;
        }
    }
}
```