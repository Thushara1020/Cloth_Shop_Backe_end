package edu.icet.ecom.service;

import edu.icet.ecom.model.dto.CustomerDto;
import java.util.List;

public interface CustomerService {
    void saveCustomer(CustomerDto customerDto);
    List<CustomerDto> getAllCustomers();
    CustomerDto getCustomerById(Long id);
    void updateCustomer(CustomerDto customerDto);
    void deleteCustomer(Long id);
}