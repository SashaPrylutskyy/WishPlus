package com.sashaprylutsky.wishplus.repository;

import com.sashaprylutsky.wishplus.model.ImportantDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportantDateRepository extends JpaRepository<ImportantDate, Long> {

    List<ImportantDate> findAllByUser_Id(Long userId);
}
