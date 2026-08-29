package com.orderdesk.inventory.repository;

import com.orderdesk.inventory.domain.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, String> {

    /** The oversell guard lives in the database: conditional UPDATE, rows affected tells us if it won. */
    @Modifying
    @Query("update Inventory i set i.reserved = i.reserved + :qty where i.sku = :sku and i.onHand - i.reserved >= :qty")
    int tryReserve(@Param("sku") String sku, @Param("qty") int qty);

    @Modifying
    @Query("update Inventory i set i.reserved = i.reserved - :qty where i.sku = :sku")
    int release(@Param("sku") String sku, @Param("qty") int qty);
}
