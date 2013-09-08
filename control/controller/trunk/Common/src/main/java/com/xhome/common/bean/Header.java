package com.xhome.common.bean;

public class Header {
    private Integer version;
    private Integer seqNo;
    private Integer timeStamp;
    private Integer reserved;
    private Integer commandType;
    private Integer bodyLength;

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

    public void setCommandType(Integer commandType) {
        this.commandType = commandType;
    }

    public Integer getCommandType() {
        return commandType;
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
               + ", commandtype=" + commandType + ", bodylength=" + bodyLength
               + "]";

    }

}
