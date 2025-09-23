package com.litemax.ECoPro.dto.product;

import com.litemax.ECoPro.entity.product.ProductMedia;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductMediaRequest {

    @NotBlank(message = "File URL is required")
    private String fileUrl;

    private String fileName;
    private Long fileSize;
    private String mimeType;
    private ProductMedia.MediaType type = ProductMedia.MediaType.IMAGE;
    private String altText;
    private boolean primary = false;
    private Integer sortOrder = 0;
    private Integer width;
    private Integer height;
}