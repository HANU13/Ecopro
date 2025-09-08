import os

# Base project name
base_dir = "ECoPro"

# Module/package structure
packages = {
    "config": ["SecurityConfig.java", "SwaggerConfig.java", "RedisConfig.java", "MailConfig.java", "WebhookConfig.java", "AppSettingsConfig.java"],
    "controller/auth": ["AuthController.java", "SessionController.java", "AuthProviderController.java"],
    "controller/user": ["UserController.java", "AddressController.java"],
    "controller/product": ["ProductController.java", "CategoryController.java", "BrandController.java", "ProductVariantController.java"],
    "controller/cart": ["CartController.java"],
    "controller/order": ["OrderController.java", "PaymentController.java", "ShipmentController.java", "RefundController.java"],
    "controller/promotion": ["PromotionController.java"],
    "controller/inventory": ["InventoryController.java"],
    "controller/loyalty": ["LoyaltyController.java"],
    "controller/wallet": ["WalletController.java"],
    "controller/cms": ["CmsController.java"],
    "controller/notification": ["NotificationController.java"],
    "controller/webhook": ["WebhookController.java"],
    "controller/audit": ["AuditController.java"],
    "controller/settings": ["AppSettingsController.java"],
    "entity/auth": ["User.java", "Role.java", "Permission.java", "UserRole.java", "RolePermission.java", "Session.java", "AuthProvider.java"],
    "entity/product": ["Product.java", "ProductVariant.java", "Category.java", "ProductCategory.java", "Brand.java", "ProductMedia.java", "Attribute.java", "AttributeValue.java", "VariantAttributeValue.java"],
    "entity/inventory": ["Warehouse.java", "Inventory.java", "Supplier.java", "PurchaseOrder.java", "PurchaseOrderItem.java"],
    "entity/order": ["Order.java", "OrderItem.java", "Payment.java", "Invoice.java", "InvoiceItem.java", "Shipment.java", "ShipmentItem.java", "ReturnRequest.java", "ReturnItem.java", "DeliverySlotReservation.java"],
    "entity/cart": ["Cart.java", "CartItem.java", "CartDiscount.java"],
    "entity/promotion": ["Promotion.java"],
    "entity/tax": ["TaxZone.java", "TaxRate.java"],
    "entity/loyalty": ["LoyaltyAccount.java", "LoyaltyTxn.java"],
    "entity/wallet": ["Wallet.java", "WalletTxn.java"],
    "entity/cms": ["CmsBlock.java"],
    "entity/messaging": ["MessageTemplate.java", "Notification.java"],
    "entity/webhook": ["WebhookEndpoint.java", "WebhookEvent.java"],
    "entity/audit": ["AuditLog.java"],
    "entity/settings": ["AppSettings.java"],
    "entity/store": ["Store.java", "StorePayout.java"],
    "repository": [],
    "service": [],
    "dto/auth": [],
    "dto/product": [],
    "dto/order": [],
    "util": ["JwtUtil.java", "MapperUtil.java", "EmailUtil.java", "FileUploadUtil.java"],
    "exception": ["GlobalExceptionHandler.java", "ResourceNotFoundException.java", "ValidationException.java"]
}

# Create folders and files
for package, files in packages.items():
    dir_path = os.path.join(base_dir, "src/main/java/com/litemax/ECoPro", package.replace("/", os.sep))
    os.makedirs(dir_path, exist_ok=True)
    
    for file_name in files:
        file_path = os.path.join(dir_path, file_name)
        if not os.path.exists(file_path):
            with open(file_path, "w") as f:
                # Add basic Java class template
                class_name = file_name.replace(".java", "")
                f.write(f"package com.example.ecommerce.{package.replace('/', '.')};\n\n")
                f.write(f"public class {class_name} " + "{\n\n}")
                
# Create resources and test folders
resource_dirs = [
    "src/main/resources",
    "src/test/java/com/litemax/ECoPro/controller",
    "src/test/java/com/litemax/ECoPro/service",
    "src/test/java/com/litemax/ECoPro/repository",
    "docker"
]

for rd in resource_dirs:
    os.makedirs(os.path.join(base_dir, rd), exist_ok=True)

# Create placeholder resource files
resource_files = [
    "src/main/resources/application.yml",
    "src/main/resources/schema.sql",
    "src/main/resources/data.sql",
    "src/main/resources/logback-spring.xml",
    "docker/Dockerfile",
    "docker/docker-compose.yml",
    "docker/kubernetes-manifest.yml"
]

for rf in resource_files:
    open(os.path.join(base_dir, rf), 'a').close()

# Create main Spring Boot application file
main_app_path = os.path.join(base_dir, "src/main/java/com/litemax/ECoPro/ECoProApplication.java")
with open(main_app_path, "w") as f:
    f.write("package com.litemax.ECoPro\n\n")
    f.write("import org.springframework.boot.SpringApplication;\n")
    f.write("import org.springframework.boot.autoconfigure.SpringBootApplication;\n\n")
    f.write("@SpringBootApplication\n")
    f.write("public class EcommerceApplication {\n")
    f.write("    public static void main(String[] args) {\n")
    f.write("        SpringApplication.run(EcommerceApplication.class, args);\n")
    f.write("    }\n")
    f.write("}\n")

print(f"E-commerce backend folder structure created successfully in '{base_dir}'")
