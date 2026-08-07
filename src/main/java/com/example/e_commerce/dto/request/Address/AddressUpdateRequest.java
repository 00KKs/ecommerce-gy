package com.example.e_commerce.dto.request.Address;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AddressUpdateRequest {
    private Long addressId;
    private String recipientName;
    private String recipientPhone;
    private String address;
    private String deliveryRequest;
}
