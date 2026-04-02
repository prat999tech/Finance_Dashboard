package com.finance.dashboard.service;

import com.finance.dashboard.dto.response.DashboardSummaryResponse;
import com.finance.dashboard.dto.response.DashboardSummaryResponse.CategoryBreakdown;
import com.finance.dashboard.dto.response.MonthlyTrendResponse;
import com.finance.dashboard.dto.response.MonthlyTrendResponse.MonthlyEntry;
import com.finance.dashboard.dto.response.TransactionResponse;
import com.finance.dashboard.enums.TransactionType;
import com.finance.dashboard.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final TransactionRepository transactionRepository;

    // ── Overall summary ───────────────────────────────────────────────────────

    public DashboardSummaryResponse getSummary() {
        BigDecimal totalIncome   = transactionRepository.sumByType(TransactionType.INCOME);
        BigDecimal totalExpenses = transactionRepository.sumByType(TransactionType.EXPENSE);
        BigDecimal netBalance    = totalIncome.subtract(totalExpenses);
        long totalTransactions   = transactionRepository.countByDeletedAtIsNull();

        List<CategoryBreakdown> incomeByCategory   = toCategoryBreakdowns(
                transactionRepository.sumByCategory(TransactionType.INCOME));
        List<CategoryBreakdown> expenseByCategory  = toCategoryBreakdowns(
                transactionRepository.sumByCategory(TransactionType.EXPENSE));

        return DashboardSummaryResponse.builder()
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netBalance(netBalance)
                .totalTransactions(totalTransactions)
                .incomeByCategory(incomeByCategory)
                .expenseByCategory(expenseByCategory)
                .build();
    }

    // ── Monthly trends (last N months) ────────────────────────────────────────

    public MonthlyTrendResponse getMonthlyTrends(int months) {
        LocalDate from = LocalDate.now().minusMonths(months).withDayOfMonth(1);

        List<Object[]> rawRows = transactionRepository.monthlyTrend(from);

        // key: "YYYY-MM" -> separate income/expense accumulators
        Map<String, BigDecimal> incomeMap   = new LinkedHashMap<>();
        Map<String, BigDecimal> expenseMap  = new LinkedHashMap<>();
        Map<String, int[]>      yearMonthMap = new LinkedHashMap<>();

        for (Object[] row : rawRows) {
            int year  = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            TransactionType type   = TransactionType.valueOf(row[2].toString());
            BigDecimal amount = (BigDecimal) row[3];
            String key = year + "-" + String.format("%02d", month);

            yearMonthMap.putIfAbsent(key, new int[]{year, month});
            if (type == TransactionType.INCOME) {
                incomeMap.merge(key, amount, BigDecimal::add);
            } else {
                expenseMap.merge(key, amount, BigDecimal::add);
            }
        }

        // Build a complete list for every month in the range (filling zeros where no data)
        List<MonthlyEntry> entries = new ArrayList<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(LocalDate.now())) {
            String key  = cursor.getYear() + "-" + String.format("%02d", cursor.getMonthValue());
            BigDecimal inc = incomeMap.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal exp = expenseMap.getOrDefault(key, BigDecimal.ZERO);

            entries.add(MonthlyEntry.builder()
                    .year(cursor.getYear())
                    .month(cursor.getMonthValue())
                    .monthName(Month.of(cursor.getMonthValue()).name())
                    .income(inc)
                    .expenses(exp)
                    .net(inc.subtract(exp))
                    .build());

            cursor = cursor.plusMonths(1);
        }

        return MonthlyTrendResponse.builder().trends(entries).build();
    }

    // ── Recent transactions ───────────────────────────────────────────────────

    public List<TransactionResponse> getRecentTransactions() {
        return transactionRepository.findTop10ByOrderByDateDesc()
                .stream()
                .map(t -> TransactionResponse.builder()
                        .id(t.getId())
                        .amount(t.getAmount())
                        .type(t.getType())
                        .category(t.getCategory())
                        .date(t.getDate())
                        .notes(t.getNotes())
                        .userId(t.getUser().getId())
                        .userName(t.getUser().getName())
                        .createdAt(t.getCreatedAt())
                        .build())
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<CategoryBreakdown> toCategoryBreakdowns(List<Object[]> rows) {
        return rows.stream()
                .map(row -> CategoryBreakdown.builder()
                        .category((String) row[0])
                        .total((BigDecimal) row[1])
                        .build())
                .toList();
    }
}
