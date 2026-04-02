package com.finance.dashboard.service;

import com.finance.dashboard.dto.response.DashboardSummaryResponse;
import com.finance.dashboard.dto.response.MonthlyTrendResponse;
import com.finance.dashboard.entity.Transaction;
import com.finance.dashboard.entity.User;
import com.finance.dashboard.enums.Role;
import com.finance.dashboard.enums.TransactionType;
import com.finance.dashboard.enums.UserStatus;
import com.finance.dashboard.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService tests")
class DashboardServiceTest {

    @Mock private TransactionRepository transactionRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L).name("Alice Admin")
                .email("admin@finance.com")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
    }

    // ── Summary ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getSummary: calculates net balance correctly")
    void getSummary_netBalance() {
        when(transactionRepository.sumByType(TransactionType.INCOME))
                .thenReturn(new BigDecimal("10000.00"));
        when(transactionRepository.sumByType(TransactionType.EXPENSE))
                .thenReturn(new BigDecimal("4500.00"));
        when(transactionRepository.countByDeletedAtIsNull()).thenReturn(30L);
        when(transactionRepository.sumByCategory(TransactionType.INCOME)).thenReturn(List.of());
        when(transactionRepository.sumByCategory(TransactionType.EXPENSE)).thenReturn(List.of());

        DashboardSummaryResponse summary = dashboardService.getSummary();

        assertThat(summary.getTotalIncome()).isEqualByComparingTo("10000.00");
        assertThat(summary.getTotalExpenses()).isEqualByComparingTo("4500.00");
        assertThat(summary.getNetBalance()).isEqualByComparingTo("5500.00");
        assertThat(summary.getTotalTransactions()).isEqualTo(30L);
    }

    @Test
    @DisplayName("getSummary: handles zero transactions gracefully")
    void getSummary_zeroData() {
        when(transactionRepository.sumByType(any())).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.countByDeletedAtIsNull()).thenReturn(0L);
        when(transactionRepository.sumByCategory(any())).thenReturn(List.of());

        DashboardSummaryResponse summary = dashboardService.getSummary();

        assertThat(summary.getNetBalance()).isEqualByComparingTo("0.00");
        assertThat(summary.getTotalTransactions()).isZero();
        assertThat(summary.getIncomeByCategory()).isEmpty();
        assertThat(summary.getExpenseByCategory()).isEmpty();
    }

    // ── Monthly Trends ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getMonthlyTrends: returns an entry for every month in range")
    void getMonthlyTrends_completesMonthRange() {
        // 3 months of data requested, no raw rows — should still return 3+ entries with zeros
        when(transactionRepository.monthlyTrend(any(LocalDate.class))).thenReturn(List.of());

        MonthlyTrendResponse response = dashboardService.getMonthlyTrends(3);

        assertThat(response.getTrends()).hasSizeGreaterThanOrEqualTo(3);
        response.getTrends().forEach(entry -> {
            assertThat(entry.getIncome()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(entry.getExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(entry.getNet()).isEqualByComparingTo(BigDecimal.ZERO);
        });
    }

    // ── Recent Transactions ───────────────────────────────────────────────────

    @Test
    @DisplayName("getRecentTransactions: maps list correctly")
    void getRecentTransactions_mapsCorrectly() {
        Transaction t = Transaction.builder()
                .id(5L)
                .amount(new BigDecimal("99.99"))
                .type(TransactionType.EXPENSE)
                .category("Shopping")
                .date(LocalDate.now())
                .user(adminUser)
                .build();

        when(transactionRepository.findTop10ByOrderByDateDesc()).thenReturn(List.of(t));

        var recent = dashboardService.getRecentTransactions();

        assertThat(recent).hasSize(1);
        assertThat(recent.get(0).getId()).isEqualTo(5L);
        assertThat(recent.get(0).getCategory()).isEqualTo("Shopping");
        assertThat(recent.get(0).getUserName()).isEqualTo("Alice Admin");
    }
}
