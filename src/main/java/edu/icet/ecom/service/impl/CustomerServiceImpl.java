package edu.icet.ecom.service.impl;

import edu.icet.ecom.exceptions.ResourceNotFoundException;
import edu.icet.ecom.model.dto.CreditSettlementDto;
import edu.icet.ecom.model.dto.CustomerDto;
import edu.icet.ecom.model.entity.CreditSettlementEntity;
import edu.icet.ecom.model.entity.CustomerEntity;
import edu.icet.ecom.repository.CreditSettlementRepository;
import edu.icet.ecom.repository.CustomerRepository;
import edu.icet.ecom.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CreditSettlementRepository settlementRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public void saveCustomer(CustomerDto customerDto) {
        CustomerEntity entity = modelMapper.map(customerDto, CustomerEntity.class);
        if (entity.getCurrentBalance() == null) {
            entity.setCurrentBalance(0.0);
        }
        customerRepository.save(entity);
    }

    @Override
    public List<CustomerDto> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(entity -> modelMapper.map(entity, CustomerDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public CustomerDto getCustomerById(Long id) {
        CustomerEntity entity = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        return modelMapper.map(entity, CustomerDto.class);
    }

    @Override
    @Transactional
    public void updateCustomer(CustomerDto customerDto) {
        CustomerEntity existingCustomer = customerRepository.findById(customerDto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerDto.getId()));

        existingCustomer.setName(customerDto.getName());
        existingCustomer.setPhone(customerDto.getPhone());
        existingCustomer.setNic(customerDto.getNic());
        existingCustomer.setAddress(customerDto.getAddress());
        existingCustomer.setCreditLimit(customerDto.getCreditLimit());

        if (customerDto.getCurrentBalance() != null) {
            existingCustomer.setCurrentBalance(customerDto.getCurrentBalance());
        }

        customerRepository.save(existingCustomer);
    }

    @Override
    @Transactional
    public void deleteCustomer(Long id) {
        CustomerEntity customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        customerRepository.delete(customer);
    }

    @Override
    @Transactional
    public void settleCredit(CreditSettlementDto settlementDto) {
        CustomerEntity customer = customerRepository.findById(settlementDto.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + settlementDto.getCustomerId()));

        double newBalance = customer.getCurrentBalance() - settlementDto.getAmountPaid();
        customer.setCurrentBalance(Math.max(0.0, newBalance));
        customerRepository.save(customer);

        CreditSettlementEntity settlementEntity = new CreditSettlementEntity();
        settlementEntity.setCustomer(customer);
        settlementEntity.setAmountPaid(settlementDto.getAmountPaid());
        settlementEntity.setPaymentMethod(settlementDto.getPaymentMethod().toUpperCase());
        settlementEntity.setReferenceNote(settlementDto.getReferenceNote());
        settlementEntity.setSettlementDate(LocalDateTime.now());

        settlementRepository.save(settlementEntity);
    }
    
    @Override
    public List<CreditSettlementDto> getSettlementHistory(Long customerId) {
        return settlementRepository.findByCustomerIdOrderBySettlementDateDesc(customerId).stream()
                .map(entity -> {
                    CreditSettlementDto dto = modelMapper.map(entity, CreditSettlementDto.class);
                    dto.setCustomerId(entity.getCustomer().getId());
                    dto.setCustomerName(entity.getCustomer().getName());
                    return dto;
                }).collect(Collectors.toList());
    }
}