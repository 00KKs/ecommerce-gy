package com.example.e_commerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, name = "memeber_id")
    private Long memberId;

    @Column(nullable = false, name = "member_name")
    private String memberName;

    @Column(nullable = false, name = "member_email")
    private String memberEmail;

    @Column(nullable = false, name = "member_password")
    private String memberPassword;

    @Enumerated(EnumType.STRING)
    private Role role = Role.BUYER;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses = new ArrayList<>();

    public Member(String memberEmail, String memberPassword, String memberName) {
        this.memberEmail = memberEmail;
        this.memberPassword = memberPassword;
        this.memberName = memberName;
        this.role = Role.BUYER;
    }

    public void addAddress(Address address) {
        addresses.add(address);
        address.setMember(this);
    }

}
