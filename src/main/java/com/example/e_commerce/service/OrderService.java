package com.example.e_commerce.service;

import com.example.e_commerce.client.pg.PgPaymentClient;
import com.example.e_commerce.client.pg.dto.response.PgPaymentResponse;
import com.example.e_commerce.dto.request.Order.OrderCreateRequest;
import com.example.e_commerce.dto.response.Order.OrderDetailResponse;
import com.example.e_commerce.dto.response.OrderCreateResponse;
import com.example.e_commerce.entity.*;
import com.example.e_commerce.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final MemberService memberService;
    private final SkuService skuService;
    private final StockService stockService;
    private final PgPaymentClient pgPaymentClient;

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final TransactionTemplate transactionTemplate;

    public OrderCreateResponse createOrder(Long memberId, OrderCreateRequest request) {
        OrderPreparation preparation =
                transactionTemplate.execute(status -> prepareOrder(memberId, request));

        PgPaymentResponse created =
                pgPaymentClient.create(preparation.orderId(), preparation.amount());
        PgPaymentResponse confirmed =
                pgPaymentClient.confirm(created.paymentKey(), preparation.orderId(), preparation.amount());

        return transactionTemplate.execute(status -> completeOrder(preparation.orderId(), confirmed));
    }

    public OrderPreparation prepareOrder(Long memberId, OrderCreateRequest request) {
        Member member = memberService.getMember(memberId);


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

        Sku sku = skuService.getSku(request.getSkuId());
        stockService.decrease(sku.getId(), request.getQuantity());

        OrderItem orderItem = new OrderItem(
                order,
                sku.getId(),
                sku.getProduct().getName(),
                sku.getOptionName(),
                sku.getPrice(),
                request.getQuantity()
        );
        order.addItem(orderItem);

        return new OrderPreparation(order.getId(), order.getTotalAmount());
    }

    private OrderCreateResponse completeOrder(Long orderId, PgPaymentResponse confirmed) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        Payment payment = new Payment(order, order.getTotalAmount(), PaymentStatus.DONE, confirmed.paymentKey());
        paymentRepository.save(payment);

        order.confirm();

        return new OrderCreateResponse(order, confirmed.paymentKey());
    }

    private record OrderPreparation(Long orderId, int amount) {}

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
