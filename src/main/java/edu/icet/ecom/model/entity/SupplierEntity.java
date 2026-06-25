package edu.icet.ecom.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Entity
@Table(name = "supplier")
@Getter
@Setter
public class SupplierEntity {

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

    // One Supplier can supply many Products
    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL)
    private List<ProductEntity> products;
}