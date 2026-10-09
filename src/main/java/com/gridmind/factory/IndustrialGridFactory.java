package com.gridmind.factory;

import com.gridmind.domain.GridStatus;
import com.gridmind.domain.RiskLevel;
import com.gridmind.domain.SensorType;
import com.gridmind.domain.SubstationType;
import com.gridmind.entity.SensorEntity;
import com.gridmind.entity.SubstationEntity;
import com.gridmind.entity.TransformerEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class IndustrialGridFactory implements GridComponentFactory {

    @Override
    public SubstationEntity createSubstation(String name, String location) {
        String code = "SUB-IND-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        return SubstationEntity.builder()
                .id(UUID.randomUUID().toString())
                .code(code)
                .name(name != null ? name : "Subestación Industrial High Capacity")
                .location(location != null ? location : "Parque Industrial")
                .type(SubstationType.INDUSTRIAL)
                .status(GridStatus.NORMAL)
                .build();
    }

    @Override
    public TransformerEntity createTransformer(SubstationEntity substation, String name, Double ratingKva, Integer ageYears) {
        String code = "T-IND-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        return TransformerEntity.builder()
                .id(UUID.randomUUID().toString())
                .substation(substation)
                .code(code)
                .name(name != null ? name : "Transformador Industrial Heavy Duty")
                .ratingKva(ratingKva != null ? ratingKva : 2000.0)
                .ageYears(ageYears != null ? ageYears : 8)
                .maxTempC(95.0) // Higher threshold for industrial load
                .maxCurrentA(800.0)
                .status(GridStatus.NORMAL)
                .failureProbability(0.08)
                .riskLevel(RiskLevel.LOW)
                .priorityScore(25.0)
                .build();
    }

    @Override
    public SensorEntity createSensor(TransformerEntity transformer, String typeStr) {
        SensorType type = SensorType.valueOf(typeStr.toUpperCase());
        String code = "SI-" + transformer.getCode() + "-" + type.name().substring(0, 3);
        return SensorEntity.builder()
                .id(UUID.randomUUID().toString())
                .transformer(transformer)
                .code(code)
                .type(type)
                .unit(type.getDefaultUnit())
                .status("ACTIVE")
                .build();
    }
}
