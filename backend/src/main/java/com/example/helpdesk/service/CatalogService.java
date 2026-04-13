package com.example.helpdesk.service;

import com.example.helpdesk.domain.Priority;
import com.example.helpdesk.domain.Role;
import com.example.helpdesk.domain.TicketStatus;
import com.example.helpdesk.dto.MetaModels;
import com.example.helpdesk.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class CatalogService {

    private static final List<String> CATEGORIES = List.of(
            "软件问题",
            "权限申请",
            "设备故障",
            "网络问题",
            "数据支持",
            "其他"
    );

    public List<String> getCategories() {
        return CATEGORIES;
    }

    public void validateCategory(String category) {
        if (!CATEGORIES.contains(category)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "不支持的问题类别: " + category);
        }
    }

    public MetaModels.MetaResponse getMeta() {
        return new MetaModels.MetaResponse(
                Arrays.stream(TicketStatus.values())
                        .filter(TicketStatus::isVisibleStatus)
                        .map(item -> new MetaModels.OptionItem(item.name(), item.getLabel()))
                        .toList(),
                Arrays.stream(Priority.values())
                        .map(item -> new MetaModels.OptionItem(item.name(), item.getLabel()))
                        .toList(),
                Arrays.stream(Role.values())
                        .map(item -> new MetaModels.OptionItem(item.name(), item.getLabel()))
                        .toList(),
                CATEGORIES.stream()
                        .map(item -> new MetaModels.OptionItem(item, item))
                        .toList()
        );
    }
}
