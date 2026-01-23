package com.anasol.cafe.entity;

public enum NetWeight {
    KILOGRAM("kg"),
    GRAM("g"),
    UNITS("units");

    private final String unit;

    NetWeight(String unit) {
        this.unit = unit;
    }

    public String getUnit() {
        return unit;
    }

    public String getDisplayName() {
        return this.name().toLowerCase();
    }

    // Conversion logic for weight units
    public double convertTo(double value, NetWeight targetUnit) {
        if (this == targetUnit) {
            return value;
        }

        if (this == KILOGRAM && targetUnit == GRAM) {
            return value * 1000;
        } else if (this == GRAM && targetUnit == KILOGRAM) {
            return value / 1000;
        }

        // UNITS cannot be converted to weight units
        throw new IllegalArgumentException("Cannot convert " + this + " to " + targetUnit);
    }

    // New method: Check if conversion is possible
    public boolean canConvertTo(NetWeight targetUnit) {
        if (this == targetUnit) {
            return true;
        }

        // Allow conversion between KILOGRAM and GRAM only
        if ((this == KILOGRAM && targetUnit == GRAM) ||
                (this == GRAM && targetUnit == KILOGRAM)) {
            return true;
        }

        return false;
    }

    // New method: Convert with validation
    public double convertWithValidation(double value, NetWeight targetUnit) {
        if (!canConvertTo(targetUnit)) {
            throw new IllegalArgumentException(
                    String.format("Cannot convert from %s to %s. Only kg↔g conversion is supported.",
                            this, targetUnit)
            );
        }
        return convertTo(value, targetUnit);
    }
}