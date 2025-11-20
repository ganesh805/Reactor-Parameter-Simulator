package com.reactor.simulator;

import javafx.application.Platform;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Enhanced SimulationController: event logging, SCRAM, emergency coolant, auto-shutdown.
 */
public class SimulationController {

    private final SimulationEngine engine;
    private final ReactorModel reactor;
    private final CoolantModel coolant;
    private final Dashboard dashboard;
    private final ScenarioManager scenarios;
    private final EventLog eventLog = new EventLog();

    private final StringBuilder logBuffer = new StringBuilder();
    private double simTime = 0.0;

    // safety thresholds (tweakable)
    private volatile double cautionTemp = 500.0;
    private volatile double criticalTemp = 700.0;
    private volatile boolean autoShutdownEnabled = true;
    // emergency injection parameters
    private volatile double emergencyInjectionFlow = 1000.0; // kg/s when emergency coolant injected
    private volatile long emergencyInjectionDurationSec = 10L;

    public SimulationController(SimulationEngine engine,
                                ReactorModel reactor,
                                CoolantModel coolant,
                                Dashboard dashboard) {
        this.engine = engine;
        this.reactor = reactor;
        this.coolant = coolant;
        this.dashboard = dashboard;
        this.scenarios = new ScenarioManager(reactor, coolant);

        double dt = dashboard != null ? dashboard.getDtSeconds() : 0.5;

        // record each sample and also monitor safety
        engine.addListener((coreTemp, coolantTemp) -> {
            simTime += dt;
            logBuffer.append(String.format("%.3f,%.6f,%.6f%n", simTime, coreTemp, coolantTemp));
            monitorSafety(coreTemp);
        });
    }

    // Expose event log to UI
    public EventLog getEventLog() {
        return eventLog;
    }

    private void monitorSafety(double coreTemp) {
        if (!autoShutdownEnabled) return;

        if (coreTemp >= criticalTemp) {
            eventLog.append(String.format("CRITICAL: core temp %.2f >= %.1f — initiating SCRAM & emergency actions", coreTemp, criticalTemp));
            scram();
            emergencyInject(emergencyInjectionDurationSec, emergencyInjectionFlow);
            engine.stop();
            if (dashboard != null) Platform.runLater(() -> dashboard.setStatusText("CRITICAL - SCRAMED"));
        } else if (coreTemp >= cautionTemp) {
            eventLog.append(String.format("CAUTION: core temp %.2f >= %.1f", coreTemp, cautionTemp));
            if (dashboard != null) Platform.runLater(() -> dashboard.setStatusText("WARNING"));
        } else {
            if (dashboard != null) Platform.runLater(() -> dashboard.setStatusText(engine.isRunning() ? "Running" : "Stopped"));
        }
    }

    // Start/stop/reset
    public void startSimulation() {
        if (!engine.isRunning()) {
            simTime = 0.0;
            logBuffer.setLength(0);
            eventLog.append("Simulation started");
        }
        engine.start();
        System.out.println("[Controller] startSimulation()");
    }

    public void stopSimulation() {
        engine.stop();
        eventLog.append("Simulation stopped");
        System.out.println("[Controller] stopSimulation()");
    }

    public boolean isRunning() { return engine.isRunning(); }

    public void resetSimulation() {
        stopSimulation();
        simTime = 0.0;
        logBuffer.setLength(0);
        if (dashboard != null) dashboard.resetSimulation();
        if (reactor != null) reactor.setControlRodPosition(1.0);
        if (coolant != null) coolant.setFlowRate(200.0);
        eventLog.append("Simulation reset");
        System.out.println("[Controller] resetSimulation()");
    }

    // EXPORT
    public void exportCsvTo(File file) throws IOException {
        if (file == null) return;
        try (FileWriter fw = new FileWriter(file)) {
            fw.write("# Reactor simulation export\n");
            fw.write("# Generated: " + DateTimeFormatter.ISO_INSTANT.format(Instant.now().atZone(ZoneOffset.UTC)) + " UTC\n");
            fw.write("time_s,core_temp_c,coolant_temp_c\n");
            fw.write(logBuffer.toString());
            fw.flush();
        }
        eventLog.append("CSV exported to " + (file != null ? file.getAbsolutePath() : "null"));
    }

    public void exportCsvWithDialog() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Simulation CSV");
        chooser.setInitialFileName("reactor_sim_" + System.currentTimeMillis() + ".csv");
        File out = chooser.showSaveDialog(null);
        if (out == null) return;
        try { exportCsvTo(out); } catch (IOException e) { e.printStackTrace(); }
    }

    // Safety actions
    public void scram() {
        if (reactor != null) {
            reactor.setControlRodPosition(1.0); // insert rods
            eventLog.append("SCRAM executed: rods inserted (pos=1.0)");
            // update UI immediately — avoid waiting for next engine tick
            if (dashboard != null) Platform.runLater(() -> {
                dashboard.setRodSliderValue(1.0);
                dashboard.setStatusText("SCRAMMED");
                dashboard.updateStatusLabels();
            });
        }
    }

    public void emergencyInject(long durationSeconds, double boostFlowKgPerS) {
        if (coolant == null) return;
        double prevFlow = coolant.getFlowRate();
        coolant.setFlowRate(boostFlowKgPerS);
        eventLog.append(String.format("Emergency coolant injected: flow set to %.1f kg/s for %ds", boostFlowKgPerS, durationSeconds));

        // update UI immediately
        if (dashboard != null) Platform.runLater(() -> {
            dashboard.setFlowSliderValue(boostFlowKgPerS);
            dashboard.setStatusText("EMERGENCY COOLANT");
            dashboard.updateStatusLabels();
        });

        // restore previous flow after duration and update UI then
        new Thread(() -> {
            try { Thread.sleep(durationSeconds * 1000L); } catch (InterruptedException ignored) {}
            coolant.setFlowRate(prevFlow);
            eventLog.append(String.format("Emergency coolant restored to %.1f kg/s", prevFlow));
            if (dashboard != null) Platform.runLater(() -> {
                dashboard.setFlowSliderValue(prevFlow);
                dashboard.setStatusText(engine.isRunning() ? "Running" : "Stopped");
                dashboard.updateStatusLabels();
            });
        }, "EmergencyInject").start();
    }

    // Scenario wrappers (log & forward)
    public void triggerReactivitySpike(double durationSeconds, double newRodPosition) {
        eventLog.append(String.format("Scenario: Reactivity spike for %.1fs to rod=%.2f", durationSeconds, newRodPosition));
        scenarios.reactivitySpike(durationSeconds, newRodPosition);
    }

    public void triggerCoolantFailure(double durationSeconds) {
        eventLog.append(String.format("Scenario: Coolant failure for %.1fs", durationSeconds));
        scenarios.coolantFailure(durationSeconds);
    }

    // Control safety parameters at runtime
    public void setCautionTemp(double t) { this.cautionTemp = t; eventLog.append("Caution temp set to " + t); }
    public void setCriticalTemp(double t) { this.criticalTemp = t; eventLog.append("Critical temp set to " + t); }
    public void setAutoShutdownEnabled(boolean v) { this.autoShutdownEnabled = v; eventLog.append("Auto-shutdown set to " + v); }

    // Public getters for dashboard / UI
    public double getCautionTemp() { return cautionTemp; }
    public double getCriticalTemp() { return criticalTemp; }
    public double getEmergencyInjectionFlow() { return emergencyInjectionFlow; }
    public long getEmergencyInjectionDurationSec() { return emergencyInjectionDurationSec; }
    public boolean isAutoShutdownEnabled() { return autoShutdownEnabled; }

    // allow changing emergency injection params
    public void setEmergencyInjectionFlow(double flow) { this.emergencyInjectionFlow = flow; eventLog.append("Emergency injection flow set to " + flow); }
    public void setEmergencyInjectionDurationSec(long secs) { this.emergencyInjectionDurationSec = secs; eventLog.append("Emergency injection duration set to " + secs); }

    public void shutdown() {
        scenarios.shutdown();
        eventLog.append("Controller shutdown");
    }
}
