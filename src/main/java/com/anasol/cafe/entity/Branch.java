package com.anasol.cafe.entity;



import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "branches",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "branchCode")
        }
)
@Getter
@Setter
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String branchCode;

    @Column(nullable = false, length = 100)
    private String branchName;

    private String address;

    @Column(nullable = false)
    private boolean active = true;
}

