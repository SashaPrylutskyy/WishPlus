package com.sashaprylutsky.wishplus;

import com.sashaprylutsky.wishplus.model.User;
import com.sashaprylutsky.wishplus.model.UserPrincipal;
import com.sashaprylutsky.wishplus.model.Wish;
import com.sashaprylutsky.wishplus.repository.WishRepository;
import com.sashaprylutsky.wishplus.service.WishService;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishServiceTest {

    @Mock
    private WishRepository wishRepository; // Мокаємо репозиторій

    @Mock // Моки для SecurityContext
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private WishService wishService; // Клас, що тестуємо

    private User testUser;
    private UserPrincipal testUserPrincipal;
    private User otherUser;
    private UserPrincipal otherUserPrincipal;
    private Wish testWish1;
    private Wish testWish2;
    private Wish otherUserWish;

    // Допоміжний метод для мокування SecurityContext
    private void mockSecurityContext(UserPrincipal principal) {
        if (principal == null) {
             when(securityContext.getAuthentication()).thenReturn(authentication);
             when(authentication.isAuthenticated()).thenReturn(false);
        } else {
            Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            when(authentication.getPrincipal()).thenReturn(principal);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(securityContext.getAuthentication()).thenReturn(authentication);
        }
        SecurityContextHolder.setContext(securityContext);
    }

    @BeforeEach
    void setUp() {
        // Готуємо тестові дані
        testUser = new User("test@user.com", "testuser", "pass1", "Test", "User");
        testUser.setId(1L);
        testUserPrincipal = new UserPrincipal(1L, testUser.getEmail(), testUser.getUsername(), "encodedPass1");

        otherUser = new User("other@user.com", "otheruser", "pass2", "Other", "User");
        otherUser.setId(2L);
        otherUserPrincipal = new UserPrincipal(2L, otherUser.getEmail(), otherUser.getUsername(), "encodedPass2");

        Instant now = Instant.now();
        testWish1 = new Wish("Laptop", "New gaming laptop", "http://example.com/laptop");
        testWish1.setId(10L);
        testWish1.setUser(testUser);
        testWish1.setCreatedAt(now.minus(1, ChronoUnit.DAYS));
        testWish1.setArchived(false);

        testWish2 = new Wish("Phone", "Latest model", "http://example.com/phone");
        testWish2.setId(11L);
        testWish2.setUser(testUser);
        testWish2.setCreatedAt(now);
        testWish2.setArchived(false);

        otherUserWish = new Wish("Book", "Interesting book", "http://example.com/book");
        otherUserWish.setId(12L);
        otherUserWish.setUser(otherUser); // Належить іншому користувачу
        otherUserWish.setCreatedAt(now);
        otherUserWish.setArchived(false);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext(); // Очищуємо контекст
    }

    // --- Тести для getAllWishesByUserId ---
    // SecurityContext тут не потрібен

    @Test
    @DisplayName("getAllWishesByUserId повинен повертати список бажань користувача")
    void getAllWishesByUserId_shouldReturnUserWishes() {
        // Arrange
        Long userId = testUser.getId();
        List<Wish> expectedWishes = List.of(testWish1, testWish2);
        when(wishRepository.findAllByUser_Id(userId)).thenReturn(expectedWishes);

        // Act
        List<Wish> actualWishes = wishService.getAllWishesByUserId(userId);

        // Assert
        assertThat(actualWishes).isNotNull();
        assertThat(actualWishes).hasSize(2);
        assertThat(actualWishes).containsExactlyInAnyOrder(testWish1, testWish2);
        verify(wishRepository, times(1)).findAllByUser_Id(userId);
    }

    @Test
    @DisplayName("getAllWishesByUserId повинен повертати порожній список, якщо бажань немає")
    void getAllWishesByUserId_shouldReturnEmptyList_whenNoWishes() {
        // Arrange
        Long userIdWithNoWishes = 3L;
        when(wishRepository.findAllByUser_Id(userIdWithNoWishes)).thenReturn(List.of());

        // Act
        List<Wish> actualWishes = wishService.getAllWishesByUserId(userIdWithNoWishes);

        // Assert
        assertThat(actualWishes).isNotNull();
        assertThat(actualWishes).isEmpty();
        verify(wishRepository, times(1)).findAllByUser_Id(userIdWithNoWishes);
    }

    // --- Тести для getWishById ---
    // SecurityContext тут не потрібен

    @Test
    @DisplayName("getWishById повинен повертати бажання, якщо воно існує")
    void getWishById_shouldReturnWish_whenExists() {
        // Arrange
        Long wishId = testWish1.getId();
        when(wishRepository.findById(wishId)).thenReturn(Optional.of(testWish1));

        // Act
        Wish actualWish = wishService.getWishById(wishId);

        // Assert
        assertThat(actualWish).isNotNull();
        assertThat(actualWish.getId()).isEqualTo(wishId);
        assertThat(actualWish.getTitle()).isEqualTo(testWish1.getTitle());
        verify(wishRepository, times(1)).findById(wishId);
    }

    @Test
    @DisplayName("getWishById повинен кидати NoResultException, якщо бажання не існує")
    void getWishById_shouldThrowNoResultException_whenNotExists() {
        // Arrange
        Long nonExistentId = 99L;
        when(wishRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> wishService.getWishById(nonExistentId))
                .isInstanceOf(NoResultException.class)
                .hasMessageContaining("No wish found with ID " + nonExistentId);
        verify(wishRepository, times(1)).findById(nonExistentId);
    }

    // --- Тести для createWish ---
    // Тут потрібен SecurityContext

    @Test
    @DisplayName("createWish повинен встановити користувача з Principal та зберегти бажання")
    void createWish_shouldSetUserAndSave() {
        // Arrange
        mockSecurityContext(testUserPrincipal); // Мокуємо залогованого користувача

        Wish inputWish = new Wish("New Gadget", "Description", "http://new.gadget");
        // Очікуваний об'єкт після встановлення User та збереження (з ID)
        Wish savedWish = new Wish("New Gadget", "Description", "http://new.gadget");
        savedWish.setUser(new User(testUserPrincipal.getId())); // Встановлюємо User ID з principal
        savedWish.setId(13L); // Припускаємо згенерований ID

        ArgumentCaptor<Wish> wishCaptor = ArgumentCaptor.forClass(Wish.class);
        when(wishRepository.save(wishCaptor.capture())).thenReturn(savedWish);

        // Act
        Wish result = wishService.createWish(inputWish);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(13L);
        assertThat(result.getTitle()).isEqualTo("New Gadget");
        assertThat(result.getUser()).isNotNull();
        assertThat(result.getUser().getId()).isEqualTo(testUserPrincipal.getId()); // Перевіряємо User ID у відповіді

        // Перевіряємо об'єкт, переданий в save
        Wish capturedWish = wishCaptor.getValue();
        assertThat(capturedWish.getUser()).isNotNull();
        assertThat(capturedWish.getUser().getId()).isEqualTo(testUserPrincipal.getId()); // Перевіряємо, чи встановлено User ID
        assertThat(capturedWish.getTitle()).isEqualTo("New Gadget");
        assertThat(capturedWish.getDescription()).isEqualTo("Description");
        assertThat(capturedWish.getUrl()).isEqualTo("http://new.gadget");

        verify(wishRepository, times(1)).save(any(Wish.class));
        verify(securityContext, atLeastOnce()).getAuthentication(); // Перевірка, що контекст використовувався
    }

    @Test
    @DisplayName("createWish повинен кидати виняток, якщо користувач не автентифікований")
    void createWish_shouldThrowException_whenNotAuthenticated() {
        // Arrange
        mockSecurityContext(null); // Немає автентифікації
        Wish inputWish = new Wish("Unauthorized", "Desc", null);

        // Act & Assert
        assertThatThrownBy(() -> wishService.createWish(inputWish))
                .isInstanceOf(NullPointerException.class) // Очікуємо від UserService.getUserPrincipal()
                .hasMessageContaining("User is not authenticated");

        verify(wishRepository, never()).save(any(Wish.class)); // Збереження не має відбутися
        verify(securityContext, atLeastOnce()).getAuthentication();
    }

    // --- Тести для deleteWishById ---
    // Тут потрібен SecurityContext

    @Test
    @DisplayName("deleteWishById повинен видалити бажання, якщо користувач є власником")
    void deleteWishById_shouldDelete_whenUserIsOwner() {
        // Arrange
        mockSecurityContext(testUserPrincipal); // Мокуємо власника
        Long wishIdToDelete = testWish1.getId();

        when(wishRepository.findById(wishIdToDelete)).thenReturn(Optional.of(testWish1)); // Знаходимо бажання
        doNothing().when(wishRepository).delete(any(Wish.class)); // Налаштовуємо мок для delete

        ArgumentCaptor<Wish> wishCaptor = ArgumentCaptor.forClass(Wish.class);

        // Act
        wishService.deleteWishById(wishIdToDelete);

        // Assert
        verify(wishRepository, times(1)).findById(wishIdToDelete);
        verify(wishRepository, times(1)).delete(wishCaptor.capture()); // Перевіряємо виклик delete
        assertThat(wishCaptor.getValue().getId()).isEqualTo(wishIdToDelete); // Перевіряємо, що видаляється правильний об'єкт
        verify(securityContext, atLeastOnce()).getAuthentication();
    }

    @Test
    @DisplayName("deleteWishById повинен кидати AccessDeniedException, якщо користувач не є власником")
    void deleteWishById_shouldThrowAccessDenied_whenUserIsNotOwner() {
        // Arrange
        mockSecurityContext(testUserPrincipal); // Користувач 1 намагається видалити
        Long wishIdToDelete = otherUserWish.getId(); // Бажання іншого користувача (User 2)

        when(wishRepository.findById(wishIdToDelete)).thenReturn(Optional.of(otherUserWish)); // Знаходимо бажання

        // Act & Assert
        assertThatThrownBy(() -> wishService.deleteWishById(wishIdToDelete))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access to delete is denied.");

        verify(wishRepository, times(1)).findById(wishIdToDelete);
        verify(wishRepository, never()).delete(any(Wish.class)); // Видалення не має відбутися
        verify(securityContext, atLeastOnce()).getAuthentication();
    }

    @Test
    @DisplayName("deleteWishById повинен кидати NoResultException, якщо бажання не знайдено")
    void deleteWishById_shouldThrowNoResultException_whenWishNotFound() {
        // Arrange
        mockSecurityContext(testUserPrincipal); // Не важливо хто, бо помилка раніше
        Long nonExistentId = 99L;
        when(wishRepository.findById(nonExistentId)).thenReturn(Optional.empty()); // Не знаходимо бажання

        // Act & Assert
        assertThatThrownBy(() -> wishService.deleteWishById(nonExistentId))
                .isInstanceOf(NoResultException.class)
                .hasMessageContaining("No wish found with ID " + nonExistentId);

        verify(wishRepository, times(1)).findById(nonExistentId);
        verify(wishRepository, never()).delete(any(Wish.class));
        // SecurityContext може не викликатись, бо помилка виникає до перевірки прав
        // verify(securityContext, never()).getAuthentication();
    }

    // --- Тести для updateWish ---
    // Тут потрібен SecurityContext

    @Test
    @DisplayName("updateWish повинен оновити поля та викликати save, якщо користувач є власником")
    void updateWish_shouldUpdateFieldsAndSave_whenUserIsOwner() {
        // Arrange
        mockSecurityContext(testUserPrincipal); // Мокуємо власника

        Long wishIdToUpdate = testWish1.getId();
        Wish existingWish = testWish1; // Використовуємо об'єкт з setUp

        Wish updates = new Wish(); // Об'єкт тільки з оновленнями
        updates.setTitle("Updated Laptop");
        updates.setDescription(null); // Не оновлюємо опис
        updates.setUrl("http://new.url");
        updates.setArchived(true); // Оновлюємо статус архівування

        when(wishRepository.findById(wishIdToUpdate)).thenReturn(Optional.of(existingWish));
        // Мок save повертає аргумент, щоб перевірити зміни перед збереженням
        when(wishRepository.save(any(Wish.class))).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<Wish> wishCaptor = ArgumentCaptor.forClass(Wish.class);

        // Act
        Wish result = wishService.updateWish(wishIdToUpdate, updates);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(wishIdToUpdate);
        assertThat(result.getTitle()).isEqualTo("Updated Laptop"); // Перевірка оновлених полів у відповіді
        assertThat(result.getDescription()).isEqualTo(existingWish.getDescription()); // Опис не змінився
        assertThat(result.getUrl()).isEqualTo("http://new.url");
        assertThat(result.isArchived()).isTrue();
        // assertThat(result.getUpdatedAt()).isNotNull(); // Перевіряємо, чи встановлено updatedAt (хоча воно залежить від Instant.now())

        // Перевірка викликів
        verify(wishRepository, times(1)).findById(wishIdToUpdate);
        verify(wishRepository, times(1)).save(wishCaptor.capture()); // Перевірка виклику save
        verify(securityContext, atLeastOnce()).getAuthentication();

        // Перевірка об'єкта, переданого в save
        Wish capturedWish = wishCaptor.getValue();
        assertThat(capturedWish.getTitle()).isEqualTo("Updated Laptop");
        assertThat(capturedWish.getDescription()).isEqualTo(existingWish.getDescription());
        assertThat(capturedWish.getUrl()).isEqualTo("http://new.url");
        assertThat(capturedWish.isArchived()).isTrue();
        assertThat(capturedWish.getUpdatedAt()).isNotNull(); // Логіка в сервісі мала б встановити updatedAt
        assertThat(capturedWish.getUser().getId()).isEqualTo(testUserPrincipal.getId()); // Користувач не змінився
    }

     @Test
    @DisplayName("updateWish повинен викликати save, але не оновлювати поля, якщо оновлення порожні або null")
    void updateWish_shouldCallSaveButNotUpdate_whenUpdatesAreBlankOrNull() {
        // Arrange
        mockSecurityContext(testUserPrincipal);

        Long wishIdToUpdate = testWish1.getId();
        // Створюємо копію, щоб оригінальний testWish1 не змінився
        Wish existingWish = new Wish(testWish1.getTitle(), testWish1.getDescription(), testWish1.getUrl());
        existingWish.setId(testWish1.getId());
        existingWish.setUser(testWish1.getUser());
        existingWish.setCreatedAt(testWish1.getCreatedAt());
        existingWish.setArchived(testWish1.isArchived());
        existingWish.setUpdatedAt(null); // Припустимо, updatedAt спочатку null

        Wish updates = new Wish();
        updates.setTitle("   "); // Порожній рядок
        updates.setDescription(null);
        updates.setUrl(""); // Порожній рядок
        // isArchived = false (не змінюємо, бо в existingWish теж false)

        when(wishRepository.findById(wishIdToUpdate)).thenReturn(Optional.of(existingWish));
        when(wishRepository.save(any(Wish.class))).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<Wish> wishCaptor = ArgumentCaptor.forClass(Wish.class);

        // Act
        Wish result = wishService.updateWish(wishIdToUpdate, updates);

        // Assert
        assertThat(result).isNotNull();
        // Поля в результаті не мають змінитися
        assertThat(result.getTitle()).isEqualTo(testWish1.getTitle());
        assertThat(result.getDescription()).isEqualTo(testWish1.getDescription());
        assertThat(result.getUrl()).isEqualTo(testWish1.getUrl());
        assertThat(result.isArchived()).isEqualTo(testWish1.isArchived());
        // updatedAt не має бути встановлено логікою всередині updateWish (бо isUpdated = false)
        assertThat(result.getUpdatedAt()).isNull();

        // Перевірка викликів
        verify(wishRepository, times(1)).findById(wishIdToUpdate);
        verify(wishRepository, times(1)).save(wishCaptor.capture()); // Save все одно викликається
        verify(securityContext, atLeastOnce()).getAuthentication();

        // Перевірка об'єкта, переданого в save
        Wish capturedWish = wishCaptor.getValue();
        assertThat(capturedWish.getTitle()).isEqualTo(testWish1.getTitle());
        assertThat(capturedWish.getDescription()).isEqualTo(testWish1.getDescription());
        assertThat(capturedWish.getUrl()).isEqualTo(testWish1.getUrl());
        assertThat(capturedWish.isArchived()).isEqualTo(testWish1.isArchived());
        assertThat(capturedWish.getUpdatedAt()).isNull(); // Перевірка, що updatedAt не встановлено
    }


    @Test
    @DisplayName("updateWish повинен кидати AccessDeniedException, якщо користувач не є власником")
    void updateWish_shouldThrowAccessDenied_whenUserIsNotOwner() {
        // Arrange
        mockSecurityContext(testUserPrincipal); // Користувач 1
        Long wishIdToUpdate = otherUserWish.getId(); // Бажання користувача 2

        Wish updates = new Wish();
        updates.setTitle("Illegal Update");

        when(wishRepository.findById(wishIdToUpdate)).thenReturn(Optional.of(otherUserWish));

        // Act & Assert
        assertThatThrownBy(() -> wishService.updateWish(wishIdToUpdate, updates))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Change access is prohibited.");

        verify(wishRepository, times(1)).findById(wishIdToUpdate);
        verify(wishRepository, never()).save(any(Wish.class)); // Save не має викликатись
        verify(securityContext, atLeastOnce()).getAuthentication();
    }

    @Test
    @DisplayName("updateWish повинен кидати NoResultException, якщо бажання не знайдено")
    void updateWish_shouldThrowNoResultException_whenWishNotFound() {
        // Arrange
        mockSecurityContext(testUserPrincipal); // Не важливо хто
        Long nonExistentId = 99L;
        Wish updates = new Wish();
        updates.setTitle("Update Non Existent");

        when(wishRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> wishService.updateWish(nonExistentId, updates))
                .isInstanceOf(NoResultException.class)
                .hasMessageContaining("No wish found with ID " + nonExistentId);

        verify(wishRepository, times(1)).findById(nonExistentId);
        verify(wishRepository, never()).save(any(Wish.class));
    }
}