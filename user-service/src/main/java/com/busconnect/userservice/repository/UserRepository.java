package com.busconnect.userservice.repository;

import com.busconnect.userservice.model.User;
import com.busconnect.userservice.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Encuentra un usuario por su email
    Optional<User> findByEmail(String email);

    // Verifica si existe un usuario con un email específico
    boolean existsByEmail(String email);

    // Encuentra usuarios por su rol
    List<User> findByRole(UserRole role);
}
