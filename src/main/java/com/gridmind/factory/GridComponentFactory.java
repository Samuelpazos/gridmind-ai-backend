package com.gridmind.factory;

import com.gridmind.entity.SensorEntity;
import com.gridmind.entity.SubstationEntity;
import com.gridmind.entity.TransformerEntity;

/**
 * Abstract Factory Pattern Interface: Creates families of Grid Components (Urban vs Industrial).
 */
public interface GridComponentFactory {
    SubstationEntity createSubstation(String name, String location);
    TransformerEntity createTransformer(SubstationEntity substation, String name, Double ratingKva, Integer ageYears);
    SensorEntity createSensor(TransformerEntity transformer, String typeStr);
}
