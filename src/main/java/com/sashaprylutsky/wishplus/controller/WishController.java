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

    private WishService service;

    public WishController(WishService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Wish> getWishById(@PathVariable Long id) {
        Wish wish = service.getWishById(id);
        return ResponseEntity.ok(wish);
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<List<Wish>> getAllWishesByUserId(@PathVariable Long id) {
        List<Wish> wishes = service.getAllWishesByUserId(id);
        return ResponseEntity.ok(wishes);
    }

    @PostMapping
    public ResponseEntity<Wish> createWish(@RequestBody @Validated Wish wish) {
        return ResponseEntity.ok(service.createWish(wish));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Wish> updateWish(@PathVariable Long id,
                                           @RequestBody @Validated Wish wish){
        Wish updatedWish = service.updateWish(id, wish);
        return ResponseEntity.ok(updatedWish);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteWishById(@PathVariable Long id) {
        service.deleteWishById(id);
        return ResponseEntity.ok("Wish of id: " + id + " is deleted");
    }
}
