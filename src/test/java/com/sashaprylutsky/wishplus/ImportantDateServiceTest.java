package com.sashaprylutsky.wishplus;

import com.sashaprylutsky.wishplus.model.ImportantDate;
import com.sashaprylutsky.wishplus.model.User;
import com.sashaprylutsky.wishplus.model.UserPrincipal;
import com.sashaprylutsky.wishplus.repository.ImportantDateRepository;
import com.sashaprylutsky.wishplus.service.ImportantDateService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;


import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportantDateServiceTest {

    @Mock
    private ImportantDateRepository importantDateRepository;

    @Mock
    private UserService userService;

    // Моки для SecurityContext потрібні тільки в певних тестах
    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ImportantDateService importantDateService;

    private User testUser;
    private UserPrincipal testUserPrincipal;
    private ImportantDate testDate1;
    private ImportantDate testDate2;
    private Date dateNow;

    // Допоміжний метод для мокування SecurityContext
    // Викликається тільки в тих тестах, де потрібна перевірка Principal
    private void mockSecurityContext(UserPrincipal principal) {
        if (principal == null) {
            // Симуляція відсутності автентифікації або principal не є UserPrincipal
            // Authentication може бути не null (Anonymous), але principal може бути не UserPrincipal,
            // або isAuthenticated() може бути false.
             when(securityContext.getAuthentication()).thenReturn(authentication);
             // Переконуємося, що isAuthenticated() повертає false, якщо principal null або не UserPrincipal
             when(authentication.isAuthenticated()).thenReturn(false);
             // Можна також мокнути getPrincipal() щоб повернути щось інше, але перевірка isAuthenticated достатня
             // when(authentication.getPrincipal()).thenReturn("anonymousUser"); // наприклад
        } else {
            // Симуляція успішної автентифікації з UserPrincipal
            Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            when(authentication.getPrincipal()).thenReturn(principal);
            when(authentication.isAuthenticated()).thenReturn(true); // Користувач автентифікований
            when(securityContext.getAuthentication()).thenReturn(authentication);
        }
        // Встановлюємо налаштований securityContext в SecurityContextHolder
        SecurityContextHolder.setContext(securityContext);
    }


    @BeforeEach
    void setUp() {
        // Ініціалізація тестових даних
        testUser = new User("test@example.com", "testuser", "password", "Test", "User");
        testUser.setId(1L);

        testUserPrincipal = new UserPrincipal(1L, "test@example.com", "testuser", "encodedPassword");

        dateNow = new Date();

        testDate1 = new ImportantDate(testUser, "Birthday", dateNow);
        testDate1.setId(101L);

        testDate2 = new ImportantDate(testUser, "Anniversary", dateNow);
        testDate2.setId(102L);

        // НЕ викликаємо mockSecurityContext тут, бо не всі тести його потребують
    }

    @AfterEach
    void tearDown() {
        // Очищення SecurityContextHolder після кожного тесту є ОБОВ'ЯЗКОВИМ
        SecurityContextHolder.clearContext();
    }

    // --- Тести для getRecordsByUserId ---
    // Ці методи не використовують SecurityContext, тому моки не потрібні

    @Test
    @DisplayName("getRecordsByUserId повинен повернути список дат для існуючого User ID")
    void getRecordsByUserId_shouldReturnDates_whenUserExists() {
        // Arrange
        Long userId = 1L;
        List<ImportantDate> expectedDates = List.of(testDate1, testDate2);
        when(importantDateRepository.findAllByUser_Id(userId)).thenReturn(expectedDates);

        // Act
        List<ImportantDate> actualDates = importantDateService.getRecordsByUserId(userId);

        // Assert
        assertThat(actualDates).isNotNull();
        assertThat(actualDates).hasSize(2);
        assertThat(actualDates).containsExactlyInAnyOrder(testDate1, testDate2);
        verify(importantDateRepository, times(1)).findAllByUser_Id(userId);
    }

    @Test
    @DisplayName("getRecordsByUserId повинен повернути порожній список, якщо дати не знайдені")
    void getRecordsByUserId_shouldReturnEmptyList_whenNoDatesFound() {
        // Arrange
        Long userId = 2L;
        when(importantDateRepository.findAllByUser_Id(userId)).thenReturn(List.of());

        // Act
        List<ImportantDate> actualDates = importantDateService.getRecordsByUserId(userId);

        // Assert
        assertThat(actualDates).isNotNull();
        assertThat(actualDates).isEmpty();
        verify(importantDateRepository, times(1)).findAllByUser_Id(userId);
    }

    // --- Тести для createRecord ---
    // Ці методи ВИКОРИСТОВУЮТЬ SecurityContext

    @Test
    @DisplayName("createRecord повинен встановити користувача з Principal і зберегти запис")
    void createRecord_shouldSetUserFromPrincipalAndSave() {
        // Arrange
        mockSecurityContext(testUserPrincipal); // Налаштовуємо SecurityContext тут

        ImportantDate newDateInput = new ImportantDate(null, "New Event", dateNow);
        ImportantDate savedDate = new ImportantDate(new User(testUserPrincipal.getId()), "New Event", dateNow);
        savedDate.setId(103L);

        ArgumentCaptor<ImportantDate> dateCaptor = ArgumentCaptor.forClass(ImportantDate.class);
        when(importantDateRepository.save(dateCaptor.capture())).thenReturn(savedDate);

        // Act
        ImportantDate result = importantDateService.createRecord(newDateInput);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(103L);
        assertThat(result.getTitle()).isEqualTo("New Event");
        assertThat(result.getUser()).isNotNull();
        assertThat(result.getUser().getId()).isEqualTo(testUserPrincipal.getId());

        ImportantDate capturedDate = dateCaptor.getValue();
        assertThat(capturedDate.getUser()).isNotNull();
        assertThat(capturedDate.getUser().getId()).isEqualTo(testUserPrincipal.getId());
        assertThat(capturedDate.getTitle()).isEqualTo("New Event");
        assertThat(capturedDate.getDate()).isEqualTo(dateNow);

        verify(importantDateRepository, times(1)).save(any(ImportantDate.class));
        // Перевіряємо, чи викликались моки security (опціонально, але для впевненості)
        verify(securityContext, atLeastOnce()).getAuthentication();
        verify(authentication, atLeastOnce()).getPrincipal();
    }

    @Test
    @DisplayName("createRecord повинен кидати виняток, якщо користувач не автентифікований")
    void createRecord_shouldThrowException_whenUserNotAuthenticated() {
        // Arrange
        mockSecurityContext(null); // Налаштовуємо SecurityContext на відсутність автентифікації
        ImportantDate newDateInput = new ImportantDate(null, "New Event", dateNow);

        // Act & Assert
        assertThatThrownBy(() -> importantDateService.createRecord(newDateInput))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("User is not authenticated");

        verify(importantDateRepository, never()).save(any(ImportantDate.class));
        // Перевіряємо, чи викликались моки security
        verify(securityContext, atLeastOnce()).getAuthentication();
    }

    // --- Тести для getRecordById ---
    // Ці методи не використовують SecurityContext

    @Test
    @DisplayName("getRecordById повинен повернути запис, якщо він існує")
    void getRecordById_shouldReturnDate_whenExists() {
        // Arrange
        Long dateId = 101L;
        when(importantDateRepository.findById(dateId)).thenReturn(Optional.of(testDate1));

        // Act
        ImportantDate result = importantDateService.getRecordById(dateId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(dateId);
        assertThat(result.getTitle()).isEqualTo(testDate1.getTitle());
        verify(importantDateRepository, times(1)).findById(dateId);
    }

    @Test
    @DisplayName("getRecordById повинен кидати NoResultException, якщо запис не знайдено")
    void getRecordById_shouldThrowNoResultException_whenNotFound() {
        // Arrange
        Long dateId = 999L;
        when(importantDateRepository.findById(dateId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> importantDateService.getRecordById(dateId))
                .isInstanceOf(NoResultException.class)
                .hasMessageContaining("No record is being found with id: " + dateId);
        verify(importantDateRepository, times(1)).findById(dateId);
    }

    // --- Тести для updateRecordById ---
    // Ці методи ВИКОРИСТОВУЮТЬ SecurityContext (крім випадку Not Found)

    @Test
    @DisplayName("updateRecordById повинен оновити запис, якщо ID існує і користувач є власником")
    void updateRecordById_shouldUpdateRecord_whenUserIsOwner() {
        // Arrange
        mockSecurityContext(testUserPrincipal); // Налаштовуємо SecurityContext

        Long dateIdToUpdate = 101L;
        ImportantDate existingRecord = new ImportantDate(new User(testUserPrincipal.getId()), "Old Title", dateNow);
        existingRecord.setId(dateIdToUpdate);

        ImportantDate updates = new ImportantDate();
        updates.setTitle("Updated Title");
        Date newDate = new Date(dateNow.getTime() + 10000);
        updates.setDate(newDate);

        when(importantDateRepository.findById(dateIdToUpdate)).thenReturn(Optional.of(existingRecord));
        when(userService.getUserById(testUserPrincipal.getId())).thenReturn(testUser);
        when(importantDateRepository.save(any(ImportantDate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<ImportantDate> dateCaptor = ArgumentCaptor.forClass(ImportantDate.class);

        // Act
        ImportantDate result = importantDateService.updateRecordById(dateIdToUpdate, updates);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(dateIdToUpdate);
        assertThat(result.getTitle()).isEqualTo("Updated Title");
        assertThat(result.getDate()).isEqualTo(newDate);

        verify(importantDateRepository, times(1)).findById(dateIdToUpdate);
        verify(userService, times(1)).getUserById(testUserPrincipal.getId());
        verify(importantDateRepository, times(1)).save(dateCaptor.capture());
        // verify(securityContext, atLeastOnce()).getAuthentication(); // Перевірка виклику Security

        ImportantDate capturedForSave = dateCaptor.getValue();
        assertThat(capturedForSave.getTitle()).isEqualTo("Updated Title");
        assertThat(capturedForSave.getDate()).isEqualTo(newDate);
        assertThat(capturedForSave.getUser().getId()).isEqualTo(testUserPrincipal.getId());
    }

    @Test
    @DisplayName("updateRecordById повинен кидати AccessDeniedException, якщо користувач не є власником")
    void updateRecordById_shouldThrowAccessDeniedException_whenUserIsNotOwner() {
        // Arrange
        mockSecurityContext(testUserPrincipal); // Налаштовуємо SecurityContext для користувача, що намагається оновити

        Long dateIdToUpdate = 101L;
        User otherUser = new User(); otherUser.setId(2L);
        ImportantDate existingRecord = new ImportantDate(otherUser, "Other User Title", dateNow);
        existingRecord.setId(dateIdToUpdate);

        ImportantDate updates = new ImportantDate();
        updates.setTitle("Attempted Update Title");

        when(importantDateRepository.findById(dateIdToUpdate)).thenReturn(Optional.of(existingRecord));
        when(userService.getUserById(otherUser.getId())).thenReturn(otherUser);

        // Act & Assert
        assertThatThrownBy(() -> importantDateService.updateRecordById(dateIdToUpdate, updates))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Change access is prohibited.");

        verify(importantDateRepository, times(1)).findById(dateIdToUpdate);
        verify(userService, times(1)).getUserById(otherUser.getId());
        verify(importantDateRepository, never()).save(any(ImportantDate.class));
        // verify(securityContext, atLeastOnce()).getAuthentication(); // Перевірка виклику Security
    }

    @Test
    @DisplayName("updateRecordById повинен кидати NoResultException, якщо запис не знайдено")
    void updateRecordById_shouldThrowNoResultException_whenRecordNotFound() {
        // Arrange
        // mockSecurityContext НЕ потрібен, бо помилка виникне до перевірки Principal
        Long nonExistentId = 999L;
        ImportantDate updates = new ImportantDate();
        updates.setTitle("Update Title");

        when(importantDateRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> importantDateService.updateRecordById(nonExistentId, updates))
                .isInstanceOf(NoResultException.class)
                .hasMessageContaining("No record is being found with id: " + nonExistentId);

        verify(userService, never()).getUserById(anyLong());
        verify(importantDateRepository, never()).save(any(ImportantDate.class));
        // verify(securityContext, never()).getAuthentication(); // SecurityContext не мав викликатись
    }

    @Test
    @DisplayName("updateRecordById не повинен оновлювати поля, якщо вони null або порожні в updates")
    void updateRecordById_shouldNotUpdateFields_whenUpdatesAreNullOrBlank() {
        // Arrange
        mockSecurityContext(testUserPrincipal); // Налаштовуємо SecurityContext

        Long dateIdToUpdate = 101L;
        Date originalDate = new Date(dateNow.getTime() - 5000);
        ImportantDate existingRecord = new ImportantDate(new User(testUserPrincipal.getId()), "Original Title", originalDate);
        existingRecord.setId(dateIdToUpdate);

        ImportantDate updates = new ImportantDate();
        updates.setTitle("   ");
        updates.setDate(null);

        when(importantDateRepository.findById(dateIdToUpdate)).thenReturn(Optional.of(existingRecord));
        when(userService.getUserById(testUserPrincipal.getId())).thenReturn(testUser);
        when(importantDateRepository.save(any(ImportantDate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<ImportantDate> dateCaptor = ArgumentCaptor.forClass(ImportantDate.class);

        // Act
        ImportantDate result = importantDateService.updateRecordById(dateIdToUpdate, updates);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Original Title");
        assertThat(result.getDate()).isEqualTo(originalDate);

        verify(importantDateRepository, times(1)).save(dateCaptor.capture());
        ImportantDate capturedForSave = dateCaptor.getValue();
        assertThat(capturedForSave.getTitle()).isEqualTo("Original Title");
        assertThat(capturedForSave.getDate()).isEqualTo(originalDate);
        // verify(securityContext, atLeastOnce()).getAuthentication(); // Перевірка виклику Security
    }

    // --- Тести для deleteRecordById ---
    // Ці методи ВИКОРИСТОВУЮТЬ SecurityContext (крім випадку Not Found)

    @Test
    @DisplayName("deleteRecordById повинен видалити запис, якщо ID існує і користувач є власником")
    void deleteRecordById_shouldDeleteRecord_whenUserIsOwner() {
        // Arrange
        mockSecurityContext(testUserPrincipal); // Налаштовуємо SecurityContext

        Long dateIdToDelete = 101L;
        ImportantDate recordToDelete = new ImportantDate(new User(testUserPrincipal.getId()), "TitleToDelete", dateNow);
        recordToDelete.setId(dateIdToDelete);

        when(importantDateRepository.findById(dateIdToDelete)).thenReturn(Optional.of(recordToDelete));
        when(userService.getUserById(testUserPrincipal.getId())).thenReturn(testUser);
        doNothing().when(importantDateRepository).delete(any(ImportantDate.class));

        ArgumentCaptor<ImportantDate> dateCaptor = ArgumentCaptor.forClass(ImportantDate.class);

        // Act
        importantDateService.deleteRecordById(dateIdToDelete);

        // Assert
        verify(importantDateRepository, times(1)).findById(dateIdToDelete);
        verify(userService, times(1)).getUserById(testUserPrincipal.getId());
        verify(importantDateRepository, times(1)).delete(dateCaptor.capture());
        assertThat(dateCaptor.getValue().getId()).isEqualTo(dateIdToDelete);
        // verify(securityContext, atLeastOnce()).getAuthentication(); // Перевірка виклику Security
    }

    @Test
    @DisplayName("deleteRecordById повинен кидати AccessDeniedException, якщо користувач не є власником")
    void deleteRecordById_shouldThrowAccessDeniedException_whenUserIsNotOwner() {
        // Arrange
        mockSecurityContext(testUserPrincipal); // Налаштовуємо SecurityContext

        Long dateIdToDelete = 101L;
        User otherUser = new User(); otherUser.setId(2L);
        ImportantDate recordToDelete = new ImportantDate(otherUser, "Other Title", dateNow);
        recordToDelete.setId(dateIdToDelete);

        when(importantDateRepository.findById(dateIdToDelete)).thenReturn(Optional.of(recordToDelete));
        when(userService.getUserById(otherUser.getId())).thenReturn(otherUser);

        // Act & Assert
        assertThatThrownBy(() -> importantDateService.deleteRecordById(dateIdToDelete))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Change access is prohibited.");

        verify(importantDateRepository, never()).delete(any(ImportantDate.class));
        // verify(securityContext, atLeastOnce()).getAuthentication(); // Перевірка виклику Security
    }

    @Test
    @DisplayName("deleteRecordById повинен кидати NoResultException, якщо запис не знайдено")
    void deleteRecordById_shouldThrowNoResultException_whenRecordNotFound() {
        // Arrange
        // mockSecurityContext НЕ потрібен
        Long nonExistentId = 999L;
        when(importantDateRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        // mockSecurityContext(testUserPrincipal); // Не потрібен

        // Act & Assert
        assertThatThrownBy(() -> importantDateService.deleteRecordById(nonExistentId))
                .isInstanceOf(NoResultException.class)
                .hasMessageContaining("No record is being found with id: " + nonExistentId);

        verify(userService, never()).getUserById(anyLong());
        verify(importantDateRepository, never()).delete(any(ImportantDate.class));
        // verify(securityContext, never()).getAuthentication(); // SecurityContext не мав викликатись
    }
}