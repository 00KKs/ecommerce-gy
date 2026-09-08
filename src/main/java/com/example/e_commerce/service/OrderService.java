package com.example.e_commerce.service;

import com.example.e_commerce.client.pg.PgPaymentClient;
import com.example.e_commerce.client.pg.PgPaymentException;
import com.example.e_commerce.client.pg.dto.response.PgPaymentResponse;
import com.example.e_commerce.dto.request.Order.OrderCreateRequest;
import com.example.e_commerce.dto.response.Address.DefaultAddressInfo;
import com.example.e_commerce.dto.response.Order.OrderDetailResponse;
import com.example.e_commerce.dto.response.OrderCreateResponse;
import com.example.e_commerce.dto.response.Sku.SkuOrderInfo;
import com.example.e_commerce.entity.*;
import com.example.e_commerce.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final AddressService addressService;
    private final SkuService skuService;
    private final StockService stockService;
    private final PaymentService paymentService;
    private final PgPaymentClient pgPaymentClient;

    private final OrderRepository orderRepository;
    private final TransactionTemplate transactionTemplate;

    public OrderCreateResponse createOrder(Long memberId, OrderCreateRequest request) {
        OrderPreparation preparation =
                transactionTemplate.execute(status -> prepareOrder(memberId, request));

        PgPaymentResponse created = null;
        PgPaymentResponse confirmed;
        try {
            created = pgPaymentClient.create(preparation.orderId(), preparation.amount());
            confirmed = pgPaymentClient.confirm(created.paymentKey(), preparation.orderId(), preparation.amount());
        } catch (PgPaymentException e) {
            String paymentKey = created != null ? created.paymentKey() : null;
            transactionTemplate.executeWithoutResult(status -> failOrder(preparation, paymentKey));
            throw e;
        }

        return transactionTemplate.execute(status -> completeOrder(preparation.orderId(), confirmed));
    }

    public OrderPreparation prepareOrder(Long memberId, OrderCreateRequest request) {
        DefaultAddressInfo defaultAddress = addressService.getDefaultAddress(memberId);

        Order order = new Order(
                memberId,
                defaultAddress.recipientName(),
                defaultAddress.recipientPhone(),
                defaultAddress.address(),
                defaultAddress.deliveryRequest()
        );
        orderRepository.save(order);

        SkuOrderInfo skuInfo = skuService.getSkuOrderInfo(request.getSkuId());
        stockService.decrease(skuInfo.skuId(), request.getQuantity());

        OrderItem orderItem = new OrderItem(
                order,
                skuInfo.skuId(),
                skuInfo.productName(),
                skuInfo.optionName(),
                skuInfo.price(),
                request.getQuantity()
        );
        order.addItem(orderItem);

        return new OrderPreparation(order.getId(), order.getTotalAmount(), skuInfo.skuId(), request.getQuantity());
    }

    private OrderCreateResponse completeOrder(Long orderId, PgPaymentResponse confirmed) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        paymentService.confirmSuccess(order, order.getTotalAmount(), confirmed.paymentKey());

        order.confirm();

        return new OrderCreateResponse(order, confirmed.paymentKey());
    }

    private void failOrder(OrderPreparation preparation, String paymentKey) {
        Order order = orderRepository.findById(preparation.orderId())
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        stockService.restore(preparation.skuId(), preparation.quantity());

        paymentService.confirmFailure(order, preparation.amount(), paymentKey);
    }

    private record OrderPreparation(Long orderId, int amount, Long skuId, int quantity) {}

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrder(Long memberId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        if (!order.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 주문만 조회할 수 있습니다.");
        }

        String paymentKey = paymentService.getPaymentKey(orderId);

        return new OrderDetailResponse(order, paymentKey);
    }
}
