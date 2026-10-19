package com.lalitha.sweets.service;



import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.lalitha.sweets.model.CartItem;
import com.lalitha.sweets.model.Order;
import com.lalitha.sweets.model.OrderStatus;
import com.lalitha.sweets.model.OrderStatusHistory;
import com.lalitha.sweets.repository.OrderRepository;
import com.lalitha.sweets.repository.OrderStatusHistoryRepository;

import jakarta.transaction.Transactional;

@Service
public class OrderService {
	
	
	@Autowired
	private OrderRepository orderRepository;
	
	@Autowired
	private WhatsAppService whatsAppService;
	
	@Value("${whatsapp.admin.number}")
	private String adminNumber;
	
	@Autowired
	private OrderStatusHistoryRepository historyRepository;
	
	public Order getOrder(Long id) {
		return orderRepository.findById(id).orElseThrow(()-> new RuntimeException("Order not found"));
	}

	
	@Transactional
	public Order updateStatus(Long id, OrderStatus newStatus) {
			
		Order order = orderRepository.findById(id)
	            .orElseThrow(() -> new RuntimeException("Order not found"));

	    OrderStatus current = order.getStatus();

	    if (!isValidTransition(current, newStatus)) {
	        throw new IllegalStateException("Invalid status transition");
	    }

	    order.setStatus(newStatus);

	    order.setLastUpdatedAt(LocalDateTime.now());	    
	    if (newStatus == OrderStatus.SHIPPED) {
	        order.setEstimatedDeliveryDate(LocalDate.now().plusDays(3));
	    }
	    
	    if (current == OrderStatus.DELIVERED) {
			throw new IllegalStateException("Delivered orders cannot be modified");
		}
		return orderRepository.save(order);
	}

	
	public Order calculateCharges(Order order, Collection<CartItem> collection, String state) {
		
		BigDecimal subtotal = BigDecimal.ZERO;
		BigDecimal totalWeightInKg = BigDecimal.ZERO;
		System.out.println("State Received: "+state);
		for (CartItem item : collection) {
			BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
		
			subtotal = subtotal.add(itemTotal);
			
			String label = item.getPriceLabel().toLowerCase().trim();
			
			BigDecimal weight=BigDecimal.ZERO;
			
			try {
			if(label.contains("kg")) {
				label = label.replace("kg", "").trim();
				weight = new BigDecimal(label);
			}
			else if(label.contains("g")) {
				label = label.replace("g", "").trim();
				weight = new BigDecimal(label).divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP);
			}
			}catch(Exception e) {
				System.out.println("Weight parsing error for label: "+item.getPriceLabel());
			}
			totalWeightInKg = totalWeightInKg.add(weight.multiply(BigDecimal.valueOf(item.getQuantity())));
		}
		
		System.out.println("CALCULATION STATE PARAM: " + state);
		//System.out.println("CUSTOMER STATE: " + order.getCustomer().getState());

		
		BigDecimal handling = BigDecimal.valueOf(20);
		
		//BigDecimal delivery = calculateDeliveryCharge(totalWeightInKg, subtotal, state);
		BigDecimal total = subtotal.add(handling);
		//BigDecimal total = subtotal.add(handling).add(delivery);
		
		System.out.println("TOTAL WEIGHT: " + totalWeightInKg);
		System.out.println("STATE: " + state);
		//System.out.println("DELIVERY: " + delivery);
		
		order.setSubtotal(subtotal);
		order.setHandlingCharges(handling);
		order.setDeliveryCharges(BigDecimal.ZERO);
		order.setTotalAmount(total);
		
		return order;
	}


//	private BigDecimal calculateDeliveryCharge(BigDecimal weight,
//            BigDecimal subtotal,
//            String state) {
//
//		// FREE DELIVERY RULE
//		if (subtotal.compareTo(BigDecimal.valueOf(700)) >= 0) {
//			return BigDecimal.ZERO;
//		}
//
//		BigDecimal baseCharge = BigDecimal.ZERO;
//		BigDecimal ratePerKg = BigDecimal.ZERO;
//
//		if (state == null) state = "";
//
//		state = state.tr9im();
//
//		// ---------------- ZONE A ----------------
//		if (state.equalsIgnoreCase("Andhra Pradesh")) {
//			baseCharge = BigDecimal.valueOf(40);
//			ratePerKg = BigDecimal.valueOf(50);
//		}
//
//		// ---------------- ZONE B ----------------
//		else if (state.equalsIgnoreCase("Telangana")
//				|| state.equalsIgnoreCase("Tamil Nadu")
//				|| state.equalsIgnoreCase("Karnataka")
//				|| state.equalsIgnoreCase("Kerala")) {
//
//			baseCharge = BigDecimal.valueOf(60);
//			ratePerKg = BigDecimal.valueOf(70);
//		}
//
//		// ---------------- ZONE C ----------------
//		else if (state.equalsIgnoreCase("Maharashtra")
//				|| state.equalsIgnoreCase("Gujarat")
//				|| state.equalsIgnoreCase("Rajasthan")
//				|| state.equalsIgnoreCase("Madhya Pradesh")
//				|| state.equalsIgnoreCase("Goa")
//				|| state.equalsIgnoreCase("Chhattisgarh")) {
//
//			baseCharge = BigDecimal.valueOf(80);
//			ratePerKg = BigDecimal.valueOf(90);
//		}
//
//		// ---------------- ZONE D ----------------
//		else {
//			baseCharge = BigDecimal.valueOf(100);
//			ratePerKg = BigDecimal.valueOf(120);
//		}
//
//		BigDecimal delivery = baseCharge.add(weight.multiply(ratePerKg));
//
//		return delivery.setScale(0, RoundingMode.CEILING);
//	}


	
	public boolean isValidTransition(OrderStatus current, OrderStatus next) {

	    Map<OrderStatus, List<OrderStatus>> allowed = Map.of(
	        OrderStatus.PLACED, List.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
	        OrderStatus.CONFIRMED, List.of(OrderStatus.PREPARING, OrderStatus.CANCELLED),
	        OrderStatus.PREPARING, List.of(OrderStatus.PACKED),
	        OrderStatus.PACKED, List.of(OrderStatus.SHIPPED),
	        OrderStatus.SHIPPED, List.of(OrderStatus.OUT_FOR_DELIVERY),
	        OrderStatus.OUT_FOR_DELIVERY, List.of(OrderStatus.DELIVERED),
	        OrderStatus.DELIVERED, List.of(),
	        OrderStatus.CANCELLED, List.of()
	    );

	    return allowed.get(current).contains(next);
	}

	
	public LocalDate getEstimatedDelivery(Order order) {

	    LocalDate orderDate = order.getOrderDate().toLocalDate();

	    return switch (order.getStatus()) {
	        case PLACED, CONFIRMED -> orderDate.plusDays(5);
	        case PREPARING, PACKED -> orderDate.plusDays(3);
	        case SHIPPED -> orderDate.plusDays(2);
	        case OUT_FOR_DELIVERY -> orderDate.plusDays(1);
	        case DELIVERED -> orderDate;
	        default -> null;
	    };
	}
	
	
	public Order cancelOrder(Long orderId, String reason) {

	    Order order = orderRepository.findById(orderId)
	            .orElseThrow(() -> new RuntimeException("Order not found"));

	    order.setStatus(OrderStatus.CANCELLED);
	    order.setCancelReason(reason);

	    Order saved = orderRepository.save(order);

	    OrderStatusHistory history = new OrderStatusHistory();
	    history.setOrder(saved);
	    history.setStatus(OrderStatus.CANCELLED);
	    history.setUpdatedTime(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
	    history.setUpdatedBy("CUSTOMER");

	    historyRepository.save(history);

	    return saved;
	}
	
	
	public List<Order> getRecentOrders(Pageable pageable){
		return orderRepository.findTop5ByOrderByIdDesc();
	}
	
	
	public List<Order> filterOrders(String keyword,
            OrderStatus status,
            String fromDate,
            String toDate) {

	List<Order> orders = orderRepository.findAllByOrderByOrderDateDesc();

	if (keyword != null && !keyword.isBlank()) {
		String lower = keyword.toLowerCase();
		orders = orders.stream()
				.filter(o ->
				String.valueOf(o.getId()).contains(keyword) ||
				(o.getCustomerNameSnapshot() != null &&
				o.getCustomerNameSnapshot().toLowerCase().contains(lower)))
				.toList();
	}

	if (status != null) {
		orders = orders.stream()
				.filter(o -> o.getStatus() == status)
				.toList();
	}

	if (fromDate != null && !fromDate.isBlank()) {
		LocalDate from = LocalDate.parse(fromDate);
		orders = orders.stream()
				.filter(o -> !o.getOrderDate().toLocalDate().isBefore(from))
				.toList();
	}

	if (toDate != null && !toDate.isBlank()) {
		LocalDate to = LocalDate.parse(toDate);
		orders = orders.stream()
				.filter(o -> !o.getOrderDate().toLocalDate().isAfter(to))
				.toList();
	}

	return orders;
	}


	public List<String> getAllowedStatuses(OrderStatus status) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	

}
