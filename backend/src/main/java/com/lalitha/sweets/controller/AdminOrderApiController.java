package com.lalitha.sweets.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import com.lalitha.sweets.dto.OrderDto;
import com.lalitha.sweets.model.Order;
import com.lalitha.sweets.model.OrderStatus;
import com.lalitha.sweets.model.OrderStatusHistory;
import com.lalitha.sweets.repository.OrderRepository;
import com.lalitha.sweets.repository.OrderStatusHistoryRepository;
import com.lalitha.sweets.service.EmailService;
import com.lalitha.sweets.service.OrderService;
import com.lalitha.sweets.service.SmsService;
import com.lalitha.sweets.service.WhatsAppService;

/**
 * Admin-only order management. Requires a valid admin JWT (enforced by
 * SecurityConfig's `.requestMatchers("/api/admin/**").hasRole("ADMIN")`).
 */
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderApiController {

	private final OrderRepository orderRepository;
	private final OrderService orderService;
	private final EmailService emailService;
	private final OrderStatusHistoryRepository historyRepository;
	private final WhatsAppService whatsAppService;
	private final SmsService smsService;

	@Value("${app.frontend.base-url}")
	private String frontendBaseUrl;

	public AdminOrderApiController(
			OrderRepository orderRepository,
			OrderService orderService,
			EmailService emailService,
			OrderStatusHistoryRepository historyRepository,
			WhatsAppService whatsAppService,
			SmsService smsService) {

		this.orderRepository = orderRepository;
		this.orderService = orderService;
		this.emailService = emailService;
		this.historyRepository = historyRepository;
		this.whatsAppService = whatsAppService;
		this.smsService = smsService;
	}

	@GetMapping
	public Map<String, Object> list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) OrderStatus status,
			@RequestParam(required = false) String fromDate,
			@RequestParam(required = false) String toDate,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {

		Pageable pageable = PageRequest.of(page, size);

		LocalDateTime from = null;
		LocalDateTime to = null;

		if (fromDate != null && !fromDate.isEmpty()) {
			from = LocalDate.parse(fromDate).atStartOfDay();
		}
		if (toDate != null && !toDate.isEmpty()) {
			to = LocalDate.parse(toDate).atTime(23, 59, 59);
		}

		Page<Order> orderPage = orderRepository.findOrdersWithFilters(keyword, status, from, to, pageable);

		List<OrderDto> orders = orderPage.getContent().stream().map(OrderDto::new).toList();

		return Map.of(
				"orders", orders,
				"page", orderPage.getNumber(),
				"totalPages", orderPage.getTotalPages(),
				"totalElements", orderPage.getTotalElements()
		);
	}

	@GetMapping("/{id}")
	public OrderDto getOrder(@PathVariable Long id) {
		Order order = orderRepository.findByIdWithItems(id)
				.orElseThrow(() -> new NoSuchElementException("Order not found"));
		return new OrderDto(order);
	}

	@PutMapping("/{id}/status")
	public OrderDto updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {

		OrderStatus status = OrderStatus.valueOf(body.get("status"));

		Order order = orderService.getOrder(id);
		order.setStatus(status);
		Order savedOrder = orderRepository.save(order);

		notifyCustomer(savedOrder, status);

		OrderStatusHistory history = new OrderStatusHistory();
		history.setOrder(order);
		history.setStatus(status);
		history.setUpdatedTime(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
		history.setUpdatedBy("ADMIN");
		historyRepository.save(history);

		try {
			emailService.sendStatusUpdate(order);
		} catch (Exception e) {
			// Non-fatal: status is already persisted, don't fail the request over email.
		}

		return new OrderDto(savedOrder);
	}

	private void notifyCustomer(Order order, OrderStatus status) {

		// Always prefer the snapshot taken at checkout time. order.getCustomer()
		// is a live FK to a Customer row that can be shared/reused across
		// multiple checkouts under the same email (see CheckoutApiController's
		// findByEmail-or-create logic), so its phone can have since been
		// overwritten by a *later* order. The snapshot is what this specific
		// order's customer actually was at the time it was placed.
		String phone = order.getCustomerPhoneSnapshot() != null
				? order.getCustomerPhoneSnapshot()
				: (order.getCustomer() != null ? order.getCustomer().getPhone() : null);

		if (phone == null) {
			return;
		}

		// SMS is the interim customer-facing channel while the official
		// WhatsApp Business API application (Meta verification + template
		// approval) is pending. whatsAppService is left wired but unused
		// below - swap the two lines back once Meta approval comes through.
		String trackUrl = frontendBaseUrl + "/track/" + order.getId();
		smsService.sendOrderStatusSms(order, status, phone, trackUrl);
		// whatsAppService.sendWhatsApp(phone, statusMessage(order, status));
	}

	private String statusMessage(Order order, OrderStatus status) {

		String name = order.getCustomerNameSnapshot();
		Long id = order.getId();

		return switch (status) {
			case CONFIRMED -> "🎉 *Order Confirmed!*\n\nHi " + name + ",\n\nYour order *#" + id + "* has been successfully confirmed.\n\n🧾 *Amount:* ₹" + order.getTotalAmount() + "\n📍 *Delivery Address:*\n" + order.getAddress();
			case PREPARING -> "👨‍🍳 *Preparing Your Order*\n\nHi " + name + ",\n\nYour order *#" + id + "* is now being freshly prepared.";
			case PACKED -> "📦 *Order Packed!*\n\nHi " + name + ",\n\nYour order *#" + id + "* has been packed and is ready for dispatch.";
			case SHIPPED -> "🚚 *Order Shipped!*\n\nHi " + name + ",\n\nYour order *#" + id + "* has been shipped and is on the way!";
			case OUT_FOR_DELIVERY -> "🚴 *Out for Delivery!*\n\nHi " + name + ",\n\nYour order *#" + id + "* is on the way!";
			case DELIVERED -> "✅🎉 *Order Delivered!*\n\nHi " + name + ",\n\nYour order *#" + id + "* has been delivered successfully 🎉";
			case CANCELLED -> "❌ Your order #" + id + " has been cancelled.";
			default -> "";
		};
	}
}
