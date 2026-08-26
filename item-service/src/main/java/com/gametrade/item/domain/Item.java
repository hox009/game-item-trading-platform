package com.gametrade.item.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A tradable listing (SPU). Each item groups one or more {@link Sku} variants.
 * {@code minPrice} is denormalized from its SKUs to speed up list/search.
 */
@Getter
@Setter
@Entity
@Table(name = "items", indexes = {
        @Index(name = "idx_item_game", columnList = "game"),
        @Index(name = "idx_item_category", columnList = "category"),
        @Index(name = "idx_item_status_price", columnList = "status,min_price")
})
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(nullable = false, length = 64)
    private String game;

    @Column(nullable = false, length = 64)
    private String category;

    @Column(length = 1024)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ItemStatus status = ItemStatus.ON_SHELF;

    @Column(name = "min_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal minPrice = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = jakarta.persistence.FetchType.EAGER)
    @JoinColumn(name = "item_id")
    private List<Sku> skus = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        recalculateMinPrice();
    }

    /** Recomputes the denormalized minimum SKU price. */
    public void recalculateMinPrice() {
        this.minPrice = skus.stream()
                .map(Sku::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }
}
