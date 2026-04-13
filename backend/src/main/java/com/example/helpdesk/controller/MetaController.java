package com.example.helpdesk.controller;

import com.example.helpdesk.dto.MetaModels;
import com.example.helpdesk.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meta")
public class MetaController {

    private final CatalogService catalogService;

    public MetaController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public MetaModels.MetaResponse getMeta() {
        return catalogService.getMeta();
    }
}
