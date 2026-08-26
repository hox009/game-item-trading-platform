package com.gametrade.item.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A concrete sellable variant of an {@link Item}, e.g. a specific wear/level.
 * Stock is owned by inventory-service; this entity holds catalog data only.
 */
@Getter
@Setter
@Entity
@Table(name = "skus")
public class Sku {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false, insertable = false, updatable = false)
    private Long itemId;

    @Column(nullable = false, length = 128)
    private String spec;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal price;
}
