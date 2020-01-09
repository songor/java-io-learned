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

* 异步 IO

  * Socket 数据报文示例

    ![Figure 6.5](https://github.com/songor/java-io-learned/blob/master/capture/Figure%206.5.png?raw=true)

  * 描述

    告诉内核执行系统调用（aio_read），并让内核在整个操作完成后通知进程。

    信号驱动 IO 模型是内核通知我们何时可以执行系统调用，异步 IO 模型是内核告诉进程 IO 何时完成。

  * 代码

    io/com.io.aio.AioServer

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

        io/com.io.nio.ServerSocketChannelDemo

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

* AIO

  * Path

    Path 通常代表文件系统中的位置，你所创建和处理的 Path 可以不马上绑定到对应的物理位置上，JVM 只会把 Path 绑定到运行时的物理位置上。Path 并不仅限于传统的文件系统，它也能表示 zip 或 jar 这样的文件系统。

    * 创建一个 Path

      在 NIO.2 的 API 中，Path 和 Paths 中的各种方法抛出的受检异常只有 IOException。

      创建 Path 时可以用相对路径，通过调用 toAbsolutePath() 方法，很容易把这个相对路径转换成绝对路径。

      ```java
      Path path = Paths.get(str);
      ```

    * 从 Path 中获取信息

      ```java
      System.out.println("File Name [" + path.getFileName() + "]");
      System.out.println("Number of Name Elements in the Path [" + path.getNameCount() + "]");
      System.out.println("Parent Path [" + path.getParent() + "]");
      System.out.println("Root of Path [" + path.getRoot() + "]");
      System.out.println("Subpath from Root, 2 elements deep [" + path.subpath(0, 2) + "]");
      ```

    * 移除冗余项

      需要处理的 Path 中可能会有一个或两个点，. 表示当前目录，.. 表示父目录。

      normalize() 方法去掉 Path 中的冗余信息。

      ```java
      Path normalizedPath = Paths.get("./PathDemo.java").normalize();
      ```

      此外，toRealPath() 方法也很有效，它融合了 toAbsolutePath() 和 normalize() 两个方法的功能，还能检测并跟随符号连接。

    * 转换 Path

      通过调用 resolve 方法，将 /uat/ 和 conf/application.properties 合并成表示 /uat/conf/application.properties 的完整 Path。

      ```java
      Path prefix = Paths.get("/uat/");
      Path completePath = prefix.resolve("conf/application.properties");
      ```

      取得两个 Path 之间的路径，可以用 relativize( Path path) 方法。

      可以使用 startsWith(Path prefix)，endsWith(Path suffix)，equals(Path path) 来对路径进行比较。

    * NIO.2 Path 和 Java 已有的 File 类

      java.io.File 类中新增了 toPath() 方法，它可以马上把已有的 File 转化为新的 Path。

      Path 类中有一个 toFile() 方法，它可以马上把已有的 Path 转化为 File。

  * Files

    * 处理目录和目录树

      io/com.io.aio.DirectoryStreamDemo

    * 创建和删除文件

      通常出于安全考虑，要定义所创建的文件是用于读、写、执行，或三者权限的某种组合时，你要指明该文件的某些 FileAttributes。因为这取决于文件系统，所以需要使用与文件系统相关的文件权限类（*FilePermission 类）。

      ```java
      Path source = Paths.get("tmp.txt");
      // Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-rw-rw-");
      // FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(perms);
      // Files.createFile(source, attr);
      Files.createFile(source);
      System.out.println("File [" + source.getFileName() + "] exists: " + Files.exists(source));
      
      Files.delete(source);
      ```

    * 文件的复制和移动

      ```java
      import static java.nio.file.StandardCopyOption.*;
      
      Path target = Paths.get("copy.txt");
      Files.copy(source, target, REPLACE_EXISTING);
      System.out.println("File [" + target.getFileName() + "] exists: " + Files.exists(target));
      
      Files.move(source, target, COPY_ATTRIBUTES, COPY_ATTRIBUTES);
      ```

    * 文件的属性

      文件的属性控制着谁能对文件做什么。一般情况下，做什么许可包括能够读取、写入或执行文件，而由谁许可包括属主、群组或所有人。

      ```java
      // 基本文件属性支持
      Path path = Paths.get("attributes.txt");
      Files.getLastModifiedTime(path);
      Files.size(path);
      Files.isSymbolicLink(path);
      Files.isDirectory(path);
      Files.readAttributes(path, "*");
      ```

      ```java
      // 特定文件属性支持
      import static java.nio.file.attribute.PosixFilePermission.*;
      
      Path profile = Paths.get("/user/.profile");
      PosixFileAttributes attrs = Files.readAttributes(profile, PosixFileAttributes.class);
      Set<PosixFilePermission> perms = attrs.permissions();
      // 清除已有的许可
      perms.clear();
      // 为文件添加新的许可访问
      perms.add(OWNER_READ);
      perms.add(GROUP_READ);
      perms.add(OTHERS_READ);
      perms.add(OTHERS_WRITE);
      Files.setPosixFilePermissions(profile, perms);
      ```

    * 符号链接

      可以把符号链接看做指向另一个文件或目录的入口。

      NIO.2 API 默认会跟随符号链接。如果不想跟随，需要用 `LinkOption.NOFOLLOW_LINKS` 选项。

      ```java
      Path file = Paths.get("/opt/platform/java");
      // 读取符号链接本身的基本文件属性
      Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      if (Files.isSymbolicLink(file)) {
          file = Files.readSymbolicLink(file);
      }
      Files.readAttributes(file, BasicFileAttributes.class);
      ```

    * 快速读写数据

      ```java
      try (BufferedWriter writer = Files.newBufferedWriter(source, StandardCharsets.UTF_8, StandardOpenOption.WRITE)) {
          writer.write("Hello World");
      }
      
      try (BufferedReader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
          String line;
          while ((line = reader.readLine()) != null) {
              System.out.println("Reader: " + line);
          }
      }
      
      Files.readAllLines(source, StandardCharsets.UTF_8);
      Files.readAllBytes(source);
      ```

    * 文件修改通知

      可以用 java.nio.file.WatchService 类监测文件或目录的变化。该类用客户线程监视注册文件或目录的变化，并且在检测到变化时返回一个事件。这种事件通知对于安全监测、属性文件中的数据刷新等很多用例都很有用。是现在某些应用程序中常用的轮询机制（相对而言性能较差）的理想替代品。

      io/com.io.aio.WatchServiceDemo

    * SeekableByteChannel

      Java 7 引入 SeekableByteChannel 接口，是为了让开发人员能够改变字节通道的位置和大小。

      ```java
      Path log = Paths.get("tmp.log");
      ByteBuffer buffer = ByteBuffer.allocate(1024);
      FileChannel channel = FileChannel.open(log, StandardOpenOption.READ);
      // 读取日志文件最后 50 个字符
      channel.read(buffer, channel.size() - 50);
      ```

  * 异步 IO 操作

    Java 7 中有三个新的异步通道：AsynchronousFileChannel（用于文件 IO），AsynchronousSocketChannel（用于套接字 IO，支持超时），AsynchronousServerSocketChannel（用于套接字接受异步连接）。使用新的异步 IO API 时，主要有两种形式，将来式和回调式。

    * 将来式

      当你希望由主控线程发起 IO 操作并轮询等待结果时，一般都会用将来式异步处理。

    * 回调式

      主线程会派一个侦查员 CompletionHandler 到独立的线程中执行 IO 操作。这个侦查员将带着 IO 操作的结果返回到主线程中，这个结果会触发它自己的 completed 或 failed 方法。

      在异步事件刚一成功或失败并需要马上采取行动时，一般会用回调式。

      io/com.io.aio.AsyncDemo

* 