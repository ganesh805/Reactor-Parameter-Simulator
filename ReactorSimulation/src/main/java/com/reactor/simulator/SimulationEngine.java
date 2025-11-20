package com.reactor.simulator;

import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SimulationEngine implements Runnable {
    private final ReactorModel reactor;
    private final CoolantModel coolant;
    private final double dt;
    private volatile boolean running = false;
    private final List<SimulationListener> listeners = new CopyOnWriteArrayList<>();

    public SimulationEngine(ReactorModel reactor, CoolantModel coolant, double dt) {
        this.reactor = reactor;
        this.coolant = coolant;
        this.dt = dt;
    }

    public void addListener(SimulationListener listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeListener(SimulationListener listener) {
        listeners.remove(listener);
    }

    public void start() {
        if (running) return;
        running = true;
        Thread t = new Thread(this, "SimulationEngine");
        t.setDaemon(true);
        t.start();
    }

    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void run() {
        while (running) {
            reactor.update(dt, coolant.getCoolantTemp());
            coolant.update(dt, reactor.getCoreTemp());

            double core = reactor.getCoreTemp();
            double cool = coolant.getCoolantTemp();

            if (!listeners.isEmpty()) {
                Platform.runLater(() -> {
                    for (SimulationListener l : listeners) {
                        try { l.onUpdate(core, cool); } catch (Exception ignored) {}
                    }
                });
            }

            try {
                Thread.sleep((long) (dt * 1000));
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
