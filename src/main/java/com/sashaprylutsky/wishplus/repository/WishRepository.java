package com.sashaprylutsky.wishplus.repository;

import com.sashaprylutsky.wishplus.model.Wish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WishRepository extends JpaRepository<Wish, Long> {

    List<Wish> findAllByUser_Id(Long userId);
}
