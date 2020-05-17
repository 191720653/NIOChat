package com.demo.nio.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

/**
 * 客户端
 */
public class NIOChatClient extends Thread {

    private String host;

    private int port;

    private SocketChannel socketChannel;

    public NIOChatClient(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        init();
    }

    private void init() throws IOException {
        socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress(host, port));
        // 非阻塞模式
        socketChannel.configureBlocking(Boolean.FALSE);
    }

    @Override
    public void run() {
        try {
            // 读取消息
            ReadMessageThread readMessageThread = new ReadMessageThread();
            readMessageThread.start();
            // 控制台输入消息
            Scanner scanner = new Scanner(System.in);
            while (Boolean.TRUE) {
                String msg = scanner.nextLine();
                if (msg.length() > 0) {
                    socketChannel.write(ByteBuffer.wrap(msg.getBytes()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class ReadMessageThread extends Thread {

        private Selector selector;

        public ReadMessageThread() throws IOException {
            // 创建选择器
            selector = Selector.open();
            // 注册读取事件
            socketChannel.register(selector, SelectionKey.OP_READ);
        }

        @Override
        public void run() {
            try {
                while (Boolean.TRUE) {
                    int readyChannels = selector.select();
                    if (readyChannels == 0) {
                        continue;
                    }
                    // 有事件发生
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                    while (keyIterator.hasNext()) {
                        SelectionKey key = keyIterator.next();
                        if (key.isReadable()) {
                            ByteBuffer read = ByteBuffer.allocate(1024);
                            int count = socketChannel.read(read);
                            read.flip();
                            if (count > 0) {
                                System.out.println(new String(read.array(), 0, count));
                            }
                        }
                        keyIterator.remove();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        String host = "127.0.0.1";
        int port = 9090;
        // run配置多个客户端，然后分别启动
        NIOChatClient first = new NIOChatClient(host, port);
        first.start();
    }

}
