package com.moneta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    private String name;

    @NotNull(message = "Category type is required (INCOME or EXPENSE)")
    private String type;

    public CategoryRequest() {
    }

    public CategoryRequest(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
