package com.xhome.tcpserver.handler;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import com.xhome.common.bean.DeviceMetadata;
import com.xhome.common.bean.Header;
import com.xhome.common.bean.Message;
import com.xhome.common.dataenum.CommandType;
import com.xhome.common.util.ProtocolUtil;
import com.xhome.tcpserver.impl.TCPServer;

public class ServerHandler extends SimpleChannelHandler {

    private static final Logger logger = Logger.getLogger(ClientHandler.class);

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
    throws Exception {
        logger.info("Server MessageReceived " + e.getChannel());

        if(e.getMessage() instanceof Message) {
            Message msg = (Message) e.getMessage();
            logger.info("Recevied message from client: " + msg.toString());

            // 转动摄像头
            if(msg.getMessageType().equals(CommandType.ROTATE.getValue())) {
                logger.info("Rotate message: " + msg.toString());
                String deviceId = msg.getData().get(4).toString();
                Channel targetChannel = TCPServer.allChannels.find(deviceId);
                List<Object> responseData = new ArrayList<Object>();

                if(targetChannel != null) {
                    targetChannel.write(msg);

                    // 发送完目标之后，通知来源方
                    responseData.add(msg.getMessageType());
                    responseData.add("Send done");
                    msg.setData(responseData);

                } else {
                    responseData.add(msg.getMessageType());
                    responseData.add("Send failed");
                    msg.setData(responseData);
                    logger.warn("Could not find channl of device id: "
                                + deviceId);
                }

                Channels.write(ctx, e.getFuture(), msg);

            }

            // 转动回复
            else if(msg.getMessageType().equals(
                        CommandType.ROTATE_RESP.getValue())) {
                logger.info("Rotate response result: " + msg.getData().get(1));
            }

            // 停止转动摄像头
            else if(msg.getMessageType().equals(
                        CommandType.STOP_ROTATE.getValue())) {
                logger.info("Stop rotate message: " + msg.toString());
                String deviceId = msg.getData().get(1).toString();
                Channel targetChannel = TCPServer.allChannels.find(deviceId);
                List<Object> responseData = new ArrayList<Object>();

                if(targetChannel != null) {
                    targetChannel.write(msg);

                    responseData.add(msg.getMessageType());
                    responseData.add("Send done");
                    msg.setData(responseData);

                } else {
                    responseData.add(msg.getMessageType());
                    responseData.add("Send failed");
                    msg.setData(responseData);
                    logger.warn("Could not find channl of device id: "
                                + deviceId);
                }

                Channels.write(ctx, e.getFuture(), msg);
            }

            // 停止转动回复
            else if(msg.getMessageType().equals(
                        CommandType.STOP_ROTATE_RESP.getValue())) {
                logger.info("Stop rotate response result: "
                            + msg.getData().get(1));
            }

            // 客户端注册
            else if(msg.getMessageType().equals(CommandType.REGIST.getValue())) {

                DeviceMetadata deviceData = new DeviceMetadata();
                deviceData.setSerialNum(msg.getData().get(1).toString());
                deviceData.setDeviceModel(msg.getData().get(2).toString());
                logger.info("Regist from client ip=>"
                            + ProtocolUtil.getClientIp(e.getChannel())
                            + " device serial num: " + deviceData.getSerialNum());
                // 对应channel绑定设备信息

                e.getFuture().getChannel().setAttachment(deviceData);

                // 加入全局列表
                if(TCPServer.allChannels
                   .containsKey(deviceData.getSerialNum())) {
                    TCPServer.allChannels.remove(deviceData.getSerialNum());
                }

                TCPServer.allChannels.add(deviceData.getSerialNum(), e
                                          .getFuture().getChannel());
                logger.info("Regist done");

            }

            // 心跳
            else if(msg.getMessageType().equals(
                        CommandType.HEARDBEAT.getValue())) {
                // logger.info("Bind client id to channel");

            }
        } else {
            logger.info("Recevied message is not instance of Mssage");
        }

        /*
         * ChannelBuffer buf = (ChannelBuffer) e.getMessage();
         * while(buf.readable()) { System.out.println((char) buf.readByte()); }
         */
        // logger.info(e.getMessage());

        logger.info("Server MessageReceived end " + e.getChannel());

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
    throws Exception {
        logger.error("Server handler exception:", e.getCause());
        e.getChannel().close();

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
