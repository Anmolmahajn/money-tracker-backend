package com.moneytracker.controller;

import com.moneytracker.dto.BudgetDTO;
import com.moneytracker.service.BudgetService;
import com.moneytracker.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<BudgetDTO>> getUserBudgets(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        return ResponseEntity.ok(budgetService.getUserBudgets(userId));
    }

    @PostMapping
    public ResponseEntity<BudgetDTO> createBudget(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BudgetDTO budgetDTO) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(budgetService.createBudget(userId, budgetDTO));
    }

    @PostMapping("/check-alerts")
    public ResponseEntity<Void> checkBudgetAlerts(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        budgetService.checkBudgetAlerts(userId);
        return ResponseEntity.ok().build();
    }
}
