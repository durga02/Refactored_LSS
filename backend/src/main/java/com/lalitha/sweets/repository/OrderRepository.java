package com.lalitha.sweets.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lalitha.sweets.model.*;

public interface OrderRepository extends JpaRepository<Order, Long>{

	Order findTopByOrderByIdDesc();

	List<Order> findByStatus(OrderStatus status);
	
	@Query("SELECT COUNT(o) FROM Order o")
	long countTotalOrders();
	
	@Query("SELECT COALESCE(SUM(o.totalAmount),0) FROM Order o")
	Double totalRevenue();
	
	@Query("SELECT COALESCE(SUM(o.totalAmount),0) FROM Order o WHERE CAST(o.orderDate AS LocalDate) = LOCAL DATE")
	Double todayRevenue();

	@Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
	long countByStatus(@Param("status") OrderStatus status);

	@Query("SELECT COUNT(o) FROM Order o WHERE CAST(o.orderDate AS LocalDate) = LOCAL DATE")
	long countTodayOrders();
	
	
	@Query("SELECT MONTH(o.orderDate), COUNT(o) FROM Order o GROUP BY MONTH(o.orderDate)")
	List<Object[]> monthlyOrders();
	
	@Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
	Optional<Order> findByIdWithItems(@Param("id") Long id);

	@Query("""
		    SELECT o FROM Order o
		    JOIN FETCH o.customer c
		    WHERE 
		        CAST(o.id AS string) LIKE %:keyword%
		        OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
		""")
		List<Order> search(@Param("keyword") String keyword);

	//@Query("SELECT o FROM Order o ORDER BY o.id DESC")
	List<Order> findTop5ByOrderByIdDesc();

	List<Order> findAllByOrderByIdDesc();
	
	Page<Order> findAllByOrderByOrderDateDesc(Pageable pageable);

	List<Order> findAllByOrderByOrderDateDesc();

	
	@Query("""
        SELECT o FROM Order o
        WHERE
        (
            :search IS NULL
            OR CAST(o.id AS string) LIKE CONCAT('%', CAST(:search AS string), '%')
            OR LOWER(o.customerNameSnapshot) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
        )
        AND
        (
            (:status IS NULL AND o.status <> com.lalitha.sweets.model.OrderStatus.PENDING)
            OR o.status = :status
        )
        AND
        (
            o.orderDate >= COALESCE(:fromDate, o.orderDate)
        )
        AND
        (
            o.orderDate <= COALESCE(:toDate, o.orderDate)
        )
        ORDER BY o.orderDate DESC
    	""")
	Page<Order> findOrdersWithFilters(
        @Param("search") String search,
        @Param("status") OrderStatus status,
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDate") LocalDateTime toDate,
        Pageable pageable
	);
}
