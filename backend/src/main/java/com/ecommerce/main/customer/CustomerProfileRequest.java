package com.ecommerce.main.customer;

import lombok.Data;

@Data
public class CustomerProfileRequest {
    private String gender;
    private Integer age;
    private String city;
    private MembershipType membershipType;
    private String satisfactionLevel;
}
