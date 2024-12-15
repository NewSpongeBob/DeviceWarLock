package com.xiaoc.warlock.Util;

import com.xiaoc.warlock.ui.adapter.InfoItem;

public class WarningBuilder {
    private InfoItem infoItem;

    public WarningBuilder(String title, String content) {
        this.infoItem = new InfoItem(title, content);
    }

    public WarningBuilder addDetail(String key, String value) {
        infoItem.addDetail(key, value);
        return this;
    }

    public InfoItem build() {
        return infoItem;
    }
}