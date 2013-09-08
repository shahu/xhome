package com.xhome.tcpserver.handler;

import java.nio.channels.Channel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import com.xhome.common.bean.Header;
import com.xhome.common.bean.Message;
import com.xhome.common.dataenum.CommandType;

public class ServerHandler extends SimpleChannelHandler {

    private static final Logger logger = Logger.getLogger(ClientHandler.class);

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
    throws Exception {
        logger.info("Server MessageReceived " + e.getChannel());

        if(e.getMessage() instanceof Message) {
            Message msg = (Message)e.getMessage();
            logger.info("Recevied message from client: " + msg.toString());

            if(msg.getHeader().getCommandType().equals(CommandType.ROTATE_RESP.getValue())) {

            } else if(msg.getHeader().getCommandType().equals(CommandType.HEARDBEAT.getValue())) {
                //logger.info("Bind client id to channel");
                Header header = new Header();
                header.setVersion(msg.getHeader().getVersion());
                header.setSeqNo(msg.getHeader().getSeqNo());
                header.setReserved(msg.getHeader().getReserved());
                header.setCommandType(msg.getHeader().getCommandType());
                String data = "Response from server, hello world again";
                List<Object> obj = new ArrayList<Object>();
                obj.add(data);
                Message sendMsg = new Message();
                sendMsg.setHeader(header);
                sendMsg.setData(obj);
                Channels.write(ctx, e.getFuture(), sendMsg);
            }

        } else {
            logger.info("Recevied message is not instance of Mssage");
        }

        /*
         * ChannelBuffer buf = (ChannelBuffer) e.getMessage();
         * while(buf.readable()) { System.out.println((char) buf.readByte()); }
         */
        //logger.info(e.getMessage());

        logger.info("Server MessageReceived end " + e.getChannel());

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
    throws Exception {
        logger.error("Server handler exception:", e.getCause());

    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e)
    throws Exception {
        logger.info("Server channelConnected " + e.getChannel());

        logger.info("Server channelConnected end " + e.getChannel());

    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e)
    throws Exception {
        logger.info("Server channelClosed " + e.getChannel());
        super.closeRequested(ctx, e);
        logger.info("Server channelClosed end " + e.getChannel());
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx,
                                    ChannelStateEvent e) throws Exception {
        logger.info("Server channelDisconnected " + e.getChannel());
        super.channelDisconnected(ctx, e);

        logger.info("Server channelDisconnected end " + e.getChannel());
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e)
    throws Exception {
        logger.info("Server channelOpen " + e.getChannel());
        // 增加通道
        super.channelOpen(ctx, e);
        logger.info("Server channelOpen end " + e.getChannel());

    }

}
