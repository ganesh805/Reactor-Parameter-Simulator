package com.reactor.simulator;

public class ReactorModel {
    private double coreTemp;
    private final double nominalPower;
    private final double coreMass;
    private final double coreSpecificHeat;
    private final double uCoreToCoolant;
    private double controlRodPosition = 1.0;

    public ReactorModel(double initialCoreTemp,
                        double nominalPower,
                        double coreMass,
                        double coreSpecificHeat,
                        double uCoreToCoolant) {
        this.coreTemp = initialCoreTemp;
        this.nominalPower = nominalPower;
        this.coreMass = coreMass;
        this.coreSpecificHeat = coreSpecificHeat;
        this.uCoreToCoolant = uCoreToCoolant;
    }

    public double getCoreTemp() { return coreTemp; }
    public double getControlRodPosition() { return controlRodPosition; }
    public void setControlRodPosition(double controlRodPosition) {
        this.controlRodPosition = Math.max(0.0, Math.min(1.0, controlRodPosition));
    }

    public double getNominalPower() { return nominalPower; }

    public void update(double dt, double coolantTemp) {
        double powerGen = nominalPower * (1.0 - controlRodPosition);
        double qCoreToCoolant = uCoreToCoolant * (coreTemp - coolantTemp);
        double qNet = powerGen - qCoreToCoolant;
        double dTdt = qNet / (coreMass * coreSpecificHeat);
        coreTemp += dTdt * dt;
    }
}
