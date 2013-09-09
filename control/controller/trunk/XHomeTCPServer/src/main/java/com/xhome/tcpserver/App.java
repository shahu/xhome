package com.xhome.tcpserver;

import javax.sound.midi.SysexMessage;

import org.jboss.netty.buffer.ChannelBuffers;

import com.xhome.tcpserver.impl.TCPServer;

public class App {

    public static void main(String[] args) {
        int port = 8080;
        TCPServer server = new TCPServer(port);
        boolean isStart = server.start();
        System.out.println(isStart ? "Start Ok!!!!!!" : "Start failed!!!!!");
    }

}
