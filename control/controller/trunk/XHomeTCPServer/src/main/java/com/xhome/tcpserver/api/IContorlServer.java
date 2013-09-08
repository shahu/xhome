package com.xhome.tcpserver.api;

public interface IContorlServer {

    /**
    * 启动服务器
    */
    public boolean start();
    /**
     * 重启程序
     */
    public boolean restart();

    /**
     * 停止程序运行
     */
    public boolean stop();


}
