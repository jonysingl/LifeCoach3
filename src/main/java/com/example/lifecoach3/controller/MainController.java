package com.example.lifecoach3.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @GetMapping("/")
    public String index() {
        return "chatbot";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}