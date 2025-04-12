package com.sashaprylutsky.wishplus.repository;

import com.sashaprylutsky.wishplus.model.ImportantDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImportantDateRepository extends JpaRepository<ImportantDate, Long> {

    List<ImportantDate> findAllByUser_Id(Long userId);

    @Query("select r from ImportantDate r join fetch r.user where r.id =:record_id and r.user.id =:user_id")
    Optional<ImportantDate> selectRecordByRecordIdAndUserId(@Param("record_id") Long record_id,
                                                            @Param("user_id") Long user_id);
}
