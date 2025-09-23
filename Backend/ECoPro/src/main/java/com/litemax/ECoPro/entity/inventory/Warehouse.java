package com.litemax.ECoPro.entity.inventory;

import com.litemax.ECoPro.entity.auth.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "warehouses", 
       indexes = {
           @Index(name = "idx_warehouse_code", columnList = "warehouseCode"),
           @Index(name = "idx_warehouse_status", columnList = "status"),
           @Index(name = "idx_warehouse_type", columnList = "type"),
           @Index(name = "idx_warehouse_location", columnList = "city,state,country")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"inventories", "manager"})
@EqualsAndHashCode(exclude = {"inventories"})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Warehouse {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Warehouse code is required")
    @Size(min = 2, max = 20, message = "Warehouse code must be between 2 and 20 characters")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Warehouse code must contain only uppercase letters, numbers, underscores, and hyphens")
    @Column(unique = true, nullable = false, length = 20)
    private String warehouseCode;
    
    @NotBlank(message = "Warehouse name is required")
    @Size(min = 2, max = 100, message = "Warehouse name must be between 2 and 100 characters")
    @Column(nullable = false, length = 100)
    private String name;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Column(length = 500)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WarehouseType type = WarehouseType.DISTRIBUTION_CENTER;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WarehouseStatus status = WarehouseStatus.ACTIVE;
    
    // Address Information
    @NotBlank(message = "Address line 1 is required")
    @Size(max = 255, message = "Address line 1 must not exceed 255 characters")
    @Column(nullable = false)
    private String addressLine1;
    
    @Size(max = 255, message = "Address line 2 must not exceed 255 characters")
    private String addressLine2;
    
    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String city;
    
    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State must not exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String state;
    
    @NotBlank(message = "Postal code is required")
    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    @Column(nullable = false, length = 20)
    private String postalCode;
    
    @NotBlank(message = "Country is required")
    @Size(max = 100, message = "Country must not exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String country;
    
    // Geographic Coordinates (for logistics optimization)
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    @Digits(integer = 2, fraction = 8, message = "Invalid latitude format")
    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;
    
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    @Digits(integer = 3, fraction = 8, message = "Invalid longitude format")
    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;
    
    // Contact Information
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    @Pattern(regexp = "^[+]?[0-9\\s\\-\\(\\)]+$", message = "Invalid phone number format")
    @Column(length = 20)
    private String phoneNumber;
    
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Column(length = 100)
    private String email;
    
    // Capacity Information
    @Min(value = 0, message = "Total capacity must be non-negative")
    @Column
    private Integer totalCapacity; // in cubic meters or square feet
    
    @Min(value = 0, message = "Available capacity must be non-negative")
    @Column
    private Integer availableCapacity;
    
    @Size(max = 20, message = "Capacity unit must not exceed 20 characters")
    @Column(length = 20)
    private String capacityUnit = "CUBIC_METERS"; // CUBIC_METERS, SQUARE_FEET, PALLETS
    
    // Operational Information
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager; // Warehouse manager
    
    @Column
    private LocalDateTime operationalSince;
    
    @Size(max = 10, message = "Time zone must not exceed 10 characters")
    @Column(length = 10)
    private String timeZone = "UTC";
    
    // Working Hours (stored as JSON or separate entity if complex)
    @Size(max = 500, message = "Operating hours must not exceed 500 characters")
    @Column(length = 500)
    private String operatingHours; // "MON-FRI: 8:00-18:00, SAT: 9:00-15:00"
    
    // Cost Information
    @DecimalMin(value = "0.00", message = "Storage cost must be non-negative")
    @Digits(integer = 8, fraction = 4, message = "Invalid storage cost format")
    @Column(precision = 12, scale = 4)
    private BigDecimal storageCostPerUnit = BigDecimal.ZERO;
    
    @DecimalMin(value = "0.00", message = "Handling cost must be non-negative")
    @Digits(integer = 8, fraction = 4, message = "Invalid handling cost format")
    @Column(precision = 12, scale = 4)
    private BigDecimal handlingCostPerItem = BigDecimal.ZERO;
    
    // Priority and Preferences
    @Min(value = 1, message = "Priority must be at least 1")
    @Max(value = 10, message = "Priority must not exceed 10")
    @Column
    private Integer priority = 5; // 1 = highest priority, 10 = lowest
    
    @Column(nullable = false)
    private Boolean canShipInternationally = false;
    
    @Column(nullable = false)
    private Boolean canReceiveReturns = true;
    
    @Column(nullable = false)
    private Boolean hasRefrigeration = false;
    
    @Column(nullable = false)
    private Boolean hasHazmatCapability = false;
    
    // Automation and Technology
    @Column(nullable = false)
    private Boolean isAutomated = false;
    
    @Size(max = 50, message = "WMS system must not exceed 50 characters")
    @Column(length = 50)
    private String wmsSystem; // Warehouse Management System
    
    @Size(max = 100, message = "Integration endpoint must not exceed 100 characters")
    @Column(length = 100)
    private String integrationEndpoint; // API endpoint for third-party systems
    
    // Compliance and Certifications
    @Size(max = 1000, message = "Certifications must not exceed 1000 characters")
    @Column(length = 1000)
    private String certifications; // JSON array or comma-separated: ISO9001,FDA,etc.
    
    @Column
    private LocalDateTime lastInspectionDate;
    
    @Column
    private LocalDateTime nextInspectionDate;
    
    // Performance Metrics
    @DecimalMin(value = "0.0", message = "Accuracy rate must be non-negative")
    @DecimalMax(value = "100.0", message = "Accuracy rate cannot exceed 100%")
    @Digits(integer = 3, fraction = 2, message = "Invalid accuracy rate format")
    @Column(precision = 5, scale = 2)
    private BigDecimal accuracyRate; // Picking accuracy percentage
    
    @DecimalMin(value = "0.0", message = "Average processing time must be non-negative")
    @Digits(integer = 8, fraction = 2, message = "Invalid processing time format")
    @Column(precision = 10, scale = 2)
    private BigDecimal avgProcessingTimeHours;
    
    // Audit fields
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @Size(max = 100, message = "Created by must not exceed 100 characters")
    @Column(length = 100)
    private String createdBy;
    
    @Size(max = 100, message = "Updated by must not exceed 100 characters")
    @Column(length = 100)
    private String updatedBy;
    
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    @Column(length = 1000)
    private String notes;
    
    // Relationships
    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Inventory> inventories;
    
    // Enums
    public enum WarehouseType {
        DISTRIBUTION_CENTER("Distribution Center"),
        FULFILLMENT_CENTER("Fulfillment Center"),
        CROSS_DOCK("Cross-Dock Facility"),
        COLD_STORAGE("Cold Storage"),
        BONDED_WAREHOUSE("Bonded Warehouse"),
        MANUFACTURING("Manufacturing Facility"),
        RETURNS_CENTER("Returns Processing Center"),
        THIRD_PARTY("Third-Party Logistics"),
        RETAIL_STORE("Retail Store Warehouse"),
        DROPSHIP_SUPPLIER("Dropship Supplier");
        
        private final String displayName;
        
        WarehouseType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum WarehouseStatus {
        ACTIVE("Active"),
        INACTIVE("Inactive"),
        MAINTENANCE("Under Maintenance"),
        CONSTRUCTION("Under Construction"),
        TEMPORARY_CLOSURE("Temporarily Closed"),
        DECOMMISSIONED("Decommissioned");
        
        private final String displayName;
        
        WarehouseStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // Utility Methods
    public String getFullAddress() {
        StringBuilder address = new StringBuilder();
        address.append(addressLine1);
        
        if (addressLine2 != null && !addressLine2.trim().isEmpty()) {
            address.append(", ").append(addressLine2);
        }
        
        address.append(", ").append(city)
               .append(", ").append(state)
               .append(" ").append(postalCode)
               .append(", ").append(country);
        
        return address.toString();
    }
    
    public boolean isOperational() {
        return status == WarehouseStatus.ACTIVE;
    }
    
    public boolean hasAvailableCapacity() {
        return availableCapacity != null && availableCapacity > 0;
    }
    
    public double getCapacityUtilization() {
        if (totalCapacity == null || totalCapacity == 0) {
            return 0.0;
        }
        
        int usedCapacity = totalCapacity - (availableCapacity != null ? availableCapacity : 0);
        return (double) usedCapacity / totalCapacity * 100.0;
    }
    
    public boolean canHandleInternationalShipping() {
        return canShipInternationally && isOperational();
    }
    
    public boolean canHandleReturns() {
        return canReceiveReturns && isOperational();
    }
    
    public boolean isHighPriority() {
        return priority != null && priority <= 3;
    }
    
    public boolean requiresInspection() {
        if (nextInspectionDate == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(nextInspectionDate);
    }
    
    public String getLocationSummary() {
        return city + ", " + state + ", " + country;
    }
    
    // Builder pattern for easy object creation
    public static WarehouseBuilder builder() {
        return new WarehouseBuilder();
    }
    
    public static class WarehouseBuilder {
        private final Warehouse warehouse = new Warehouse();
        
        public WarehouseBuilder warehouseCode(String warehouseCode) {
            warehouse.setWarehouseCode(warehouseCode);
            return this;
        }
        
        public WarehouseBuilder name(String name) {
            warehouse.setName(name);
            return this;
        }
        
        public WarehouseBuilder type(WarehouseType type) {
            warehouse.setType(type);
            return this;
        }
        
        public WarehouseBuilder status(WarehouseStatus status) {
            warehouse.setStatus(status);
            return this;
        }
        
        public WarehouseBuilder address(String addressLine1, String city, String state, String postalCode, String country) {
            warehouse.setAddressLine1(addressLine1);
            warehouse.setCity(city);
            warehouse.setState(state);
            warehouse.setPostalCode(postalCode);
            warehouse.setCountry(country);
            return this;
        }
        
        public WarehouseBuilder coordinates(BigDecimal latitude, BigDecimal longitude) {
            warehouse.setLatitude(latitude);
            warehouse.setLongitude(longitude);
            return this;
        }
        
        public WarehouseBuilder contact(String phoneNumber, String email) {
            warehouse.setPhoneNumber(phoneNumber);
            warehouse.setEmail(email);
            return this;
        }
        
        public WarehouseBuilder capacity(Integer totalCapacity, String capacityUnit) {
            warehouse.setTotalCapacity(totalCapacity);
            warehouse.setAvailableCapacity(totalCapacity);
            warehouse.setCapacityUnit(capacityUnit);
            return this;
        }
        
        public WarehouseBuilder manager(User manager) {
            warehouse.setManager(manager);
            return this;
        }
        
        public WarehouseBuilder priority(Integer priority) {
            warehouse.setPriority(priority);
            return this;
        }
        
        public WarehouseBuilder capabilities(boolean canShipInternationally, boolean canReceiveReturns, 
                                           boolean hasRefrigeration, boolean hasHazmatCapability) {
            warehouse.setCanShipInternationally(canShipInternationally);
            warehouse.setCanReceiveReturns(canReceiveReturns);
            warehouse.setHasRefrigeration(hasRefrigeration);
            warehouse.setHasHazmatCapability(hasHazmatCapability);
            return this;
        }
        
        public Warehouse build() {
            return warehouse;
        }
    }
}
