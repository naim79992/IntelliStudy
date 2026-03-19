package com.example.naim.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class User extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String email;

    @Column
    private String password;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "provider")
    private String provider; // "LOCAL" or "GOOGLE"

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "picture_url")
    private String pictureUrl;

    @Column(name = "enabled")
    @Builder.Default
    private boolean enabled = false;

    @Column(name = "verification_token")
    private String verificationToken;
}
