package com.example.helpdesk.domain;

public enum TicketStatus {
    DRAFT("\u8349\u7a3f"),
    PENDING("\u5f85\u5904\u7406"),
    PROCESSING("\u5904\u7406\u4e2d"),
    RESOLVED("\u5df2\u89e3\u51b3"),
    NEW("\u5f85\u5904\u7406"),
    CLOSED("\u5df2\u89e3\u51b3");

    private final String label;

    TicketStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public TicketStatus toDisplayStatus() {
        return toDisplayStatus(false);
    }

    public TicketStatus toDisplayStatus(boolean assigned) {
        return switch (this) {
            case NEW -> PENDING;
            case PENDING -> assigned ? PROCESSING : PENDING;
            case CLOSED -> RESOLVED;
            default -> this;
        };
    }

    public boolean isVisibleStatus() {
        return this == DRAFT || this == PENDING || this == PROCESSING || this == RESOLVED;
    }

    public boolean isResolvedLike() {
        return this == RESOLVED || this == CLOSED;
    }

    public boolean hasStartedProcessing() {
        return this == PROCESSING || isResolvedLike();
    }

    public boolean hasStartedProcessing(boolean assigned) {
        TicketStatus displayStatus = toDisplayStatus(assigned);
        return displayStatus == PROCESSING || displayStatus == RESOLVED;
    }
}
