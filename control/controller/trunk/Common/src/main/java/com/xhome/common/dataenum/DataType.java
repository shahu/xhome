package com.xhome.common.dataenum;

public enum DataType {
    Integer(0),
    LONG(1),
    STRING(2),
    ARRAY(3),
    STRUCT(4);

    private int value;
    private DataType(int value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}
