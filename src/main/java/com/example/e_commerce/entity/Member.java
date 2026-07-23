package com.example.e_commerce.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
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

    @Column(nullable = false, name = "member_address")
    private String memberAddress;



}
