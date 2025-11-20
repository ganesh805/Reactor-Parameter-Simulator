package com.reactor.simulator;

public class CoolantModel {
    private double coolantTemp;
    private final double coolantMass;
    private final double coolantSpecificHeat;
    private final double sinkTemp;
    private final double uCoreToCoolant;
    private double flowRate = 0.0;

    public CoolantModel(double initialCoolantTemp,
                        double coolantMass,
                        double coolantSpecificHeat,
                        double sinkTemp,
                        double uCoreToCoolant) {
        this.coolantTemp = initialCoolantTemp;
        this.coolantMass = coolantMass;
        this.coolantSpecificHeat = coolantSpecificHeat;
        this.sinkTemp = sinkTemp;
        this.uCoreToCoolant = uCoreToCoolant;
    }

    public double getCoolantTemp() { return coolantTemp; }
    public double getFlowRate() { return flowRate; }
    public void setFlowRate(double flowRate) { this.flowRate = Math.max(0.0, flowRate); }

    public void update(double dt, double coreTemp) {
        double qFromCore = uCoreToCoolant * (coreTemp - coolantTemp);
        double qRemoved = flowRate * coolantSpecificHeat * (coolantTemp - sinkTemp);
        double qNet = qFromCore - qRemoved;
        double dTdt = qNet / (coolantMass * coolantSpecificHeat);
        coolantTemp += dTdt * dt;
    }
}
