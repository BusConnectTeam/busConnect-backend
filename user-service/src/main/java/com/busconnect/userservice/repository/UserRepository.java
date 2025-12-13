package com.busconnect.userservice.repository;

import com.busconnect.userservice.model.User;
import com.busconnect.userservice.model.UserRole;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public interface UserRepository extends R2dbcRepository <User, Long> {

    /**
     * Busca un usuario por su email.
     * @param email el correo electrónico del usuario.
     * @return un Mono que emite el usuario si se encuentra, o vacío si no existe.
     */
   @Query("SELECT * FROM users WHERE email = :email")
   Mono<User> findByEmail(String email);

    /**
    *Verifica si existe un usuario con un email específico.
    * @param email el correo electrónico a verificar.
    * @return un Mono que emite true si el usuario existe, false en caso contrario.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE email = :email)")
    Mono<Boolean> existsByEmail(String email);

    /**
     * Encuentra usuarios por su rol.
     * @param role el rol del usuario.
     * @return un Flux que emite los usuarios con el rol especificado.
     */
    @Query("SELECT * FROM users WHERE role = :role")
    Flux<User> findByRole(UserRole role);

    /**
     * Obtiene todos los usuarios activos.
     *
     * @return un flujo de usuarios activos.
     */
    @Query("SELECT * FROM users WHERE active = true")
    Flux<User> findAllActive();

    /**
     * Elimina lógicamente (soft delete) un usuario marcándolo como inactivo.
     *
     * @param id identificador del usuario.
     * @return un Mono indicando el número de registros actualizados.
     */
    @Query("UPDATE users SET active = false WHERE id = :id")
    Mono<Integer> softDeleteById(Long id);

}

