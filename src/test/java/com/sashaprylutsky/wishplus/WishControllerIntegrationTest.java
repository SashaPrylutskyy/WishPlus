package com.sashaprylutsky.wishplus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sashaprylutsky.wishplus.model.User;
import com.sashaprylutsky.wishplus.model.Wish;
import com.sashaprylutsky.wishplus.repository.UserRepository;
import com.sashaprylutsky.wishplus.repository.WishRepository;
import jakarta.validation.ConstraintViolationException; // Імпорт для перевірки валідації
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.MethodArgumentNotValidException; // Імпорт для перевірки валідації

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue; // Для перевірки типу винятку
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WishControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WishRepository wishRepository; // Репозиторій для Wish

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private User testUserAuth;
    private User testUserOther;
    private String userAuthPassword = "passwordAuthWish";
    private String userOtherPassword = "passwordOtherWish";

    private Wish wishUserAuth1;
    private Wish wishUserAuth2;
    private Wish wishUserOther;

    // Допоміжні методи для створення даних в БД
    private User createUserInDb(String email, String username, String rawPassword, String firstName, String lastName) {
        User user = new User(email, username, passwordEncoder.encode(rawPassword), firstName, lastName);
        return userRepository.saveAndFlush(user);
    }

    // Додаємо метод для створення Wish
    private Wish createWishInDb(User user, String title, String description, String url, boolean isArchived) {
        Wish wish = new Wish(title, description, url);
        wish.setUser(user);
        wish.setArchived(isArchived);
        // createdAt та updatedAt будуть встановлені Hibernate (@CreationTimestamp, @UpdateTimestamp)
        return wishRepository.saveAndFlush(wish);
    }

    @BeforeEach
    void setUp() {
        // Очищення
        wishRepository.deleteAll();
        userRepository.deleteAll();
        wishRepository.flush();
        userRepository.flush();

        // Користувачі
        testUserAuth = createUserInDb("auth.wish@example.com", "userAuthWish", userAuthPassword, "AuthW", "UserW");
        testUserOther = createUserInDb("other.wish@example.com", "userOtherWish", userOtherPassword, "OtherW", "PersonW");

        // Бажання
        wishUserAuth1 = createWishInDb(testUserAuth, "Wish A1", "Description A1", "http://a1.com", false);
        wishUserAuth2 = createWishInDb(testUserAuth, "Wish A2", "Description A2", "http://a2.com", true); // Одне архівоване
        wishUserOther = createWishInDb(testUserOther, "Wish O1", "Description O1", "http://o1.com", false);
    }

    // --- Тести для POST /api/wishlist ---

    @Test
    @DisplayName("POST /api/wishlist - Успішне створення бажання автентифікованим користувачем")
    void createWish_shouldCreateWish_whenAuthenticated() throws Exception {
        Map<String, Object> newWishMap = new HashMap<>();
        newWishMap.put("title", "New Wish Title"); // Валідний title (2-25)
        newWishMap.put("description", "Detailed description");
        newWishMap.put("url", "http://valid.url/wish"); // Валідний URL
        // Не передаємо user, createdAt, updatedAt, isArchived - вони мають бути встановлені сервісом/БД

        mockMvc.perform(post("/api/wishlist")
                        .with(httpBasic(testUserAuth.getUsername(), userAuthPassword))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newWishMap)))
                .andDo(print())
                .andExpect(status().isOk()) // Контролер повертає OK
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("New Wish Title"))
                .andExpect(jsonPath("$.description").value("Detailed description"))
                .andExpect(jsonPath("$.url").value("http://valid.url/wish"))
                .andExpect(jsonPath("$.archived").value(false)) // За замовчуванням false
                .andExpect(jsonPath("$.createdAt").isNotEmpty()) // Перевірка наявності
//                .andExpect(jsonPath("$.updatedAt").doesNotExist()) // Спочатку updatedAt може бути null
                .andExpect(jsonPath("$.user.id").value(testUserAuth.getId())) // Перевірка власника
                .andExpect(jsonPath("$.user.password").doesNotExist()); // Перевірка безпеки

        // Перевірка в БД
        var wishesInDb = wishRepository.findAllByUser_Id(testUserAuth.getId());
        assertThat(wishesInDb).hasSize(3); // 2 з setUp + 1 нова
        Wish createdWish = wishesInDb.stream()
                .filter(w -> "New Wish Title".equals(w.getTitle()))
                .findFirst().orElseThrow();
        assertThat(createdWish.getUser().getId()).isEqualTo(testUserAuth.getId());
        assertThat(createdWish.isArchived()).isFalse();
        assertThat(createdWish.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("POST /api/wishlist - Помилка 400 Bad Request при невалідному title")
    void createWish_shouldReturnBadRequest_whenTitleIsInvalid() throws Exception {
        Map<String, Object> invalidWishMap = new HashMap<>();
        invalidWishMap.put("title", "T"); // Невалідний title (менше 2)
        invalidWishMap.put("url", "http://valid.url");

        MvcResult result = mockMvc.perform(post("/api/wishlist")
                        .with(httpBasic(testUserAuth.getUsername(), userAuthPassword))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidWishMap)))
                .andDo(print())
                .andExpect(status().isBadRequest()) // Очікуємо 400
                .andExpect(jsonPath("$.title").value("Title must be 2-25 characters long")) // Перевірка повідомлення валідації
                .andReturn();

        // Перевірка типу винятку (опціонально, але корисно)
        Exception resolvedException = result.getResolvedException();
        assertThat(resolvedException).isInstanceOf(MethodArgumentNotValidException.class);
    }

     @Test
    @DisplayName("POST /api/wishlist - Помилка 400 Bad Request при невалідному URL")
    void createWish_shouldReturnBadRequest_whenUrlIsInvalid() throws Exception {
        Map<String, Object> invalidWishMap = new HashMap<>();
        invalidWishMap.put("title", "Valid Title");
        invalidWishMap.put("url", "not-a-valid-url"); // Невалідний URL

        MvcResult result = mockMvc.perform(post("/api/wishlist")
                        .with(httpBasic(testUserAuth.getUsername(), userAuthPassword))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidWishMap)))
                .andDo(print())
                .andExpect(status().isBadRequest()) // Очікуємо 400
                // Конкретне повідомлення для URL може залежати від конфігурації, перевіримо наявність помилки поля url
                .andExpect(jsonPath("$.url").exists())
                .andReturn();

        Exception resolvedException = result.getResolvedException();
        assertThat(resolvedException).isInstanceOf(MethodArgumentNotValidException.class);
        // Якщо хочете перевірити сам текст повідомлення:
        // .andExpect(jsonPath("$.url").value("must be a valid URL")) // Залежить від реалізації валідатора
    }

    @Test
    @DisplayName("POST /api/wishlist - Помилка 403 Forbidden при створенні без автентифікації")
    void createWish_shouldReturnForbidden_whenNotAuthenticated() throws Exception {
        Map<String, Object> newWishMap = new HashMap<>();
        newWishMap.put("title", "Unauthorized Wish");
        newWishMap.put("url", "http://unauth.com");

        mockMvc.perform(post("/api/wishlist")
                        // БЕЗ .with(httpBasic(...))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newWishMap)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("User is not authenticated."));
    }

    // --- Тести для GET /api/wishlist/{id} ---

    @Test
    @DisplayName("GET /api/wishlist/{id} - Успішне отримання існуючого бажання")
    void getWishById_shouldReturnWish_whenExists() throws Exception {
        Long wishId = wishUserAuth1.getId();

        mockMvc.perform(get("/api/wishlist/{id}", wishId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(wishId))
                .andExpect(jsonPath("$.title").value(wishUserAuth1.getTitle()))
                .andExpect(jsonPath("$.description").value(wishUserAuth1.getDescription()))
                .andExpect(jsonPath("$.url").value(wishUserAuth1.getUrl()))
                .andExpect(jsonPath("$.archived").value(wishUserAuth1.isArchived()))
                .andExpect(jsonPath("$.user.id").value(testUserAuth.getId()))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/wishlist/{id} - Помилка 404 Not Found при запиті неіснуючого бажання")
    void getWishById_shouldReturnNotFound_whenNotExists() throws Exception {
        Long nonExistentId = 999L;

        mockMvc.perform(get("/api/wishlist/{id}", nonExistentId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No wish found with ID " + nonExistentId));
    }

    // --- Тести для GET /api/wishlist/user/{id} ---

    @Test
    @DisplayName("GET /api/wishlist/user/{id} - Успішне отримання списку бажань користувача")
    void getAllWishesByUserId_shouldReturnWishList_forExistingUser() throws Exception {
        Long userId = testUserAuth.getId();

        mockMvc.perform(get("/api/wishlist/user/{id}", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()", is(2))) // Має бути 2 бажання для userAuth
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(wishUserAuth1.getId().intValue(), wishUserAuth2.getId().intValue())))
                .andExpect(jsonPath("$[0].user.id").value(userId));
    }

    @Test
    @DisplayName("GET /api/wishlist/user/{id} - Повернення порожнього списку для користувача без бажань")
    void getAllWishesByUserId_shouldReturnEmptyList_forUserWithNoWishes() throws Exception {
        User userWithoutWishes = createUserInDb("nowish@ex.com", "noWishUser", "pass", "No", "Wish");
        Long userId = userWithoutWishes.getId();

        mockMvc.perform(get("/api/wishlist/user/{id}", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    // --- Тести для PUT /api/wishlist/{id} ---

    @Test
    @DisplayName("PUT /api/wishlist/{id} - Успішне оновлення власного бажання")
    void updateWish_shouldUpdateOwnWish_whenAuthenticatedAndOwner() throws Exception {
        Long wishIdToUpdate = wishUserAuth1.getId();
        Instant initialUpdatedAt = wishUserAuth1.getUpdatedAt(); // Може бути null

        Map<String, Object> updatesMap = new HashMap<>();
        updatesMap.put("title", "Updated Wish A1"); // Валідний title
        updatesMap.put("description", "New Description");
        updatesMap.put("archived", true);
        // URL не оновлюємо

        MvcResult result = mockMvc.perform(put("/api/wishlist/{id}", wishIdToUpdate)
                        .with(httpBasic(testUserAuth.getUsername(), userAuthPassword))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatesMap)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(wishIdToUpdate))
                .andExpect(jsonPath("$.title").value("Updated Wish A1"))
                .andExpect(jsonPath("$.description").value("New Description"))
                .andExpect(jsonPath("$.url").value(wishUserAuth1.getUrl())) // URL не змінився
                .andExpect(jsonPath("$.archived").value(true))
                .andExpect(jsonPath("$.user.id").value(testUserAuth.getId()))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty()) // updatedAt має бути встановлено
                .andReturn();

        // Перевірка в БД
        Wish updatedWishDb = wishRepository.findById(wishIdToUpdate).orElseThrow();
        assertThat(updatedWishDb.getTitle()).isEqualTo("Updated Wish A1");
        assertThat(updatedWishDb.getDescription()).isEqualTo("New Description");
        assertThat(updatedWishDb.getUrl()).isEqualTo(wishUserAuth1.getUrl());
        assertThat(updatedWishDb.isArchived()).isTrue();
        assertThat(updatedWishDb.getUpdatedAt()).isNotNull();
        // Перевіряємо, що час оновлення змінився (якщо він був не null) або став не null
        if (initialUpdatedAt != null) {
             assertThat(updatedWishDb.getUpdatedAt()).isAfter(initialUpdatedAt);
        }
    }

    @Test
    @DisplayName("PUT /api/wishlist/{id} - Помилка 400 Bad Request при оновленні з невалідним title")
    void updateWish_shouldReturnBadRequest_whenTitleIsInvalid() throws Exception {
        Long wishIdToUpdate = wishUserAuth1.getId();
        Map<String, Object> updatesMap = new HashMap<>();
        updatesMap.put("title", "A"); // Невалідний title

        mockMvc.perform(put("/api/wishlist/{id}", wishIdToUpdate)
                        .with(httpBasic(testUserAuth.getUsername(), userAuthPassword))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatesMap)))
                .andDo(print())
                .andExpect(status().isBadRequest()) // Валідація спрацює першою
                .andExpect(jsonPath("$.title").value("Title must be 2-25 characters long"));
    }


    @Test
    @DisplayName("PUT /api/wishlist/{id} - Помилка 403 Forbidden при оновленні чужого бажання")
    void updateWish_shouldReturnForbidden_whenUpdatingOthersWish() throws Exception {
        Long wishIdToUpdate = wishUserOther.getId(); // Бажання іншого користувача
        Map<String, Object> updatesMap = new HashMap<>();
        updatesMap.put("title", "Attempted Update Wish");

        mockMvc.perform(put("/api/wishlist/{id}", wishIdToUpdate)
                        .with(httpBasic(testUserAuth.getUsername(), userAuthPassword)) // Автентифікація як перший користувач
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatesMap)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Change access is prohibited."));
    }

     @Test
    @DisplayName("PUT /api/wishlist/{id} - Помилка 403 Forbidden при оновленні без автентифікації")
    void updateWish_shouldReturnForbidden_whenNotAuthenticated() throws Exception {
        Long wishIdToUpdate = wishUserAuth1.getId();
        Map<String, Object> updatesMap = new HashMap<>();
        updatesMap.put("title", "Unauthorized Update W");

        mockMvc.perform(put("/api/wishlist/{id}", wishIdToUpdate)
                        // БЕЗ .with(httpBasic(...))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatesMap)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("User is not authenticated."));
    }

    @Test
    @DisplayName("PUT /api/wishlist/{id} - Помилка 404 Not Found при оновленні неіснуючого бажання")
    void updateWish_shouldReturnNotFound_whenWishNotExists() throws Exception {
        Long nonExistentId = 998L;
        Map<String, Object> updatesMap = new HashMap<>();
        updatesMap.put("title", "Update Non Existent W");

        mockMvc.perform(put("/api/wishlist/{id}", nonExistentId)
                        .with(httpBasic(testUserAuth.getUsername(), userAuthPassword))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatesMap)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No wish found with ID " + nonExistentId));
    }


    // --- Тести для DELETE /api/wishlist/{id} ---

    @Test
    @DisplayName("DELETE /api/wishlist/{id} - Успішне видалення власного бажання")
    void deleteWishById_shouldDeleteOwnWish_whenAuthenticatedAndOwner() throws Exception {
        Long wishIdToDelete = wishUserAuth1.getId();
        assertThat(wishRepository.findById(wishIdToDelete)).isPresent(); // Перевірка існування

        mockMvc.perform(delete("/api/wishlist/{id}", wishIdToDelete)
                        .with(httpBasic(testUserAuth.getUsername(), userAuthPassword)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Wish of id: " + wishIdToDelete + " is deleted"));

        assertThat(wishRepository.findById(wishIdToDelete)).isEmpty(); // Перевірка видалення
    }

    @Test
    @DisplayName("DELETE /api/wishlist/{id} - Помилка 403 Forbidden при видаленні чужого бажання")
    void deleteWishById_shouldReturnForbidden_whenDeletingOthersWish() throws Exception {
        Long wishIdToDelete = wishUserOther.getId();
        assertThat(wishRepository.findById(wishIdToDelete)).isPresent();

        mockMvc.perform(delete("/api/wishlist/{id}", wishIdToDelete)
                        .with(httpBasic(testUserAuth.getUsername(), userAuthPassword))) // Автентифікація як інший користувач
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access to delete is denied."));

        assertThat(wishRepository.findById(wishIdToDelete)).isPresent(); // Перевірка, що не видалено
    }

     @Test
    @DisplayName("DELETE /api/wishlist/{id} - Помилка 403 Forbidden при видаленні без автентифікації")
    void deleteWishById_shouldReturnForbidden_whenNotAuthenticated() throws Exception {
        Long wishIdToDelete = wishUserAuth1.getId();

        mockMvc.perform(delete("/api/wishlist/{id}", wishIdToDelete))
                // БЕЗ .with(httpBasic(...))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("User is not authenticated."));
    }

    @Test
    @DisplayName("DELETE /api/wishlist/{id} - Помилка 404 Not Found при видаленні неіснуючого бажання")
    void deleteWishById_shouldReturnNotFound_whenWishNotExists() throws Exception {
        Long nonExistentId = 997L;

        mockMvc.perform(delete("/api/wishlist/{id}", nonExistentId)
                        .with(httpBasic(testUserAuth.getUsername(), userAuthPassword)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No wish found with ID " + nonExistentId));
    }
}