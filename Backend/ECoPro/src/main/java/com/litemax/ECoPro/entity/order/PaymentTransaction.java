package com.litemax.ECoPro.entity.order;

import com.litemax.ECoPro.entity.auth.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PaymentTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 100)
    private String transactionId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status = TransactionStatus.PENDING;
    
    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    
    @Size(max = 3)
    @Column(length = 3)
    private String currency = "USD";
    
    @Size(max = 100)
    private String gatewayTransactionId;
    
    @Size(max = 100)
    private String gatewayReference;
    
    @Lob
    private String gatewayRequest; // JSON request sent to gateway
    
    @Lob
    private String gatewayResponse; // JSON response from gateway
    
    @Size(max = 500)
    private String failureReason;
    
    @Size(max = 50)
    private String processorResponseCode;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @Column
    private LocalDateTime processedAt;
    
    public enum TransactionType {
        AUTHORIZATION, CAPTURE, SALE, REFUND, VOID, PARTIAL_REFUND
    }
    
    public enum TransactionStatus {
        PENDING, PROCESSING, SUCCESS, FAILED, CANCELLED, EXPIRED
    }
}
