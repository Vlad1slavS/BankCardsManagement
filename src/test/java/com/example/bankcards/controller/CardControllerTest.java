package com.example.bankcards.controller;

import com.example.bankcards.config.TestSecurityConfig;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.enums.UserRole;
import com.example.bankcards.mapping.CardMapper;
import com.example.bankcards.security.JwtUtil;
import com.example.bankcards.security.UserPrincipal;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.TransferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(CardController.class)
@Import(TestSecurityConfig.class)
public class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    @MockitoBean
    private CardService cardService;

    @MockitoBean
    private CardMapper cardMapper;

    @MockitoBean
    private TransferService transferService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;


    private UUID cardId;
    private UUID userId;
    private CardResponse cardResponse;
    private Card card;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {

        cardId = UUID.randomUUID();
        userId = UUID.randomUUID();

        cardResponse = CardResponse.builder()
                .id(cardId)
                .maskedNumber("**** **** **** 1234")
                .holderName("IVAN IVANOV")
                .ownerId(userId)
                .ownerUsername("Ivan Ivanov")
                .expiryDate(LocalDate.of(2027, 12, 31))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(5000))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User testUser = User.builder()
                .id(userId)
                .username("Ivan Ivanov")
                .password("password")
                .role(UserRole.USER)
                .enabled(true)
                .build();

        card = Card.builder()
                .id(cardId)
                .encryptedNumber("encrypted_value")
                .maskedNumber("**** **** **** 1234")
                .owner(testUser)
                .holderName("IVAN IVANOV")
                .expiryDate(LocalDate.of(2027, 12, 31))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(5000))
                .build();

        userPrincipal = new UserPrincipal(testUser);
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void createCard_admin_returns201() throws Exception {
        CreateCardRequest request = new CreateCardRequest(
                userId, "1234567890123456", "IVAN IVANOV",
                LocalDate.of(2027, 12, 31), BigDecimal.valueOf(5000)
        );

        when(cardService.createCard(any())).thenReturn(new Card());
        when(cardMapper.toResponse(any())).thenReturn(cardResponse);

        mockMvc.perform(post("/api/v1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(cardId.toString()))
                .andExpect(jsonPath("$.maskedNumber").value("**** **** **** 1234"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createCard_user_returns403() throws Exception {
        CreateCardRequest request = new CreateCardRequest(
                userId, "1234567890123456", "IVAN IVANOV",
                LocalDate.of(2027, 12, 31), BigDecimal.valueOf(5000)
        );

        mockMvc.perform(post("/api/v1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCard_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllCards_admin_returns200() throws Exception {
        when(cardService.getAllCards(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(card), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/cards")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllCards_filterByStatus_returns200() throws Exception {
        when(cardService.getAllCards(eq(CardStatus.ACTIVE), any(), any()))
                .thenReturn(new PageImpl<>(List.of(card), PageRequest.of(0, 20), 1));
        when(cardMapper.toResponse(any())).thenReturn(cardResponse);

        mockMvc.perform(get("/api/v1/cards?status=ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllCards_user_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/cards"))
                .andExpect(status().isForbidden());
    }


    @Test
    void getMyCards_user_returns200() throws Exception {
        when(cardService.getCardsByOwner(eq(userId), any(), any()))
                .thenReturn(new PageImpl<>(List.of(card), PageRequest.of(0, 20), 1));
        when(cardMapper.toResponse(any())).thenReturn(cardResponse);

        mockMvc.perform(get("/api/v1/cards/myCards")
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].ownerId").value(userId.toString()));
    }

    @Test
    void getMyCards_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/cards/myCards"))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void getCard_owner_returns200() throws Exception {
        when(cardService.getCard(eq(cardId), eq(userId))).thenReturn(card);
        when(cardMapper.toResponse(any())).thenReturn(cardResponse);

        mockMvc.perform(get("/api/v1/cards/{id}", cardId)
                        .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId.toString()));
    }

    @Test
    void getCard_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/cards/{id}", cardId))
                .andExpect(status().isUnauthorized());
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void activateCard_admin_returns200() throws Exception {
        when(cardService.activateCard(cardId)).thenReturn(new Card());
        when(cardMapper.toResponse(any())).thenReturn(cardResponse);

        mockMvc.perform(patch("/api/v1/cards/{id}/activate", cardId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId.toString()));
    }

    @Test
    @WithMockUser(roles = "USER")
    void activateCard_user_returns403() throws Exception {
        mockMvc.perform(patch("/api/v1/cards/{id}/activate", cardId)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void blockCard_admin_returns200() throws Exception {
        CardResponse blocked = CardResponse.builder()
                .id(cardId)
                .status(CardStatus.BLOCKED)
                .build();

        when(cardService.blockCard(cardId)).thenReturn(new Card());
        when(cardMapper.toResponse(any())).thenReturn(blocked);

        mockMvc.perform(patch("/api/v1/cards/{id}/block", cardId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void blockCard_user_returns403() throws Exception {
        mockMvc.perform(patch("/api/v1/cards/{id}/block", cardId)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCard_admin_returns204() throws Exception {
        doNothing().when(cardService).deleteCard(cardId);

        mockMvc.perform(delete("/api/v1/cards/{id}", cardId)
                )
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteCard_user_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/cards/{id}", cardId)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void requestBlock_user_returns200() throws Exception {
        when(cardService.requestBlock(eq(cardId), eq(userId))).thenReturn(card);
        when(cardMapper.toResponse(any())).thenReturn(cardResponse);

        mockMvc.perform(patch("/api/v1/cards/{id}/request-block", cardId)
                        .with(user(userPrincipal))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void requestBlock_admin_returns403() throws Exception {
        mockMvc.perform(patch("/api/v1/cards/{id}/request-block", cardId)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void transfer_user_returns200() throws Exception {
        TransferRequest request = new TransferRequest(
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(1000)
        );
        Transfer response = Transfer.builder()
                .id(UUID.randomUUID())
                .fromCard(Card.builder().maskedNumber("**** **** **** 1111").build())
                .toCard(Card.builder().maskedNumber("**** **** **** 2222").build())
                .amount(BigDecimal.valueOf(1000))
                .createdAt(LocalDateTime.now())
                .build();

        when(transferService.transfer(any(), eq(userId))).thenReturn(response);

        mockMvc.perform(post("/api/v1/cards/transfer")
                        .with(user(userPrincipal))

                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromCardMasked").value("**** **** **** 1111"))
                .andExpect(jsonPath("$.toCardMasked").value("**** **** **** 2222"))
                .andExpect(jsonPath("$.amount").value(1000));
    }

    @Test
    void transfer_zeroAmount_returns400() throws Exception {
        TransferRequest request = new TransferRequest(
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ZERO
        );

        mockMvc.perform(post("/api/v1/cards/transfer")
                        .with(user(userPrincipal))

                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transfer_unauthenticated_returns401() throws Exception {
        TransferRequest request = new TransferRequest(
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(100)
        );

        mockMvc.perform(post("/api/v1/cards/transfer")

                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}