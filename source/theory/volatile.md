# <center>volatile</center>

+ volatile特性

  + 保证了不同线程对这个变量进行操作时的可见性，即一个线程修改了某个变量的值，这新值对其他线程来说是立即可见的。（实现可见性）

  + 禁止进行指令重排序。（实现有序性）

  + volatile 只能保证对单次读/写的原子性。i++ 这种操作不能保证原子性。

+ 总结

  （1）volatile关键字可以保证可见性；

  （2）volatile关键字可以保证有序性；

  （3）volatile关键字不可以保证原子性；

  （4）volatile关键字的底层主要是通过内存屏障来实现的；

  （5）volatile关键字的使用场景必须是场景本身就是原子的；

    

     

     

     