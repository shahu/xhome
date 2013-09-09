package com.xhome.tcpserver;

import java.util.ArrayList;
import java.util.List;

import org.jboss.netty.channel.ChannelFuture;

import com.xhome.common.bean.Header;
import com.xhome.common.bean.Message;
import com.xhome.tcpserver.impl.TCPClient;

public class CamApp {
    public static void main(String[] args) {
        int port = 8080;
        String host = "localhost";
        TCPClient client = new TCPClient(host, port);
        client.start();
        ChannelFuture channelFuture = client.getChannelFuture();

        if(null != channelFuture && channelFuture.isSuccess()) {
            Header header = new Header();
            header.setVersion(0);
            header.setSeqNo(2);

            header.setReserved(0);
            List<Object> obj = new ArrayList<Object>();
            obj.add(4);
            obj.add("1234-4567-890");
            obj.add("cam1");
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
                    //System.out.println("wait close");
                    Thread.sleep(50);

                } catch(Exception e) {
                    // TODO: handle exception
                }
            }

            System.out.println("wait close");
            client.stop();
        }
    }
}
