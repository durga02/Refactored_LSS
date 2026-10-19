package com.lalitha.sweets.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.web.bind.annotation.*;

import com.lalitha.sweets.dto.DashboardStatsDto;
import com.lalitha.sweets.model.PaymentStatus;
import com.lalitha.sweets.repository.OrderItemRepository;
import com.lalitha.sweets.repository.OrderRepository;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardApiController {

	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;

	public AdminDashboardApiController(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
		this.orderRepository = orderRepository;
		this.orderItemRepository = orderItemRepository;
	}

	@GetMapping
	public DashboardStatsDto dashboard() {

		DashboardStatsDto dto = new DashboardStatsDto();

		dto.setTotalOrders(orderRepository.countTotalOrders());
		dto.setTotalRevenue(Objects.requireNonNullElse(orderRepository.totalRevenue(), 0d));
		dto.setPendingOrders(orderRepository.countByPaymentStatus(PaymentStatus.PENDING));
		dto.setTodayOrders(orderRepository.countTodayOrders());
		dto.setTodayRevenue(Objects.requireNonNullElse(orderRepository.todayRevenue(), 0d));

		int[] monthlyData = new int[12];
		for (Object[] row : orderRepository.monthlyOrders()) {
			int month = (int) row[0];
			int count = ((Long) row[1]).intValue();
			monthlyData[month - 1] = count;
		}
		dto.setMonthlyOrders(monthlyData);

		List<DashboardStatsDto.TopProduct> topProducts = new ArrayList<>();
		for (Object[] row : orderItemRepository.getTopSellingProducts()) {
			topProducts.add(new DashboardStatsDto.TopProduct((String) row[0], (Long) row[1]));
		}
		dto.setTopProducts(topProducts);

		return dto;
	}
}
