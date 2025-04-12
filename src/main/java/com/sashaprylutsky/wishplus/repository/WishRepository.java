package com.sashaprylutsky.wishplus.repository;

import com.sashaprylutsky.wishplus.model.Wish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WishRepository extends JpaRepository<Wish, Long> {

    @Query("select r from Wish r join fetch r.user where r.user.id =:user_id")
    List<Wish> findAllByUser_Id(@Param("user_id") Long user_id);
}
