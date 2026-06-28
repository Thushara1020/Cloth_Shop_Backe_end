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

    @Column(name = "current_balance", nullable = false)
    private Double currentBalance = 0.0;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<SaleEntity> sales;
    
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<CreditSettlementEntity> settlements;
}