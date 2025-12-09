package com.busconnect.catalogservice.service;

import com.busconnect.catalogservice.model.Municipality;
import com.busconnect.catalogservice.repository.MunicipalityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MunicipalityService {

    private final MunicipalityRepository municipalityRepository;

    /**
     * Obtener todos los municipios activos (con cache)
     */
    @Cacheable("municipalities")
    public Flux<Municipality> getAllActive() {
        log.debug("Obteniendo todos los municipios activos");
        return municipalityRepository.findAllActive()
                .doOnSubscribe(subscription -> log.info("Consultando municipios activos"))
                .doOnNext(municipality -> log.debug("Municipio encontrado: {}", municipality.getName()))
                .doOnComplete(() -> log.debug("Consulta de municipios completada"));
    }

    /**
     * Buscar municipio por nombre exacto
     */
    public Mono<Municipality> findByName(String name) {
        log.debug("Buscando municipio por nombre: {}", name);
        return municipalityRepository.findByNameIgnoreCase(name)
                .doOnSuccess(municipality -> {
                    if (municipality != null) {
                        log.debug("Municipio encontrado: {}", municipality.getName());
                    } else {
                        log.debug("Municipio no encontrado: {}", name);
                    }
                });
    }

    /**
     * Buscar municipios similares (para sugerencias)
     */
    public Flux<Municipality> searchByName(String name) {
        log.debug("Búsqueda de municipios similares a: {}", name);
        return municipalityRepository.findSimilarByName(name)
                .doOnNext(municipality -> log.debug("Municipio similar encontrado: {}", municipality.getName()))
                .doOnComplete(() -> log.debug("Búsqueda de similares completada para: {}", name));
    }

    /**
     * Obtener municipios por provincia
     */
    public Flux<Municipality> getByProvince(String province) {
        log.debug("Obteniendo municipios de la provincia: {}", province);
        return municipalityRepository.findByProvinceIgnoreCase(province)
                .doOnSubscribe(subscription -> log.info("Consultando municipios de provincia: {}", province))
                .doOnNext(municipality -> log.debug("Municipio de {} encontrado: {}", province, municipality.getName()))
                .doOnComplete(() -> log.debug("Consulta completada para provincia: {}", province));
    }

    /**
     * Buscar municipio por ID
     */
    public Mono<Municipality> findById(UUID id) {
        log.debug("Buscando municipio por ID: {}", id);
        return municipalityRepository.findById(id)
                .doOnSuccess(municipality -> {
                    if (municipality != null) {
                        log.debug("Municipio encontrado por ID: {}", municipality.getName());
                    } else {
                        log.debug("Municipio no encontrado por ID: {}", id);
                    }
                });
    }

    /**
     * Verificar si un municipio existe por coordenadas
     */
    public Mono<Boolean> existsByCoordinates(Double latitude, Double longitude) {
        log.debug("Verificando municipio por coordenadas: {}, {}", latitude, longitude);
        return municipalityRepository.existsByCoordinates(latitude, longitude)
                .doOnSuccess(exists -> log.debug("Municipio existe en coordenadas {}, {}: {}", 
                    latitude, longitude, exists));
    }

    /**
     * Crear o actualizar municipio
     */
    public Mono<Municipality> save(Municipality municipality) {
        log.info("Guardando municipio: {}", municipality.getName());
        return municipalityRepository.save(municipality)
                .doOnSuccess(saved -> log.info("Municipio guardado exitosamente: {} (ID: {})", 
                    saved.getName(), saved.getId()))
                .doOnError(error -> log.error("Error guardando municipio {}: {}", 
                    municipality.getName(), error.getMessage()));
    }

    /**
     * Eliminar municipio (soft delete)
     */
    public Mono<Municipality> deactivate(UUID id) {
        log.info("Desactivando municipio con ID: {}", id);
        return municipalityRepository.findById(id)
                .flatMap(municipality -> {
                    municipality.setActive(false);
                    return municipalityRepository.save(municipality);
                })
                .doOnSuccess(municipality -> log.info("Municipio desactivado: {}", municipality.getName()))
                .doOnError(error -> log.error("Error desactivando municipio {}: {}", id, error.getMessage()));
    }

    /**
     * Validar que un municipio pertenece a Catalunya
     */
    public Mono<Boolean> isValidCatalunya(String municipalityName) {
        log.debug("Validando si {} pertenece a Catalunya", municipalityName);
        return findByName(municipalityName)
                .map(municipality -> municipality != null)
                .defaultIfEmpty(false)
                .doOnSuccess(isValid -> log.debug("Validación Catalunya para {}: {}", municipalityName, isValid));
    }

    /**
     * Obtener sugerencias de municipios para corrección de errores tipográficos
     */
    public Flux<String> getSuggestions(String incorrectName) {
        log.debug("Obteniendo sugerencias para: {}", incorrectName);
        return municipalityRepository.findSimilarByName(incorrectName)
                .map(Municipality::getName)
                .take(5)  // Máximo 5 sugerencias
                .doOnNext(suggestion -> log.debug("Sugerencia encontrada: {}", suggestion))
                .doOnComplete(() -> log.debug("Sugerencias completadas para: {}", incorrectName));
    }
}