package com.sashaprylutsky.wishplus.service;

import com.sashaprylutsky.wishplus.model.User;
import com.sashaprylutsky.wishplus.model.Wish;
import com.sashaprylutsky.wishplus.repository.WishRepository;
import jakarta.persistence.NoResultException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class WishService {

    private final WishRepository wishRepo;
    private final UserService userService;

    public WishService(WishRepository wishRepo, UserService userService) {
        this.wishRepo = wishRepo;
        this.userService = userService;
    }

    public Wish createWish(Wish wish) {
        User principal = userService.getPrincipal();
        wish.setUser(new User(principal.getId()));

        return wishRepo.save(wish);
    }

    public List<Wish> getAllWishesByUserId(Long user_id) {
        return wishRepo.findAllByUser_Id(user_id);
    }

    public Wish getWishById(Long id) {
        return wishRepo.findById(id)
                .orElseThrow(() -> new NoResultException("No wish found with ID " + id));
    }

    public Wish updateWish(Long wish_id, Wish wish) {
        User principal = userService.getPrincipal();
        Wish wishRecord = getWishById(wish_id);

        boolean isUpdated = false;

        if (!Objects.equals(principal.getId(), wishRecord.getUser().getId())) {
            throw new AccessDeniedException("Change access is prohibited.");
        }
        if (wish.getTitle() != null && !wish.getTitle().isBlank()) {
            isUpdated = true;
            wishRecord.setTitle(wish.getTitle());
        }
        if (wish.getDescription() != null && !wish.getDescription().isBlank()) {
            isUpdated = true;
            wishRecord.setDescription(wish.getDescription());
        }
        if (wish.getUrl() != null && !wish.getUrl().isBlank()) {
            isUpdated = true;
            wishRecord.setUrl(wish.getUrl());
        }
        if (!Objects.equals(wish.isArchived(), wishRecord.isArchived())) {
            isUpdated = true;
            wishRecord.setArchived(wish.isArchived());
        }
        if (isUpdated) {
            wishRecord.setUpdatedAt(Instant.now());
        }

        return wishRepo.save(wishRecord);
    }

    public void deleteWishById(Long wish_id) {
        User principal = userService.getPrincipal();
        Wish wishRecord = getWishById(wish_id);

        if (!Objects.equals(principal.getId(), wishRecord.getUser().getId())) {
            throw new AccessDeniedException("Access to delete is denied.");
        }

        wishRepo.delete(wishRecord);
    }
}
