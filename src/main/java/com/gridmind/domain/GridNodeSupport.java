package com.gridmind.domain;

import java.util.ArrayList;
import java.util.List;

abstract class GridNodeSupport implements GridComponent {

    private final String id;
    private final String name;
    private final String code;
    private final GridStatus ownStatus;
    private final double ownRiskScore;
    private final List<GridComponent> children = new ArrayList<>();

    GridNodeSupport(String id, String name, String code, GridStatus ownStatus, double ownRiskScore) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.ownStatus = ownStatus;
        this.ownRiskScore = ownRiskScore;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public GridStatus getStatus() {
        GridStatus worst = ownStatus == null ? GridStatus.NORMAL : ownStatus;
        for (GridComponent child : children) {
            worst = GridStatus.worstOf(worst, child.getStatus());
        }
        return worst;
    }

    @Override
    public double getRiskScore() {
        double highest = ownRiskScore;
        for (GridComponent child : children) {
            highest = Math.max(highest, child.getRiskScore());
        }
        return highest;
    }

    @Override
    public int countComponents() {
        return 1 + children.stream().mapToInt(GridComponent::countComponents).sum();
    }

    @Override
    public List<GridComponent> getChildren() {
        return List.copyOf(children);
    }

    @Override
    public void addComponent(GridComponent component) {
        if (component == null || component == this) {
            throw new IllegalArgumentException("El componente hijo no puede ser nulo ni ser el mismo nodo.");
        }
        children.add(component);
    }

    @Override
    public void removeComponent(GridComponent component) {
        children.remove(component);
    }
}
