package com.pujiyam.chatter.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserListing {
    private String name;
    private String address;
    private String state;
    private String country;
    private BigDecimal rate;
    private String status;
}
