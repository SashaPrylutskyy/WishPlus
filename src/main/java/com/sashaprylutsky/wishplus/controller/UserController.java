package com.sashaprylutsky.wishplus.controller;

import com.sashaprylutsky.wishplus.model.User;
import com.sashaprylutsky.wishplus.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody @Validated User user) {
        String jwt = userService.login(user);
        return ResponseEntity.ok(jwt);
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody @Validated User user) {
        User createdUser = userService.registerUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @GetMapping
    public ResponseEntity<List<User>> getUsers() {
        List<User> users = userService.getUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User userDTO = userService.getUserById(id);
        return ResponseEntity.status(HttpStatus.OK).body(userDTO);
    }

    @PutMapping
    public ResponseEntity<User> updateUser(@Validated @RequestBody User user) {
        return ResponseEntity.ok(userService.updateUser(user));
    }

    @DeleteMapping
    public ResponseEntity<String> deleteCurrentUser(@RequestBody Map<String, String> requestBody) {
        String submitMessage = requestBody.get("submitMessage");
        userService.deleteUser(submitMessage);
        return ResponseEntity.ok("User is successfully deleted.");
    }

    @GetMapping("/me")
    public ResponseEntity<User> getPrincipal() {
        User principal = userService.getPrincipal();
        User currentUser = userService.getUserById(principal.getId());
        return ResponseEntity.ok(currentUser);
    }

    @GetMapping("/search/{user_prefix}")
    public ResponseEntity<List<User>> userSearch(@PathVariable String user_prefix) {
        List<User> users = userService.getUsersByPrefix(user_prefix);
        return ResponseEntity.ok(users);
    }

}
