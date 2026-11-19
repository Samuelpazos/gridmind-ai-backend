package com.gridmind.controller;

import com.gridmind.service.OperationalCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/catalogs")
public class OperationalCatalogController {

    private final OperationalCatalogService catalogService;

    public OperationalCatalogController(OperationalCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/overview")
    public Map<String, Long> getCatalogInventory() {
        return catalogService.getCatalogInventory();
    }
}
