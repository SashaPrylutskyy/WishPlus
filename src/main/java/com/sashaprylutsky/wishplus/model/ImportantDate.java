package com.sashaprylutsky.wishplus.model;

import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "important_dates")
public class ImportantDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    private String title;
    private Date date;

    public ImportantDate() {}

    public ImportantDate(User user, String title, Date date) {
        this.date = date;
        this.title = title;
        this.user = user;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
