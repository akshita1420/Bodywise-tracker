package com.bodywise.domain;

import jakarta.persistence.*;

@Entity
public class GroceryList {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false)
    public Long userId;
    @Column(nullable = false)
    public String name;
}
