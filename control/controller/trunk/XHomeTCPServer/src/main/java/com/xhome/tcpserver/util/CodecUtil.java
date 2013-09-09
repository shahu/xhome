package com.xhome.tcpserver.util;

import java.nio.ByteOrder;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;

import com.xhome.common.bean.Message;
import com.xhome.common.util.ProtocolUtil;

public class CodecUtil {
    private static Logger logger = Logger.getLogger(CodecUtil.class);

    public static ChannelBuffer encodeMessage(Message msg) {
        if(null != msg) {
            ChannelBuffer headBuffer = ChannelBuffers.buffer(
                                           ByteOrder.LITTLE_ENDIAN, 20);
            ChannelBuffer dataBuffer = ProtocolUtil.encode(msg.getData());
            headBuffer.writeInt(msg.getHeader().getVersion());
            headBuffer.writeInt(msg.getHeader().getSeqNo());
            headBuffer.writeInt(msg.getHeader().getTimeStamp());
            headBuffer.writeInt(msg.getHeader().getReserved());
            headBuffer.writeInt(dataBuffer.readableBytes());
            logger.info("Header buffer size: " + headBuffer.readableBytes());
            logger.info("Header buffer : " + headBuffer);
            logger.info("Data buffer size: " + dataBuffer.readableBytes());
            logger.info("Data buffer : " + dataBuffer);
            ChannelBuffer totalBuffer = ChannelBuffers.buffer(
                                            ByteOrder.LITTLE_ENDIAN, headBuffer.readableBytes()
                                            + dataBuffer.readableBytes());
            totalBuffer.writeBytes(headBuffer);
            totalBuffer.writeBytes(dataBuffer);
            return totalBuffer;
        }

        return ChannelBuffers.EMPTY_BUFFER;
    }
}
