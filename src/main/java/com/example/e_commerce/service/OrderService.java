package com.example.e_commerce.service;

import com.example.e_commerce.dto.request.Order.OrderCreateRequest;
import com.example.e_commerce.dto.response.Order.OrderDetailResponse;
import com.example.e_commerce.dto.response.OrderCreateResponse;
import com.example.e_commerce.entity.*;
import com.example.e_commerce.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final SkuRepository skuRepository;
    private final StockRepository stockRepository;
    private final FakePaymentGateway paymentGateway;


    @Transactional
    public OrderCreateResponse createOrder(Long memberId, OrderCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("멤버를 찾을 수 없습니다."));

        Address defaultAddress = member.getAddresses().stream()
                .filter(Address::isDefault)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("기본 배송지가 없습니다."));

        Order order = new Order(
                member,
                defaultAddress.getRecipientName(),
                defaultAddress.getRecipientPhone(),
                defaultAddress.getAddress(),
                defaultAddress.getDeliveryRequest()
        );
        orderRepository.save(order);

        Sku sku = skuRepository.findById(request.getSkuId())
                .orElseThrow(() -> new IllegalArgumentException("SKU를 찾을 수 없습니다."));

        Stock stock = stockRepository.findBySkuIdForUpdate(sku.getId())
                .orElseThrow(() -> new IllegalArgumentException("재고 정보를 찾을 수 없습니다."));

        stock.decrease(request.getQuantity());

        OrderItem orderItem = new OrderItem(
                order,
                sku.getId(),
                sku.getProduct().getName(),
                sku.getOptionName(),
                sku.getPrice(),
                request.getQuantity()
        );
        order.addItem(orderItem);

        FakePaymentGateway.PaymentResult result =
                paymentGateway.requestPayment(order.getId(), order.getTotalAmount());

        Payment payment = new Payment(order, order.getTotalAmount(), PaymentStatus.APPROVED, result.paymentKey());
        paymentRepository.save(payment);

        order.confirm();

        return new OrderCreateResponse(order, result.paymentKey());
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrder(Long memberId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        if (!order.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 주문만 조회할 수 있습니다.");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("결제 정보를 찾을 수 없습니다."));

        return new OrderDetailResponse(order, payment.getPaymentKey());
    }
}
