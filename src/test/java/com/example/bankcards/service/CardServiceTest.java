package com.example.bankcards.service;

import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.enums.UserRole;
import com.example.bankcards.exception.AccessDeniedException;
import com.example.bankcards.exception.CardOperationException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.mapping.CardMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardEncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardEncryptionUtil cardEncryptionUtil;

    @Mock
    private CardMapper cardMapper;

    @InjectMocks
    private CardService cardService;

    private UUID cardId;
    private UUID userId;
    private UUID otherUserId;
    private User owner;
    private Card activeCard;
    private Card expiredCard;
    private Card blockedCard;

    @BeforeEach
    void setUp() {
        cardId = UUID.randomUUID();
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();

        owner = User.builder()
                .id(userId)
                .username("Ivan Ivanov")
                .role(UserRole.USER)
                .enabled(true)
                .build();

        activeCard = Card.builder()
                .id(cardId)
                .owner(owner)
                .maskedNumber("**** **** **** 1234")
                .holderName("IVAN IVANOV")
                .expiryDate(LocalDate.of(2027, 12, 31))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(5000))
                .build();

        expiredCard = Card.builder()
                .id(cardId)
                .owner(owner)
                .status(CardStatus.EXPIRED)
                .balance(BigDecimal.ZERO)
                .build();

        blockedCard = Card.builder()
                .id(cardId)
                .owner(owner)
                .status(CardStatus.BLOCKED)
                .balance(BigDecimal.valueOf(1000))
                .build();
    }


    @Test
    void createCard_success() {
        CreateCardRequest request = new CreateCardRequest(
                userId, "1234567890123456", "IVAN IVANOV",
                LocalDate.of(2027, 12, 31), BigDecimal.valueOf(5000)
        );
        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));
        when(cardEncryptionUtil.encrypt("1234567890123456")).thenReturn("encrypted");
        when(cardEncryptionUtil.mask("1234567890123456")).thenReturn("**** **** **** 3456");
        when(cardRepository.save(any())).thenReturn(activeCard);

        Card result = cardService.createCard(request);

        assertNotNull(result);
        assertEquals(CardStatus.ACTIVE, result.getStatus());
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void createCard_ownerNotFound_throwsResourceNotFoundException() {
        CreateCardRequest request = new CreateCardRequest(
                userId, "1234567890123456", "IVAN IVANOV",
                LocalDate.of(2027, 12, 31), BigDecimal.valueOf(5000)
        );
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cardService.createCard(request));
        verify(cardRepository, never()).save(any());
    }

    @Test
    void activateCard_success() {
        blockedCard.setStatus(CardStatus.BLOCKED);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(blockedCard));
        when(cardRepository.save(any())).thenReturn(blockedCard);

        Card result = cardService.activateCard(cardId);

        assertEquals(CardStatus.ACTIVE, result.getStatus());
    }

    @Test
    void activateCard_expiredCard_throwsCardOperationException() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(expiredCard));

        assertThrows(CardOperationException.class, () -> cardService.activateCard(cardId));
        verify(cardRepository, never()).save(any());
    }

    @Test
    void activateCard_notFound_throwsResourceNotFoundException() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cardService.activateCard(cardId));
    }

    @Test
    void blockCard_success() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(activeCard));
        when(cardRepository.save(any())).thenReturn(activeCard);

        Card result = cardService.blockCard(cardId);

        assertEquals(CardStatus.BLOCKED, result.getStatus());
    }

    @Test
    void blockCard_expiredCard_throwsCardOperationException() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(expiredCard));

        assertThrows(CardOperationException.class, () -> cardService.blockCard(cardId));
        verify(cardRepository, never()).save(any());
    }

    @Test
    void deleteCard_success() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(activeCard));

        assertDoesNotThrow(() -> cardService.deleteCard(cardId));
        verify(cardRepository, never()).delete(activeCard);
    }

    @Test
    void deleteCard_notFound_throwsResourceNotFoundException() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cardService.deleteCard(cardId));
        verify(cardRepository, never()).delete(any(Card.class));
    }

    @Test
    void getCard_ownerAccess_returnsCard() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(activeCard));

        Card result = cardService.getCard(cardId, userId);

        assertEquals(cardId, result.getId());
    }

    @Test
    void getCard_anotherUser_throwsAccessDeniedException() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(activeCard));

        assertThrows(AccessDeniedException.class, () -> cardService.getCard(cardId, otherUserId));
    }

    @Test
    void getCard_notFound_throwsResourceNotFoundException() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cardService.getCard(cardId, userId));
    }

    @Test
    void getCardsByOwner_success() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(userRepository.existsById(userId)).thenReturn(true);
        when(cardRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(activeCard), pageable, 1));

        Page<Card> result = cardService.getCardsByOwner(userId, null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(cardId, result.getContent().getFirst().getId());
    }

    @Test
    void getCardsByOwner_ownerNotFound_throwsResourceNotFoundException() {
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> cardService.getCardsByOwner(userId, null, PageRequest.of(0, 10)));
    }

    @Test
    void requestBlock_success() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(activeCard));
        when(cardRepository.save(any())).thenReturn(activeCard);

        Card result = cardService.requestBlock(cardId, userId);

        assertEquals(CardStatus.BLOCKED, result.getStatus());
    }

    @Test
    void requestBlock_anotherUser_throwsAccessDeniedException() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(activeCard));

        assertThrows(AccessDeniedException.class, () -> cardService.requestBlock(cardId, otherUserId));
    }

    @Test
    void requestBlock_expiredCard_throwsCardOperationException() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(expiredCard));

        assertThrows(CardOperationException.class, () -> cardService.requestBlock(cardId, userId));
    }

    @Test
    void requestBlock_alreadyBlocked_throwsCardOperationException() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(blockedCard));

        assertThrows(CardOperationException.class, () -> cardService.requestBlock(cardId, userId));
    }
}
