package com.sashaprylutsky.wishplus.controller;

import com.sashaprylutsky.wishplus.model.Wish;
import com.sashaprylutsky.wishplus.service.WishService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
public class WishController {

    private final WishService service;

    public WishController(WishService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Wish> getWishById(@PathVariable Long id) {
        Wish wish = service.getWishById(id);
        return ResponseEntity.ok(wish);
    }

    @GetMapping("/user/{user_id}")
    public ResponseEntity<List<Wish>> getAllWishesByUserId(@PathVariable Long user_id) {
        List<Wish> wishes = service.getAllWishesByUserId(user_id);
        return ResponseEntity.ok(wishes);
    }

    @PostMapping
    public ResponseEntity<Wish> createWish(@RequestBody @Validated Wish wish) {
        return ResponseEntity.ok(service.createWish(wish));
    }

    @PutMapping("/{wish_id}")
    public ResponseEntity<Wish> updateWish(@PathVariable Long wish_id,
                                           @RequestBody @Validated Wish wish){
        Wish updatedWish = service.updateWish(wish_id, wish);
        return ResponseEntity.ok(updatedWish);
    }

    @DeleteMapping("/{wish_id}")
    public ResponseEntity<String> deleteWishById(@PathVariable Long wish_id) {
        service.deleteWishById(wish_id);
        return ResponseEntity.ok("Wish Num.%d is deleted".formatted(wish_id));
    }
}
