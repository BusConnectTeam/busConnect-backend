package com.busconnect.catalogservice.repository;

import com.busconnect.catalogservice.model.Driver;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Repositorio para operaciones de base de datos de conductores.
 */
@Repository
public interface DriverRepository extends R2dbcRepository<Driver, UUID> {

    /**
     * Busca todos los conductores activos ordenados por apellido.
     */
    @Query("SELECT * FROM catalog.drivers WHERE is_active = true ORDER BY last_name, first_name")
    Flux<Driver> findAllActive();

    /**
     * Busca conductores por empresa.
     */
    @Query("SELECT * FROM catalog.drivers WHERE company_id = :companyId AND is_active = true ORDER BY last_name, first_name")
    Flux<Driver> findByCompanyId(@Param("companyId") UUID companyId);

    /**
     * Busca un conductor por DNI.
     */
    @Query("SELECT * FROM catalog.drivers WHERE dni = :dni AND is_active = true")
    Mono<Driver> findByDni(@Param("dni") String dni);

    /**
     * Busca conductores por tipo de licencia.
     */
    @Query("SELECT * FROM catalog.drivers WHERE license_type = :licenseType AND is_active = true ORDER BY last_name")
    Flux<Driver> findByLicenseType(@Param("licenseType") String licenseType);

    /**
     * Busca conductores con licencia que expira antes de una fecha.
     */
    @Query("SELECT * FROM catalog.drivers WHERE license_expiry_date <= :expiryDate AND is_active = true ORDER BY license_expiry_date")
    Flux<Driver> findByLicenseExpiringBefore(@Param("expiryDate") LocalDate expiryDate);

    /**
     * Busca conductores con licencia válida (no expirada).
     */
    @Query("SELECT * FROM catalog.drivers WHERE license_expiry_date > :today AND is_active = true ORDER BY last_name")
    Flux<Driver> findWithValidLicense(@Param("today") LocalDate today);

    /**
     * Busca conductores por experiencia mínima.
     */
    @Query("SELECT * FROM catalog.drivers WHERE years_experience >= :minYears AND is_active = true ORDER BY years_experience DESC")
    Flux<Driver> findByMinExperience(@Param("minYears") Integer minYears);

    /**
     * Busca conductores que hablan un idioma específico.
     */
    @Query("SELECT * FROM catalog.drivers WHERE languages LIKE CONCAT('%', :language, '%') AND is_active = true ORDER BY last_name")
    Flux<Driver> findByLanguage(@Param("language") String language);

    /**
     * Busca conductores por nombre (búsqueda parcial).
     */
    @Query("""
        SELECT * FROM catalog.drivers
        WHERE (LOWER(first_name) LIKE LOWER(CONCAT('%', :name, '%'))
           OR LOWER(last_name) LIKE LOWER(CONCAT('%', :name, '%')))
        AND is_active = true
        ORDER BY last_name, first_name
        """)
    Flux<Driver> searchByName(@Param("name") String name);

    /**
     * Verifica si existe un conductor con el DNI dado.
     */
    @Query("SELECT COUNT(*) > 0 FROM catalog.drivers WHERE dni = :dni")
    Mono<Boolean> existsByDni(@Param("dni") String dni);

    /**
     * Cuenta conductores por empresa.
     */
    @Query("SELECT COUNT(*) FROM catalog.drivers WHERE company_id = :companyId AND is_active = true")
    Mono<Long> countByCompanyId(@Param("companyId") UUID companyId);

    /**
     * Cuenta conductores con licencia próxima a expirar (en los próximos días).
     */
    @Query("SELECT COUNT(*) FROM catalog.drivers WHERE license_expiry_date <= :warningDate AND license_expiry_date > :today AND is_active = true")
    Mono<Long> countWithExpiringLicense(@Param("today") LocalDate today, @Param("warningDate") LocalDate warningDate);
}
