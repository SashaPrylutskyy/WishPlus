package com.sashaprylutsky.wishplus.repository;

import com.sashaprylutsky.wishplus.model.User;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findUserByUsername(String username);

    List<User> findUsersByUsernameStartsWith(String user_prefix);
}
