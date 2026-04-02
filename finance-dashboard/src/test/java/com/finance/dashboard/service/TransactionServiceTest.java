package com.finance.dashboard.service;

import com.finance.dashboard.dto.request.TransactionRequest;
import com.finance.dashboard.dto.response.TransactionResponse;
import com.finance.dashboard.entity.Transaction;
import com.finance.dashboard.entity.User;
import com.finance.dashboard.enums.Role;
import com.finance.dashboard.enums.TransactionType;
import com.finance.dashboard.enums.UserStatus;
import com.finance.dashboard.exception.ResourceNotFoundException;
import com.finance.dashboard.repository.TransactionRepository;
import com.finance.dashboard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService tests")
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User adminUser;
    private Transaction sampleTransaction;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L)
                .name("Alice Admin")
                .email("admin@finance.com")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        sampleTransaction = Transaction.builder()
                .id(1L)
                .amount(new BigDecimal("250.00"))
                .type(TransactionType.EXPENSE)
                .category("Food")
                .date(LocalDate.of(2025, 3, 15))
                .notes("Groceries")
                .user(adminUser)
                .build();
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create: saves transaction and returns response")
    void create_success() {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("250.00"));
        request.setType(TransactionType.EXPENSE);
        request.setCategory("Food");
        request.setDate(LocalDate.of(2025, 3, 15));
        request.setNotes("Groceries");

        when(userRepository.findByEmail("admin@finance.com")).thenReturn(Optional.of(adminUser));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);

        TransactionResponse response = transactionService.create(request, "admin@finance.com");

        assertThat(response.getAmount()).isEqualByComparingTo("250.00");
        assertThat(response.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(response.getCategory()).isEqualTo("Food");
        assertThat(response.getUserName()).isEqualTo("Alice Admin");

        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("create: throws ResourceNotFoundException when user not found")
    void create_userNotFound_throws() {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(BigDecimal.TEN);
        request.setType(TransactionType.INCOME);
        request.setCategory("Salary");
        request.setDate(LocalDate.now());

        when(userRepository.findByEmail("ghost@finance.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.create(request, "ghost@finance.com"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(transactionRepository, never()).save(any());
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById: returns transaction response for valid id")
    void getById_success() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(sampleTransaction));

        TransactionResponse response = transactionService.getById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCategory()).isEqualTo("Food");
    }

    @Test
    @DisplayName("getById: throws ResourceNotFoundException for unknown id")
    void getById_notFound_throws() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update: modifies fields and returns updated response")
    void update_success() {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("300.00"));
        request.setType(TransactionType.EXPENSE);
        request.setCategory("Dining");
        request.setDate(LocalDate.of(2025, 4, 1));
        request.setNotes("Restaurant dinner");

        Transaction updated = Transaction.builder()
                .id(1L)
                .amount(new BigDecimal("300.00"))
                .type(TransactionType.EXPENSE)
                .category("Dining")
                .date(LocalDate.of(2025, 4, 1))
                .notes("Restaurant dinner")
                .user(adminUser)
                .build();

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(sampleTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(updated);

        TransactionResponse response = transactionService.update(1L, request);

        assertThat(response.getAmount()).isEqualByComparingTo("300.00");
        assertThat(response.getCategory()).isEqualTo("Dining");
    }

    // ── Soft Delete ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("softDelete: sets deletedAt timestamp")
    void softDelete_success() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(sampleTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);

        transactionService.softDelete(1L);

        assertThat(sampleTransaction.getDeletedAt()).isNotNull();
        verify(transactionRepository).save(sampleTransaction);
    }

    @Test
    @DisplayName("softDelete: throws ResourceNotFoundException for unknown id")
    void softDelete_notFound_throws() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.softDelete(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(transactionRepository, never()).save(any());
    }
}
