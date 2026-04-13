package com.example.helpdesk.dto;

import java.util.List;

public final class MetaModels {

    private MetaModels() {
    }

    public record OptionItem(
            String value,
            String label
    ) {
    }

    public record MetaResponse(
            List<OptionItem> statuses,
            List<OptionItem> priorities,
            List<OptionItem> roles,
            List<OptionItem> categories
    ) {
    }
}
