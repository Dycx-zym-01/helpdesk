package com.example.helpdesk.domain;

public enum Role {
    EMPLOYEE("普通员工"),
    DEVELOPER("开发人员"),
    ADMIN("管理员");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
