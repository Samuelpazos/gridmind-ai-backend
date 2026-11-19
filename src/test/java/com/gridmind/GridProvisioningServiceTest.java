package com.gridmind;

import com.gridmind.domain.SensorType;
import com.gridmind.domain.SubstationType;
import com.gridmind.domain.TransformerAssetType;
import com.gridmind.dto.ManualTransformerRequestDto;
import com.gridmind.dto.GridProvisioningRequestDto;
import com.gridmind.entity.SubstationEntity;
import com.gridmind.entity.TransformerEntity;
import com.gridmind.factory.IndustrialGridFactory;
import com.gridmind.factory.UrbanGridFactory;
import com.gridmind.mapper.EntityDtoMapper;
import com.gridmind.repository.SensorRepository;
import com.gridmind.repository.SubstationRepository;
import com.gridmind.entity.SensorTypeCatalogEntity;
import com.gridmind.repository.SensorTypeCatalogRepository;
import com.gridmind.repository.TransformerRepository;
import com.gridmind.service.GridProvisioningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GridProvisioningServiceTest {

    @Mock
    private SubstationRepository substationRepository;

    @Mock
    private SensorTypeCatalogRepository sensorTypeCatalogRepository;

    @Mock
    private TransformerRepository transformerRepository;

    @Mock
    private SensorRepository sensorRepository;

    private GridProvisioningService service;

    @BeforeEach
    void setUp() {
        service = new GridProvisioningService(
                substationRepository,
                sensorTypeCatalogRepository,
                transformerRepository,
                sensorRepository,
                new EntityDtoMapper(),
                new UrbanGridFactory(),
                new IndustrialGridFactory()
        );
    }

    @Test
    void provisionsUrbanGridWithMatchingFactoryAndSensors() {
        when(sensorTypeCatalogRepository.findByCodeAndActiveTrue(any()))
                .thenAnswer(invocation -> Optional.of(sensorType(invocation.getArgument(0))));
        when(substationRepository.save(any(SubstationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.provision(new GridProvisioningRequestDto(
                SubstationType.URBAN,
                "Urban substation",
                "North",
                "Urban transformer",
                750.0,
                4,
                List.of(SensorType.TEMPERATURE, SensorType.VOLTAGE)
        ));

        assertEquals(SubstationType.URBAN, result.getType());
        assertEquals(1, result.getTransformers().size());
        assertEquals(85.0, result.getTransformers().getFirst().getMaxTempC());
        assertEquals(2, result.getTransformers().getFirst().getSensors().size());
        assertTrue(result.getTransformers().getFirst().getSensors().stream()
                .allMatch(sensor -> sensor.getCode().startsWith("SU-")));
        var savedCaptor = org.mockito.ArgumentCaptor.forClass(SubstationEntity.class);
        verify(substationRepository).save(savedCaptor.capture());
        assertTrue(savedCaptor.getValue().getTransformers().getFirst().getSensors().stream()
                .allMatch(sensor -> sensor.getSensorTypeCatalog().isActive()));
        assertEquals(TransformerAssetType.TEST, result.getTransformers().getFirst().getAssetType());
    }

    @Test
    void provisionsIndustrialGridWithIndustrialFactory() {
        when(sensorTypeCatalogRepository.findByCodeAndActiveTrue("CURRENT"))
                .thenReturn(Optional.of(sensorType("CURRENT")));
        when(substationRepository.save(any(SubstationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.provision(new GridProvisioningRequestDto(
                SubstationType.INDUSTRIAL,
                "Industrial substation",
                "South",
                "Industrial transformer",
                2000.0,
                8,
                List.of(SensorType.CURRENT)
        ));

        assertEquals(SubstationType.INDUSTRIAL, result.getType());
        assertEquals(95.0, result.getTransformers().getFirst().getMaxTempC());
        assertTrue(result.getTransformers().getFirst().getSensors().getFirst().getCode().startsWith("SI-"));
    }

    @Test
    void rejectsDuplicateSensorTypesBeforePersistence() {
        assertThrows(IllegalArgumentException.class, () -> service.provision(new GridProvisioningRequestDto(
                SubstationType.URBAN,
                "Urban substation",
                "North",
                "Urban transformer",
                750.0,
                4,
                List.of(SensorType.TEMPERATURE, SensorType.TEMPERATURE)
        )));

        verify(substationRepository, never()).save(any(SubstationEntity.class));
    }

    @Test
    void provisionsHardwareInAnExistingSubstationWithItsSensorCode() {
        var substation = SubstationEntity.builder()
                .id("sub-001")
                .type(SubstationType.URBAN)
                .transformers(new java.util.ArrayList<>())
                .build();
        when(substationRepository.findById("sub-001")).thenReturn(Optional.of(substation));
        when(transformerRepository.existsByCode("T-N-002")).thenReturn(false);
        when(sensorRepository.existsByCode("SN-002-TMP")).thenReturn(false);
        when(sensorTypeCatalogRepository.findByCodeAndActiveTrue("TEMPERATURE"))
                .thenReturn(Optional.of(sensorType("TEMPERATURE")));
        when(transformerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.provisionTransformer(new ManualTransformerRequestDto(
                "sub-001",
                null,
                null,
                null,
                "Hardware Norte 2",
                "T-N-002",
                TransformerAssetType.HARDWARE,
                750.0,
                4,
                List.of(SensorType.TEMPERATURE),
                Map.of(SensorType.TEMPERATURE, "SN-002-TMP")
        ));

        assertEquals(TransformerAssetType.HARDWARE, result.getAssetType());
        assertEquals("T-N-002", result.getCode());
        assertEquals("SN-002-TMP", result.getSensors().getFirst().getCode());
        verify(transformerRepository).save(any());
        verify(substationRepository, never()).save(any(SubstationEntity.class));
    }

    @Test
    void provisionsTestTransformerInANewSubstation() {
        when(transformerRepository.existsByCode(any())).thenReturn(false);
        when(sensorRepository.existsByCode(any())).thenReturn(false);
        when(sensorTypeCatalogRepository.findByCodeAndActiveTrue("TEMPERATURE"))
                .thenReturn(Optional.of(sensorType("TEMPERATURE")));
        when(substationRepository.save(any(SubstationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.provisionTransformer(new ManualTransformerRequestDto(
                null,
                SubstationType.INDUSTRIAL,
                "Subestación de prueba",
                "Zona de pruebas",
                "Transformador de prueba",
                null,
                TransformerAssetType.TEST,
                1500.0,
                2,
                List.of(SensorType.TEMPERATURE),
                Map.of()
        ));

        assertEquals(TransformerAssetType.TEST, result.getAssetType());
        assertEquals("Transformador de prueba", result.getName());
        assertEquals("Subestación de prueba", result.getSubstationName());
        verify(substationRepository).save(any(SubstationEntity.class));
        verify(transformerRepository, never()).save(any());
    }

    @Test
    void rejectsHardwareWithoutSensorCodesBeforePersistence() {
        when(substationRepository.findById("sub-001"))
                .thenReturn(Optional.of(SubstationEntity.builder()
                        .id("sub-001")
                        .type(SubstationType.URBAN)
                        .transformers(new java.util.ArrayList<>())
                        .build()));
        when(transformerRepository.existsByCode(any())).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.provisionTransformer(new ManualTransformerRequestDto(
                        "sub-001",
                        null,
                        null,
                        null,
                        "Hardware sin sensor",
                        null,
                        TransformerAssetType.HARDWARE,
                        750.0,
                        4,
                        List.of(SensorType.TEMPERATURE),
                        Map.of()
                )));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(transformerRepository, never()).save(any());
        verify(substationRepository, never()).save(any(SubstationEntity.class));
    }

    @Test
    void rejectsSensorCodesAlreadyAssignedToAnotherTransformer() {
        when(substationRepository.findById("sub-001"))
                .thenReturn(Optional.of(SubstationEntity.builder()
                        .id("sub-001")
                        .type(SubstationType.URBAN)
                        .transformers(new java.util.ArrayList<>())
                        .build()));
        when(transformerRepository.existsByCode("T-N-002")).thenReturn(false);
        when(sensorRepository.existsByCode("SN-001-TMP")).thenReturn(true);
        when(sensorTypeCatalogRepository.findByCodeAndActiveTrue("TEMPERATURE"))
                .thenReturn(Optional.of(sensorType("TEMPERATURE")));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.provisionTransformer(new ManualTransformerRequestDto(
                        "sub-001",
                        null,
                        null,
                        null,
                        "Hardware duplicado",
                        "T-N-002",
                        TransformerAssetType.HARDWARE,
                        750.0,
                        4,
                        List.of(SensorType.TEMPERATURE),
                        Map.of(SensorType.TEMPERATURE, "SN-001-TMP")
                )));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(transformerRepository, never()).save(any());
        verify(substationRepository, never()).save(any(SubstationEntity.class));
    }

    @Test
    void archivesTransformerWithoutDeletingItOrItsHistory() {
        var transformer = TransformerEntity.builder()
                .id("transformer-1")
                .code("T-N-002")
                .name("Transformador Norte 2")
                .build();
        when(transformerRepository.findByIdAndArchivedAtIsNull("transformer-1"))
                .thenReturn(Optional.of(transformer));

        service.archiveTransformer("transformer-1");

        assertNotNull(transformer.getArchivedAt());
        verify(transformerRepository).save(transformer);
        verify(transformerRepository, never()).deleteById("transformer-1");
    }

    @Test
    void restoresArchivedTransformer() {
        var transformer = TransformerEntity.builder()
                .id("transformer-1")
                .code("T-N-002")
                .name("Transformador Norte 2")
                .archivedAt(ZonedDateTime.now().minusDays(1))
                .build();
        when(transformerRepository.findByIdAndArchivedAtIsNotNull("transformer-1"))
                .thenReturn(Optional.of(transformer));
        when(transformerRepository.save(transformer)).thenReturn(transformer);

        var result = service.restoreTransformer("transformer-1");

        assertNull(transformer.getArchivedAt());
        assertEquals("transformer-1", result.getId());
        verify(transformerRepository).save(transformer);
    }

    @Test
    void renamesOnlyAnActiveTransformerAndTrimsTheName() {
        var transformer = TransformerEntity.builder()
                .id("transformer-1")
                .code("T-N-002")
                .name("Nombre anterior")
                .build();
        when(transformerRepository.findByIdAndArchivedAtIsNull("transformer-1"))
                .thenReturn(Optional.of(transformer));
        when(transformerRepository.save(transformer)).thenReturn(transformer);

        var result = service.renameTransformer("transformer-1", "  Nuevo nombre  ");

        assertEquals("Nuevo nombre", result.getName());
        verify(transformerRepository).save(transformer);
    }

    private SensorTypeCatalogEntity sensorType(String code) {
        var catalogType = new SensorTypeCatalogEntity();
        catalogType.setId("sensor-type-" + code);
        catalogType.setCode(code);
        catalogType.setActive(true);
        return catalogType;
    }
}
