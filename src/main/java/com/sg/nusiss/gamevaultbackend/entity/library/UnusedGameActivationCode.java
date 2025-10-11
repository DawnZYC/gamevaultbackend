package com.sg.nusiss.gamevaultbackend.entity.library;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "unused_game_activation_code")
public class UnusedGameActivationCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long activationId;

    @Column(nullable = false)
    private Long gameId;

    @Column(nullable = false, unique = true)
    private String activationCode;

}
