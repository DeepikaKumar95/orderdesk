package com.orderdesk.inventory.service;

import com.orderdesk.inventory.domain.Reservation;
import com.orderdesk.inventory.repository.InventoryRepository;
import com.orderdesk.inventory.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InventoryService {
    private final InventoryRepository inventory;
    private final ReservationRepository reservations;

    public InventoryService(InventoryRepository inventory, ReservationRepository reservations) {
        this.inventory = inventory; this.reservations = reservations;
    }

    public record LineRequest(String sku, int qty) {}

    /**
     * All-or-nothing reservation for one order. REQUIRES_NEW: a rejected line rolls back this inner
     * transaction only, so the listener's outer transaction (dedupe marker) can still commit.
     * Throws InsufficientStock when any line cannot be reserved.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean reserve(UUID orderId, List<LineRequest> lines) {
        for (LineRequest l : lines) {
            if (inventory.tryReserve(l.sku(), l.qty()) != 1) {
                throw new InsufficientStock(l.sku());
            }
            reservations.save(new Reservation(orderId, l.sku(), l.qty()));
        }
        return true;
    }

    @Transactional
    public void release(UUID orderId) {
        for (Reservation r : reservations.findByOrderIdAndState(orderId, Reservation.State.RESERVED)) {
            inventory.release(r.getSku(), r.getQty());
            r.release();
        }
    }

    public static class InsufficientStock extends RuntimeException {
        public InsufficientStock(String sku) { super("Insufficient stock for " + sku); }
    }
}
