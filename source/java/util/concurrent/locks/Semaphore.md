# <center>Semaphore </center>

### 简介

Semaphore（信号量），内部维护一组许可证，通过acquire方法获取许可证，如果获取不到，则阻塞；
通过release释放许可，即添加许可证。
许可证其实是Semaphore中维护的一个volatile整型state变量，初始化的时候定义一个数量，获取时减少，
释放时增加，一直都是在操作state。
Semaphore内部基于AQS(同步框架)实现了公平或分公平两种方式获取资源。
Semaphore主要用于限制线程数量、一些公共资源的访问。

