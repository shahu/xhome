package com.xhome.tcpserver;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

import com.xhome.common.bean.Header;
import com.xhome.common.bean.Message;
import com.xhome.common.dataenum.CommandType;
import com.xhome.tcpserver.impl.TCPClient;

public class App1 {

    public static void main(String[] args) {
        int port = 8080;
        String host = "localhost";
        TCPClient client = new TCPClient(host, port);
        client.start();
        ChannelFuture channelFuture = client.getChannelFuture();

        if(null != channelFuture && channelFuture.isSuccess()) {
            Header header = new Header();
            header.setVersion(0);
            header.setSeqNo(1);
            header.setReserved(0);
            header.setCommandType(CommandType.HEARDBEAT.getValue());
            String data = "Hello world";
            List<Object> obj = new ArrayList<Object>();
            obj.add(data);
            Message sendMsg = new Message();
            sendMsg.setHeader(header);
            sendMsg.setData(obj);
            channelFuture.getChannel().write(sendMsg);

            /*channelFuture.addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) {
                   future.getChannel().close();
                }
            });*/
            while(!channelFuture.getChannel().getCloseFuture().isSuccess()) {
                try {
                    System.out.println("wait close");
                    Thread.sleep(50);

                } catch(Exception e) {
                    // TODO: handle exception
                }
            }

            client.stop();
        }
    }

}
