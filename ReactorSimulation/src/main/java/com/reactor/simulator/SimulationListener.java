package com.reactor.simulator;

public interface SimulationListener {
    void onUpdate(double coreTemp, double coolantTemp);
}
