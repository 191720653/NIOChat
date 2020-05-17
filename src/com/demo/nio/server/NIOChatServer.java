package com.demo.nio.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 服务端
 */
public class NIOChatServer extends Thread {

    private int port;

    private Map<String, SelectionKey> selectionKeyMap;

    private ServerSocketChannel serverSocketChannel;

    private Selector selector;

    public NIOChatServer(int port) throws IOException {
        this.port = port;
        init ();
    }

    private void init () throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        // 非阻塞模式
        serverSocketChannel.configureBlocking(Boolean.FALSE);
        serverSocketChannel.bind(new InetSocketAddress(port));

        // 创建选择器，并注册新连接接收事件
        selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        // 用于存放selectionKey
        selectionKeyMap = new HashMap<>();
    }

    @Override
    public void run() {
        try {
            while (selector.select() > 0) {
                // 有事件发生
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    if (key.isAcceptable()) {
                        System.out.println("a connection was accepted by a ServerSocketChannel.");
                        acceptSocket(key);
                    } else if (key.isConnectable()) {
                        System.out.println("a connection was established with a remote server.");
                    } else if (key.isReadable()) {
                        System.out.println("a channel is ready for reading.");
                        readMessage(key);
                    } else if (key.isWritable()) {
                        System.out.println("a channel is ready for writing.");
                    }
                    keyIterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void acceptSocket(SelectionKey key) throws IOException {
        ServerSocketChannel channel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = channel.accept();
        // 非阻塞模式
        socketChannel.configureBlocking(false);
        // 注册读取事件
        SelectionKey socketChannelSelectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
        selectionKeyMap.put(getSocketAddress(socketChannel), socketChannelSelectionKey);
    }

    private void readMessage(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        try {
            // 接受消息
            ByteBuffer read = ByteBuffer.allocate(1024);
            channel.read(read);
            read.flip();
            String msg = "This is a message from client " + getSocketAddress(channel) + "：" + new String(read.array(), 0, read.limit());
            System.out.println(msg);
            key.interestOps(SelectionKey.OP_READ);
            // 遍历发送到各个客户端
            selectionKeyMap.forEach((k, v) -> {
                if (!k.equals(getSocketAddress(channel))) {
                    try {
                        SocketChannel otherChannel = (SocketChannel) v.channel();
                        otherChannel.write(ByteBuffer.wrap(msg.getBytes()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            selectionKeyMap.remove(getSocketAddress(channel));
            key.cancel();
            channel.close();
        }
    }

    public static String getSocketAddress(SocketChannel socketChannel) {
        try {
            // 获取客户端地址、端口号
            InetSocketAddress socketAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
            return socketAddress.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
        NIOChatServer server = new NIOChatServer(9090);
        server.start();
    }

}
