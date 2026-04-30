package com.example.library.common.vo;

import java.util.Objects;

public class Item {
    private Long no;
    private String title;

    public Item() {
    }

    public Item(Long no, String title) {
        this.no = no;
        this.title = title;
    }

    public Long getNo() {
        return no;
    }

    public void setNo(Long no) {
        this.no = no;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Item item)) {
            return false;
        }
        return Objects.equals(no, item.no) && Objects.equals(title, item.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(no, title);
    }
}
