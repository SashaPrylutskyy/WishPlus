package com.sashaprylutsky.wishplus.service;

import com.sashaprylutsky.wishplus.model.ImportantDate;
import com.sashaprylutsky.wishplus.model.User;
import com.sashaprylutsky.wishplus.repository.ImportantDateRepository;
import jakarta.persistence.NoResultException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ImportantDateService {

    private final UserService userService;
    private final ImportantDateRepository repo;

    public ImportantDateService(ImportantDateRepository repo, UserService userService) {
        this.repo = repo;
        this.userService = userService;
    }

    public List<ImportantDate> getRecordsByUserId(Long id) {
        return repo.findAllByUser_Id(id);
    }

    public ImportantDate createRecord(ImportantDate importantDate) {
        User principal = userService.getPrincipal();
        importantDate.setUser(new User(principal.getId()));

        return repo.save(importantDate);
    }

    public ImportantDate getRecordById(Long record_id) {
        return repo.findById(record_id)
                .orElseThrow(() -> new NoResultException("No record is being found with id: " + record_id));
    }

    public ImportantDate getRecordByRecordIdAndUserId(Long record_id, Long user_id) {
        return repo.selectRecordByRecordIdAndUserId(record_id, user_id)
                .orElseThrow(() -> new RuntimeException("User doesn't have a record Num." + record_id));
    }

    public ImportantDate updateRecordById(Long record_id, ImportantDate importantDate) {
        User principal = userService.getPrincipal();
        ImportantDate dateRecord = getRecordByRecordIdAndUserId(record_id, principal.getId());

        if (importantDate.getTitle() != null && !importantDate.getTitle().isBlank()) {
            dateRecord.setTitle(importantDate.getTitle());
        }
        if (importantDate.getDate() != null) {
            dateRecord.setDate(importantDate.getDate());
        }
        return repo.save(dateRecord);
    }

    @Transactional
    public void deleteRecordById(Long record_id) {
        User principal = userService.getPrincipal();
        ImportantDate dateRecord = getRecordByRecordIdAndUserId(record_id, principal.getId());
        repo.delete(dateRecord);
    }
}