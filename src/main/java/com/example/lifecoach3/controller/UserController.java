package com.example.lifecoach3.controller;

import com.example.lifecoach3.model.User;
import com.example.lifecoach3.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }


    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/api/login")
    @ResponseBody
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials, HttpSession session) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        Map<String, Object> response = new HashMap<>();

        if (username == null || password == null) {
            response.put("success", false);
            response.put("message", "用户名和密码不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        Optional<User> userOpt = userService.findByUsername(username);

        if (userOpt.isPresent() && userService.verifyPassword(userOpt.get(), password)) {
            User user = userOpt.get();

            // 更新最后登录时间
            userService.updateLastLogin(user);

            // 存储用户信息到会话
            session.setAttribute("userId", user.getId());
            session.setAttribute("username", user.getUsername());

            response.put("success", true);
            response.put("username", user.getUsername());
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "用户名或密码不正确");
            return ResponseEntity.status(401).body(response);
        }
    }

    @PostMapping("/api/register")
    @ResponseBody
    public ResponseEntity<?> register(@RequestBody Map<String, String> userData) {
        String username = userData.get("username");
        String email = userData.get("email");
        String password = userData.get("password");

        Map<String, Object> response = new HashMap<>();

        try {
            User user = userService.registerUser(username, email, password);

            response.put("success", true);
            response.put("username", user.getUsername());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "注册失败，请稍后重试");
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/api/check-login")
    @ResponseBody
    public ResponseEntity<?> checkLogin(HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        Long userId = (Long) session.getAttribute("userId");
        String username = (String) session.getAttribute("username");

        if (userId != null && username != null) {
            response.put("loggedIn", true);
            response.put("username", username);
            return ResponseEntity.ok(response);
        } else {
            response.put("loggedIn", false);
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/logout")
    @ResponseBody
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        return ResponseEntity.ok(response);
    }
}