package com.sashaprylutsky.wishplus;

import com.sashaprylutsky.wishplus.model.User;
import com.sashaprylutsky.wishplus.model.UserPrincipal;
import com.sashaprylutsky.wishplus.repository.UserRepository;
import com.sashaprylutsky.wishplus.service.UserService;
import jakarta.persistence.NoResultException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Ініціалізує моки та інжекції Mockito
class UserServiceTest {

    @Mock // Створюємо мок для UserRepository
    private UserRepository userRepository;

    @Mock // Створюємо мок для BCryptPasswordEncoder
    private BCryptPasswordEncoder encoder;

    @Mock // Мок для SecurityContext (для тестування getUserPrincipal)
    private SecurityContext securityContext;

    @Mock // Мок для Authentication (для тестування getUserPrincipal)
    private Authentication authentication;

    @InjectMocks // Створюємо екземпляр UserService та інжектуємо моки (@Mock) в нього
    private UserService userService;

    private User testUser;
    private UserPrincipal testUserPrincipal;

    // Виконується перед кожним тестом
    @BeforeEach
    void setUp() {
        // Налаштовуємо тестові дані
        testUser = new User("test@example.com", "testuser123", "password123", "Test", "User");
        testUser.setId(1L); // Припустимо, що користувач вже існує для деяких тестів

        testUserPrincipal = new UserPrincipal(1L, "test@example.com", "testuser123", "encodedPassword");

        // Налаштування SecurityContextHolder для тестів, що потребують автентифікованого користувача
        // Це потрібно робити обережно, лише в тестах де це необхідно, або використовувати @WithMockUser в інтеграційних
        // SecurityContextHolder.setContext(securityContext); // Встановимо контекст при потребі в тестах
    }

    // Виконується після кожного тесту для очищення статичного SecurityContextHolder
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- Тести для registerUser ---

    @Test
    @DisplayName("registerUser повинен закодувати пароль, зберегти користувача та повернути його")
    void registerUser_shouldEncodePasswordAndSaveUser() {
        // Arrange (Налаштування)
        User newUser = new User("new@example.com", "newuser123", "rawPassword", "New", "User");
        User savedUser = new User("new@example.com", "newuser123", "encodedPassword", "New", "User");
        savedUser.setId(2L); // Симулюємо згенерований ID

        // Налаштовуємо мок енкодера: коли викликається encode з "rawPassword", повернути "encodedPassword"
        when(encoder.encode("rawPassword")).thenReturn("encodedPassword");
        // Налаштовуємо мок репозиторію: коли викликається save з будь-яким User, повернути savedUser
        // Використовуємо ArgumentCaptor щоб перевірити, який саме юзер передається в save
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenReturn(savedUser);

        // Act (Дія)
        User result = userService.registerUser(newUser);

        // Assert (Перевірка)
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getPassword()).isEqualTo("encodedPassword"); // Перевіряємо, що пароль у повернутого юзера закодований

        // Перевіряємо, що метод encode був викликаний рівно один раз з правильним паролем
        verify(encoder, times(1)).encode("rawPassword");
        // Перевіряємо, що метод save був викликаний рівно один раз
        verify(userRepository, times(1)).save(any(User.class));

        // Перевіряємо дані, передані в userRepository.save()
        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getPassword()).isEqualTo("encodedPassword"); // Найважливіше - чи закодовано пароль перед збереженням
        assertThat(capturedUser.getEmail()).isEqualTo(newUser.getEmail());
        assertThat(capturedUser.getUsername()).isEqualTo(newUser.getUsername());
    }

    @Test
    @DisplayName("registerUser повинен кидати DuplicateKeyException при DataIntegrityViolationException")
    void registerUser_shouldThrowDuplicateKeyException_whenEmailExists() {
        // Arrange
        User duplicateUser = new User("test@example.com", "anotheruser", "password123", "Another", "User");

        // Налаштовуємо мок енкодера
        when(encoder.encode(anyString())).thenReturn("encodedPassword");
        // Налаштовуємо мок репозиторію: коли викликається save, кинути DataIntegrityViolationException
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("constraint violation"));

        // Act & Assert
        assertThatThrownBy(() -> userService.registerUser(duplicateUser))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("Email is already taken.");

        // Перевіряємо, що метод encode був викликаний
        verify(encoder, times(1)).encode("password123");
        // Перевіряємо, що метод save був викликаний
        verify(userRepository, times(1)).save(any(User.class));
    }

    // --- Тести для getUserById ---

    @Test
    @DisplayName("getUserById повинен повернути користувача, якщо він існує")
    void getUserById_shouldReturnUser_whenUserExists() {
        // Arrange
        Long userId = 1L;
        // Налаштовуємо мок репозиторію: коли викликається findById з userId, повернути Optional з testUser
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.getUserById(userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getUsername()).isEqualTo(testUser.getUsername());

        // Перевіряємо, що findById був викликаний 1 раз з правильним ID
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("getUserById повинен кидати NoResultException, якщо користувач не існує")
    void getUserById_shouldThrowNoResultException_whenUserNotFound() {
        // Arrange
        Long userId = 99L;
        // Налаштовуємо мок репозиторію: коли викликається findById з userId, повернути пустий Optional
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(NoResultException.class)
                .hasMessageContaining("No user found with ID " + userId);

        // Перевіряємо, що findById був викликаний 1 раз з правильним ID
        verify(userRepository, times(1)).findById(userId);
    }

    // --- Тести для updateUserPrincipalDetails ---

    private void mockSecurityContext(UserPrincipal principal) {
        // Створюємо мок Authentication
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        // Налаштовуємо мок SecurityContext
        when(securityContext.getAuthentication()).thenReturn(auth);
        // Встановлюємо мок SecurityContext у SecurityContextHolder
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("updateUserPrincipalDetails повинен оновити та зберегти користувача, якщо ID співпадають")
    void updateUserPrincipalDetails_shouldUpdateAndSaveUser_whenIdsMatch() {
        // Arrange
        mockSecurityContext(testUserPrincipal); // Мокуємо залогованого користувача

        Long userIdToUpdate = 1L;
        User userUpdates = new User(); // DTO або об'єкт з оновленнями
        userUpdates.setUsername("newUsername123");
        userUpdates.setFirstName("UpdatedName");
        userUpdates.setLastName(null); // Перевіримо, що null не перезаписує
        userUpdates.setProfilePhoto("new_photo.jpg");

        User existingUser = new User(testUserPrincipal.getEmail(), testUserPrincipal.getUsername(), "encodedPassword", "Test", "User");
        existingUser.setId(userIdToUpdate);

        when(userRepository.findById(userIdToUpdate)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0)); // Повертаємо той самий об'єкт, що передали в save

        // Act
        User updatedUser = userService.updateUserPrincipalDetails(userIdToUpdate, userUpdates);

        // Assert
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getId()).isEqualTo(userIdToUpdate);
        assertThat(updatedUser.getUsername()).isEqualTo("newUsername123"); // Оновлено
        assertThat(updatedUser.getFirstName()).isEqualTo("UpdatedName");   // Оновлено
        assertThat(updatedUser.getLastName()).isEqualTo("User");        // Не оновлено (бо в userUpdates було null)
        assertThat(updatedUser.getProfilePhoto()).isEqualTo("new_photo.jpg"); // Оновлено

        // Перевіряємо виклики
        verify(userRepository, times(1)).findById(userIdToUpdate);

        // Використовуємо ArgumentCaptor для перевірки об'єкта, переданого в save
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());

        User savedUserArg = userCaptor.getValue();
        assertThat(savedUserArg.getUsername()).isEqualTo("newUsername123");
        assertThat(savedUserArg.getFirstName()).isEqualTo("UpdatedName");
        assertThat(savedUserArg.getLastName()).isEqualTo("User"); // Переконуємося, що null не перезаписав
        assertThat(savedUserArg.getProfilePhoto()).isEqualTo("new_photo.jpg");
        assertThat(savedUserArg.getEmail()).isEqualTo(existingUser.getEmail()); // Email не мав змінитися
        assertThat(savedUserArg.getPassword()).isEqualTo(existingUser.getPassword()); // Пароль не мав змінитися
    }

    @Test
    @DisplayName("updateUserPrincipalDetails повинен кидати AccessDeniedException, якщо ID не співпадають")
    void updateUserPrincipalDetails_shouldThrowAccessDeniedException_whenIdsDoNotMatch() {
        // Arrange
        UserPrincipal loggedInUserPrincipal = new UserPrincipal(1L, "user1@example.com", "user1", "pass");
        mockSecurityContext(loggedInUserPrincipal); // Мокуємо залогованого користувача з ID 1

        Long userIdToUpdate = 2L; // Намагаємося оновити іншого користувача (ID 2)
        User userUpdates = new User();
        userUpdates.setUsername("newUsername");

        User userToUpdateRecord = new User("user2@example.com", "user2", "pass2", "User", "Two");
        userToUpdateRecord.setId(userIdToUpdate);

        when(userRepository.findById(userIdToUpdate)).thenReturn(Optional.of(userToUpdateRecord));

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUserPrincipalDetails(userIdToUpdate, userUpdates))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Change access is prohibited.");

        // Перевіряємо, що findById був викликаний
        verify(userRepository, times(1)).findById(userIdToUpdate);
        // Перевіряємо, що save НЕ був викликаний
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("updateUserPrincipalDetails повинен кидати NoResultException, якщо користувач для оновлення не знайдений")
    void updateUserPrincipalDetails_shouldThrowNoResultException_whenUserToUpdateNotFound() {
        // Arrange
        mockSecurityContext(testUserPrincipal); // Мокуємо залогованого користувача

        Long nonExistentUserId = 99L;
        User userUpdates = new User();
        userUpdates.setUsername("newUsername");

        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUserPrincipalDetails(nonExistentUserId, userUpdates))
                .isInstanceOf(NoResultException.class)
                .hasMessageContaining("No user found with ID " + nonExistentUserId);

        // Перевіряємо, що findById був викликаний
        verify(userRepository, times(1)).findById(nonExistentUserId);
        // Перевіряємо, що save НЕ був викликаний
        verify(userRepository, never()).save(any(User.class));
    }

    // --- Тести для getUsers ---

    @Test
    @DisplayName("getUsers повинен повернути список користувачів з репозиторію")
    void getUsers_shouldReturnListOfUsers() {
        // Arrange
        User user1 = new User("user1@example.com", "user1", "pass1", "User", "One"); user1.setId(1L);
        User user2 = new User("user2@example.com", "user2", "pass2", "User", "Two"); user2.setId(2L);
        List<User> userList = List.of(user1, user2);

        when(userRepository.findAll()).thenReturn(userList);

        // Act
        List<User> result = userService.getUsers();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(user1, user2);

        // Перевіряємо виклик findAll
        verify(userRepository, times(1)).findAll();
    }

    // --- Тести для deleteUserById ---

    @Test
    @DisplayName("deleteUserById повинен видалити користувача, якщо ID співпадають і повідомлення коректне")
    void deleteUserById_shouldDeleteUser_whenIdsMatchAndMessageCorrect() {
        // Arrange
        mockSecurityContext(testUserPrincipal); // Мокуємо залогованого користувача з ID 1

        Long userIdToDelete = 1L;
        String correctMessage = "Delete my account forever!";

        User userToDeleteRecord = new User(testUserPrincipal.getEmail(), testUserPrincipal.getUsername(), "pass", "Test", "User");
        userToDeleteRecord.setId(userIdToDelete);

        when(userRepository.findById(userIdToDelete)).thenReturn(Optional.of(userToDeleteRecord));
        // doNothing() використовується для void методів
        doNothing().when(userRepository).delete(any(User.class));

        // Act
        userService.deleteUserById(userIdToDelete, correctMessage);

        // Assert
        // Перевіряємо виклики
        verify(userRepository, times(1)).findById(userIdToDelete);

        // Використовуємо ArgumentCaptor для перевірки об'єкта, переданого в delete
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).delete(userCaptor.capture());
        assertThat(userCaptor.getValue().getId()).isEqualTo(userIdToDelete);
    }

    @Test
    @DisplayName("deleteUserById повинен кидати AccessDeniedException, якщо ID не співпадають")
    void deleteUserById_shouldThrowAccessDeniedException_whenIdsDoNotMatch() {
        // Arrange
        UserPrincipal loggedInUserPrincipal = new UserPrincipal(1L, "user1@example.com", "user1", "pass");
        mockSecurityContext(loggedInUserPrincipal); // Залогований користувач з ID 1

        Long userIdToDelete = 2L; // Намагаємося видалити користувача з ID 2
        String correctMessage = "Delete my account forever!";

        User userToDeleteRecord = new User("user2@example.com", "user2", "pass2", "User", "Two");
        userToDeleteRecord.setId(userIdToDelete);

        when(userRepository.findById(userIdToDelete)).thenReturn(Optional.of(userToDeleteRecord));

        // Act & Assert
        assertThatThrownBy(() -> userService.deleteUserById(userIdToDelete, correctMessage))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access to delete is denied.");

        // Перевіряємо виклики
        verify(userRepository, times(1)).findById(userIdToDelete);
        verify(userRepository, never()).delete(any(User.class)); // Переконуємось, що delete не викликався
    }

    @Test
    @DisplayName("deleteUserById повинен кидати CancellationException, якщо повідомлення некоректне")
    void deleteUserById_shouldThrowCancellationException_whenMessageIsIncorrect() {
        // Arrange
        mockSecurityContext(testUserPrincipal); // Мокуємо залогованого користувача з ID 1

        Long userIdToDelete = 1L;
        String incorrectMessage = "Please delete my account"; // Неправильне повідомлення

        User userToDeleteRecord = new User(testUserPrincipal.getEmail(), testUserPrincipal.getUsername(), "pass", "Test", "User");
        userToDeleteRecord.setId(userIdToDelete);

        when(userRepository.findById(userIdToDelete)).thenReturn(Optional.of(userToDeleteRecord));

        // Act & Assert
        assertThatThrownBy(() -> userService.deleteUserById(userIdToDelete, incorrectMessage))
                .isInstanceOf(CancellationException.class)
                .hasMessageContaining("The deletion is aborted.");

        // Перевіряємо виклики
        verify(userRepository, times(1)).findById(userIdToDelete);
        verify(userRepository, never()).delete(any(User.class)); // Переконуємось, що delete не викликався
    }

    @Test
    @DisplayName("deleteUserById повинен кидати NoResultException, якщо користувач не знайдений")
    void deleteUserById_shouldThrowNoResultException_whenUserNotFound() {
         // Arrange
        mockSecurityContext(testUserPrincipal); // Мокуємо залогованого користувача

        Long nonExistentUserId = 99L;
        String correctMessage = "Delete my account forever!";

        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.deleteUserById(nonExistentUserId, correctMessage))
                .isInstanceOf(NoResultException.class)
                .hasMessageContaining("No user found with ID " + nonExistentUserId);

        // Перевіряємо виклики
        verify(userRepository, times(1)).findById(nonExistentUserId);
        verify(userRepository, never()).delete(any(User.class)); // Переконуємось, що delete не викликався
    }

     // --- Тести для getUserPrincipal (статичний метод) ---
     // Тестування статичних методів, що використовують SecurityContextHolder може бути складним.
     // Краще тестувати його опосередковано через методи, що його викликають (як ми робили вище).
     // Але якщо дуже потрібно, можна зробити так:

     @Test
     @DisplayName("getUserPrincipal повинен повернути UserPrincipal з SecurityContext")
     void getUserPrincipal_shouldReturnPrincipalFromContext() {
         // Arrange
         // Важливо: Створюємо реальний Authentication об'єкт з нашим UserPrincipal
         Authentication auth = new UsernamePasswordAuthenticationToken(testUserPrincipal, null, testUserPrincipal.getAuthorities());
         when(securityContext.getAuthentication()).thenReturn(auth); // Налаштовуємо мок securityContext
         when(authentication.getPrincipal()).thenReturn(testUserPrincipal); // Можна і так, якщо мокаємо authentication
         when(securityContext.getAuthentication()).thenReturn(authentication);
         when(authentication.isAuthenticated()).thenReturn(true); // Важливо!
         SecurityContextHolder.setContext(securityContext); // Встановлюємо мок контекст

         // Act
         UserPrincipal result = UserService.getUserPrincipal(); // Викликаємо статичний метод

         // Assert
         assertThat(result).isNotNull();
         assertThat(result.getId()).isEqualTo(testUserPrincipal.getId());
         assertThat(result.getUsername()).isEqualTo(testUserPrincipal.getUsername());

         // Перевірка взаємодії з моками (опціонально, але корисно)
         verify(securityContext, times(1)).getAuthentication();
         // verify(authentication, times(1)).getPrincipal(); // Залежить від того, як ви його використовуєте
     }

     @Test
     @DisplayName("getUserPrincipal повинен кидати NullPointerException, якщо Authentication is null")
     void getUserPrincipal_shouldThrowException_whenAuthenticationIsNull() {
         // Arrange
         when(securityContext.getAuthentication()).thenReturn(null); // Симулюємо відсутність Authentication
         SecurityContextHolder.setContext(securityContext);

         // Act & Assert
         assertThatThrownBy(UserService::getUserPrincipal) // Виклик статичного методу
                 .isInstanceOf(NullPointerException.class)
                 .hasMessageContaining("User is not authenticated.");
     }

     @Test
     @DisplayName("getUserPrincipal повинен кидати NullPointerException, якщо користувач не автентифікований")
     void getUserPrincipal_shouldThrowException_whenNotAuthenticated() {
         // Arrange
         when(securityContext.getAuthentication()).thenReturn(authentication);
         when(authentication.isAuthenticated()).thenReturn(false); // Симулюємо неавтентифікованого користувача
         SecurityContextHolder.setContext(securityContext);

         // Act & Assert
         assertThatThrownBy(UserService::getUserPrincipal)
                 .isInstanceOf(NullPointerException.class)
                 .hasMessageContaining("User is not authenticated.");
     }
}