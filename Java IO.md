# Java IO

### Linux IO 模型

* Linux 内存空间

  * User space（用户空间）和 Kernel space（内核空间）

    Kernel space 是 Linux 内核的运行空间，User space 是用户程序的运行空间。为了安全起见，它们是隔离的。Kernel space 可以调用系统的资源；User space 不能直接调用系统资源，必须通过系统调用（又称 system call），才能向内核发出指令。

  * 对于一次 IO 操作（以 read 举例）

    数据会先被拷贝到内核空间的缓冲区中，然后才会从内核空间的缓冲区拷贝到用户空间的缓冲区。所以说，当一个 read 系统调用发生时，它会经历两个阶段：

    等待数据准备（Waiting for the data to be ready）

    将数据从内核拷贝到进程中（Copying the data from the kernel to the process）

* 阻塞 IO

  * Socket 数据报文示例

    ![Figure 6.1](https://github.com/songor/java-io-learned/blob/master/capture/Figure%206.1.png?raw=true)

  * 描述

    在 IO 执行的两个阶段用户进程都被阻塞了。

    适用于并发量小的网络应用开发。

  * 举例

    饭店共有 10 张桌子，且配备了 10 位服务员。只要有客人来了，大堂经理就把客人带到一张桌子，并安排一位服务员全程陪同。

  * 问题

    很多客户端同时发起请求的话，服务端要创建很多的线程（在 Java 虚拟机中，线程是宝贵的资源，线程的创建和销毁成本很高，除此之外，线程的切换成本也是很高的），可能会因为超过了上限而造成崩溃（采用线程池和任务队列可以实现伪异步 IO，避免了为每个请求都创建一个独立线程而造成的线程资源耗尽的问题）。

    每个线程的大部分时间都是在阻塞着，无事可干，造成了极大的资源浪费。

  * 代码

    io/com.io.bio.Client

    io/com.io.bio.BioServer

* 

### Java IO 模型

* BIO

  * 类库基本架构

    基于字节操作的 IO 接口：InputStream 和 OutputStream

    基于字符操作的 IO 接口：Reader 和 Writer

    基于磁盘操作的 IO 接口：File

    基于网络操作的 IO 接口：Socket

  * 设计模式

    * 适配器模式

      把一个类的接口变换成客户端所能接受的另一种接口，从而使两个不匹配接口而无法在一起工作的两个类能够在一起工作。

      InputStreamReader 和 OutputStreamWriter 类分别继承了 Reader 和 Writer 接口，但是要创建它们的对象必须在构造函数中传入一个 InputStream 和 OutputStream 的实例。InputStreamReader 和 OutputStreamWriter 的作用也就是将 InputStream 和 OutputStream 适配到 Reader 和 Writer。

    * 装饰器模式

      将某个类重新装扮一下，使得它比原来更“漂亮”，或者在功能上更强大。但是作为原来的这个类的使用者不应该感受到装饰前与装饰后有什么不同，否则就破坏了原有类的结构了，所以装饰器模式要做到对被装饰类的使用者透明。

      InputStream 类就是以抽象组件存在的；而 FileInputStream 就是具体组件，它实现了抽象组件的所有接口；FilterInputStream 类无疑就是装饰角色，它实现了 InputStream 类的所有接口，并且持有 InputStream 的对象实例的引用；BufferedInputStream 是具体的装饰器实现者，它给 InputStream 类增加了功能，这个装饰器类的作用就是使得 InputStream 读取的数据保存在内存中，而提高读取的性能。

    * 适配器模式与装饰器模式的区别

      适配器与装饰器模式都有一个别名就是包装模式（Wrapper），它们看似都是起到包装一个类或对象的作用，但是使用它们的目的很不一样。适配器模式的意义是要将一个接口转变成另外一个接口，它的目的是通过改变接口来达到重复使用的目的；而装饰器模式不是要改变被装饰对象的接口，而是恰恰要保持原有的接口，但是增强原有对象的功能，或者改变原有对象的处理方法而提升性能。

* 