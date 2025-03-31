package com.sashaprylutsky.wishplus.service;

import com.sashaprylutsky.wishplus.model.ImportantDate;
import com.sashaprylutsky.wishplus.model.User;
import com.sashaprylutsky.wishplus.model.UserPrincipal;
import com.sashaprylutsky.wishplus.repository.ImportantDateRepository;
import jakarta.persistence.NoResultException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        UserPrincipal user = UserService.getUserPrincipal();
        importantDate.setUser(new User(user.getId()));

        return repo.save(importantDate);
    }

    public ImportantDate getRecordById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NoResultException("No record is being found with id: " + id));
    }

    public ImportantDate updateRecordById(Long id, ImportantDate importantDate) {
        ImportantDate record = getRecordById(id);
        UserPrincipal userPrincipal = UserService.getUserPrincipal();
        User userRecord = userService.getUserById(record.getUser().getId());

        if (!Objects.equals(userPrincipal.getId(), userRecord.getId())) {
            throw new AccessDeniedException("Change access is prohibited.");
        }
        if (importantDate.getTitle() != null && !importantDate.getTitle().isBlank()) {
            record.setTitle(importantDate.getTitle());
        }
        if (importantDate.getDate() != null) {
            record.setDate(importantDate.getDate());
        }

        return repo.save(record);
    }

    public void deleteRecordById(Long id) {
        ImportantDate record = getRecordById(id);
        UserPrincipal userPrincipal = UserService.getUserPrincipal();

        if (!Objects.equals(userPrincipal.getId(), record.getUser().getId())) {
            throw new AccessDeniedException("Delete access is prohibited.");
        }

        repo.delete(record);
    }
}