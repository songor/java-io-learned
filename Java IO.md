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

    io/com.io.Client

    io/com.io.bio.BioServer

* 非阻塞 IO

  * Socket 数据报文示例

    ![Figure 6.2](https://github.com/songor/java-io-learned/blob/master/capture/Figure%206.2.png?raw=true)

  * 描述

    Socket 设置为 NONBLOCK（非阻塞）就是告诉内核，当所请求的 IO 操作无法满足时，不要将用户进程挂起，而是返回一个错误码（EWOULDBLOCK）。从用户进程的角度讲，它发起一个 recvfrom 系统调用后，并不需要等待，而是马上就得到一个结果。

    在 IO 执行的第二个阶段用户进程被阻塞了。

    用户进程需要不断地主动询问 Kernal 数据准备好了没有。

  * 问题

    不断地轮询消耗大量的 CPU 资源。

* 多路复用 IO

  * Socket 数据报文示例

    ![Figure 3](https://github.com/songor/java-io-learned/blob/master/capture/Figure%206.3.png?raw=true)

  * 描述

    多个进程的 IO 可以注册到一个 select 上，当用户进程调用该 select，select 会监听所有注册进来的 IO，如果 select 所监听的 IO 在内核缓冲区都没有可读数据，select 调用进程会被阻塞。而当任一 IO 在内存缓冲区有可读数据时，select 调用就会返回，而后 select 调用进程可以自己或通知注册的进程再次发起 recvfrom 系统调用，读取内核中准备好的数据。多个进程注册 IO 后，只有一个 select 调用进程被阻塞。 

    多路复用 IO 模型和阻塞 IO 模型并没有太大的不同，事实上，还更差一些，因为需要使用两个系统调用 select 和 recvfrom，而阻塞 IO 只有一次系统调用 recvfrom。但是，使用 select 最大的优势是可以在一个进程内处理多个 IO（等待数据准备），而在阻塞 IO 模型中，必须通过多进程的方式才能达到这个目的。

    在多路复用 IO 模型中，对于每个 Socket，一般都设置为非阻塞。如上图所示，用户的进程其实是一直被阻塞的，只不过进程是被 select 这个函数阻塞，而不是被 IO 给阻塞。

    select/poll 是顺序扫描 fd 是否就绪，而且支持的 fd 数量有限，因此它的使用受到一些制约（比如 select 限制 fd 为 1024）。epoll 使用基于事件驱动方式替代顺序扫描，因此性能更高。

  * 问题

    多路复用 IO 模型是通过轮询的方式来检测是否有事件（readable）到达，并且对到达的事件逐一进行响应。一旦事件响应体很大，就会导致后续的事件迟迟得不到处理，并且会影响新的事件轮询。

  * 举例

    专门设立一个“跑腿”服务员，工作职责单一，就是问问客人是否需要服务。

    站在门口接待客人，本来是“大堂经理”的工作，但是他不愿意在门口盯着，于是就委托给“跑腿”服务员，你帮我盯着，有人来了告诉我。于是“跑腿”服务员就有了一个任务，替“大堂经理”盯梢（OP_ACCEPT）。终于来客人了，“跑腿”服务员赶紧告诉“大堂经理”。

    “大堂经理”把客人带到座位上，对“跑腿”服务员说，客人接下来肯定是要点餐的，但是现在在看菜单，不知道什么时候能看好，所以你不时地过来问问，看需不需要点餐，需要的话就再喊一个“点餐”服务员给客人写菜单。于是“跑腿”就又多了一个任务，就是盯着这桌客人，不时地来问问，如果需要服务的话，就叫“点餐”服务员过来服务（OP_READ）。

    “跑腿”服务员在某次询问中，客人终于决定点餐了，“跑腿”服务员赶紧找来一个“点餐”服务员为客人写菜单。

    就这样，“跑腿”服务员既要盯着门外新过来的客人，也要盯着门内已经就坐的客人。新客人来了，通知“大堂经理”去接待。就坐的客人决定点餐了，通知“点餐”服务员去写菜单。

  * 代码

    io/com.io.Client

    io/com.io.nio.NioServer

* 信号驱动 IO

  * Socket 数据报文示例

    ![Figure 6.4](https://github.com/songor/java-io-learned/blob/master/capture/Figure%206.4.png?raw=true)

  * 描述

    进程预先告知内核，向内核注册一个信号处理函数（establish SIGIO signal handler），然后进程继续运行并不阻塞。当内核数据准备好时，进程会收到一个 SIGIO 信号，可以在信号处理函数中调用 recvfrom 系统调用读取数据。

    将数据从内核拷贝到进程中的过程还是阻塞的。

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

* NIO

  * 特性

    * Non-blocking IO

    * Channels and Buffers

      标准的 IO 编程接口是面向字节流和字符流的，而 NIO 是面向 Channel 和 Buffer 的。数据总是从 Channel 读到 Buffer，或者从 Buffer 写入 Channel。

    * Selectors

      Selector 是一个可以用于监视多个通道的对象，因此单线程可以监视多个通道中的数据，相比使用多个线程，避免了线程上下文切换带来的开销。

  * 核心组件

    * Buffer

      可以把 Buffer 简单地理解为一组基本数据类型的元素列表。

      ByteBuffer，CharBuffer，ShortBuffer，IntBuffer，FloatBuffer，DoubleBuffer，LongBuffer

      | 索引     | 说明                                           |
      | :------- | ---------------------------------------------- |
      | capacity | 缓冲区数组的总长度                             |
      | position | 下一个要操作的数据元素的位置                   |
      | limit    | 缓冲区数据中不可操作的下一个元素的位置         |
      | mark     | 用于记录当前 position 的前一个位置或者默认是 0 |

      Buffer 提供了另外一种直接操作操作系统缓冲区的方式，即 `ByteBuffer.allocateDirect(size)` ，这个方法返回的 `DirectByteBuffer` 就是与底层存储空间关联的缓冲区，它通过 Native 代码操作非 JVM 堆的内存空间。每次创建或者释放的时候都调用一个 `System.gc()`。

      io/com.io.nio.BufferDemo

    * Channel

      FileChannel，DatagramChannel，SocketChannel，ServerSocketChannel

      * Channel 和 Stream

        Channel 可以读也可以写，Stream 一般来说是单向的（只能读或者写，所以之前我们用流进行 IO 操作的时候需要分别创建一个输入流和一个输出流）。

        Channel 可以异步读写。

        Channel 总是基于 Buffer 来读写。
        
      * FileChannel

        io/com.io.nio.FileChannelDemo

        FileChannel.transferXXX 与传统的访问文件方式相比可以减少数据从内核到用户空间的复制，数据直接在内核空间中移动，在 Linux 中使用 sendfile 系统调用。

        FileChannel.map 将文件按照一定大小块映射为内存区域，当程序访问这个内存区域时将直接操作这个文件数据，这种方式省去了数据从内核空间向用户空间复制的损耗。这种方式适合对大文件的只读性操作，如大文件的 MD5 校验。

      * SocketChannel

        io/com.io.nio.SocketChannelDemo

      * ServerSocketChannel

        io/com.io.nioServerSocketChannelDemo

    * Selectors

      Selector 一般称为选择器或多路复用器，用于检查一个或多个 Channel 的状态，如此可以实现单线程管理多个 Channel。

      * Selector 创建

        ```java
        Selector selector = Selector.open();
        ```

      * 注册 Channel 到 Selector

        ```java
        ServerSocketChannel ssc = ServerSocketChannel.open();
        // Channel 必须是非阻塞的
        ssc.configureBlocking(false);
        ssc.socket().bind(new InetSocketAddress("localhost", 8080));
        ssc.register(selector, SelectionKey.OP_ACCEPT);
        ```

      * Selector 监听 Channel 触发事件

        |        事件         |   SelectionKey 常量    |
        | :-----------------: | :--------------------: |
        | Connect（连接就绪） | SelectionKey.OP_ACCEPT |
        | Accept（接收就绪）  | SelectionKey.OP_ACCEPT |
        |   Read（读就绪）    |  SelectionKey.OP_READ  |
        |   Write（写就绪）   | SelectionKey.OP_WRITE  |

      * SelectionKey

        SelectionKey 表示了一个特定的 Channel 和一个特定的 Selector 之间的注册关系。

        感兴趣事件集合 `key.interestOps()`。

        已经就绪事件集合 `key.readyOps()`。

        从 SelectionKey 访问 Channel `key.channel()`  和 Selector `key.selector()`。

* 