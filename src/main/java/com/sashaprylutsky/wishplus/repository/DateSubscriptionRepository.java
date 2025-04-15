package com.sashaprylutsky.wishplus.repository;

import com.sashaprylutsky.wishplus.model.DateSubscription;
import com.sashaprylutsky.wishplus.model.ImportantDate;
import com.sashaprylutsky.wishplus.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DateSubscriptionRepository extends JpaRepository<DateSubscription, Long> {

    List<DateSubscription> findAllByFolloweeAndFollower(User followee, User follower);

    Optional<DateSubscription> findByFolloweeAndFollowerAndImportantDate(User followee, User follower, ImportantDate importantDate);
}
