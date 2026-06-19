package com.tahaberkamcadev.e_com.inventory_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.tahaberkamcadev.inventory_service.InventoryServiceApplication;

@SpringBootTest(classes = InventoryServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(
        properties = {
            "spring.docker.compose.enabled=false",
            "spring.datasource.url=jdbc:h2:mem:inventory_it;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
            "spring.kafka.bootstrap-servers=127.0.0.1:19092",
            "spring.kafka.consumer.group-id=inventory-service",
            "spring.kafka.listener.auto-startup=false",
            "app.kafka.topics.reserve-request=saga.inventory.reserve.request",
            "app.kafka.topics.reserve-request-dlt=saga.inventory.reserve.request.DLT"
        })
class InventoryServiceApplicationTests {

    @Test
    void contextLoads() {}
}
