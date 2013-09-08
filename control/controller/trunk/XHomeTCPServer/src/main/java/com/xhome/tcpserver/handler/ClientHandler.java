package com.xhome.tcpserver.handler;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import com.xhome.common.bean.Message;

public class ClientHandler extends SimpleChannelHandler {

    private static final Logger logger = Logger.getLogger(ClientHandler.class);

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
    throws Exception {
        logger.info("Client MessageReceived");

        if(e.getMessage() instanceof Message) {
            Message msg = (Message) e.getMessage();
            logger.info("Recevied message from server: " + msg.toString());

        } else {
            logger.info("Recevied message is not instance of Mssage");
        }

        logger.info("Client MessageReceived end");
        e.getChannel().close();

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
    throws Exception {
        logger.error("Client handler exception:", e.getCause());
        e.getChannel().close();
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e)
    throws Exception {
        logger.info("Client channelConnected");

        logger.info("Client channelConnected end");

    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e)
    throws Exception {
        logger.info("Client channelClosed");

        logger.info("Client channelClosed end");
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx,
                                    ChannelStateEvent e) throws Exception {
        logger.info("Client channelDisconnected");
        super.channelDisconnected(ctx, e);

        logger.info("Client channelDisconnected end");
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e)
    throws Exception {
        logger.info("Client channelOpen");
        // 增加通道

        logger.info("Client channelOpen end");

    }

}
