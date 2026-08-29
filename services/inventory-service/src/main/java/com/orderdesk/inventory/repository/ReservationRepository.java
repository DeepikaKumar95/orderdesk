package com.orderdesk.inventory.repository;

import com.orderdesk.inventory.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    List<Reservation> findByOrderIdAndState(UUID orderId, Reservation.State state);
}
