package com.example.bankcards.controller;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.dto.PageResponseDto;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.dto.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.mapping.CardMapper;
import com.example.bankcards.security.UserPrincipal;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;
    private final CardMapper cardMapper;
    private final TransferService transferService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponse> createCard(@Valid @RequestBody CreateCardRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(cardMapper.toResponse(cardService.createCard(request)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponseDto<CardResponse>> getAllCards(
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) String holderName,
            Pageable pageable
    ) {
        Page<Card> page = cardService.getAllCards(status, holderName, pageable);
        return ResponseEntity.ok(new PageResponseDto<>(
                page.getContent().stream().map(cardMapper::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        ));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponse> activateCard(@PathVariable UUID id) {
        return ResponseEntity.ok(cardMapper.toResponse(cardService.activateCard(id)));
    }

    @PatchMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponse> blockCard(@PathVariable UUID id) {
        return ResponseEntity.ok(cardMapper.toResponse(cardService.blockCard(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCard(@PathVariable UUID id) {
        cardService.deleteCard(id);
    }

    @GetMapping("/myCards")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PageResponseDto<CardResponse>> getMyCards(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) CardStatus status,
            Pageable pageable
    ) {
        Page<Card> page = cardService.getCardsByOwner(principal.getId(), status, pageable);
        return ResponseEntity.ok(new PageResponseDto<>(
                page.getContent().stream().map(cardMapper::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CardResponse> getCard(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(cardMapper.toResponse(cardService.getCard(id, principal.getId())));
    }

    @PatchMapping("/{id}/request-block")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CardResponse> requestBlock(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(cardMapper.toResponse(cardService.requestBlock(id, principal.getId())));
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Transfer savedTransfer = transferService.transfer(request, principal.getId());
        return ResponseEntity.ok(
                TransferResponse.builder()
                        .fromCardMasked(savedTransfer.getFromCard().getMaskedNumber())
                        .toCardMasked(savedTransfer.getToCard().getMaskedNumber())
                        .id(savedTransfer.getId())
                        .amount(savedTransfer.getAmount())
                        .createdAt(savedTransfer.getCreatedAt())
                        .build()
        );
    }
}
