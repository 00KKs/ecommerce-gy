package com.example.e_commerce.dto.response.Address;

import com.example.e_commerce.entity.Address;

public record DefaultAddressInfo(
        String recipientName,
        String recipientPhone,
        String address,
        String deliveryRequest
) {
    public DefaultAddressInfo(Address address) {
        this(address.getRecipientName(), address.getRecipientPhone(),
                address.getAddress(), address.getDeliveryRequest());
    }
}
