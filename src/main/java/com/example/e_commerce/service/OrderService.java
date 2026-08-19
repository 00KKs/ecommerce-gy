package com.example.e_commerce.service;

import com.example.e_commerce.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final MemberRepository memberRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final SkuRepository skuRepository;
    private final StockRepository stockRepository;
    private final FakePaymentGateway paymentGateway;


    @Transactional

}
