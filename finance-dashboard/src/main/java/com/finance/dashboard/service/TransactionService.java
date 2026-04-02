package com.finance.dashboard.service;

import com.finance.dashboard.dto.request.TransactionRequest;
import com.finance.dashboard.dto.response.TransactionResponse;
import com.finance.dashboard.entity.Transaction;
import com.finance.dashboard.entity.User;
import com.finance.dashboard.enums.TransactionType;
import com.finance.dashboard.exception.ResourceNotFoundException;
import com.finance.dashboard.repository.TransactionRepository;
import com.finance.dashboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public TransactionResponse create(TransactionRequest request, String creatorEmail) {
        User user = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        Transaction transaction = Transaction.builder()
                .amount(request.getAmount())
                .type(request.getType())
                .category(request.getCategory().trim())
                .date(request.getDate())
                .notes(request.getNotes())
                .user(user)
                .build();

        return toResponse(transactionRepository.save(transaction));
    }

    // ── Read (single) ─────────────────────────────────────────────────────────

    public TransactionResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    // ── Read (list with filters + pagination) ─────────────────────────────────

    public Page<TransactionResponse> getAll(
            TransactionType type,
            String category,
            LocalDate from,
            LocalDate to,
            Pageable pageable) {

        return transactionRepository
                .findWithFilters(type, category, from, to, pageable)
                .map(this::toResponse);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public TransactionResponse update(Long id, TransactionRequest request) {
        Transaction transaction = findOrThrow(id);

        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setCategory(request.getCategory().trim());
        transaction.setDate(request.getDate());
        transaction.setNotes(request.getNotes());

        return toResponse(transactionRepository.save(transaction));
    }

    // ── Soft Delete ───────────────────────────────────────────────────────────

    @Transactional
    public void softDelete(Long id) {
        Transaction transaction = findOrThrow(id);
        transaction.setDeletedAt(LocalDateTime.now());
        transactionRepository.save(transaction);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Transaction findOrThrow(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
    }

    private TransactionResponse toResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .amount(t.getAmount())
                .type(t.getType())
                .category(t.getCategory())
                .date(t.getDate())
                .notes(t.getNotes())
                .userId(t.getUser().getId())
                .userName(t.getUser().getName())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
