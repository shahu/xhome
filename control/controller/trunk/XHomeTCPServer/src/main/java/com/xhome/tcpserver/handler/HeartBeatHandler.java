package com.xhome.tcpserver.handler;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.timeout.IdleState;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.jboss.netty.handler.timeout.IdleStateEvent;

import com.xhome.tcpserver.codec.BinaryClientEncoder;

public class HeartBeatHandler extends IdleStateAwareChannelHandler {

    private static final Logger logger = Logger
                                         .getLogger(BinaryClientEncoder.class.getName());

    @Override
    public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent e)
    throws Exception {
        // TODO Auto-generated method stub
        super.channelIdle(ctx, e);

        if(e.getState() == IdleState.READER_IDLE) {
            e.getChannel().close();
            logger.warn("Channel: " + e.getChannel() + " is idle, close it");

        }
    }

}
