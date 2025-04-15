package com.sashaprylutsky.wishplus.controller;

import com.sashaprylutsky.wishplus.service.DataSubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscription")
public class DateSubscriptionController {

    private final DataSubscriptionService service;

    public DateSubscriptionController(DataSubscriptionService service) {
        this.service = service;
    }

    @PostMapping("/{followee_id}/{imp_date_id}")
    public ResponseEntity<String> subscribeById(@PathVariable Long followee_id,
                                                @PathVariable Long imp_date_id) {
        service.subscribeById(followee_id, imp_date_id);
        return ResponseEntity.ok("You've successfully subscribed to this record.");
    }

    @PostMapping("/{followee_id}/all")
    public ResponseEntity<String> subscribeToAll(@PathVariable Long followee_id) {
        service.subscribeToAll(followee_id);
        return ResponseEntity.ok("You've successfully subscribed to this user.");
    }

    @DeleteMapping("/{followee_id}/{imp_date_id}")
    public ResponseEntity<String> unsubscribeById(@PathVariable Long followee_id,
                                                  @PathVariable Long imp_date_id) {
        service.unsubscribeById(followee_id, imp_date_id);
        return ResponseEntity.ok("You've successfully unsubscribed from this record.");
    }

    @DeleteMapping("/{followee_id}/all")
    public ResponseEntity<String> unsubscribeFromALl(@PathVariable Long followee_id) {
        service.unsubscribeFromAll(followee_id);
        return ResponseEntity.ok("You've successfully unsubscribed from this user.");
    }
}
