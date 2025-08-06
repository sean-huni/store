package com.example.store.persistence.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Entity
@Setter
@Getter
public class Customer extends AbstractSuperEntity {
    private String name;
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Order> orders = new HashSet<>();
}
