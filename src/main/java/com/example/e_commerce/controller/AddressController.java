package com.example.e_commerce.controller;

import com.example.e_commerce.auth.CustomUserDetail;
import com.example.e_commerce.dto.request.Address.AddressCreateRequest;
import com.example.e_commerce.dto.request.Address.AddressUpdateRequest;
import com.example.e_commerce.dto.response.Address.AddressResponse;
import com.example.e_commerce.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/addresses")
public class AddressController {

    private final AddressService addressService;

    // 배송지 추가
    @PostMapping
    public void add(@AuthenticationPrincipal CustomUserDetail user,
                    @RequestBody AddressCreateRequest request) {
        addressService.addAddress(user.getMemberId(), request);
    }

    // 배송지 전체 조회
    @GetMapping
    public List<AddressResponse> getAll(@AuthenticationPrincipal CustomUserDetail user) {
        return addressService.getAddresses(user.getMemberId());
    }

    // 배송지 업뎃
    @PostMapping("/update")
    public void update(@AuthenticationPrincipal CustomUserDetail user,
                       @RequestBody AddressUpdateRequest request) {
        addressService.updateAddress(user.getMemberId(), request);
    }

    // 배송지 삭제
    @PostMapping("/delete")
    public void delete(@AuthenticationPrincipal CustomUserDetail user,
                       @RequestParam Long addressId) {
        addressService.deleteAddress(user.getMemberId(), addressId);
    }

    // 기본 배송지 지정
    @PostMapping("/default")
    public void setDefault(@AuthenticationPrincipal CustomUserDetail user,
                           @RequestParam Long addressId) {
        addressService.setDefault(user.getMemberId(), addressId);
    }


}
