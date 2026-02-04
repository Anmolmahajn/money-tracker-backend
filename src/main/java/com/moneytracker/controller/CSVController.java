package com.moneytracker.controller;

import com.moneytracker.model.Transaction;
import com.moneytracker.model.User;
import com.moneytracker.service.CSVImportService;
import com.moneytracker.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/csv")
@RequiredArgsConstructor
public class CSVController {

    private final CSVImportService csvImportService;
    private final UserService userService;

    @PostMapping("/import")
    public ResponseEntity<List<Transaction>> importCSV(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) throws Exception {
        User user = userService.getUserByUsername(userDetails.getUsername());
        List<Transaction> transactions = csvImportService.importTransactions(file, user);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/template")
    public ResponseEntity<String> downloadTemplate() {
        String template = csvImportService.generateCSVTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=template.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(template);
    }
}