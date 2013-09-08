package com.xhome.tcpserver.codec;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;


import com.xhome.common.bean.Message;
import com.xhome.tcpserver.util.CodecUtil;

public class BinaryClientEncoder  extends OneToOneEncoder  {

    private static final Logger logger = Logger
                                         .getLogger(BinaryClientEncoder.class.getName());

    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel channel,
                            Object msg) throws Exception {
        logger.info("client encode start.");

        if(msg instanceof Message) {
            Message sendMsg = (Message) msg;

            ChannelBuffer totalBuffer = CodecUtil.encodeMessage(sendMsg);
            return totalBuffer;

        } else {
            logger.info("Channel message is not instance of Message");
        }

        logger.info("client encode end.");
        return ChannelBuffers.EMPTY_BUFFER;
    }

    /*@Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent e)
    		throws Exception {
    	logger.info("client writeRequested start.");

    	if (e.getMessage() instanceof Message) {
    		Message msg = (Message) e.getMessage();

    		ChannelBuffer totalBuffer = CodecUtil.encodeMessage(msg);
    		Channels.write(ctx, e.getFuture(), totalBuffer);

    	}
    	else
    		logger.info("Channel message is not instance of Message");
    	logger.info("client writeRequested end.");

    }*/

}
