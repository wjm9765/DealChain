package com.example.sogeding_certification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private boolean success;
    private String message;
    private String redirectUrl;
    private String name;
    private String ci;
}



