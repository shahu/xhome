package com.xhome.common.dataenum;

public enum CommandType {
    ROTATE(0), ROTATE_RESP(1), STOP_ROTATE(2), STOP_ROTATE_RESP(3), REGIST(
        4), HEARDBEAT(5);

    private Integer value;

    CommandType(int value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

}
