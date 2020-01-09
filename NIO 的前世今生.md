# NIO 的前世今生

* NIO 核心

  * Channel - 通道

    * 双向性

      BIO - InputStream、OutputStream

    * 非阻塞性

    * 操作唯一性，基于数据块的，通过 Buffer 操作

    * Channel 实现

      文件类：FileChannel

      UDP 类：DatagramChannel

      TCP 类：ServerSocketChannel、SocketChannel
      
    * API

      BIO：

      ServerSocket.accept()、Socket.getInputStream()、Socket.getOutputStream()

      NIO：

      ServerSocketChannel serverSocketChannel = ServerSocketChannel.open(); // 服务器端创建 Channel

      serverSocketChannel.bind(new InetSocketAddress(8000)); // 服务器端绑定端口

      SocketChannel socketChannel = serverSocketChannel.accept(); // 服务器端监听客户端连接，建立 socketChannel

      SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", 8000)); // 客户端连接远程主机及端口

  * Buffer - 缓冲区

    * 作用：读取 Channel 中的数据

    * 本质：一块内存区域

    * 属性（ByteBuffer）

      Capacity：容量，数组最大可容纳多少字节，一旦写入的字节数超过最大容量，需要清空后才能继续写入数据

      Position：位置，初始为 0；写模式、读模式

      Limit：上限，写模式下，等于 Capacity；读模式下，为写模式下的 Position

      Mark：标记，存储一个特定的 Position 位置，通过 reset() 方法恢复到此位置

    * API

      ByteBuffer.allocate(10); // 初始化长度为 10 的 byte 类型 buffer

      ![ByteBuffer_1](https://github.com/songor/java-io-learned/blob/master/capture/ByteBuffer_1.PNG?raw=true)

      byteBuffer.put("abc".getByte(Charset.forName("UTF-8"))); // 向 byteBuffer 中写入三个字节

      ![ByteBuffer_2](https://github.com/songor/java-io-learned/blob/master/capture/ByteBuffer_2.PNG?raw=true)

      byteBuffer.flip(); // 将 byteBuffer 从写模式切换到读模式

      ![ByteBuffer_3](https://github.com/songor/java-io-learned/blob/master/capture/ByteBuffer_3.PNG?raw=true)

      byteBuffer.get(); // 从 byteBuffer 中读取一个字节
      
      ![ByteBuffer_4](https://github.com/songor/java-io-learned/blob/master/capture/ByteBuffer_4.PNG?raw=true)
      
      byteBuffer.mark(); // 调用 mark 方法记录当前 position 的位置
      
      ![ByteBuffer_5](https://github.com/songor/java-io-learned/blob/master/capture/ByteBuffer_5.PNG?raw=true)
      
      byteBuffer.reset(); // 调用 reset 方法将 position 重置到 mark 位置
      
      ![ByteBuffer_6](https://github.com/songor/java-io-learned/blob/master/capture/ByteBuffer_6.PNG?raw=true)
      
      byteBuffer.clear(); // 将所有属性重置
      
      ![ByteBuffer_7](https://github.com/songor/java-io-learned/blob/master/capture/ByteBuffer_7.PNG?raw=true)

  * Selector - 选择器 或 多路复用器

    * 作用：I/O 就绪选择
    
    * 地位：NIO 网络编程的基础
    
    * API
    
      Selector selector = Selector.open(); // 创建 Selector
    
      SelectionKey selectionKey = channel.register(selector, SelectionKey.OP_READ); // 将 channel 注册到 selector 上，监听读就绪事件
    
      int selectNum = selector.select(); // 阻塞等待 channel 有就绪事件发生
    
      Set\<SelectionKey\> selectedKeys = selector.selectedKeys(); // 获取发生就绪事件的 channel 集合
    
    * SelectionKey

* NIO 编程实现步骤
  * 创建 Selector
  * 创建 ServerSocketChannel，并绑定监听端口
  * 将 Channel 设置为非阻塞模式
  * 将 Channel 注册到 Selector 上，监听连接事件
  * 循环调用 Selector 的 select 方法，检测就绪情况
  * 调用 selectedKeys 方法获取就绪 channel 集合
  * 判断就绪事件种类，调用业务处理方法
  * 根据业务需要决定是否再次注册监听事件，重复第三步