package com.sashaprylutsky.wishplus.controller;

import com.sashaprylutsky.wishplus.model.ImportantDate;
import com.sashaprylutsky.wishplus.service.ImportantDateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dates")
public class ImportantDateController {

    private final ImportantDateService service;

    public ImportantDateController(ImportantDateService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ImportantDate> createImportantDate(@RequestBody ImportantDate importantDate) {
        ImportantDate record = service.createRecord(importantDate);
        return ResponseEntity.ok(record);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImportantDate> getRecordById(@PathVariable Long id) {
        ImportantDate record = service.getRecordById(id);
        return ResponseEntity.ok(record);
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<List<ImportantDate>> getRecordsByUserId(@PathVariable Long id) {
        List<ImportantDate> dates = service.getRecordsByUserId(id);
        return ResponseEntity.ok(dates);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ImportantDate> updateRecordById(@PathVariable Long id,
                                                          @RequestBody ImportantDate importantDate) {
        ImportantDate record = service.updateRecordById(id, importantDate);
        return ResponseEntity.ok(record);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRecordById(@PathVariable Long id) {
        service.deleteRecordById(id);
        return ResponseEntity.ok("Record Num.%d is successfully deleted.".formatted(id));
    }

}
