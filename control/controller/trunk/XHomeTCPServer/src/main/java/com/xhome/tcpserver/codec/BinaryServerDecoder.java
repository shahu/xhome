package com.xhome.tcpserver.codec;

import java.nio.ByteOrder;
import java.util.List;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

import com.xhome.common.bean.Header;
import com.xhome.common.bean.Message;
import com.xhome.common.util.ProtocolUtil;
import com.xhome.tcpserver.constant.Constant;

public class BinaryServerDecoder  extends FrameDecoder {

    private static final Logger logger = Logger.getLogger(BinaryServerDecoder.class);



    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel,
                            ChannelBuffer buffer) throws Exception {
        logger.info("binary decode start.");

        if(buffer.readableBytes() < Constant.HEADER_SIZE) {
            logger.info("Header size<" + Constant.HEADER_SIZE);
            return null;
        }

        buffer.markReaderIndex();
        Header header = new Header();
        header.setVersion(buffer.readInt());
        header.setSeqNo(buffer.readInt());
        header.setTimeStamp(buffer.readInt());
        header.setReserved(buffer.readInt());
        header.setBodyLength(buffer.readInt());
        logger.info("Header info: " + header.toString());

        if(buffer.readableBytes() < header.getBodyLength()) {
            buffer.resetReaderIndex();
            logger.info("Data receiving");
            return null;
        }

        ChannelBuffer dataBuffer = ChannelBuffers.buffer(ByteOrder.LITTLE_ENDIAN, header.getBodyLength());
        buffer.readBytes(dataBuffer, header.getBodyLength());
        Message msg = new Message();
        msg.setHeader(header);
        List<Object> data = ProtocolUtil.decoder(dataBuffer);

        if(null == data) {
            logger.info("Data received null data");
            return null;
        }

        msg.setMessageType((Integer)data.get(0));
        msg.setData(data);

        logger.info("binary decode end.");
        // TODO Auto-generated method stub
        return msg;
    }






}
