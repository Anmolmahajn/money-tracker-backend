package com.moneytracker.controller;

import com.moneytracker.dto.MonthlyAnalyticsDTO;
import com.moneytracker.model.User;
import com.moneytracker.service.MonthlyAnalyticsService;
import com.moneytracker.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final MonthlyAnalyticsService analyticsService;
    private final UserService userService;

    @GetMapping("/monthly")
    public ResponseEntity<List<MonthlyAnalyticsDTO>> getMonthlyAnalytics(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "12") int months) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        return ResponseEntity.ok(analyticsService.getUserMonthlyAnalytics(userId, months));
    }

    @PostMapping("/generate/{yearMonth}")
    public ResponseEntity<MonthlyAnalyticsDTO> generateMonthlyReport(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String yearMonth) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(
                analyticsService.generateMonthlyAnalytics(user, YearMonth.parse(yearMonth))
        );
    }
}
