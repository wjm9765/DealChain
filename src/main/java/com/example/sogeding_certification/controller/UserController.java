package com.example.sogeding_certification.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UserController {
    @GetMapping("/verify")
    public String verifyForm() {
        return "verify";
    }
}



