package com.nexoracommerce.initializer;

import com.nexoracommerce.cart.entity.CartItem;
import com.nexoracommerce.coupon.entity.Coupon;
import com.nexoracommerce.order.entity.Order;
import com.nexoracommerce.order.entity.OrderItem;
import com.nexoracommerce.product.entity.Product;
import com.nexoracommerce.product.entity.ProductVariant;
import com.nexoracommerce.product.entity.ProductImage;
import com.nexoracommerce.user.entity.User;
import com.nexoracommerce.user.entity.Role;
import com.nexoracommerce.common.enums.OrderStatus;
import com.nexoracommerce.cart.repository.CartRepository;
import com.nexoracommerce.coupon.repository.CouponRepository;
import com.nexoracommerce.order.repository.OrderRepository;
import com.nexoracommerce.product.repository.ProductRepository;
import com.nexoracommerce.product.repository.ProductVariantRepository;
import com.nexoracommerce.product.repository.ProductImageRepository;
import com.nexoracommerce.user.repository.UserRepository;
import com.nexoracommerce.user.repository.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Data Initializer - Creates default users and catalog items on application startup
 */
@Component
@org.springframework.context.annotation.Profile("!test")
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final CouponRepository couponRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           RoleRepository roleRepository,
                           ProductRepository productRepository,
                           ProductVariantRepository productVariantRepository,
                           ProductImageRepository productImageRepository,
                           CouponRepository couponRepository,
                           CartRepository cartRepository,
                           OrderRepository orderRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productImageRepository = productImageRepository;
        this.couponRepository = couponRepository;
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Seed default roles
        Role customerRole = seedRoleIfMissing("ROLE_CUSTOMER");
        Role adminRole = seedRoleIfMissing("ROLE_ADMIN");

        // Seed default users
        User customer = createUserIfMissing("customer1", "password123", "Customer One", "customer1@shopcart.com", "ROLE_CUSTOMER");
        createUserIfMissing("admin", "admin123", "Administrator", "admin@shopcart.com", "ROLE_ADMIN");

        // Seed products and default variants
        createDefaultProducts();

        // Seed coupons
        createDefaultCoupons();

        // Seed cart items
        createDefaultCartItems(customer);

        // Seed orders
        createDefaultOrders(customer);
    }

    private Role seedRoleIfMissing(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));
    }

    private User createUserIfMissing(String username, String rawPassword, String fullName, String email, String roleName) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> {
                    Role role = roleRepository.findByName(roleName)
                            .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));
                    User user = User.builder()
                            .username(username)
                            .password(passwordEncoder.encode(rawPassword))
                            .fullName(fullName)
                            .email(email)
                            .roles(Set.of(role))
                            .build();
                    User saved = userRepository.save(user);
                    log.info("Created default user: {}", username);
                    return saved;
                });
    }

    private void createDefaultProducts() {
        seedProductIfMissing("P001", "iPhone 15 128GB", 21990000L,
                "Smartphone Apple A16, man hinh Super Retina XDR 6.1 inch",
                "https://res.cloudinary.com/baofan/image/upload/c_fill,g_auto,w_500,h_500,q_auto,f_auto/v1778497692/iphone_15_vpedjn.webp");
        seedProductIfMissing("P002", "Samsung Galaxy S24", 18990000L,
                "Smartphone Android cao cap, man hinh Dynamic AMOLED 2X",
                "https://res.cloudinary.com/baofan/image/upload/c_fill,g_auto,w_500,h_500,q_auto,f_auto/v1778497692/samsung_s24_c91ta6.webp");
        seedProductIfMissing("P003", "Xiaomi 14", 15990000L,
                "Smartphone Snapdragon 8 Gen 3, camera Leica",
                "https://res.cloudinary.com/baofan/image/upload/c_fill,g_auto,w_500,h_500,q_auto,f_auto/v1778497692/xiaomi_14_aiz1x3.webp");
        seedProductIfMissing("P004", "MacBook Air M3 13 inch", 28990000L,
                "Laptop mong nhe chip Apple M3, pin toi uu",
                "https://res.cloudinary.com/baofan/image/upload/c_fill,g_auto,w_500,h_500,q_auto,f_auto/v1778497692/macbook_air_13_gy3hye.webp");
        seedProductIfMissing("P005", "Dell XPS 13", 32990000L,
                "Ultrabook cao cap, man hinh InfinityEdge",
                "https://res.cloudinary.com/baofan/image/upload/c_fill,g_auto,w_500,h_500,q_auto,f_auto/v1778497693/dell_xps_13_rbgewa.webp");
        seedProductIfMissing("P006", "iPad Air 11 inch", 16990000L,
                "May tinh bang phuc vu hoc tap va cong viec",
                "https://res.cloudinary.com/baofan/image/upload/c_fill,g_auto,w_500,h_500,q_auto,f_auto/v1778497692/ipad_air_m4_ddu4gg.webp");
        seedProductIfMissing("P007", "Sony WH-1000XM5", 7990000L,
                "Tai nghe chong on chu dong, chat am chi tiet",
                "https://res.cloudinary.com/baofan/image/upload/c_fill,g_auto,w_500,h_500,q_auto,f_auto/v1778497692/sony_qq4ywl.webp");
        seedProductIfMissing("P008", "Logitech MX Master 3S", 2490000L,
                "Chuot khong day cho dan van phong va designer",
                "https://res.cloudinary.com/baofan/image/upload/c_fill,g_auto,w_500,h_500,q_auto,f_auto/v1778497692/logictech_chbwix.webp");
        seedProductIfMissing("P009", "Keychron K8 Pro", 2790000L,
                "Ban phim co wireless ho tro macOS va Windows",
                "https://res.cloudinary.com/baofan/image/upload/c_fill,g_auto,w_500,h_500,q_auto,f_auto/v1778497692/keychron_k8_jw8b6c.webp");
        seedProductIfMissing("P010", "Anker 737 Power Bank", 3290000L,
                "Pin du phong dung luong lon, sac nhanh 140W",
                "https://res.cloudinary.com/baofan/image/upload/c_fill,g_auto,w_500,h_500,q_auto,f_auto/v1778497693/anker_737_fdw6oe.webp");
    }

    private void seedProductIfMissing(String id, String name, Long price, String description, String imageUrl) {
        if (!productRepository.existsById(id)) {
            Product product = Product.builder()
                    .id(id)
                    .name(name)
                    .description(description)
                    .build();
            Product savedProduct = productRepository.save(product);

            // Initialize default product variant
            ProductVariant variant = ProductVariant.builder()
                    .sku(id) // default variant SKU is same as product ID
                    .product(savedProduct)
                    .price(price)
                    .quantity(50) // seed physical stock = 50
                    .reservedQuantity(0)
                    .soldQuantity(0)
                    .build();
            productVariantRepository.save(variant);

            // Initialize featured image
            if (imageUrl != null) {
                ProductImage image = ProductImage.builder()
                        .product(savedProduct)
                        .imageUrl(imageUrl)
                        .isPrimary(true)
                        .build();
                productImageRepository.save(image);
            }
            log.info("Seeded product and default variant: {}", id);
        }
    }

    private void updateVariantInventory(String sku, int qtyDelta, int reservedDelta, int soldDelta) {
        productVariantRepository.findBySku(sku).ifPresent(variant -> {
            variant.setQuantity(variant.getQuantity() + qtyDelta);
            variant.setReservedQuantity(variant.getReservedQuantity() + reservedDelta);
            variant.setSoldQuantity(variant.getSoldQuantity() + soldDelta);
            productVariantRepository.save(variant);
        });
    }

    private void createDefaultCoupons() {
        if (couponRepository.count() > 0) {
            return;
        }
        List<Coupon> coupons = List.of(
                Coupon.builder().code("WELCOME10").discountPercent(10).active(true).build(),
                Coupon.builder().code("SALE15").discountPercent(15).active(true).build(),
                Coupon.builder().code("VIP20").discountPercent(20).active(false).build()
        );
        couponRepository.saveAll(coupons);
        log.info("Seeded {} coupons", coupons.size());
    }

    private void createDefaultCartItems(User customer) {
        if (customer == null || cartRepository.count() > 0) {
            return;
        }

        ProductVariant variantP001 = productVariantRepository.findBySku("P001").orElseThrow();
        ProductVariant variantP008 = productVariantRepository.findBySku("P008").orElseThrow();

        List<CartItem> cartItems = List.of(
                CartItem.builder().user(customer).variant(variantP001).quantity(1).build(),
                CartItem.builder().user(customer).variant(variantP008).quantity(2).build()
        );
        cartRepository.saveAll(cartItems);
        
        // Cập nhật reservation trong kho cho cart items
        for (CartItem item : cartItems) {
            updateVariantInventory(item.getVariant().getSku(), 0, item.getQuantity(), 0);
        }
        
        log.info("Seeded {} cart items and updated variant reservations", cartItems.size());
    }

    private void createDefaultOrders(User customer) {
        if (customer == null || orderRepository.count() > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        ProductVariant variantP002 = productVariantRepository.findBySku("P002").orElseThrow();
        ProductVariant variantP003 = productVariantRepository.findBySku("P003").orElseThrow();
        ProductVariant variantP007 = productVariantRepository.findBySku("P007").orElseThrow();
        ProductVariant variantP009 = productVariantRepository.findBySku("P009").orElseThrow();

        Coupon couponWelcome10 = couponRepository.findById("WELCOME10").orElse(null);

        Order order1 = Order.builder()
                .id("ORD-1001")
                .user(customer)
                .shippingFee(29900L)
                .coupon(couponWelcome10)
                .shippingAddress("123 Duong Le Loi, Q1, HCMC")
                .phoneNumber("0901234567")
                .status(OrderStatus.DELIVERED)
                .createdAt(now.minusDays(5))
                .lastModifiedDate(now.minusDays(3))
                .orderItems(new ArrayList<>())
                .build();
        order1.getOrderItems().add(OrderItem.builder().order(order1).variant(variantP002).quantity(1).price(18990000L).build());
        order1.getOrderItems().add(OrderItem.builder().order(order1).variant(variantP007).quantity(1).price(7990000L).build());
        order1.setTotalPrice(calculateTotal(order1));

        Order order2 = Order.builder()
                .id("ORD-1002")
                .user(customer)
                .shippingFee(29900L)
                .coupon(null)
                .shippingAddress("456 Duong Nguyen Hue, Q1, HCMC")
                .phoneNumber("0907654321")
                .status(OrderStatus.PENDING)
                .createdAt(now.minusHours(6))
                .lastModifiedDate(now.minusHours(2))
                .orderItems(new ArrayList<>())
                .build();
        order2.getOrderItems().add(OrderItem.builder().order(order2).variant(variantP003).quantity(1).price(15990000L).build());
        order2.getOrderItems().add(OrderItem.builder().order(order2).variant(variantP009).quantity(1).price(2790000L).build());
        order2.setTotalPrice(calculateTotal(order2));

        orderRepository.saveAll(List.of(order1, order2));
        
        // Cập nhật kho theo trạng thái đơn hàng
        // Order 1 (DELIVERED): Giảm thực tế, tăng đã bán
        for (OrderItem item : order1.getOrderItems()) {
            updateVariantInventory(item.getVariant().getSku(), -item.getQuantity(), 0, item.getQuantity());
        }
        
        // Order 2 (PENDING): Tăng đang giữ
        for (OrderItem item : order2.getOrderItems()) {
            updateVariantInventory(item.getVariant().getSku(), 0, item.getQuantity(), 0);
        }

        log.info("Seeded {} orders and updated variant stock accordingly", 2);
    }

    private long calculateTotal(Order order) {
        long itemsTotal = order.getOrderItems().stream()
                .mapToLong(item -> item.getPrice() * item.getQuantity())
                .sum();
        return itemsTotal + (order.getShippingFee() == null ? 0L : order.getShippingFee());
    }
}
