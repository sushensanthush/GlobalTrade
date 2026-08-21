package com.global.integration;

import com.global.entity.Shipment;
import com.global.entity.ShipmentStatus;
import com.global.session.shipment.ShipmentManagementBean;
import jakarta.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ArquillianExtension.class)
public class ShipmentManagementIT {

    @EJB
    private ShipmentManagementBean shipmentManagementBean;

    @Deployment
    public static WebArchive createDeployment() {

        return ShrinkWrap
                .create(WebArchive.class, "GlobalTradeSCM-test.war")


                .addPackages(
                        true,
                        "com.global.entity",
                        "com.global.session",
                        "com.global.exception",
                        "com.global.interceptor"
                )


                .addAsResource(
                        "META-INF/persistence.xml",
                        "META-INF/persistence.xml"
                )


                .addAsWebInfResource(
                        "beans.xml",
                        "beans.xml"
                );
    }

    @Test
    void createShipment_insideRealContainer_shouldPersistShipment()
            throws Exception {

        Shipment shipment =
                shipmentManagementBean.createShipment(
                        "China",
                        "United States",
                        "GlobalShip",
                        BigDecimal.valueOf(200),
                        BigDecimal.valueOf(8000),
                        "USD",
                        LocalDateTime.now().plusDays(14),
                        null,
                        "Arquillian integration test"
                );

        assertNotNull(shipment);

        assertNotNull(shipment.getTrackingNumber());

        assertTrue(
                shipment.getTrackingNumber().startsWith("GT-")
        );

        assertEquals(
                ShipmentStatus.PENDING,
                shipment.getStatus()
        );
    }
}