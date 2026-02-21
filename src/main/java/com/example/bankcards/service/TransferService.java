package com.example.bankcards.service;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.exception.AccessDeniedException;
import com.example.bankcards.exception.CardOperationException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final CardRepository cardRepository;
    private final TransferRepository transferRepository;

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Transfer transfer(TransferRequest request, UUID currentUserId) {
        if (request.fromCardId().equals(request.toCardId())) {
            throw new CardOperationException("Нельзя выполнить перевод на ту же карту");
        }

        Card fromCard = cardRepository.findById(request.fromCardId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Карта отправителя с id: " + request.fromCardId() + " не найдена"));

        Card toCard = cardRepository.findById(request.toCardId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Карта получателя с id: " + request.toCardId() + " не найдена"));

        if (!fromCard.getOwner().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Карта отправителя вам не принадлежит");
        }
        if (!toCard.getOwner().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Карта получателя вам не принадлежит");
        }

        validateCardStatus(fromCard, "Карта отправителя");
        validateCardStatus(toCard, "Карта получателя");

        if (fromCard.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException("Недостаточно средств");
        }

        fromCard.setBalance(fromCard.getBalance().subtract(request.amount()));
        toCard.setBalance(toCard.getBalance().add(request.amount()));

        cardRepository.save(fromCard);
        cardRepository.save(toCard);

        Transfer transfer = Transfer.builder()
                .fromCard(fromCard)
                .toCard(toCard)
                .amount(request.amount())
                .build();

        return transferRepository.save(transfer);
    }

    private void validateCardStatus(Card card, String label) {
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new CardOperationException(
                    label + " неактивна (статус: " + card.getStatus() + ")");
        }
    }

}
