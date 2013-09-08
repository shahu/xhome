package com.xhome.common.bean;

import java.util.List;

public class Message {

    private Header header;
    private List<Object> data;

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

        buffer.append("; Data[");

        if(data != null) {
            for(Object obj : data) {
                buffer.append(String.valueOf(obj));
            }
        }

        buffer.append("]");
        return buffer.toString();
    }

}
