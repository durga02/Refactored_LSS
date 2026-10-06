package com.lalitha.sweets.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import com.lalitha.sweets.dto.CheckoutRequest;
import com.lalitha.sweets.dto.OrderDto;
import com.lalitha.sweets.model.Customer;
import com.lalitha.sweets.model.Order;
import com.lalitha.sweets.model.OrderStatus;
import com.lalitha.sweets.model.OrderStatusHistory;
import com.lalitha.sweets.model.PaymentStatus;
import com.lalitha.sweets.model.PincodeData;
import com.lalitha.sweets.repository.CustomerRepository;
import com.lalitha.sweets.repository.OrderRepository;
import com.lalitha.sweets.repository.OrderStatusHistoryRepository;
import com.lalitha.sweets.repository.PincodeRepository;
import com.lalitha.sweets.service.CartService;
import com.lalitha.sweets.service.EmailService;
import com.lalitha.sweets.service.InvoiceService;
import com.lalitha.sweets.service.OrderService;
import com.lalitha.sweets.service.PaymentService;
import com.lalitha.sweets.service.SmsService;
import com.lalitha.sweets.service.WhatsAppService;
import com.razorpay.Utils;
import com.lalitha.sweets.service.OrderNotificationService;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutApiController {

    @Autowired
    private CartService cartService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private WhatsAppService whatsAppService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private OrderStatusHistoryRepository historyRepository;

    @Autowired
    private PincodeRepository pincodeRepository;

    @Autowired
    private OrderNotificationService orderNotificationService;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Value("${whatsapp.admin.number}")
    private String adminNumber;

    @GetMapping
    public Map<String, Object> checkout() {

        Map<String, Object> response = new HashMap<>();

        BigDecimal handling = BigDecimal.ZERO;
        if (!cartService.getItems().isEmpty()) {
            handling = BigDecimal.valueOf(20);
        }

        response.put("items", cartService.getItems());
        response.put("subtotal", cartService.getTotal());
        response.put("handling", handling);
        response.put("delivery", BigDecimal.ZERO);
        response.put("total", cartService.getTotal().add(handling));

        return response;
    }

    @GetMapping("/pincode/{pincode}")
    public Map<String, String> getLocationByPincode(@PathVariable String pincode) {

        Map<String, String> result = new HashMap<>();

        Optional<PincodeData> existing = pincodeRepository.findByPincode(pincode);

        if (existing.isPresent()) {
            PincodeData data = existing.get();
            result.put("city", data.getCity());
            result.put("district", data.getDistrict());
            result.put("state", data.getState());
            return result;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://pinlookup.in/api/pincode?pincode=" + pincode;

            @SuppressWarnings("rawtypes")
            Map response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.get("success") != null) {

                @SuppressWarnings("rawtypes")
                Map data = (Map) response.get("data");

                String city = String.valueOf(data.get("office_name"));
                String district = String.valueOf(data.get("district"));
                String state = String.valueOf(data.get("state"));

                PincodeData saveData = new PincodeData();
                saveData.setPincode(pincode);
                saveData.setCity(city);
                saveData.setDistrict(district);
                saveData.setState(state);

                pincodeRepository.save(saveData);

                result.put("city", city);
                result.put("district", district);
                result.put("state", state);
                return result;
            }

        } catch (Exception e) {
            // Best-effort lookup - fall through to empty result so the
            // customer can still fill city/state manually.
        }

        result.put("city", "");
        result.put("district", "");
        result.put("state", "");
        return result;
    }

    @PostMapping("/place-order")
    public Map<String, Object> placeOrder(@jakarta.validation.Valid @RequestBody CheckoutRequest request) throws Exception {

        Map<String, Object> response = new HashMap<>();

        if (cartService.getItems().isEmpty()) {
            response.put("success", false);
            response.put("message", "Cart is empty");
            return response;
        }

        Customer customer = customerRepository
                .findByEmail(request.email())
                .orElse(new Customer());

        String fullPhone = request.phone();

        if (fullPhone != null && !fullPhone.startsWith("91")) {
            fullPhone = "91" + fullPhone;
        }

        customer.setName(request.name());
        customer.setEmail(request.email());
        customer.setPhone(fullPhone);
        customer.setAddress(request.address());
        customer.setState(request.state());
        customer.setPincode(request.pincode());

        customer = customerRepository.save(customer);

        Order order = new Order();
        order.setCustomer(customer);
        order.setCustomerNameSnapshot(customer.getName());
        order.setCustomerPhoneSnapshot(customer.getPhone());
        order.setAddress(customer.getAddress());
        order.setState(customer.getState());
        order.setOrderDate(LocalDateTime.now());
        // Orders start out unpaid: PENDING until payment-verify confirms the
        // Razorpay signature. Only then do they get promoted to PLACED with
        // paymentStatus SUCCESS. This keeps abandoned/failed checkouts from
        // showing up in the admin panel as if they were real, paid orders.
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);

        orderService.calculateCharges(order, cartService.getItems(), request.state());

        for (var cartItem : cartService.getItems()) {
            var item = new com.lalitha.sweets.model.OrderItem();
            item.setProductName(cartItem.getProductName());
            item.setPriceLabel(cartItem.getPriceLabel());
            item.setPrice(cartItem.getPrice());
            item.setQuantity(cartItem.getQuantity());
            item.setOrder(order);
            order.getItems().add(item);
        }

        Order savedOrder = orderRepository.save(order);

        com.razorpay.Order razorpayOrder = paymentService.createOrder(savedOrder.getTotalAmount().doubleValue());

        response.put("success", true);
        response.put("orderId", savedOrder.getId());
        response.put("customerId", customer.getId());
        response.put("razorpayOrderId", razorpayOrder.get("id"));
        response.put("amount", savedOrder.getTotalAmount().multiply(new BigDecimal(100)).intValue());
        response.put("key", paymentService.getKeyId());

        return response;
    }

    /**
     * Called by the React app once Razorpay's checkout.js handler fires
     * with the payment result. Verifies the signature server-side (never
     * trust the client for this), marks the order paid, records a status
     * history entry, emails the invoice and notifies the customer + admin
     * over WhatsApp - mirroring what the old server-rendered
     * /checkout/payment-success flow did, but as a JSON endpoint the SPA
     * can call and then route to an order-success page with the result.
     */
    @PostMapping("/payment-verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> payload) {

        String paymentId = payload.get("paymentId");
        String razorpayOrderId = payload.get("razorpayOrderId");
        String signature = payload.get("signature");
        Long orderId = Long.valueOf(payload.get("orderId"));

        JSONObject options = new JSONObject();
        options.put("razorpay_order_id", razorpayOrderId);
        options.put("razorpay_payment_id", paymentId);
        options.put("razorpay_signature", signature);

        boolean isValid;
        try {
            isValid = Utils.verifyPaymentSignature(options, razorpayKeySecret);
        } catch (Exception e) {
            isValid = false;
        }

        if (!isValid) {
            return ResponseEntity.status(400).body(Map.of("message", "Payment verification failed"));
        }

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        order.setPaymentId(paymentId);
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setStatus(OrderStatus.PLACED);
        order.setRazorpayOrderId(razorpayOrderId);
        order.setRazorpayPaymentId(paymentId);
        order.setRazorpaySignature(signature);

        orderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(order.getStatus());
        history.setUpdatedTime(LocalDateTime.now());
        history.setUpdatedBy("CUSTOMER");
        historyRepository.save(history);

        byte[] invoicePdf = invoiceService.generateInvoice(order);

        String trackUrl = frontendBaseUrl + "/track/" + order.getId();

        // Run email/SMS/admin WhatsApp in the background.
        // Payment verification no longer waits for external notification services.
        orderNotificationService.sendOrderPlacedNotifications(
            order,
            trackUrl,
            invoicePdf
        );

        // SMS is the interim customer-facing channel while the official
        // WhatsApp Business API application (Meta verification + template
        // approval) is pending. Swap this back to whatsAppService.sendWhatsApp
        // once Meta approval comes through - the commented call below shows
        // the equivalent message.
       // smsService.sendOrderStatusSms(order, OrderStatus.PLACED, order.getCustomerPhoneSnapshot(), trackUrl);
        // whatsAppService.sendWhatsApp(
        //         order.getCustomerPhoneSnapshot(),
        //         "🎉 *Payment Successful!*\n\n" +
        //         "Hi " + order.getCustomerNameSnapshot() + ",\n\n" +
        //         "Your payment was successful and your order is Placed.\n\n" +
        //         "🧾 *Order ID:* #" + order.getId() + "\n" +
        //         "💰 *Amount:* ₹" + order.getTotalAmount() + "\n\n" +
        //         "📍 *Address:*\n" + order.getAddress() + "\n\n" +
        //         "🔎 Track Order:\n" + trackUrl
        // );

        // whatsAppService.sendWhatsApp(
        //         adminNumber,
        //         "🚨 *New Paid Order Received!*\n\n" +
        //         "🧾 *Order ID:* #" + order.getId() + "\n" +
        //         "👤 *Customer:* " + order.getCustomerNameSnapshot() + "\n" +
        //         "📞 *Phone:* " + order.getCustomerPhoneSnapshot() + "\n" +
        //         "💰 *Amount:* ₹" + order.getTotalAmount() + "\n\n" +
        //         "📍 *Address:*\n" + order.getAddress()
        // );

        cartService.clear();

        return ResponseEntity.ok(new OrderDto(order));
    }

    @GetMapping("/order/{id}")
    public OrderDto getOrder(@PathVariable Long id) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        return new OrderDto(order);
    }

    @GetMapping("/invoice/{id}")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long id) {

        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        byte[] pdf = invoiceService.generateInvoice(order);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
