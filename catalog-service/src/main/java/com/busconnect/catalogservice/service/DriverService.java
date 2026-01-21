package com.busconnect.catalogservice.service;

import com.busconnect.catalogservice.model.Driver;
import com.busconnect.catalogservice.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Servicio para la gestión de conductores.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DriverService {

    private final DriverRepository driverRepository;

    /**
     * Obtiene todos los conductores activos.
     */
    public Flux<Driver> getAllActive() {
        log.debug("Obteniendo todos los conductores activos");
        return driverRepository.findAllActive()
                .doOnSubscribe(subscription -> log.info("Consultando conductores activos"))
                .doOnComplete(() -> log.debug("Consulta de conductores completada"));
    }

    /**
     * Busca un conductor por ID.
     */
    public Mono<Driver> findById(UUID id) {
        log.debug("Buscando conductor por ID: {}", id);
        return driverRepository.findById(id)
                .doOnSuccess(driver -> {
                    if (driver != null) {
                        log.debug("Conductor encontrado: {}", driver.getFullName());
                    } else {
                        log.debug("Conductor no encontrado con ID: {}", id);
                    }
                });
    }

    /**
     * Busca conductores por empresa.
     */
    public Flux<Driver> findByCompanyId(UUID companyId) {
        log.debug("Buscando conductores para empresa: {}", companyId);
        return driverRepository.findByCompanyId(companyId)
                .doOnComplete(() -> log.debug("Búsqueda por empresa completada"));
    }

    /**
     * Busca un conductor por DNI.
     */
    public Mono<Driver> findByDni(String dni) {
        log.debug("Buscando conductor por DNI: {}", dni);
        return driverRepository.findByDni(dni)
                .doOnSuccess(driver -> {
                    if (driver != null) {
                        log.debug("Conductor encontrado por DNI: {}", driver.getFullName());
                    }
                });
    }

    /**
     * Busca conductores por nombre (búsqueda parcial).
     */
    public Flux<Driver> searchByName(String name) {
        log.debug("Buscando conductores por nombre: {}", name);
        return driverRepository.searchByName(name)
                .doOnComplete(() -> log.debug("Búsqueda por nombre completada"));
    }

    /**
     * Busca conductores por tipo de licencia.
     */
    public Flux<Driver> findByLicenseType(String licenseType) {
        log.debug("Buscando conductores con licencia tipo: {}", licenseType);
        return driverRepository.findByLicenseType(licenseType);
    }

    /**
     * Busca conductores con licencia válida.
     */
    public Flux<Driver> findWithValidLicense() {
        log.debug("Buscando conductores con licencia válida");
        return driverRepository.findWithValidLicense(LocalDate.now());
    }

    /**
     * Busca conductores con licencia que expira pronto (en los próximos 90 días).
     */
    public Flux<Driver> findWithExpiringLicense() {
        LocalDate warningDate = LocalDate.now().plusDays(90);
        log.debug("Buscando conductores con licencia que expira antes de: {}", warningDate);
        return driverRepository.findByLicenseExpiringBefore(warningDate);
    }

    /**
     * Busca conductores por experiencia mínima.
     */
    public Flux<Driver> findByMinExperience(Integer minYears) {
        log.debug("Buscando conductores con mínimo {} años de experiencia", minYears);
        return driverRepository.findByMinExperience(minYears);
    }

    /**
     * Busca conductores que hablan un idioma específico.
     */
    public Flux<Driver> findByLanguage(String language) {
        log.debug("Buscando conductores que hablan: {}", language);
        return driverRepository.findByLanguage(language);
    }

    /**
     * Guarda un conductor.
     */
    public Mono<Driver> save(Driver driver) {
        log.info("Guardando conductor: {}", driver.getFullName());
        return driverRepository.save(driver)
                .doOnSuccess(saved -> log.info("Conductor guardado exitosamente: {} (ID: {})",
                        saved.getFullName(), saved.getId()))
                .doOnError(error -> log.error("Error guardando conductor {}: {}",
                        driver.getFullName(), error.getMessage()));
    }

    /**
     * Desactiva un conductor (soft delete).
     */
    public Mono<Driver> deactivate(UUID id) {
        log.info("Desactivando conductor con ID: {}", id);
        return driverRepository.findById(id)
                .flatMap(driver -> {
                    driver.setActive(false);
                    return driverRepository.save(driver);
                })
                .doOnSuccess(driver -> {
                    if (driver != null) {
                        log.info("Conductor desactivado: {}", driver.getFullName());
                    }
                });
    }

    /**
     * Cuenta conductores por empresa.
     */
    public Mono<Long> countByCompanyId(UUID companyId) {
        return driverRepository.countByCompanyId(companyId)
                .doOnSuccess(count -> log.debug("Total conductores para empresa {}: {}", companyId, count));
    }

    /**
     * Cuenta conductores con licencia próxima a expirar.
     */
    public Mono<Long> countWithExpiringLicense() {
        LocalDate today = LocalDate.now();
        LocalDate warningDate = today.plusDays(90);
        return driverRepository.countWithExpiringLicense(today, warningDate)
                .doOnSuccess(count -> log.debug("Conductores con licencia por expirar: {}", count));
    }

    /**
     * Verifica si existe un conductor con el DNI dado.
     */
    public Mono<Boolean> existsByDni(String dni) {
        return driverRepository.existsByDni(dni);
    }
}
