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
    private Long id;

    @Column(nullable = false, name = "member_name")
    private String name;

    @Column(nullable = false, name = "member_email", unique = true)
    private String email;

    @Column(nullable = false, name = "member_password")
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role = Role.BUYER;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses = new ArrayList<>();

    public Member(String email, String password, String name) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = Role.BUYER;
    }

    public void addAddress(Address address) {
        addresses.add(address);
        address.setMember(this);
    }
}
