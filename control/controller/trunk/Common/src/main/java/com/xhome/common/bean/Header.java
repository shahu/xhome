package com.xhome.common.bean;

public class Header {
    private Integer version; //协议版本号
    private Integer seqNo;		//消息序列号
    private Integer timeStamp;	//时间戳
    private Integer reserved;	//保留字段
    private Integer bodyLength;	//Body长度

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getVersion() {
        return version;
    }

    public void setSeqNo(Integer seqNo) {
        this.seqNo = seqNo;
    }

    public Integer getSeqNo() {
        return seqNo;
    }

    public void setTimeStamp(Integer timeStamp) {
        this.timeStamp = timeStamp;
    }

    public Integer getTimeStamp() {
        return (int)(System.currentTimeMillis() / 1000);
    }

    public void setReserved(Integer reserved) {
        this.reserved = reserved;
    }

    public Integer getReserved() {
        return reserved;
    }


    public void setBodyLength(Integer bodyLength) {
        this.bodyLength = bodyLength;
    }

    public Integer getBodyLength() {
        return bodyLength;
    }

    @Override
    public String toString() {
        return "Header [version=" + version + ", seqno=" + seqNo
               + ", timestamp=" + timeStamp + ", reserved=" + reserved
               + " bodylength=" + bodyLength
               + "]";

    }

}
