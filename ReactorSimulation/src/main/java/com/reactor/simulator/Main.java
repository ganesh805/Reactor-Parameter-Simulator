package com.reactor.simulator;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {

        // ---------------------------------------------------------------
        // VERY FAST HEATING MODE
        // Increased nominal power from 1.0e7 (10 MW) → 3.0e7 (30 MW)
        // This makes the core heat up extremely fast when rods are withdrawn.
        // ---------------------------------------------------------------
        double initialCoreTemp = 300.0;
        double nominalPower = 2.0e7;      // VERY FAST HEATING (30 MW)
        double coreMass = 5.0e4;          // kg
        double coreSpecificHeat = 500.0;  // J/kg·K
        double heatTransferCoeff = 1.0e5; // W/K (to coolant)

        ReactorModel reactor = new ReactorModel(
                initialCoreTemp,
                nominalPower,
                coreMass,
                coreSpecificHeat,
                heatTransferCoeff
        );

        // Coolant model (unchanged)
        CoolantModel coolant = new CoolantModel(
                290.0,      // initial temp
                1.0e4,      // mass
                4184.0,     // specific heat
                290.0,      // sink temp
                1.0e5       // heat transfer coefficient (from core)
        );

        // Timestep
        double dt = 0.5;

        // Engine
        SimulationEngine engine = new SimulationEngine(reactor, coolant, dt);

        // Dashboard UI
        Dashboard dashboard = new Dashboard(dt);
        dashboard.setContext(engine, reactor, coolant);

        // Controller
        SimulationController controller =
                new SimulationController(engine, reactor, coolant, dashboard);

        dashboard.setController(controller);

        // Listener for real-time graph updates
        engine.addListener(dashboard);

        // ---------------------------------------------------------------
        // Optional: Auto-start simulation
        // Comment out this line if you want it only on Start button click.
        // ---------------------------------------------------------------
        controller.startSimulation();

        primaryStage.setTitle("Reactor Parameter Simulator — FAST HEATING MODE");
        primaryStage.setScene(new Scene(dashboard, 1100, 820));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
