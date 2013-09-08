package com.xhome.tcpserver.impl;

import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.CharsetUtil;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

import com.xhome.tcpserver.api.IContorlServer;
import com.xhome.tcpserver.codec.BinaryClientEncoder;
import com.xhome.tcpserver.codec.BinaryServerDecoder;
import com.xhome.tcpserver.codec.BinaryServerEncoder;
import com.xhome.tcpserver.handler.HeartBeatHandler;
import com.xhome.tcpserver.handler.ServerHandler;

public class TCPServer implements IContorlServer {

    public int port = 8080;
    public static final Logger logger = Logger.getLogger(TCPServer.class);
    public ChannelGroup allChannels = new DefaultChannelGroup("TCPServer");
    private ServerBootstrap serverBootstrap = new ServerBootstrap(
        new NioServerSocketChannelFactory(Executors.newCachedThreadPool(),
                                          Executors.newCachedThreadPool()));

    public TCPServer(int port) {
        this.port = port;
    }

    @Override
    public boolean start() {
        /**
         * 采用默认ChannelPipeline管道 这意味着同一个Handler实例将被多个Channel通道共享
         * 这种方式对于Handler中无有状态的成员变量是可以的，并且可以提高性能！
         */
        ChannelPipeline pipeline = serverBootstrap.getPipeline();
        // pipeline.addLast("frameDecoder", new DelimiterBasedFrameDecoder(80,
        // Delimiters.lineDelimiter()));
        // pipeline.addLast("stringDecoder", new
        // StringDecoder(CharsetUtil.UTF_8));
        // pipeline.addLast("timeout", new IdleStateHandler(new
        // HashedWheelTimer(), 10, 10, 0));//此两项为添加心跳机制
        // 10秒查看一次在线的客户端channel是否空闲，IdleStateHandler为netty jar包中提供的类
        // pipeline.addLast("hearbeat", new HeartBeatHandler());//
        pipeline.addLast("encoder", new BinaryServerEncoder());
        pipeline.addLast("decoder", new BinaryServerDecoder());
        pipeline.addLast("handler", new ServerHandler());

        serverBootstrap.setOption("child.tcpNoDelay", true); // 注意child前缀
        serverBootstrap.setOption("child.keepAlive", true); // 注意child前缀
        serverBootstrap.setOption("child.bufferFactory",
                                  new HeapChannelBufferFactory(ByteOrder.LITTLE_ENDIAN));

        // ServerBootstrap对象的bind方法返回了一个绑定了本地地址的服务端Channel通道对象

        try {
            Channel channel = serverBootstrap.bind(new InetSocketAddress(port));
            allChannels.add(channel);
            logger.info("server is started on port " + port);
            return true;

        } catch(ChannelException ex) {
            logger.error("Bind exception:", ex);
            serverBootstrap.releaseExternalResources();
        }

        return false;

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
             * 主动关闭服务器
             */
            ChannelGroupFuture future = allChannels.close();
            future.awaitUninterruptibly();// 阻塞，直到服务器关闭
            // serverBootstrap.releaseExternalResources();

        } catch(Exception e) {
            logger.error(e.getMessage(), e);

        } finally {
            logger.info("server is shutdown on port " + port);
            System.exit(1);
        }

        return true;

    }

}
