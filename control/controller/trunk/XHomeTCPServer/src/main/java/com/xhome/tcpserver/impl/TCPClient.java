package com.xhome.tcpserver.impl;

import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.nio.channels.ShutdownChannelGroupException;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.jboss.netty.util.CharsetUtil;

import com.xhome.tcpserver.api.IContorlServer;
import com.xhome.tcpserver.codec.BinaryClientEncoder;
import com.xhome.tcpserver.codec.BinaryServerDecoder;
import com.xhome.tcpserver.handler.ClientHandler;

public class TCPClient implements IContorlServer {
    private int port = 8080;
    private String host = "localhost";
    private static final Logger logger = Logger.getLogger(TCPClient.class);
    private NioClientSocketChannelFactory clientSocketChannelFactory = new NioClientSocketChannelFactory(
        Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
    private ClientBootstrap clientBootstrap = new ClientBootstrap(
        clientSocketChannelFactory);
    private ChannelFuture channelFuture;


    public TCPClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public ChannelFuture getChannelFuture() {
        return channelFuture;
    }

    @Override
    public boolean start() {
        /**
         * 注意：由于client中有状态的成员变量，因此不能采用默认共享ChannelPipeline的方式
         * 例如，下面的代码形式是错误的： ChannelPipeline
         * pipeline=clientBootstrap.getPipeline(); pipeline.addLast("handler",
         * new XLClientHandler());
         */
        ChannelPipeline pipeline = clientBootstrap.getPipeline();
        pipeline.addLast("encoder", new BinaryClientEncoder());
        pipeline.addLast("decoder", new BinaryServerDecoder());
        pipeline.addLast("handler", new ClientHandler());

        // clientBootstrap.setPipelineFactory(new XLClientPipelineFactory());
        // //只能这样设置
        /**
         * 请注意，这里不存在使用“child.”前缀的配置项，客户端的SocketChannel实例不存在父级Channel对象
         */
        clientBootstrap.setOption("tcpNoDelay", true);
        clientBootstrap.setOption("keepAlive", true);
        clientBootstrap.setOption("bufferFactory", new
                                  HeapChannelBufferFactory(ByteOrder.LITTLE_ENDIAN));


        /*clientBootstrap.setOption("readBufferSize", 1024);
        clientBootstrap.setOption("writeBufferSize", 1024); */


        channelFuture = clientBootstrap.connect(new InetSocketAddress(host,
                                                port));
        /**
         * 阻塞式的等待，直到ChannelFuture对象返回这个连接操作的成功或失败状态
         */
        channelFuture.awaitUninterruptibly();

        /**
         * 如果连接失败，我们将打印连接失败的原因。
         * 如果连接操作没有成功或者被取消，ChannelFuture对象的getCause()方法将返回连接失败的原因。
         */
        if(!channelFuture.isSuccess()) {
            logger.error("Client connect failed", channelFuture.getCause());
            stop();
            return false;

        } else {
            logger.info("client is connected to server " + host + ":" + port);

        }

        return true;
    }

    @Override
    public boolean restart() {
        stop();
        start();
        return true;
    }

    @Override
    public boolean stop() {
        try {
            /**
             * 主动关闭客户端连接，会阻塞等待直到通道关闭
             */
            if(null != channelFuture) {
                channelFuture.getChannel().close().awaitUninterruptibly();
                // future.getChannel().getCloseFuture().awaitUninterruptibly();
                /**
                 * 释放ChannelFactory通道工厂使用的资源。 这一步仅需要调用
                 * releaseExternalResources()方法即可。 包括NIO
                 * Secector和线程池在内的所有资源将被自动的关闭和终止。
                 */
                clientBootstrap.releaseExternalResources();
            }

        } catch(Exception e) {
            logger.error(e.getMessage(), e);

        } finally {
            System.exit(1);
            logger.info("client is shutdown to server " + host + ":" + port);
        }

        return true;
    }

}
