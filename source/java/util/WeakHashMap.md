# <center>WeakHashMap </center>

## 简介

WeakHashMap也是Map接口的一个实现类，它与HashMap相似，也是一个哈希表，存储key-value pair，而且也是非线程安全的。不过WeakHashMap并没有引入红黑树来尽量规避哈希冲突带来的影响，内部实现只是 数组+单链表。此外，WeakHashMap与HashMap 最大的不同之处在于，WeakHashMap的key是 “弱键”（weak keys），即 当一个key不再正常使用时，key对应的key-value pair将自动从WeakHashMap中删除，在这种情况下，即使key对应的key-value pair的存在，这个key依然会被GC回收，如此以来，它对应的key-value pair也就被从map中有效地删除了。

## Java的四种引用

在正式进入WeakHashMap源码之前，我们需要先对 “弱引用”有一个基本的认识，为此这里先介绍一下JDK 1.2开始推出的四种引用：

- 强引用（Strong Reference） 强引用是指在程序代码之中普遍存在的，类似 `Objective obj = new Object()`这类的引用，只要强引用还存在，垃圾收集器永远不会回收掉被引用的对象。
- 软引用（Soft Reference） 软引用是用来描述一些还有用但并非必需的对象，对于软引用关联着的对象， 在系统将要发生内存溢出异常之前，将会把这些对象列进回收范围之中进行第二次回收。如果这次回收还没有足够的内存，才会抛出内存溢出异常。在JDK 1.2之后，提供了 SoftReference类来实现软引用。
- 弱引用（Weak Reference） 弱引用也是用来描述非必需对象的，但是它的强度比软引用更弱一点， 被弱引用关联的对象只能生存到下一次垃圾收集发生之前。当垃圾收集器工作时，无论当前内存是否足够，都会回收掉只被弱引用关联的对象。在JDK 1.2之后，提供了 WeakReference类来实现弱引用。
- 虚引用（PhantomReference） 虚引用也称为幽灵引用或者幻影引用，它是最弱的一种引用关系。一个对象是否有虚引用的存在，完全不会对其生存时间构成影响，也无法通过虚引用来取得一个对象实例。为一个对象设置虚引用关联的 唯一目的就是 能在这个对象被收集器回收时收到一个系统通知。在JDK 1.2之后，提供了 PhantomReference类来实现虚引用。

我们说WeakHashMap的key是weak-keys，即是说这个Map实现类的key值都是弱引用

## 继承关系

![](F:\Git\JDK1.8\source\java\util\WeakHashMap.png)

## 源码分析

