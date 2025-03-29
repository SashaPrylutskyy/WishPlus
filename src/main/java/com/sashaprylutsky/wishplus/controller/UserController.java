package com.sashaprylutsky.wishplus.controller;

import com.sashaprylutsky.wishplus.model.User;
import com.sashaprylutsky.wishplus.model.UserPrincipal;
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

    @GetMapping("/principal")
    public ResponseEntity<UserPrincipal> getUserPrincipal() {
        UserPrincipal userPrincipal = UserService.getUserPrincipal();
        return ResponseEntity.ok(userPrincipal);
    }

    @PostMapping
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

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUserPrincipalDetails(
            @PathVariable Long id, @Validated @RequestBody User user) {
        return ResponseEntity.ok(userService.updateUserPrincipalDetails(id, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUserById(@PathVariable Long id,
                                                 @RequestBody Map<String, String> requestBody) {
        String submitMessage = requestBody.get("submitMessage");
        userService.deleteUserById(id, submitMessage);
        return ResponseEntity.ok("User with id: " + id + " is successfully deleted.");
    }


}
