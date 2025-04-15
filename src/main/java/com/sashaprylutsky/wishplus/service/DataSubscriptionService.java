package com.sashaprylutsky.wishplus.service;

import com.sashaprylutsky.wishplus.model.DateSubscription;
import com.sashaprylutsky.wishplus.model.ImportantDate;
import com.sashaprylutsky.wishplus.model.User;
import com.sashaprylutsky.wishplus.repository.DateSubscriptionRepository;
import jakarta.persistence.NoResultException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DataSubscriptionService {

    private final DateSubscriptionRepository repo;
    private final ImportantDateService importantDateService;
    private final UserService userService;

    public DataSubscriptionService(ImportantDateService importantDateService,
                                   DateSubscriptionRepository repo,
                                   UserService userService) {
        this.importantDateService = importantDateService;
        this.repo = repo;
        this.userService = userService;
    }

    @Transactional
    public void subscribeById(Long followee_id, Long impDate_id) {
        User principal = userService.getPrincipal();
        User followee = userService.getUserById(followee_id);

        ImportantDate importantDate = importantDateService.getRecordByRecordIdAndUserId(impDate_id, followee_id);
        repo.findByFolloweeAndFollowerAndImportantDate(followee, principal, importantDate)
                .ifPresentOrElse(
                        alreadyPresent -> {
                            throw new DuplicateKeyException("You're already subscribed to this record.");
                        },
                        () -> repo.save(new DateSubscription(followee, principal, importantDate))
                );
    }

    @Transactional
    public void subscribeToAll(Long followee_id) {
        User principal = userService.getPrincipal();
        User followee = userService.getUserById(followee_id);

        List<ImportantDate> followeeImportantDates = importantDateService.getRecordsByUserId(followee_id);
        List<DateSubscription> existingSubscriptions = repo.findAllByFolloweeAndFollower(followee, principal);

        List<Long> existingDateIds = existingSubscriptions.stream()
                .map(sub -> sub.getImportantDate().getId())
                .toList();

        List<DateSubscription> subscriptionsToSave = followeeImportantDates.stream()
                .filter(date -> !existingDateIds.contains(date.getId()))
                .map(date -> new DateSubscription(followee, principal, date))
                .toList();

        List<Long> currentFolloweeDateIds = followeeImportantDates.stream()
                .map(ImportantDate::getId)
                .toList();

        List<DateSubscription> subscriptionsToRemove = existingSubscriptions.stream()
                .filter(sub -> !currentFolloweeDateIds.contains(sub.getImportantDate().getId()))
                .toList();

        if (!subscriptionsToRemove.isEmpty()) {
            repo.deleteAll(subscriptionsToRemove);
        }

        if (!subscriptionsToSave.isEmpty()) {
            repo.saveAll(subscriptionsToSave);
        }
    }

    @Transactional
    public void unsubscribeById(Long followee_id, Long impDate_id) {
        User principal = userService.getPrincipal();
        User followee = userService.getUserById(followee_id);

        ImportantDate importantDate = importantDateService.getRecordByRecordIdAndUserId(impDate_id, followee_id);
        repo.findByFolloweeAndFollowerAndImportantDate(followee, principal, importantDate)
                .ifPresentOrElse(
                        repo::delete,
                        () -> {
                            throw new NoResultException("You're already unsubscribed from this record.");
                        }
                );
    }

    @Transactional
    public void unsubscribeFromAll(Long followee_id) {
        User principal = userService.getPrincipal();
        User followee = userService.getUserById(followee_id);

        List<DateSubscription> subscriptions = repo.findAllByFolloweeAndFollower(followee, principal);

        if (subscriptions.isEmpty()) {
            throw new NoResultException("You're not subscribed to this user.");
        }
        repo.deleteAll(subscriptions);
    }
}
