package com.xhome.common.util;

import java.net.SocketAddress;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;

import com.xhome.common.dataenum.DataType;

public class ProtocolUtil {

    public static ChannelBuffer encode(List<Object> data) {

        if(null == data || data.size() == 0) {
            return ChannelBuffers.EMPTY_BUFFER;
        }

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer(
                                   ByteOrder.LITTLE_ENDIAN, 256);

        for(Object obj : data) {
            if(obj.getClass().equals(Integer.class)) {
                buffer.writeInt(DataType.Integer.getValue()); // Write data type
                buffer.writeInt((Integer) obj);

            } else if(obj.getClass().equals(String.class)) {
                buffer.writeInt(DataType.STRING.getValue());
                String tmp = (String) obj;
                buffer.writeInt(tmp.length());
                buffer.writeBytes(tmp.getBytes());

            } else if(obj.getClass().equals(Long.class)) {
                buffer.writeInt(DataType.LONG.getValue());
                buffer.writeLong((Long) obj);

            } else if(obj.getClass().equals(ArrayList.class)) {
                // TODO
            } else {

                // TODO
            }
        }

        return buffer;
    }

    public static List<Object> decoder(ChannelBuffer dataBuffer) {

        if(dataBuffer == null || dataBuffer.readableBytes() == 0) {
            return null;
        }

        int processIndex = 0, length = dataBuffer.readableBytes();
        List<Object> data = new ArrayList<Object>();

        while(processIndex < length) {
            int readLength = 0;
            int type = dataBuffer.readInt(); // read type
            readLength += 4;

            if(type == DataType.Integer.getValue()) {
                data.add(dataBuffer.readInt());
                readLength += 4;

            } else if(type == DataType.STRING.getValue()) {
                int size = dataBuffer.readInt();
                readLength += 4;
                byte[] contents = new byte[size];
                dataBuffer.readBytes(contents);
                data.add(new String(contents));
                readLength += size;

            } else if(type == DataType.LONG.getValue()) {
                data.add(dataBuffer.readLong());
                readLength += 8;

            } else if(type == DataType.ARRAY.getValue()) {

                // TODO
            } else if(type == DataType.STRUCT.getValue()) {
                // TODO
            }

            processIndex += readLength;

        }

        return data;
    }

    public static String getClientIp(Channel channel) {
        /**
         * 获取客户端IP
         */
        SocketAddress address = channel.getRemoteAddress();
        String ip = "";

        if(address != null) {
            ip = address.toString().trim();
            int index = ip.lastIndexOf(':');

            if(index < 1) {
                index = ip.length();
            }

            ip = ip.substring(1, index);
        }

        if(ip.length() > 15) {
            ip = ip.substring(Math.max(ip.indexOf("/") + 1, ip.length() - 15));
        }

        return ip;
    }

}
