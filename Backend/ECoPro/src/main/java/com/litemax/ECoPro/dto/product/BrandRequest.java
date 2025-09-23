package com.litemax.ECoPro.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BrandRequest {

    @NotBlank(message = "Brand name is required")
    @Size(max = 255, message = "Brand name cannot exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    private String logoUrl;
    private String bannerUrl;
    private String websiteUrl;
    private boolean active = true;
    private boolean featured = false;
    private Integer sortOrder = 0;

    @Size(max = 255, message = "Meta title cannot exceed 255 characters")
    private String metaTitle;

    @Size(max = 500, message = "Meta description cannot exceed 500 characters")
    private String metaDescription;

    @Size(max = 1000, message = "Meta keywords cannot exceed 1000 characters")
    private String metaKeywords;

    @Size(max = 100, message = "Origin country cannot exceed 100 characters")
    private String originCountry;
}