package edu.icet.ecom.service.impl;

import edu.icet.ecom.model.dto.CustomerDto;
import edu.icet.ecom.service.CustomerService;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService {



    @Override
    public void saveCustomer(CustomerDto customerDto) {
        System.out.println("Customer saved: " + customerDto);
    }

    @Override
    public List<CustomerDto> getAllCustomers() {
        return new ArrayList<>();
    }

    @Override
    public CustomerDto getCustomerById(Long id) {
        return null;
    }

    @Override
    public void updateCustomer(CustomerDto customerDto) {
    }

    @Override
    public void deleteCustomer(Long id) {
    }
}