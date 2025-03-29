package com.sashaprylutsky.wishplus.service;

import com.sashaprylutsky.wishplus.model.User;
import com.sashaprylutsky.wishplus.model.UserPrincipal;
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

    private WishRepository repo;

    public WishService(WishRepository repo) {
        this.repo = repo;
    }

    public List<Wish> getAllWishesByUserId(Long id) {
        return repo.findAllByUser_Id(id);
    }

    public Wish getWishById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NoResultException("No wish found with ID " + id));
    }

    public Wish createWish(Wish wish) {
        UserPrincipal userPrincipal = UserService.getUserPrincipal();
        wish.setUser(new User(userPrincipal.getId()));

        return repo.save(wish);
    }

    public void deleteWishById(Long id) {
        UserPrincipal userPrincipal = UserService.getUserPrincipal();
        Wish wish = getWishById(id);

        if (!Objects.equals(userPrincipal.getId(), wish.getUser().getId())) {
            throw new AccessDeniedException("Access to delete is denied.");
        }

        repo.delete(wish);
    }

    public Wish updateWish(Long id, Wish wish) {
        UserPrincipal userPrincipal = UserService.getUserPrincipal();
        Wish wishRecord = getWishById(id);

        boolean isUpdated = false;

        if (!Objects.equals(userPrincipal.getId(), wishRecord.getUser().getId())) {
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

        return repo.save(wishRecord);
    }
}
