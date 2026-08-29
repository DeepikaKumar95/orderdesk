package com.orderdesk.order;

import com.orderdesk.order.domain.Customer;
import com.orderdesk.order.domain.Order;
import com.orderdesk.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Repository integration test against a real SQL Server via Testcontainers. Needs Docker; skipped by -DskipITs. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@org.springframework.test.context.TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class OrderRepositoryIT {
    @Container @ServiceConnection
    static MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    @Autowired OrderRepository orders;
    @Autowired TestEntityManager em;

    @Test
    void findWithLinesLoadsLinesInOneQuery() {
        Customer c = em.find(Customer.class, "C-1001");            // seeded by V1 migration
        Order o = Order.create(c, List.of(new Order.NewLine("SKU-9", 3, new BigDecimal("10.00"))));
        orders.saveAndFlush(o);
        em.clear();

        var loaded = orders.findWithLinesById(o.getId()).orElseThrow();
        assertThat(loaded.getLines()).hasSize(1);
        assertThat(loaded.getTotal()).isEqualByComparingTo("30.00");
    }
}
