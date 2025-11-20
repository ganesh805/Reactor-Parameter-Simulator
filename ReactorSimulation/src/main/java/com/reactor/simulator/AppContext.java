package com.reactor.simulator;

public final class AppContext {
    private static AppContext instance;

    public final ReactorModel reactor;
    public final CoolantModel coolant;
    public final SimulationEngine engine;
    public final Dashboard dashboard;
    public final SimulationController controller;
    public final ScenarioManager scenarios;

    private AppContext() {
        reactor = new ReactorModel(300.0, 1.0e7, 5.0e4, 500.0, 1.0e5);
        coolant = new CoolantModel(290.0, 1.0e4, 4184.0, 290.0, 1.0e5);
        engine = new SimulationEngine(reactor, coolant, 0.5);
        dashboard = new Dashboard(0.5);
        controller = new SimulationController(engine, reactor, coolant, dashboard);
        dashboard.setContext(engine, reactor, coolant);
        dashboard.setController(controller);
        engine.addListener(dashboard);
        scenarios = new ScenarioManager(reactor, coolant);
    }

    public static synchronized AppContext get() {
        if (instance == null) instance = new AppContext();
        return instance;
    }
}
