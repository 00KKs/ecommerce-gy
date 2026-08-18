package com.example.e_commerce.dto.response.Order;

import com.example.e_commerce.entity.Order;
import com.example.e_commerce.entity.OrderItem;
import lombok.Getter;

import java.util.List;

@Getter
public class OrderResponse {

    private Long id;
    private String status;
    private int totalAmount;
    private String recipientName;
    private String recipientPhone;
    private String address;
    private String deliveryRequest;
    private List<OrderItemResponse> items;

    public OrderResponse(Order order) {
        this.id = order.getId();
        this.status = order.getStatus().name();
        this.totalAmount = order.getTotalAmount();
        this.recipientName = order.getRecipientName();
        this.recipientPhone = order.getRecipientPhone();
        this.address = order.getAddress();
        this.deliveryRequest = order.getDeliveryRequest();
        this.items = order.getItems().stream().map(OrderItemResponse::new).toList();
    }

    @Getter
    public static class OrderItemResponse {
        private String productName;
        private String optionName;
        private int price;
        private int quantity;

        public OrderItemResponse(OrderItem item) {
            this.productName = item.getProductName();
            this.optionName = item.getOptionName();
            this.price = item.getPrice();
            this.quantity = item.getQuantity();
        }
    }
}
