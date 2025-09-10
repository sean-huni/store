package com.example.store.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "\"order\"")
public class Order extends AbstractSuperEntity {
    private String description;
    @ManyToOne(fetch = FetchType.LAZY)
    private Customer customer;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "order", orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductOrder> products = new ArrayList<>();

    // select * from order
    // left join product on
}
