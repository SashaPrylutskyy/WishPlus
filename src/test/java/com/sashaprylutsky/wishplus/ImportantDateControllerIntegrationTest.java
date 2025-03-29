package com.sashaprylutsky.wishplus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sashaprylutsky.wishplus.model.ImportantDate;
import com.sashaprylutsky.wishplus.model.User;
import com.sashaprylutsky.wishplus.repository.ImportantDateRepository;
import com.sashaprylutsky.wishplus.repository.UserRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ImportantDateControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ImportantDateRepository importantDateRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private User testUserAuth; // Користувач, який буде автентифікуватися
    private User testUserOther; // Інший користувач для перевірки доступу
    private String userAuthPassword = "passwordAuth";
    private String userOtherPassword = "passwordOther";

    private ImportantDate dateUserAuth1;
    private ImportantDate dateUserAuth2;
    private ImportantDate dateUserOther;

    // Допоміжний метод для створення користувача в БД
    private User createUserInDb(String email, String username, String rawPassword, String firstName, String lastName) {
        User user = new User(email, username, passwordEncoder.encode(rawPassword), firstName, lastName);
        return userRepository.saveAndFlush(user);
    }

    // Допоміжний метод для створення важливої дати в БД
    private ImportantDate createDateInDb(User user, String title, Date date) {
        ImportantDate importantDate = new ImportantDate(user, title, date);
        return importantDateRepository.saveAndFlush(importantDate);
    }

    @BeforeEach
    void setUp() {
        // Очищення репозиторіїв
        importantDateRepository.deleteAll();
        userRepository.deleteAll();
        importantDateRepository.flush();
        userRepository.flush();

        // Створення користувачів
        testUserAuth = createUserInDb("auth@example.com", "userAuth", userAuthPassword, "Auth", "User");
        testUserOther = createUserInDb("other@example.com", "userOther", userOtherPassword, "Other", "Person");

        // Створення дат
        Instant now = Instant.now();
        dateUserAuth1 = createDateInDb(testUserAuth, "Auth Birthday", Date.from(now.minus(30, ChronoUnit.DAYS)));
        dateUserAuth2 = createDateInDb(testUserAuth, "Auth Anniversary", Date.from(now));
        dateUserOther = createDateInDb(testUserOther, "Other Event", Date.from(now.plus(30, ChronoUnit.DAYS)));
    }

    // --- Тести для POST /api/dates ---

    @Test
    @DisplayName("POST /api/dates - Успішне створення дати автентифікованим користувачем")
    void createImportantDate_shouldCreateDate_whenAuthenticated() throws Exception {
        Instant futureDate = Instant.now().plus(90, ChronoUnit.DAYS);
        // Важливо: НЕ передаємо User в запиті, сервіс має взяти його з Principal
        Map<String, Object> newDateMap = new HashMap<>();
        newDateMap.put("title", "New Conference");
        newDateMap.put("date", Date.from(futureDate)); // Jackson може серіалізувати Date

        mockMvc.perform(post("/api/dates")
                        .with(httpBasic(testUserAuth.getUsername(), userAuthPassword)) // Автентифікація
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDateMap)))
                .andDo(print())
                .andExpect(status().isOk()) // Очікуємо 200 OK (згідно контролеру)
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("New Conference"))
                // Перевіряємо дату - може бути складним через формат, перевіримо що не null
                .andExpect(jsonPath("$.date").isNotEmpty())
                .andExpect(jsonPath("$.user.id").value(testUserAuth.getId())) // Перевіряємо ID користувача
                // Переконуємося, що конфіденційні дані користувача не повертаються
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.email").doesNotExist());

        // Перевірка в БД
        // Знайдемо всі дати користувача і перевіримо останню
        var datesInDb = importantDateRepository.findAllByUser_Id(testUserAuth.getId());
        assertThat(datesInDb).hasSize(3); // 2 з setUp + 1 нова
        ImportantDate createdDate = datesInDb.stream()
                                             .filter(d -> "New Conference".equals(d.getTitle()))
                                             .findFirst()
                                             .orElseThrow();
        assertThat(createdDate.getUser().getId()).isEqualTo(testUserAuth.getId());
        // Порівняння дат може бути неточним через мілісекунди, порівняємо приблизно
        assertThat(createdDate.getDate()).isCloseTo(Date.from(futureDate), 1000); // допуск 1 секунда
    }

    @Test
    @DisplayName("POST /api/dates - Помилка 403 Forbidden при створенні без автентифікації")
    void createImportantDate_shouldReturnForbidden_whenNotAuthenticated() throws Exception {
        Map<String, Object> newDateMap = new HashMap<>();
        newDateMap.put("title", "Unauthorized Event");
        newDateMap.put("date", new Date());

        mockMvc.perform(post("/api/dates")
                        // БЕЗ .with(httpBasic(...))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDateMap)))
                .andDo(print())
                .andExpect(status().isForbidden()) // Очікуємо 403 від NullPointerException в getUserPrincipal
                .andExpect(jsonPath("$.error").value("User is not authenticated."));
    }

    // --- Тести для GET /api/dates/{id} ---

    @Test
    @DisplayName("GET /api/dates/{id} - Успішне отримання існуючої дати (автентифікація не обов'язкова)")
    void getRecordById_shouldReturnDate_whenExists() throws Exception {
        Long dateId = dateUserAuth1.getId();

        mockMvc.perform(get("/api/dates/{id}", dateId))
                // Можна додати .with(httpBasic(...)), але не обов'язково, бо шлях дозволено
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(dateId))
                .andExpect(jsonPath("$.title").value(dateUserAuth1.getTitle()))
                .andExpect(jsonPath("$.user.id").value(testUserAuth.getId()))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.email").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/dates/{id} - Помилка 404 Not Found при запиті неіснуючої дати")
    void getRecordById_shouldReturnNotFound_whenNotExists() throws Exception {
        Long nonExistentId = 9999L;

        mockMvc.perform(get("/api/dates/{id}", nonExistentId))
                .andDo(print())
                .andExpect(status().isNotFound()) // Очікуємо 404 від NoResultException
                .andExpect(jsonPath("$.error").value("No record is being found with id: " + nonExistentId));
    }

    // --- Тести для GET /api/dates/user/{id} ---

    @Test
    @DisplayName("GET /api/dates/user/{id} - Успішне отримання списку дат для існуючого користувача")
    void getRecordsByUserId_shouldReturnDateList_forExistingUser() throws Exception {
        Long userId = testUserAuth.getId();

        mockMvc.perform(get("/api/dates/user/{id}", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()", is(2))) // У testUserAuth є 2 дати
                // Перевіряємо чи містяться ID наших дат
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(dateUserAuth1.getId().intValue(), dateUserAuth2.getId().intValue())))
                .andExpect(jsonPath("$[0].user.id").value(userId)) // Перевіряємо власника в першому елементі
                .andExpect(jsonPath("$[0].user.password").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/dates/user/{id} - Повернення порожнього списку для користувача без дат")
    void getRecordsByUserId_shouldReturnEmptyList_forUserWithNoDates() throws Exception {
        User userWithoutDates = createUserInDb("nodates@ex.com", "nodatesUser", "pass", "No", "Dates");
        Long userId = userWithoutDates.getId();

        mockMvc.perform(get("/api/dates/user/{id}", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()", is(0))); // Очікуємо порожній масив
    }

     @Test
    @DisplayName("GET /api/dates/user/{id} - Повернення порожнього списку для неіснуючого користувача ID")
    void getRecordsByUserId_shouldReturnEmptyList_forNonExistentUser() throws Exception {
        Long nonExistentUserId = 9998L;

        mockMvc.perform(get("/api/dates/user/{id}", nonExistentUserId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()", is(0))); // Репозиторій просто поверне порожній список
    }


    // --- Тести для PUT /api/dates/{id} ---

    @Test
    @DisplayName("PUT /api/dates/{id} - Успішне оновлення власної дати автентифікованим користувачем")
    void updateRecordById_shouldUpdateOwnDate_whenAuthenticatedAndOwner() throws Exception {
        Long dateIdToUpdate = dateUserAuth1.getId();
        String updatedTitle = "Updated Auth Birthday";
        Instant updatedInstant = dateUserAuth1.getDate().toInstant().plus(5, ChronoUnit.DAYS);

        Map<String, Object> updatesMap = new HashMap<>();
        updatesMap.put("title", updatedTitle);
        updatesMap.put("date", Date.from(updatedInstant));

        mockMvc.perform(put("/api/dates/{id}", dateIdToUpdate)
                        .with(httpBasic(testUserAuth.getUsername(), userAuthPassword))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatesMap)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(dateIdToUpdate))
                .andExpect(jsonPath("$.title").value(updatedTitle))
                .andExpect(jsonPath("$.user.id").value(testUserAuth.getId()));

        // Перевірка в БД
        ImportantDate updatedDateDb = importantDateRepository.findById(dateIdToUpdate).orElseThrow();
        assertThat(updatedDateDb.getTitle()).isEqualTo(updatedTitle);
        assertThat(updatedDateDb.getDate()).isCloseTo(Date.from(updatedInstant), 1000);
    }

    @Test
    @DisplayName("PUT /api/dates/{id} - Помилка 403 Forbidden при оновленні чужої дати")
    void updateRecordById_shouldReturnForbidden_whenUpdatingOthersDate() throws Exception {
        Long dateIdToUpdate = dateUserOther.getId(); // Дата іншого користувача
        Map<String, Object> updatesMap = new HashMap<>();
        updatesMap.put("title", "Attempted Update");

        mockMvc.perform(put("/api/dates/{id}", dateIdToUpdate)
                        .with(httpBasic(testUserAuth.getUsername(), userAuthPassword)) // Автентифікація як перший користувач
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatesMap)))
                .andDo(print())
                .andExpect(status().isForbidden()) // Очікуємо 403 від AccessDeniedException
                .andExpect(jsonPath("$.error").value("Change access is prohibited."));
    }

    @Test
    @DisplayName("PUT /api/dates/{id} - Помилка 403 Forbidden при оновленні без автентифікації")
    void updateRecordById_shouldReturnForbidden_whenNotAuthenticated() throws Exception {
        Long dateIdToUpdate = dateUserAuth1.getId();
        Map<String, Object> updatesMap = new HashMap<>();
        updatesMap.put("title", "Unauthorized Update");

        mockMvc.perform(put("/api/dates/{id}", dateIdToUpdate)
                        // БЕЗ .with(httpBasic(...))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatesMap)))
                .andDo(print())
                .andExpect(status().isForbidden()) // Очікуємо 403 від NullPointerException
                .andExpect(jsonPath("$.error").value("User is not authenticated."));
    }

    @Test
    @DisplayName("PUT /api/dates/{id} - Помилка 404 Not Found при оновленні неіснуючої дати")
    void updateRecordById_shouldReturnNotFound_whenDateNotExists() throws Exception {
        Long nonExistentId = 9997L;
        Map<String, Object> updatesMap = new HashMap<>();
        updatesMap.put("title", "Update Non Existent");

        mockMvc.perform(put("/api/dates/{id}", nonExistentId)
                        .with(httpBasic(testUserAuth.getUsername(), userAuthPassword)) // Автентифікація потрібна, щоб виключити 403
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatesMap)))
                .andDo(print())
                .andExpect(status().isNotFound()) // Очікуємо 404 від NoResultException
                .andExpect(jsonPath("$.error").value("No record is being found with id: " + nonExistentId));
    }

    // --- Тести для DELETE /api/dates/{id} ---

    @Test
    @DisplayName("DELETE /api/dates/{id} - Успішне видалення власної дати автентифікованим користувачем")
    void deleteRecordById_shouldDeleteOwnDate_whenAuthenticatedAndOwner() throws Exception {
        Long dateIdToDelete = dateUserAuth1.getId();

        // Перевірка, що дата існує перед видаленням
        assertThat(importantDateRepository.findById(dateIdToDelete)).isPresent();

        mockMvc.perform(delete("/api/dates/{id}", dateIdToDelete)
                        .with(httpBasic(testUserAuth.getUsername(), userAuthPassword)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Record deleted with id: " + dateIdToDelete));

        // Перевірка, що дата видалена з БД
        assertThat(importantDateRepository.findById(dateIdToDelete)).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/dates/{id} - Помилка 403 Forbidden при видаленні чужої дати")
    void deleteRecordById_shouldReturnForbidden_whenDeletingOthersDate() throws Exception {
        Long dateIdToDelete = dateUserOther.getId(); // Дата іншого користувача

        // Перевірка, що дата існує перед спробою видалення
        assertThat(importantDateRepository.findById(dateIdToDelete)).isPresent();

        mockMvc.perform(delete("/api/dates/{id}", dateIdToDelete)
                        .with(httpBasic(testUserAuth.getUsername(), userAuthPassword))) // Автентифікація як перший користувач
                .andDo(print())
                .andExpect(status().isForbidden()) // Очікуємо 403 від AccessDeniedException
                .andExpect(jsonPath("$.error").value("Change access is prohibited."));

        // Перевірка, що дата НЕ видалена з БД
        assertThat(importantDateRepository.findById(dateIdToDelete)).isPresent();
    }

     @Test
    @DisplayName("DELETE /api/dates/{id} - Помилка 403 Forbidden при видаленні без автентифікації")
    void deleteRecordById_shouldReturnForbidden_whenNotAuthenticated() throws Exception {
        Long dateIdToDelete = dateUserAuth1.getId();

        mockMvc.perform(delete("/api/dates/{id}", dateIdToDelete))
                        // БЕЗ .with(httpBasic(...))
                .andDo(print())
                .andExpect(status().isForbidden()) // Очікуємо 403 від NullPointerException
                .andExpect(jsonPath("$.error").value("User is not authenticated."));
    }

    @Test
    @DisplayName("DELETE /api/dates/{id} - Помилка 404 Not Found при видаленні неіснуючої дати")
    void deleteRecordById_shouldReturnNotFound_whenDateNotExists() throws Exception {
        Long nonExistentId = 9996L;

        mockMvc.perform(delete("/api/dates/{id}", nonExistentId)
                        .with(httpBasic(testUserAuth.getUsername(), userAuthPassword))) // Автентифікація потрібна
                .andDo(print())
                .andExpect(status().isNotFound()) // Очікуємо 404 від NoResultException
                .andExpect(jsonPath("$.error").value("No record is being found with id: " + nonExistentId));
    }
}