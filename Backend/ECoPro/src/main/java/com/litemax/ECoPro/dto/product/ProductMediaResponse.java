package com.litemax.ECoPro.dto.product;

import com.litemax.ECoPro.entity.product.ProductMedia;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProductMediaResponse {
    private Long id;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private ProductMedia.MediaType type;
    private String altText;
    private boolean primary;
    private Integer sortOrder;
    private Integer width;
    private Integer height;
    private LocalDateTime createdAt;
}