package com.gridmind.service;

import com.gridmind.domain.SensorType;
import com.gridmind.domain.SubstationType;
import com.gridmind.domain.TransformerAssetType;
import com.gridmind.dto.ManualTransformerRequestDto;
import com.gridmind.dto.GridProvisioningRequestDto;
import com.gridmind.dto.SubstationDto;
import com.gridmind.dto.TransformerDto;
import com.gridmind.entity.SubstationEntity;
import com.gridmind.entity.SensorTypeCatalogEntity;
import com.gridmind.entity.TransformerEntity;
import com.gridmind.factory.GridComponentFactory;
import com.gridmind.factory.IndustrialGridFactory;
import com.gridmind.factory.UrbanGridFactory;
import com.gridmind.mapper.EntityDtoMapper;
import com.gridmind.repository.SensorRepository;
import com.gridmind.repository.SubstationRepository;
import com.gridmind.repository.SensorTypeCatalogRepository;
import com.gridmind.repository.TransformerRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GridProvisioningService {

    private final SubstationRepository substationRepository;
    private final SensorTypeCatalogRepository sensorTypeCatalogRepository;
    private final TransformerRepository transformerRepository;
    private final SensorRepository sensorRepository;
    private final EntityDtoMapper mapper;
    private final UrbanGridFactory urbanGridFactory;
    private final IndustrialGridFactory industrialGridFactory;

    public GridProvisioningService(SubstationRepository substationRepository,
                                   SensorTypeCatalogRepository sensorTypeCatalogRepository,
                                   TransformerRepository transformerRepository,
                                   SensorRepository sensorRepository,
                                   EntityDtoMapper mapper,
                                   UrbanGridFactory urbanGridFactory,
                                   IndustrialGridFactory industrialGridFactory) {
        this.substationRepository = substationRepository;
        this.sensorTypeCatalogRepository = sensorTypeCatalogRepository;
        this.transformerRepository = transformerRepository;
        this.sensorRepository = sensorRepository;
        this.mapper = mapper;
        this.urbanGridFactory = urbanGridFactory;
        this.industrialGridFactory = industrialGridFactory;
    }

    @Transactional
    public SubstationDto provision(GridProvisioningRequestDto request) {
        if (new HashSet<>(request.sensorTypes()).size() != request.sensorTypes().size()) {
            throw new IllegalArgumentException("La lista de sensores no puede contener tipos duplicados.");
        }

        GridComponentFactory factory = factoryFor(request.type());
        SubstationEntity substation = factory.createSubstation(
                request.name().trim(), request.location().trim());
        TransformerEntity transformer = factory.createTransformer(
                substation, request.transformerName().trim(),
                request.ratingKva(), request.ageYears());
        transformer.setAssetType(TransformerAssetType.TEST);
        for (SensorType sensorType : request.sensorTypes()) {
            SensorTypeCatalogEntity catalogType = sensorTypeCatalogRepository
                    .findByCodeAndActiveTrue(sensorType.name())
                    .orElseThrow(() -> new IllegalStateException(
                            "No hay un tipo de sensor activo en el catálogo para " + sensorType.name() + "."));
            var sensor = factory.createSensor(transformer, sensorType.name());
            sensor.setSensorTypeCatalog(catalogType);
            transformer.getSensors().add(sensor);
        }
        substation.getTransformers().add(transformer);

        return mapper.toDto(substationRepository.save(substation));
    }

    @Transactional
    public TransformerDto provisionTransformer(ManualTransformerRequestDto request) {
        if (request.assetType() == TransformerAssetType.TEST && (request.sensorTypes().isEmpty()
                || request.ratingKva() == null || request.ageYears() == null)) {
            throw new IllegalArgumentException("El transformador simulado requiere potencia, antigüedad y al menos un sensor.");
        }
        if (new HashSet<>(request.sensorTypes()).size() != request.sensorTypes().size()) {
            throw badRequest("La lista de sensores no puede contener tipos duplicados.");
        }

        Map<SensorType, String> sensorCodes = request.sensorCodes() == null
                ? Map.of()
                : request.sensorCodes();
        if (sensorCodes.keySet().stream().anyMatch(type -> !request.sensorTypes().contains(type))) {
            throw badRequest("Solo puedes asignar códigos a los sensores seleccionados.");
        }

        boolean isNewSubstation = !hasText(request.substationId());
        SubstationEntity substation;
        if (isNewSubstation) {
            if (request.substationType() == null || !hasText(request.substationName())
                    || !hasText(request.substationLocation())) {
                throw badRequest("Indica el tipo, nombre y ubicación de la nueva subestación.");
            }
            GridComponentFactory substationFactory = factoryFor(request.substationType());
            substation = substationFactory.createSubstation(
                    request.substationName().trim(), request.substationLocation().trim());
        } else {
            substation = substationRepository.findById(request.substationId().trim())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "La subestación seleccionada no existe."));
        }

        GridComponentFactory factory = factoryFor(substation.getType());
        TransformerEntity transformer = factory.createTransformer(
                substation, request.transformerName().trim(),
                request.ratingKva(), request.ageYears());
        if (hasText(request.transformerCode())) {
            transformer.setCode(request.transformerCode().trim());
        }
        if (transformerRepository.existsByCode(transformer.getCode())) {
            throw conflict("El código del transformador ya está en uso.");
        }
        transformer.setAssetType(request.assetType());
        if (request.assetType() == TransformerAssetType.HARDWARE) {
            transformer.setRatingKva(request.ratingKva());
            transformer.setAgeYears(request.ageYears());
        }

        if (request.assetType() == TransformerAssetType.HARDWARE
                && request.sensorTypes().stream().anyMatch(type -> !hasText(sensorCodes.get(type)))) {
            throw badRequest("Indica un código para cada sensor conectado al hardware.");
        }
        long distinctCodes = sensorCodes.values().stream()
                .filter(GridProvisioningService::hasText)
                .map(String::trim)
                .distinct()
                .count();
        long suppliedCodes = sensorCodes.values().stream()
                .filter(GridProvisioningService::hasText)
                .count();
        if (distinctCodes != suppliedCodes) {
            throw badRequest("Cada sensor debe tener un código diferente.");
        }

        for (SensorType sensorType : request.sensorTypes()) {
            SensorTypeCatalogEntity catalogType = sensorTypeCatalogRepository
                    .findByCodeAndActiveTrue(sensorType.name())
                    .orElseThrow(() -> new IllegalStateException(
                            "No hay un tipo de sensor activo en el catálogo para " + sensorType.name() + "."));
            var sensor = factory.createSensor(transformer, sensorType.name());
            String customCode = sensorCodes.get(sensorType);
            if (hasText(customCode)) {
                sensor.setCode(customCode.trim());
            }
            if (sensorRepository.existsByCode(sensor.getCode())) {
                throw conflict("El código de sensor " + sensor.getCode() + " ya está en uso.");
            }
            sensor.setSensorTypeCatalog(catalogType);
            transformer.getSensors().add(sensor);
        }

        if (isNewSubstation) {
            substation.getTransformers().add(transformer);
            SubstationEntity savedSubstation = substationRepository.save(substation);
            return mapper.toDto(savedSubstation.getTransformers().getFirst());
        }
        return mapper.toDto(transformerRepository.save(transformer));
    }

    @Transactional
    public TransformerDto linkTemperatureSensor(String transformerId, String sensorCode) {
        TransformerEntity transformer = transformerRepository.lockActiveById(transformerId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el transformador activo."));
        if (transformer.getAssetType() != TransformerAssetType.HARDWARE) {
            throw new IllegalArgumentException("Solo puedes vincular sensores físicos a transformadores reales.");
        }

        String code = sensorCode.trim();
        if (transformer.getSensors().stream()
                .anyMatch(sensor -> sensor.getType() == SensorType.TEMPERATURE && code.equals(sensor.getCode()))) {
            return mapper.toDto(transformer);
        }
        if (transformer.getSensors().stream().anyMatch(sensor -> sensor.getType() == SensorType.TEMPERATURE)) {
            throw new IllegalArgumentException("Este transformador ya tiene un sensor de temperatura vinculado.");
        }
        if (sensorRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Ese código ya está vinculado a otro transformador. Revisa el código del ESP32.");
        }

        SensorTypeCatalogEntity catalog = sensorTypeCatalogRepository.findByCodeAndActiveTrue(SensorType.TEMPERATURE.name())
                .orElseThrow(() -> new IllegalArgumentException("El tipo Temperatura no está activo en el catálogo."));
        var sensor = factoryFor(transformer.getSubstation().getType())
                .createSensor(transformer, SensorType.TEMPERATURE.name());
        sensor.setCode(code);
        sensor.setSensorTypeCatalog(catalog);
        try {
            transformer.getSensors().add(sensorRepository.saveAndFlush(sensor));
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("No se pudo vincular el sensor. Comprueba que el código esté disponible.", exception);
        }
        return mapper.toDto(transformer);
    }

    @Transactional(readOnly = true)
    public List<TransformerDto> getArchivedTransformers() {
        return transformerRepository.findAllByArchivedAtIsNotNullOrderByArchivedAtDesc().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TransformerDto renameTransformer(String transformerId, String name) {
        if (!hasText(name)) {
            throw badRequest("El nombre del transformador es obligatorio.");
        }
        TransformerEntity transformer = transformerRepository.findByIdAndArchivedAtIsNull(transformerId)
                .orElseThrow(() -> notFound("No se encontró el transformador activo."));
        transformer.setName(name.trim());
        return mapper.toDto(transformerRepository.save(transformer));
    }

    @Transactional
    public void archiveTransformer(String transformerId) {
        TransformerEntity transformer = transformerRepository.findByIdAndArchivedAtIsNull(transformerId)
                .orElseThrow(() -> notFound("No se encontró el transformador activo."));
        transformer.setArchivedAt(ZonedDateTime.now());
        transformerRepository.save(transformer);
    }

    @Transactional
    public TransformerDto restoreTransformer(String transformerId) {
        TransformerEntity transformer = transformerRepository.findByIdAndArchivedAtIsNotNull(transformerId)
                .orElseThrow(() -> notFound("No se encontró el transformador archivado."));
        transformer.setArchivedAt(null);
        return mapper.toDto(transformerRepository.save(transformer));
    }

    private GridComponentFactory factoryFor(SubstationType type) {
        return switch (type) {
            case URBAN -> urbanGridFactory;
            case INDUSTRIAL -> industrialGridFactory;
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }

    private static ResponseStatusException conflict(String reason) {
        return new ResponseStatusException(HttpStatus.CONFLICT, reason);
    }

    private static ResponseStatusException notFound(String reason) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, reason);
    }
}
