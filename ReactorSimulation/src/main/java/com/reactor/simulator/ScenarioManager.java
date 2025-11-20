package com.reactor.simulator;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScenarioManager {
    private final ReactorModel reactor;
    private final CoolantModel coolant;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ScenarioManager");
        t.setDaemon(true);
        return t;
    });

    public ScenarioManager(ReactorModel reactor, CoolantModel coolant) {
        this.reactor = reactor;
        this.coolant = coolant;
    }

    public void shutdown() { executor.shutdownNow(); }

    public void reactivitySpike(double durationSeconds, double newRodPosition) {
        double prev = reactor.getControlRodPosition();
        reactor.setControlRodPosition(clamp(newRodPosition, 0.0, 1.0));
        executor.schedule(() -> reactor.setControlRodPosition(prev),
                Math.max(0, (long) durationSeconds), TimeUnit.SECONDS);
    }

    public void coolantFailure(double durationSeconds) {
        double prevFlow = coolant.getFlowRate();
        coolant.setFlowRate(0.0);
        executor.schedule(() -> coolant.setFlowRate(prevFlow),
                Math.max(0, (long) durationSeconds), TimeUnit.SECONDS);
    }

    private double clamp(double v, double lo, double hi) {
        if (Double.isNaN(v)) return lo;
        return Math.max(lo, Math.min(hi, v));
    }
}
