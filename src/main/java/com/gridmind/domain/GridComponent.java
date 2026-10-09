package com.gridmind.domain;

import java.util.List;

/**
 * Composite Pattern Interface: Represents elements in the electrical grid hierarchy.
 * Allows treating Substations, Transformers, and Sensors uniformly.
 */
public interface GridComponent {
    String getId();
    String getName();
    String getCode();
    GridStatus getStatus();
    List<GridComponent> getChildren();
    void addComponent(GridComponent component);
    void removeComponent(GridComponent component);
}
