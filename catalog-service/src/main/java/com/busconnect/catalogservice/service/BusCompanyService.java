package com.busconnect.catalogservice.service;

import com.busconnect.catalogservice.model.BusCompany;
import com.busconnect.catalogservice.repository.BusCompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Servicio para la gestión de empresas de autobuses.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BusCompanyService {

    private final BusCompanyRepository busCompanyRepository;

    /**
     * Obtiene todas las empresas activas.
     */
    public Flux<BusCompany> getAllActive() {
        log.debug("Obteniendo todas las empresas activas");
        return busCompanyRepository.findAllActive()
                .doOnSubscribe(subscription -> log.info("Consultando empresas activas"))
                .doOnComplete(() -> log.debug("Consulta de empresas completada"));
    }

    /**
     * Busca una empresa por ID.
     */
    public Mono<BusCompany> findById(UUID id) {
        log.debug("Buscando empresa por ID: {}", id);
        return busCompanyRepository.findById(id)
                .doOnSuccess(company -> {
                    if (company != null) {
                        log.debug("Empresa encontrada: {}", company.getName());
                    } else {
                        log.debug("Empresa no encontrada con ID: {}", id);
                    }
                });
    }

    /**
     * Busca una empresa por CIF.
     */
    public Mono<BusCompany> findByCif(String cif) {
        log.debug("Buscando empresa por CIF: {}", cif);
        return busCompanyRepository.findByCif(cif)
                .doOnSuccess(company -> {
                    if (company != null) {
                        log.debug("Empresa encontrada por CIF: {}", company.getName());
                    }
                });
    }

    /**
     * Busca empresas por nombre (búsqueda parcial).
     */
    public Flux<BusCompany> searchByName(String name) {
        log.debug("Buscando empresas por nombre: {}", name);
        return busCompanyRepository.searchByName(name)
                .doOnComplete(() -> log.debug("Búsqueda por nombre completada"));
    }

    /**
     * Busca empresas por ciudad.
     */
    public Flux<BusCompany> findByCity(String city) {
        log.debug("Buscando empresas en ciudad: {}", city);
        return busCompanyRepository.findByCity(city)
                .doOnComplete(() -> log.debug("Búsqueda por ciudad completada"));
    }

    /**
     * Guarda una empresa.
     */
    public Mono<BusCompany> save(BusCompany company) {
        log.info("Guardando empresa: {}", company.getName());
        return busCompanyRepository.save(company)
                .doOnSuccess(saved -> log.info("Empresa guardada exitosamente: {} (ID: {})",
                        saved.getName(), saved.getId()))
                .doOnError(error -> log.error("Error guardando empresa {}: {}",
                        company.getName(), error.getMessage()));
    }

    /**
     * Desactiva una empresa (soft delete).
     */
    public Mono<BusCompany> deactivate(UUID id) {
        log.info("Desactivando empresa con ID: {}", id);
        return busCompanyRepository.findById(id)
                .flatMap(company -> {
                    company.setActive(false);
                    return busCompanyRepository.save(company);
                })
                .doOnSuccess(company -> {
                    if (company != null) {
                        log.info("Empresa desactivada: {}", company.getName());
                    }
                });
    }

    /**
     * Cuenta el total de empresas activas.
     */
    public Mono<Long> countActive() {
        return busCompanyRepository.countActive()
                .doOnSuccess(count -> log.debug("Total empresas activas: {}", count));
    }

    /**
     * Verifica si existe una empresa con el CIF dado.
     */
    public Mono<Boolean> existsByCif(String cif) {
        return busCompanyRepository.existsByCif(cif);
    }
}
