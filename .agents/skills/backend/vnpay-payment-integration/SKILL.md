---
name: vnpay-payment-integration
description: VNPAY payment gateway integration for Phase 3 - handle payment flow, callback validation, and transaction recording.
---

## Scope & Activation Rules

Activate when:
- Implementing VNPAY payment checkout flow
- Creating payment callback handlers (Return URL / IPN)
- Validating HMAC-SHA512 signatures from VNPAY
- Recording payment transactions in database
- Updating order status based on payment success/failure
- Testing payment flow with VNPAY test credentials

## System Directives

### VNPAY Integration Rules
- **Signature Validation**: Always verify HMAC-SHA512 hash on EVERY callback; reject if mismatch
- **Separate Return URL and IPN**: Return URL is user-facing (redirect), IPN is server-to-server (async)
- **Idempotent IPN Handler**: Process IPN only once even if VNPAY retries; use database transaction
- **Amount in Cents**: VNPAY expects amount in VND without decimals (multiply by 100 if needed)
- **Transaction Recording**: Save every payment attempt to `payment_transactions` table with raw response
- **Error Response Format**: Return `{"RspCode":"00","Message":"..."}` for IPN success
- **Secure Configuration**: Store `VnpayTmnCode`, `VnpayHashSecret` in environment variables, NOT in code
- **Request Timeout**: Set 10-second timeout for VNPAY API calls

### Anti-Patterns
- No hardcoded credentials
- No signature validation skipped
- No processing IPN without checking existing transaction (idempotency)
- No storing full response in plain text logs
- No handling amount as floating point (use Long in cents)

## VNPAY Configuration & DTOs

### Properties
```java
@Configuration
@ConfigurationProperties(prefix = "vnpay")
@Data
public class VnpayProperties {
    private String tmnCode;        // From env: VNPAY_TMN_CODE
    private String hashSecret;     // From env: VNPAY_HASH_SECRET
    private String payUrl;         // https://sandbox.vnpayment.vn/paygate
    private String apiUrl;         // https://api.vnpayment.vn/
    private String returnUrl;      // Your backend callback URL
    private String notifyUrl;      // Your backend IPN URL
}

// application.yml
vnpay:
  tmn-code: ${VNPAY_TMN_CODE}
  hash-secret: ${VNPAY_HASH_SECRET}
  pay-url: https://sandbox.vnpayment.vn/paygate
  api-url: https://api.vnpayment.vn/
  return-url: https://yourdomain.com/api/payment/vnpay/callback
  notify-url: https://yourdomain.com/api/payment/vnpay/ipn
```

### DTOs
```java
public record VnpayCreatePaymentRequest(
    String orderId,
    long amount,           // in VND (NOT cents, but without decimals)
    String orderInfo,      // "Thanh toán đơn hàng #{orderId}"
    String ipAddress       // Client IP for fraud detection
) {}

public record VnpayPaymentResponse(
    String paymentUrl,     // Redirect user here
    String orderId,
    long amount
) {}

public record VnpayCallbackRequest(
    String vnp_ResponseCode,      // 00=success
    String vnp_TransactionNo,     // VNPAY transaction ID
    String vnp_OrderInfo,
    Long vnp_Amount,              // in cents (multiply by 100)
    String vnp_SecureHash,        // HMAC to validate
    String vnp_BankCode
) {}
```

## VNPAY Service Implementation

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class VnpayService {
    
    private final VnpayProperties props;
    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final RestTemplate restTemplate;
    
    // Step 1: Generate payment URL
    public String createPaymentUrl(String orderId, long amount, String ipAddress) {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", props.getTmnCode());
        params.put("vnp_CreateDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        params.put("vnp_Amount", String.valueOf(amount * 100));  // Convert to cents
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_OrderInfo", "Thanh toan don hang #" + orderId);
        params.put("vnp_OrderType", "other");
        params.put("vnp_ReturnUrl", props.getReturnUrl());
        params.put("vnp_TxnRef", orderId);
        params.put("vnp_IpAddr", ipAddress);
        params.put("vnp_Locale", "vn");
        
        // Step 2: Sort params alphabetically
        String hashData = params.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));
        
        // Step 3: Create HMAC-SHA512 signature
        String secureHash = hmacSHA512(props.getHashSecret(), hashData);
        
        return props.getPayUrl() + "?" + hashData + "&vnp_SecureHash=" + secureHash;
    }
    
    // Step 4: Validate callback signature
    public boolean verifyCallback(Map<String, String> params) {
        String receivedHash = params.remove("vnp_SecureHash");
        
        String hashData = params.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining("&"));
        
        String calculatedHash = hmacSHA512(props.getHashSecret(), hashData);
        return calculatedHash.equals(receivedHash);
    }
    
    // Step 5: Process callback and update order
    @Transactional
    public void processCallback(String orderId, String responseCode, String transactionNo, long amount) {
        // Check if already processed (idempotency)
        PaymentTransaction existing = paymentRepository.findByVnpTransactionNo(transactionNo).orElse(null);
        if (existing != null) {
            log.warn("Duplicate callback for transaction: {}", transactionNo);
            return;
        }
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        
        PaymentTransaction transaction = PaymentTransaction.builder()
            .orderId(orderId)
            .vnpTransactionNo(transactionNo)
            .amount(amount / 100)  // Convert back from cents
            .status(responseCode.equals("00") ? PaymentStatus.SUCCESS : PaymentStatus.FAILED)
            .responseCode(responseCode)
            .createdAt(LocalDateTime.now())
            .build();
        
        paymentRepository.save(transaction);
        
        if ("00".equals(responseCode)) {
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
            log.info("Order confirmed after VNPAY payment: {}", orderId);
        } else {
            log.warn("Payment failed for order {}: {}", orderId, responseCode);
        }
    }
    
    // Helper: Generate HMAC-SHA512
    private String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC generation failed", e);
        }
    }
}
```

## VNPAY Controller

```java
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    
    private final VnpayService vnpayService;
    private final OrderService orderService;
    
    // Step 1: User clicks "Pay with VNPAY"
    @PostMapping("/vnpay/create")
    public ResponseEntity<VnpayPaymentResponse> createPayment(
        @RequestBody VnpayCreatePaymentRequest request,
        HttpServletRequest httpRequest) {
        
        String clientIp = getClientIp(httpRequest);
        String paymentUrl = vnpayService.createPaymentUrl(request.orderId(), request.amount(), clientIp);
        
        return ResponseEntity.ok(new VnpayPaymentResponse(
            paymentUrl,
            request.orderId(),
            request.amount()
        ));
    }
    
    // Step 2: User redirected back from VNPAY (browser redirect)
    @GetMapping("/vnpay/callback")
    public ResponseEntity<?> handleCallback(@RequestParam Map<String, String> params) {
        if (!vnpayService.verifyCallback(params)) {
            log.warn("Invalid VNPAY signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String orderId = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");
        long amount = Long.parseLong(params.get("vnp_Amount"));
        
        try {
            vnpayService.processCallback(orderId, responseCode, transactionNo, amount);
            
            // Redirect user to frontend result page
            boolean success = "00".equals(responseCode);
            return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("https://yourdomain.com/payment/result?status=" + 
                    (success ? "SUCCESS" : "FAILED") + "&orderId=" + orderId))
                .build();
        } catch (Exception e) {
            log.error("Callback processing error", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Step 3: VNPAY server sends IPN (async confirmation)
    @PostMapping("/vnpay/ipn")
    public ResponseEntity<?> handleIpn(@RequestParam Map<String, String> params) {
        if (!vnpayService.verifyCallback(params)) {
            return ResponseEntity.ok(Map.of("RspCode", "99", "Message", "Invalid signature"));
        }
        
        String orderId = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");
        long amount = Long.parseLong(params.get("vnp_Amount"));
        
        try {
            vnpayService.processCallback(orderId, responseCode, transactionNo, amount);
            return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
        } catch (Exception e) {
            log.error("IPN processing error", e);
            return ResponseEntity.ok(Map.of("RspCode", "99", "Message", "Internal Error"));
        }
    }
    
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
```

## Checkout Integration

```java
@Service
@RequiredArgsConstructor
@Transactional
public class CheckoutService {
    
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final VnpayService vnpayService;
    
    public CheckoutResponse checkout(String userId, CheckoutRequest request) {
        // Validate cart exists and belongs to user
        Cart cart = cartRepository.findById(request.cartId())
            .orElseThrow(() -> new CartNotFoundException("Cart not found"));
        
        // Create order
        Order order = Order.builder()
            .id(UUID.randomUUID().toString())
            .userId(userId)
            .status(OrderStatus.PENDING)
            .totalAmount(cart.getTotalPrice())
            .createdAt(LocalDateTime.now())
            .build();
        
        Order savedOrder = orderRepository.save(order);
        
        // If paying with VNPAY, generate payment URL
        if ("VNPAY".equals(request.paymentMethod())) {
            String paymentUrl = vnpayService.createPaymentUrl(
                savedOrder.getId(),
                savedOrder.getTotalAmount().longValue(),
                request.ipAddress()
            );
            return new CheckoutResponse(savedOrder.getId(), paymentUrl, "PENDING");
        }
        
        // Otherwise COD (Cash on Delivery) - order confirmed immediately
        savedOrder.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(savedOrder);
        return new CheckoutResponse(savedOrder.getId(), null, "CONFIRMED");
    }
}

public record CheckoutRequest(
    String cartId,
    String paymentMethod,  // "COD" or "VNPAY"
    String ipAddress
) {}

public record CheckoutResponse(
    String orderId,
    String paymentUrl,    // null for COD
    String status
) {}
```

## Verification Commands

```bash
# Test payment URL generation (with test credentials)
curl -X POST http://localhost:8080/api/payment/vnpay/create \
  -H "Content-Type: application/json" \
  -d '{"orderId":"order-123","amount":100000,"orderInfo":"Thanh toán đơn hàng #order-123","ipAddress":"127.0.0.1"}'

# Test callback validation
curl -X GET "http://localhost:8080/api/payment/vnpay/callback?vnp_TxnRef=order-123&vnp_ResponseCode=00&..."

# Check payment transaction saved
psql nexora_db -c "SELECT * FROM payment_transactions WHERE order_id='order-123';"
```
