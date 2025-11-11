package com.busconnect.repository;

import com.busconnect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Buscar usuario por email
    Optional<User> findByEmail(String email);

    // Verificar si un email ya está registrado
    boolean existsByEmail(String email);

    // Busca usuario que existe, está ACTIVO y NO está BLOQUEADO.
    Optional<User> findByEmailAndIsActiveTrueAndIsLockedFalse(String email);

    // Buscar usuarios bloqueados
    Optional<User> findByEmailAndIsLockedTrue(String email);

    // Buscar solo usuarios activos
    Optional<User> findByEmailAndisActiveTrue(String email);

    // Buscar usuarios con intentos fallidos mayores o iguales a un número determinado
    List<User> findByFailedLoginAttemptsGreaterThanEqual(int attempts);

    // Buscar usuarios bloqueados y activos (por si quieres lógica de desbloqueo)
    List<User> findByLockedTrueAndIsActiveTrue();
}
