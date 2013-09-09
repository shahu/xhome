package com.xhome.common.bean;

import java.util.List;

public class Message {

    private Header header;		//头信息
    private List<Object> data;	//内容信息
    private Integer messageType;	//消息类型

    public void setMessageType(Integer typeId) {
        this.messageType = typeId;
    }

    public Integer getMessageType() {
        return this.messageType;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public Header getHeader() {
        return header;
    }

    public void setData(List<Object> data) {
        this.data = data;
    }

    public List<Object> getData() {
        return data;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        if(header != null) {
            buffer.append(header.toString());
        }

        buffer.append(" Message type[" + messageType + "]");
        buffer.append("; Data[");

        if(data != null) {
            for(Object obj : data) {
                buffer.append(" " + String.valueOf(obj));
            }
        }

        buffer.append("]");
        return buffer.toString();
    }

}
