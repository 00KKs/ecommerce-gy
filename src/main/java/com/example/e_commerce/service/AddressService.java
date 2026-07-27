package com.example.e_commerce.service;

import com.example.e_commerce.dto.request.AddressCreateRequest;
import com.example.e_commerce.dto.request.AddressUpdateRequest;
import com.example.e_commerce.dto.response.AddressResponse;
import com.example.e_commerce.entity.Address;
import com.example.e_commerce.entity.Member;
import com.example.e_commerce.repository.AddressRepository;
import com.example.e_commerce.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private static final int MAX_ADDRESS_CNT = 10;

    private final AddressRepository addressRepository;
    private final MemberRepository memberRepository;

    // 배송지 추가하기
    @Transactional
    public void addAddress(Long memberId, AddressCreateRequest request) {
        Member member = getMember(memberId);

        if (member.getAddresses().size() >= MAX_ADDRESS_CNT) {
            throw new IllegalStateException("배송지는 최대 " + MAX_ADDRESS_CNT + "개까지 등록 가능합니다.");
        }

        boolean isDefault = member.getAddresses().isEmpty();

        Address address = new Address(
                request.getRecipientName(),
                request.getRecipientPhone(),
                request.getAddress(),
                request.getDeliveryRequest(),
                isDefault
        );
        member.addAddress(address);
    }

    // 배송지 전체 조회
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(Long memberId) {
        Member member = getMember(memberId);
        return member.getAddresses().stream()
                .map(AddressResponse::new)
                .toList();
    }

    // 배송지 업데이트(수정)
    @Transactional
    public void updateAddress(Long memberId, AddressUpdateRequest request) {
        Address address = getOwnedAddress(request.getAddressId(), memberId);
        address.setRecipientName(request.getRecipientName());
        address.setRecipientPhone(request.getRecipientPhone());
        address.setAddress(request.getAddress());
        address.setDeliveryRequest(request.getDeliveryRequest());
    }

    // 배송지 삭제
    @Transactional
    public void deleteAddress(Long memberId, Long addressId) {
        Member member = getMember(memberId);
        Address address = getOwnedAddress(addressId, memberId);

        if (member.getAddresses().size() == 1) {
            throw new IllegalStateException("배송지가 1개일 때는 삭제할 수 없습니다");
        }
        if (address.isDefault()) {
            throw new IllegalStateException("기본 배송지를 삭제하려면 다른 배송지를 기본 배송지로 지정하세요.");
        }
        member.getAddresses().remove(address);
    }

    // 기본 배송지 지정
    public void setDefault(Long memberId, Long addressId) {
        Member member = getMember(memberId);
        Address address = getOwnedAddress(addressId, memberId);

        member.getAddresses().forEach(a -> a.setDefault(false));
        address.setDefault(true);
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 없습니다."));
    }

    // 그 회원의 배송지 주소가 맞는지 검증
    private Address getOwnedAddress(Long addressId, Long memberId) {
        return addressRepository.findByIdAndMemberId(addressId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("배송지를 찾을 수 없습니다."));
    }
}
