package com.litemax.ECoPro.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 255, message = "Category name cannot exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    private String imageUrl;
    private String iconUrl;
    private boolean active = true;
    private boolean featured = false;
    private Integer sortOrder = 0;

    @Size(max = 255, message = "Meta title cannot exceed 255 characters")
    private String metaTitle;

    @Size(max = 500, message = "Meta description cannot exceed 500 characters")
    private String metaDescription;

    @Size(max = 1000, message = "Meta keywords cannot exceed 1000 characters")
    private String metaKeywords;

    private Long parentId;
}