package com.example.e_commerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Table(name = "address")
@Entity
@NoArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private String recipientName;
    private String recipientPhone;
    private String address;
    private String deliveryRequest;

    private boolean isDefault;

    public Address(String recipientName, String recipientPhone,
                           String address, String deliveryRequest, boolean isDefault) {
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.address = address;
        this.deliveryRequest = deliveryRequest;
        this.isDefault = isDefault;
    }
}
