package com.sashaprylutsky.wishplus.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "date_subscriptions")
public class DateSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User follower;

    @ManyToOne
    private User followee;

    @ManyToOne
    private ImportantDate importantDate;

    @CreationTimestamp
    @Column(name = "subscribed_at")
    private Instant subscribedAt;

    public DateSubscription() {}

    public DateSubscription(User followee, User follower, ImportantDate importantDate) {
        this.followee = followee;
        this.follower = follower;
        this.importantDate = importantDate;
        this.subscribedAt = Instant.now();
    }

    public User getFollowee() {
        return followee;
    }

    public void setFollowee(User followee) {
        this.followee = followee;
    }

    public User getFollower() {
        return follower;
    }

    public void setFollower(User follower) {
        this.follower = follower;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ImportantDate getImportantDate() {
        return importantDate;
    }

    public void setImportantDate(ImportantDate importantDate) {
        this.importantDate = importantDate;
    }

    public Instant getSubscribedAt() {
        return subscribedAt;
    }
}
