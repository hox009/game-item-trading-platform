package com.gametrade.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

/**
 * Authoritative stock for a SKU. {@code available = total - frozen}.
 * An optimistic {@code version} guards against lost updates under concurrency.
 */
@Getter
@Setter
@Entity
@Table(name = "stock")
public class Stock {

    @Id
    @Column(name = "sku_id")
    private Long skuId;

    @Column(nullable = false)
    private Integer total = 0;

    @Column(nullable = false)
    private Integer frozen = 0;

    @Version
    private Long version;

    public int available() {
        return total - frozen;
    }
}
