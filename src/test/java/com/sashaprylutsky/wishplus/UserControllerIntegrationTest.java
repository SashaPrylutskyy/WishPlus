package com.sashaprylutsky.wishplus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sashaprylutsky.wishplus.model.User;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private User testUser1;
    private User testUser2;
    private String rawPassword = "password123";

    private User createUserInDb(String email, String username, String rawPassword, String firstName, String lastName) {
        User user = new User(email, username, passwordEncoder.encode(rawPassword), firstName, lastName);
        return userRepository.saveAndFlush(user);
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userRepository.flush();
        testUser1 = createUserInDb("test1@example.com", "testuser1", rawPassword, "Test", "One");
        testUser2 = createUserInDb("test2@example.com", "testuser2", "otherpass", "Test", "Two");
    }

    @Test
    @DisplayName("POST /api/users - Успішна реєстрація нового користувача")
    void register_shouldCreateUser_whenDataIsValid() throws Exception {
        String newUserJson = """
            {
              "email": "new@example.com",
              "username": "newuser123",
              "password": "validPass1",
              "firstName": "New",
              "lastName": "User"
            }
            """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newUserJson))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("newuser123"))
                .andExpect(jsonPath("$.firstName").value("New"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());

        Optional<User> dbUserOpt = userRepository.findByUsername("newuser123");
        assertThat(dbUserOpt).isPresent();
        User dbUser = dbUserOpt.get();
        assertThat(dbUser.getEmail()).isEqualTo("new@example.com");
        assertThat(passwordEncoder.matches("validPass1", dbUser.getPassword())).isTrue();
    }

    @Test
    @DisplayName("POST /api/users - Помилка валідації (короткий username)")
    void register_shouldReturnBadRequest_whenUsernameIsInvalid() throws Exception {
        String invalidUserJson = """
            {
              "email": "invalid@example.com",
              "username": "short",
              "password": "password",
              "firstName": "Invalid",
              "lastName": "User"
            }
            """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidUserJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").value("Username must be 8-20 characters"));
    }

    @Test
    @DisplayName("POST /api/users - Помилка конфлікту (дублікат email)")
    void register_shouldReturnConflict_whenEmailIsDuplicate() throws Exception {
        String duplicateEmailJson = """
            {
              "email": "test1@example.com",
              "username": "anotheruser",
              "password": "password",
              "firstName": "Another",
              "lastName": "User"
            }
            """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateEmailJson))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email is already taken."));
    }

    @Test
    @DisplayName("GET /api/users - Повинен повернути список користувачів")
    void getUsers_shouldReturnUserList() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").value(testUser1.getUsername()))
                .andExpect(jsonPath("$[1].username").value(testUser2.getUsername()))
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[0].email").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/users/{id} - Повинен повернути користувача за ID")
    void getUserById_shouldReturnUser_whenUserExists() throws Exception {
        Long userId = testUser1.getId();

        mockMvc.perform(get("/api/users/{id}", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.username").value(testUser1.getUsername()))
                .andExpect(jsonPath("$.firstName").value(testUser1.getFirstName()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/users/{id} - Повинен повернути 404 Not Found, якщо користувач не існує")
    void getUserById_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        Long nonExistentId = 999L;

        mockMvc.perform(get("/api/users/{id}", nonExistentId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No user found with ID " + nonExistentId));
    }

    @Test
    @DisplayName("PUT /api/users/{id} - Успішне оновлення власних даних користувачем")
    void updateUserPrincipalDetails_shouldUpdateUser_whenUserUpdatesOwnData() throws Exception {
        Long userId = testUser1.getId();
        User updateDto = new User();
        updateDto.setUsername("updatedUser1");
        updateDto.setFirstName("UpdatedFirstName");
        updateDto.setProfilePhoto("new_photo.jpg");

        mockMvc.perform(put("/api/users/{id}", userId)
                        .with(httpBasic(testUser1.getUsername(), rawPassword))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.username").value("updatedUser1"))
                .andExpect(jsonPath("$.firstName").value("UpdatedFirstName"))
                .andExpect(jsonPath("$.lastName").value(testUser1.getLastName()))
                .andExpect(jsonPath("$.profilePhoto").value("new_photo.jpg"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());

        User updatedDbUser = userRepository.findById(userId).orElseThrow();
        assertThat(updatedDbUser.getUsername()).isEqualTo("updatedUser1");
        assertThat(updatedDbUser.getFirstName()).isEqualTo("UpdatedFirstName");
        assertThat(updatedDbUser.getProfilePhoto()).isEqualTo("new_photo.jpg");
    }

    @Test
    @DisplayName("PUT /api/users/{id} - Помилка 403 Forbidden, коли користувач оновлює чужі дані")
    void updateUserPrincipalDetails_shouldReturnForbidden_whenUserUpdatesOtherUserData() throws Exception {
        Long targetUserId = testUser2.getId();
        User updateDto = new User();
        updateDto.setUsername("validUsername");

        mockMvc.perform(put("/api/users/{id}", targetUserId)
                        .with(httpBasic(testUser1.getUsername(), rawPassword))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Change access is prohibited."));
    }

    @Test
    @DisplayName("PUT /api/users/{id} - Помилка 403 Forbidden при спробі оновлення без автентифікації")
    void updateUserPrincipalDetails_shouldReturnForbidden_whenNotAuthenticated() throws Exception {
        Long userId = testUser1.getId();
        User updateDto = new User();
        updateDto.setUsername("updatedUser1");

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("User is not authenticated."));
    }

    @Test
    @DisplayName("PUT /api/users/{id} - Помилка 404 Not Found, коли користувач для оновлення не існує")
    void updateUserPrincipalDetails_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        Long nonExistentId = 999L;
        User updateDto = new User();
        updateDto.setUsername("anyUser99");

        mockMvc.perform(put("/api/users/{id}", nonExistentId)
                        .with(httpBasic(testUser1.getUsername(), rawPassword))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No user found with ID " + nonExistentId));
    }

    @Test
    @DisplayName("DELETE /api/users/{id} - Успішне видалення власного акаунту")
    void deleteUserById_shouldDeleteUser_whenUserDeletesOwnAccountWithCorrectMessage() throws Exception {
        Long userId = testUser1.getId();
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("submitMessage", "Delete my account forever!");

        mockMvc.perform(delete("/api/users/{id}", userId)
                        .with(httpBasic(testUser1.getUsername(), rawPassword))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("User with id: " + userId + " is successfully deleted."));

        assertThat(userRepository.findById(userId)).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/users/{id} - Помилка 403 Forbidden при спробі видалення чужого акаунту")
    void deleteUserById_shouldReturnForbidden_whenUserDeletesOtherUserAccount() throws Exception {
        Long targetUserId = testUser2.getId();
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("submitMessage", "Delete my account forever!");

        mockMvc.perform(delete("/api/users/{id}", targetUserId)
                        .with(httpBasic(testUser1.getUsername(), rawPassword))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access to delete is denied."));

        assertThat(userRepository.findById(targetUserId)).isPresent();
    }

    @Test
    @DisplayName("DELETE /api/users/{id} - Помилка 200 OK з повідомленням про скасування при некоректному submitMessage")
    void deleteUserById_shouldReturnOkWithCancellationMessage_whenSubmitMessageIsIncorrect() throws Exception {
        Long userId = testUser1.getId();
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("submitMessage", "Wrong message");

        mockMvc.perform(delete("/api/users/{id}", userId)
                        .with(httpBasic(testUser1.getUsername(), rawPassword))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Exception").value("The deletion is aborted."));

        assertThat(userRepository.findById(userId)).isPresent();
    }

    @Test
    @DisplayName("DELETE /api/users/{id} - Помилка 403 Forbidden при спробі видалення без автентифікації")
    void deleteUserById_shouldReturnForbidden_whenNotAuthenticated() throws Exception {
        Long userId = testUser1.getId();
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("submitMessage", "Delete my account forever!");

        mockMvc.perform(delete("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("User is not authenticated."));
    }

    @Test
    @DisplayName("DELETE /api/users/{id} - Помилка 404 Not Found, коли користувач для видалення не існує")
    void deleteUserById_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        Long nonExistentId = 999L;
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("submitMessage", "Delete my account forever!");

        mockMvc.perform(delete("/api/users/{id}", nonExistentId)
                        .with(httpBasic(testUser1.getUsername(), rawPassword))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No user found with ID " + nonExistentId));
    }
}