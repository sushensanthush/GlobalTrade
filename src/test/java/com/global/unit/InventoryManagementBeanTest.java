package com.global.unit;

import com.global.entity.InventoryItem;
import com.global.exception.InsufficientInventoryException;
import com.global.exception.SupplyChainException;
import com.global.session.inventory.InventoryManagementBean;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryManagementBean Unit Tests")
class InventoryManagementBeanTest {

    @Mock
    private EntityManager em;

    @InjectMocks
    private InventoryManagementBean inventoryBean;

    private InventoryItem sampleItem;

    @BeforeEach
    void setUp() {
        sampleItem = new InventoryItem(
                "ELC-001",
                "Circuit Boards",
                500,
                100,
                200
        );

        sampleItem.setUnitCost(BigDecimal.valueOf(45.00));
        sampleItem.setCurrencyCode("USD");
        sampleItem.setWarehouseLocation("Warehouse-A");
    }

    @Test
    @DisplayName("deductStock — sufficient stock should reduce quantity")
    void deductStock_sufficientStock_reducesQuantity()
            throws Exception {

        @SuppressWarnings("unchecked")
        TypedQuery<InventoryItem> mockQuery =
                mock(TypedQuery.class);

        when(em.createNamedQuery(
                "InventoryItem.findByItemCode",
                InventoryItem.class
        )).thenReturn(mockQuery);

        when(mockQuery.setParameter(
                eq("itemCode"),
                eq("ELC-001")
        )).thenReturn(mockQuery);

        when(mockQuery.getResultList())
                .thenReturn(List.of(sampleItem));

        doNothing().when(em)
                .lock(
                        eq(sampleItem),
                        eq(LockModeType.PESSIMISTIC_WRITE)
                );

        when(em.merge(any(InventoryItem.class)))
                .thenReturn(sampleItem);

        InventoryItem result =
                inventoryBean.deductStock("ELC-001", 100);

        assertThat(result.getQuantityOnHand())
                .isEqualTo(400);

        verify(em).merge(sampleItem);
    }

    @Test
    @DisplayName("deductStock — insufficient stock throws exception")
    void deductStock_insufficientStock_throwsException() {

        sampleItem.setQuantityOnHand(50);

        @SuppressWarnings("unchecked")
        TypedQuery<InventoryItem> mockQuery =
                mock(TypedQuery.class);

        when(em.createNamedQuery(
                "InventoryItem.findByItemCode",
                InventoryItem.class
        )).thenReturn(mockQuery);

        when(mockQuery.setParameter(
                eq("itemCode"),
                eq("ELC-001")
        )).thenReturn(mockQuery);

        when(mockQuery.getResultList())
                .thenReturn(List.of(sampleItem));

        doNothing().when(em)
                .lock(
                        eq(sampleItem),
                        eq(LockModeType.PESSIMISTIC_WRITE)
                );

        InsufficientInventoryException ex =
                catchThrowableOfType(
                        () -> inventoryBean.deductStock(
                                "ELC-001",
                                200
                        ),
                        InsufficientInventoryException.class
                );

        assertThat(ex).isNotNull();
        assertThat(ex.getItemCode())
                .isEqualTo("ELC-001");
        assertThat(ex.getRequested())
                .isEqualTo(200);
        assertThat(ex.getAvailable())
                .isEqualTo(50);

        verify(em, never()).merge(any());
    }

    @Test
    @DisplayName("deductStock — missing item throws SupplyChainException")
    void deductStock_itemNotFound_throwsException() {

        @SuppressWarnings("unchecked")
        TypedQuery<InventoryItem> mockQuery =
                mock(TypedQuery.class);

        when(em.createNamedQuery(
                "InventoryItem.findByItemCode",
                InventoryItem.class
        )).thenReturn(mockQuery);

        when(mockQuery.setParameter(
                eq("itemCode"),
                eq("MISSING")
        )).thenReturn(mockQuery);

        when(mockQuery.getResultList())
                .thenReturn(List.of());

        SupplyChainException ex =
                catchThrowableOfType(
                        () -> inventoryBean.deductStock(
                                "MISSING",
                                10
                        ),
                        SupplyChainException.class
                );

        assertThat(ex.getErrorCode())
                .isEqualTo("ITEM_NOT_FOUND");
    }

    @Test
    @DisplayName("replenishStock — increases quantity")
    void replenishStock_validItem_increasesQuantity()
            throws SupplyChainException {

        @SuppressWarnings("unchecked")
        TypedQuery<InventoryItem> mockQuery =
                mock(TypedQuery.class);

        when(em.createNamedQuery(
                "InventoryItem.findByItemCode",
                InventoryItem.class
        )).thenReturn(mockQuery);

        when(mockQuery.setParameter(
                eq("itemCode"),
                eq("ELC-001")
        )).thenReturn(mockQuery);

        when(mockQuery.getResultList())
                .thenReturn(List.of(sampleItem));

        when(em.merge(any(InventoryItem.class)))
                .thenReturn(sampleItem);

        InventoryItem result =
                inventoryBean.replenishStock(
                        "ELC-001",
                        200
                );

        assertThat(result.getQuantityOnHand())
                .isEqualTo(700);

        assertThat(result.getLastReplenishment())
                .isNotNull();
    }

    @Test
    @DisplayName("replenishStock — missing item throws exception")
    void replenishStock_itemNotFound_throwsException() {

        @SuppressWarnings("unchecked")
        TypedQuery<InventoryItem> mockQuery =
                mock(TypedQuery.class);

        when(em.createNamedQuery(
                "InventoryItem.findByItemCode",
                InventoryItem.class
        )).thenReturn(mockQuery);

        when(mockQuery.setParameter(
                eq("itemCode"),
                eq("GHOST")
        )).thenReturn(mockQuery);

        when(mockQuery.getResultList())
                .thenReturn(List.of());

        assertThatThrownBy(
                () -> inventoryBean.replenishStock(
                        "GHOST",
                        50
                )
        )
                .isInstanceOf(SupplyChainException.class)
                .hasMessageContaining("GHOST");
    }

    @Test
    @DisplayName("createItem — persists item")
    void createItem_validInputs_persistsAndReturnsItem() {

        doNothing().when(em)
                .persist(any(InventoryItem.class));

        InventoryItem result =
                inventoryBean.createItem(
                        "MEC-010",
                        "Hydraulic Valves",
                        300,
                        75,
                        150,
                        BigDecimal.valueOf(88.00),
                        "USD",
                        "Warehouse-B",
                        "8481.20"
                );

        assertThat(result).isNotNull();
        assertThat(result.getItemCode())
                .isEqualTo("MEC-010");
        assertThat(result.getQuantityOnHand())
                .isEqualTo(300);
        assertThat(result.getReorderPoint())
                .isEqualTo(75);
        assertThat(result.getWarehouseLocation())
                .isEqualTo("Warehouse-B");
        assertThat(result.getHsCode())
                .isEqualTo("8481.20");

        verify(em, times(1))
                .persist(any(InventoryItem.class));
    }

    @Test
    @DisplayName("findLowStockItems — returns low stock list")
    void findLowStockItems_returnsLowStockList() {

        InventoryItem lowItem =
                new InventoryItem(
                        "PKG-003",
                        "Packaging",
                        20,
                        50,
                        100
                );

        @SuppressWarnings("unchecked")
        TypedQuery<InventoryItem> mockQuery =
                mock(TypedQuery.class);

        when(em.createNamedQuery(
                "InventoryItem.findLowStock",
                InventoryItem.class
        )).thenReturn(mockQuery);

        when(mockQuery.getResultList())
                .thenReturn(List.of(lowItem));

        List<InventoryItem> result =
                inventoryBean.findLowStockItems();

        assertThat(result).hasSize(1);

        assertThat(result.get(0).getItemCode())
                .isEqualTo("PKG-003");

        assertThat(result.get(0).getQuantityOnHand())
                .isLessThanOrEqualTo(
                        result.get(0).getReorderPoint()
                );
    }
}