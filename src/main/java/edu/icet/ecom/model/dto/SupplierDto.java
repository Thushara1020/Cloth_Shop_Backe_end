package edu.icet.ecom.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupplierDto {
    private Long id;
    private String name;
    private String phone;
    private String nic;
    private String address;
}