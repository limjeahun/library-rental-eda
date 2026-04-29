package com.example.library.common.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class IDName {
    @Column(name = "id")
    private String id;

    @Column(name = "name")
    private String name;

    public IDName() {
    }

    public IDName(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IDName idName)) {
            return false;
        }
        return Objects.equals(id, idName.id) && Objects.equals(name, idName.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
