package com.xiaoc.warlock.ui.adapter;

import java.util.ArrayList;
import java.util.List;

public class InfoItem {
    private String title;
    private String content;
    private List<DetailItem> details;
    private boolean isExpanded;

    public static class DetailItem {
        private String key;
        private String value;

        public DetailItem(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() { return key; }
        public String getValue() { return value; }
    }

    public InfoItem(String title, String content) {
        this.title = title;
        this.content = content;
        this.details = new ArrayList<>();
        this.isExpanded = false;
    }

    public void addDetail(String key, String value) {
        details.add(new DetailItem(key, value));
    }

    public String getTitle() { return title; }
    public String getContent() { return content; }
    public List<DetailItem> getDetails() { return details; }
    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }
}