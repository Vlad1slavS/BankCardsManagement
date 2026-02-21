package com.example.bankcards.service;

import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.exception.AccessDeniedException;
import com.example.bankcards.exception.CardOperationException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.mapping.CardMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.specification.CardSpecification;
import com.example.bankcards.util.CardEncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardEncryptionUtil cardEncryptionUtil;
    private final CardMapper cardMapper;

    @Transactional
    public Card createCard(CreateCardRequest request) {
        User owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Пользователь с id: " + request.ownerId() + " не найден"));

        String encryptedNumber = cardEncryptionUtil.encrypt(request.cardNumber());
        String maskedNumber = cardEncryptionUtil.mask(request.cardNumber());

        Card card = Card.builder()
                .encryptedNumber(encryptedNumber)
                .maskedNumber(maskedNumber)
                .owner(owner)
                .holderName(request.holderName())
                .expiryDate(request.expiryDate())
                .status(CardStatus.ACTIVE)
                .balance(request.initialBalance())
                .build();

        return cardRepository.save(card);
    }

    @Transactional(readOnly = true)
    public Page<Card> getAllCards(CardStatus status, String holderName, Pageable pageable) {
        Specification<Card> spec = CardSpecification.byFilter(null, status, holderName);
        return cardRepository.findAll(spec, pageable);
    }

    @Transactional
    public Card activateCard(UUID cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Карта с id: " + cardId + " не найдена"));
        if (card.getStatus() == CardStatus.EXPIRED) {
            throw new CardOperationException("Невозможно активировать карту с истёкшим сроком действия");
        }
        card.setStatus(CardStatus.ACTIVE);
        return cardRepository.save(card);
    }

    @Transactional
    public Card blockCard(UUID cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Карта с id: " + cardId + " не найдена"));
        if (card.getStatus() == CardStatus.EXPIRED) {
            throw new CardOperationException("Невозможно заблокировать карту с истёкшим сроком действия");
        }
        card.setStatus(CardStatus.BLOCKED);
        return cardRepository.save(card);
    }

    @Transactional
    public void deleteCard(UUID cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Карта с id: " + cardId + " не найдена"));
        cardRepository.delete(card);
    }

    @Transactional(readOnly = true)
    public Page<Card> getCardsByOwner(UUID ownerId, CardStatus status, Pageable pageable) {
        if (!userRepository.existsById(ownerId)) {
            throw new ResourceNotFoundException("Пользователь с id: " + ownerId + " не найден");
        }
        Specification<Card> spec = CardSpecification.byFilter(ownerId, status, null);
        return cardRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Card getCard(UUID cardId, UUID requestingUserId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Карта с id: " + cardId + " не найдена"));

        if (!card.getOwner().getId().equals(requestingUserId)) {
            throw new AccessDeniedException("Доступ к данной карте запрещён");
        }

        return card;
    }

    @Transactional
    public Card requestBlock(UUID cardId, UUID requestingUserId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Карта с id: " + cardId + " не найдена"));

        if (!card.getOwner().getId().equals(requestingUserId)) {
            throw new AccessDeniedException("Вы можете запросить блокировку только своей карты");
        }
        if (card.getStatus() == CardStatus.EXPIRED) {
            throw new CardOperationException("Невозможно заблокировать карту с истёкшим сроком действия");
        }
        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new CardOperationException("Карта уже заблокирована");
        }

        card.setStatus(CardStatus.BLOCKED);
        return cardRepository.save(card);
    }
}