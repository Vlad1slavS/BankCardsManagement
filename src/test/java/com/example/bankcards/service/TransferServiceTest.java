package com.example.bankcards.service;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.enums.UserRole;
import com.example.bankcards.exception.AccessDeniedException;
import com.example.bankcards.exception.CardOperationException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private CardRepository cardRepository;
    @Mock
    private TransferRepository transferRepository;

    @InjectMocks
    private TransferService transferService;

    private UUID userId;
    private UUID fromCardId;
    private UUID toCardId;
    private Card fromCard;
    private Card toCard;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        fromCardId = UUID.randomUUID();
        toCardId = UUID.randomUUID();

        User owner = User.builder()
                .id(userId)
                .username("Ivan Ivanov")
                .role(UserRole.USER)
                .enabled(true)
                .build();

        fromCard = Card.builder()
                .id(fromCardId)
                .owner(owner)
                .maskedNumber("**** **** **** 1111")
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(5000))
                .expiryDate(LocalDate.of(2027, 12, 31))
                .build();

        toCard = Card.builder()
                .id(toCardId)
                .owner(owner)
                .maskedNumber("**** **** **** 2222")
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(1000))
                .expiryDate(LocalDate.of(2027, 12, 31))
                .build();
    }


    @Test
    void transfer_success_updatesBalances() {
        TransferRequest request = new TransferRequest(fromCardId, toCardId, BigDecimal.valueOf(1000));
        Transfer savedTransfer = Transfer.builder()
                .fromCard(fromCard).toCard(toCard).amount(BigDecimal.valueOf(1000)).build();

        when(cardRepository.findById(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(toCardId)).thenReturn(Optional.of(toCard));
        when(transferRepository.save(any())).thenReturn(savedTransfer);

        Transfer result = transferService.transfer(request, userId);

        assertEquals(BigDecimal.valueOf(4000), fromCard.getBalance());
        assertEquals(BigDecimal.valueOf(2000), toCard.getBalance());
        assertNotNull(result);
        verify(cardRepository, times(2)).save(any(Card.class));
    }

    @Test
    void transfer_sameCard_throwsCardOperationException() {
        TransferRequest request = new TransferRequest(fromCardId, fromCardId, BigDecimal.valueOf(100));

        assertThrows(CardOperationException.class, () -> transferService.transfer(request, userId));
        verifyNoInteractions(cardRepository);
    }

    @Test
    void transfer_fromCardNotFound_throwsResourceNotFoundException() {
        when(cardRepository.findById(fromCardId)).thenReturn(Optional.empty());

        TransferRequest request = new TransferRequest(fromCardId, toCardId, BigDecimal.valueOf(100));

        assertThrows(ResourceNotFoundException.class, () -> transferService.transfer(request, userId));
    }

    @Test
    void transfer_toCardNotFound_throwsResourceNotFoundException() {
        when(cardRepository.findById(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(toCardId)).thenReturn(Optional.empty());

        TransferRequest request = new TransferRequest(fromCardId, toCardId, BigDecimal.valueOf(100));

        assertThrows(ResourceNotFoundException.class, () -> transferService.transfer(request, userId));
    }

    @Test
    void transfer_fromCardNotOwnedByUser_throwsAccessDeniedException() {
        User otherUser = User.builder().id(UUID.randomUUID()).username("otherUser").build();
        Card otherCard = Card.builder().id(fromCardId).owner(otherUser)
                .status(CardStatus.ACTIVE).balance(BigDecimal.valueOf(5000)).build();

        when(cardRepository.findById(fromCardId)).thenReturn(Optional.of(otherCard));
        when(cardRepository.findById(toCardId)).thenReturn(Optional.of(toCard));

        TransferRequest request = new TransferRequest(fromCardId, toCardId, BigDecimal.valueOf(100));

        assertThrows(AccessDeniedException.class, () -> transferService.transfer(request, userId));
    }

    @Test
    void transfer_toCardNotOwnedByUser_throwsAccessDeniedException() {
        User otherUser = User.builder().id(UUID.randomUUID()).username("otherUser").build();
        Card otherCard = Card.builder().id(toCardId).owner(otherUser)
                .status(CardStatus.ACTIVE).balance(BigDecimal.valueOf(1000)).build();

        when(cardRepository.findById(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(toCardId)).thenReturn(Optional.of(otherCard));

        TransferRequest request = new TransferRequest(fromCardId, toCardId, BigDecimal.valueOf(100));

        assertThrows(AccessDeniedException.class, () -> transferService.transfer(request, userId));
    }

    @Test
    void transfer_fromCardBlocked_throwsCardOperationException() {
        fromCard.setStatus(CardStatus.BLOCKED);
        when(cardRepository.findById(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(toCardId)).thenReturn(Optional.of(toCard));

        TransferRequest request = new TransferRequest(fromCardId, toCardId, BigDecimal.valueOf(100));

        assertThrows(CardOperationException.class, () -> transferService.transfer(request, userId));
    }

    @Test
    void transfer_toCardExpired_throwsCardOperationException() {
        toCard.setStatus(CardStatus.EXPIRED);
        when(cardRepository.findById(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(toCardId)).thenReturn(Optional.of(toCard));

        TransferRequest request = new TransferRequest(fromCardId, toCardId, BigDecimal.valueOf(100));

        assertThrows(CardOperationException.class, () -> transferService.transfer(request, userId));
    }

    @Test
    void transfer_insufficientFunds_throwsInsufficientFundsException() {
        when(cardRepository.findById(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(toCardId)).thenReturn(Optional.of(toCard));

        TransferRequest request = new TransferRequest(fromCardId, toCardId, BigDecimal.valueOf(99999));

        assertThrows(InsufficientFundsException.class, () -> transferService.transfer(request, userId));
        verify(cardRepository, never()).save(any());
    }

    @Test
    void transfer_exactBalance_success() {
        when(cardRepository.findById(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(toCardId)).thenReturn(Optional.of(toCard));
        when(transferRepository.save(any())).thenReturn(mock(Transfer.class));

        TransferRequest request = new TransferRequest(fromCardId, toCardId, BigDecimal.valueOf(5000));

        assertDoesNotThrow(() -> transferService.transfer(request, userId));
        assertEquals(BigDecimal.ZERO, fromCard.getBalance());
    }
}
