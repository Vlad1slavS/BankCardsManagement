package com.example.bankcards.specification;

import com.example.bankcards.entity.Card;
import com.example.bankcards.enums.CardStatus;
import jakarta.persistence.criteria.Predicate;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Владислав Степанов
 */
@UtilityClass
public final class CardSpecification {

    /**
     * Добавляет фильтр по ownerId, status, holderName в Specification
     *
     * @return - Specification с нужными фильтрами
     */
    public static Specification<Card> byFilter(UUID ownerId, CardStatus status, String holderName) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (ownerId != null) {
                predicates.add(cb.equal(root.get("owner").get("id"), ownerId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (holderName != null && !holderName.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("holderName")),
                        "%" + holderName.toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
