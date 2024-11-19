package com.study.jpa.entity.item;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@DiscriminatorValue("M")
public class Movie extends Item {
    private String artist;
    private String etc;
}
