package com.xhome.tcpserver.codec;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelDownstreamHandler;

import com.xhome.common.bean.Message;
import com.xhome.tcpserver.util.CodecUtil;

public class BinaryServerEncoder extends SimpleChannelDownstreamHandler {

    private static final Logger logger = Logger
                                         .getLogger(BinaryServerEncoder.class.getName());

    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent e)
    throws Exception {
        logger.info("server writeRequested start.");

        if(e.getMessage() instanceof Message) {
            Message msg = (Message) e.getMessage();
            ChannelBuffer totalBuffer = CodecUtil.encodeMessage(msg);
            Channels.write(ctx, e.getFuture(), totalBuffer);

        } else {
            logger.info("Channel message is not instance of Message");
        }

        logger.info("server writeRequested end.");

    }

}
