package com.example.e_commerce.dto.response;

import com.example.e_commerce.entity.Address;
import lombok.Getter;

@Getter
public class AddressResponse {
    private Long addressId;
    private String recipientName;
    private String recipientPhone;
    private String address;
    private String deliveryRequest;

    public AddressResponse(Address address) {
        this.addressId = address.getId();
        this.recipientName = address.getRecipientName();
        this.recipientPhone = address.getRecipientPhone();
        this.address = address.getAddress();
        this.deliveryRequest = address.getDeliveryRequest();
    }
}
