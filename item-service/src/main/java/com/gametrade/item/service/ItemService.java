package com.gametrade.item.service;

import com.gametrade.common.api.ResultCode;
import com.gametrade.common.exception.BusinessException;
import com.gametrade.item.domain.Item;
import com.gametrade.item.domain.ItemStatus;
import com.gametrade.item.domain.Sku;
import com.gametrade.item.dto.CreateItemRequest;
import com.gametrade.item.dto.ItemResponse;
import com.gametrade.item.dto.ItemSearchQuery;
import com.gametrade.item.repository.ItemRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class ItemService {

    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Transactional
    public ItemResponse createItem(Long sellerId, CreateItemRequest request) {
        Item item = new Item();
        item.setSellerId(sellerId);
        item.setTitle(request.title());
        item.setGame(request.game());
        item.setCategory(request.category());
        item.setDescription(request.description());
        item.setStatus(ItemStatus.ON_SHELF);

        for (CreateItemRequest.SkuInput input : request.skus()) {
            Sku sku = new Sku();
            sku.setSpec(input.spec());
            sku.setPrice(input.price());
            item.getSkus().add(sku);
        }
        item.recalculateMinPrice();

        Item saved = itemRepository.save(item);
        return ItemResponse.from(saved);
    }

    /**
     * Hot-path read cached in Redis. Cache is keyed by item id and used heavily
     * by the AI assistant and detail pages.
     */
    @Cacheable(cacheNames = "items", key = "#id")
    @Transactional(readOnly = true)
    public ItemResponse getItem(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.ITEM_NOT_FOUND));
        return ItemResponse.from(item);
    }

    /** Lists all items published by a seller, newest first. */
    @Transactional(readOnly = true)
    public List<ItemResponse> listBySeller(Long sellerId) {
        return itemRepository.findBySellerIdOrderByIdDesc(sellerId).stream()
                .map(ItemResponse::from)
                .toList();
    }

    /**
     * Toggles a listing on/off shelf. Only the owning seller may change it.
     * Evicts the cached detail so buyers immediately see the new status.
     */
    @CacheEvict(cacheNames = "items", key = "#itemId")
    @Transactional
    public ItemResponse setStatus(Long sellerId, Long itemId, ItemStatus status) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ResultCode.ITEM_NOT_FOUND));
        if (!item.getSellerId().equals(sellerId)) {
            throw new BusinessException(ResultCode.ITEM_NOT_FOUND);
        }
        item.setStatus(status);
        return ItemResponse.from(itemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public Page<ItemResponse> search(ItemSearchQuery query, Pageable pageable) {
        return itemRepository.findAll(buildSpecification(query), pageable)
                .map(ItemResponse::from);
    }

    private Specification<Item> buildSpecification(ItemSearchQuery query) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), ItemStatus.ON_SHELF));

            if (StringUtils.hasText(query.keyword())) {
                predicates.add(cb.like(root.get("title"), "%" + query.keyword().trim() + "%"));
            }
            if (StringUtils.hasText(query.game())) {
                predicates.add(cb.equal(root.get("game"), query.game().trim()));
            }
            if (StringUtils.hasText(query.category())) {
                predicates.add(cb.equal(root.get("category"), query.category().trim()));
            }
            if (query.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("minPrice"), query.minPrice()));
            }
            if (query.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("minPrice"), query.maxPrice()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
