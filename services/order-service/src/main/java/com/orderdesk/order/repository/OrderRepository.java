package com.orderdesk.order.repository;

import com.orderdesk.order.api.dto.OrderSummary;
import com.orderdesk.order.domain.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @EntityGraph(attributePaths = "lines")            // one query, no N+1
    Optional<Order> findWithLinesById(UUID id);

    @Query("""
           select new com.orderdesk.order.api.dto.OrderSummary(o.id, o.customerId, o.status, o.total, o.createdAt)
           from Order o
           where (:customerId is null or o.customerId = :customerId)
           """)
    Page<OrderSummary> search(@Param("customerId") String customerId, Pageable pageable);
}
