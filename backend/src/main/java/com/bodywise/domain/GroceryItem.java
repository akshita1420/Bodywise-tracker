package com.bodywise.domain;

import jakarta.persistence.*;

@Entity
public class GroceryItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false)
    public Long userId;
    @Column(nullable = false)
    public Long listId;
    @Column(nullable = false)
    public String name;
    public String quantity;
    public boolean purchased;
}
