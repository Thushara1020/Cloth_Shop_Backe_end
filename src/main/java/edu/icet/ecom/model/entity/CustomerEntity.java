package edu.icet.ecom.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Entity
@Table(name = "customer")
@Getter
@Setter
public class CustomerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false, unique = true)
    private String nic;

    private String address;

    @Column(name = "credit_limit", nullable = false)
    private Double creditLimit;

    // One Customer can have many Credit Sales
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<SaleEntity> sales;
}