package com.gametrade.item.service;

import com.gametrade.common.exception.BusinessException;
import com.gametrade.item.domain.Item;
import com.gametrade.item.dto.CreateItemRequest;
import com.gametrade.item.dto.ItemResponse;
import com.gametrade.item.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ItemServiceTest {

    private ItemRepository itemRepository;
    private ItemService itemService;

    @BeforeEach
    void setUp() {
        itemRepository = mock(ItemRepository.class);
        itemService = new ItemService(itemRepository);
    }

    @Test
    void createItem_setsMinPriceFromCheapestSku() {
        CreateItemRequest request = new CreateItemRequest(
                "AK-47 Redline", "CS2", "rifle-skin", "field-tested",
                List.of(
                        new CreateItemRequest.SkuInput("Field-Tested", new BigDecimal("42.50")),
                        new CreateItemRequest.SkuInput("Minimal-Wear", new BigDecimal("88.00"))
                ));
        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> {
            Item i = inv.getArgument(0);
            i.setId(100L);
            return i;
        });

        ItemResponse response = itemService.createItem(9L, request);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.sellerId()).isEqualTo(9L);
        assertThat(response.minPrice()).isEqualByComparingTo("42.50");
        assertThat(response.skus()).hasSize(2);
    }

    @Test
    void getItem_throwsWhenNotFound() {
        when(itemRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.getItem(404L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void setStatus_offShelfBySeller() {
        Item item = new Item();
        item.setId(5L);
        item.setSellerId(9L);
        item.setStatus(com.gametrade.item.domain.ItemStatus.ON_SHELF);
        when(itemRepository.findById(5L)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));

        ItemResponse response = itemService.setStatus(9L, 5L, com.gametrade.item.domain.ItemStatus.OFF_SHELF);

        assertThat(response.status()).isEqualTo("OFF_SHELF");
    }

    @Test
    void setStatus_rejectsNonOwner() {
        Item item = new Item();
        item.setId(5L);
        item.setSellerId(9L);
        when(itemRepository.findById(5L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> itemService.setStatus(1L, 5L, com.gametrade.item.domain.ItemStatus.OFF_SHELF))
                .isInstanceOf(BusinessException.class);
    }
}
