package com.xhome.tcpserver;

import javax.sound.midi.SysexMessage;

import org.jboss.netty.buffer.ChannelBuffers;

import com.xhome.tcpserver.impl.TCPServer;

public class App {

    public static void main(String[] args) {
        int port = 8080;
        TCPServer server = new TCPServer(port);
        server.start();

    }

}
