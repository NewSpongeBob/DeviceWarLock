package com.xiaoc.warlock.Util;

public class InfoValue {
    private int s;          // 状态: 0成功, -1失败, -2未获取
    private Object v;       // 值，可以是String或List

    public InfoValue() {
        this.s = -2;
        this.v = null;
    }

    public InfoValue(int status, Object value) {
        this.s = status;
        this.v = value;
    }

    public static InfoValue success(Object value) {
        return new InfoValue(0, value);
    }

    public static InfoValue fail() {
        return new InfoValue(-1, null);
    }

    public static InfoValue notCollected() {
        return new InfoValue(-2, null);
    }
}
