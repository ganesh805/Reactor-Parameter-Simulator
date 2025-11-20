package com.reactor.simulator;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class Dashboard extends BorderPane implements SimulationListener {

    private final LineChart<Number, Number> coreChart;
    private final LineChart<Number, Number> coolantChart;
    private final XYChart.Series<Number, Number> coreSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> coolantSeries = new XYChart.Series<>();

    // threshold lines series
    private final XYChart.Series<Number, Number> cautionLine = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> criticalLine = new XYChart.Series<>();

    private final Label coreTempLabel = new Label("Core: -- °C");
    private final Label coolantTempLabel = new Label("Coolant: -- °C");
    private final Label timeLabel = new Label("t = 0.0 s");
    public final Label statusLabel = new Label("");
    private final Label powerLabel = new Label("Power: -- MW");
    private final Label reactivityLabel = new Label("Reactivity: -- %");
    private final Label flowLabelSmall = new Label("Flow: -- kg/s");

    private final Label rodValueLabel = new Label("Rod: --");
    private final Label flowValueLabel = new Label("Flow: -- kg/s");

    private final Slider rodSlider = new Slider(0.0, 1.0, 1.0);
    private final Slider flowSlider = new Slider(0.0, 1000.0, 200.0);

    private final Button startBtn = new Button("Start");
    private final Button stopBtn = new Button("Stop");
    private final Button resetBtn = new Button("Reset");
    private final Button saveCsvBtn = new Button("Save CSV");
    private final Button scenariosBtn = new Button("Scenarios");
    private final Button scramBtn = new Button("SCRAM");
    private final Button emCoolantBtn = new Button("Emergency Coolant");

    private SimulationEngine engine;
    private ReactorModel reactor;
    private CoolantModel coolant;
    private SimulationController controller;
    private EventLog eventLog;

    // expose logView as a field so we can bind it later
    private final ListView<String> logView = new ListView<>();

    private double time = 0.0;
    private final double chartWindowSeconds = 120.0;
    private final double dtSeconds;

    public Dashboard(double dtSeconds) {
        this.dtSeconds = dtSeconds;
        setPadding(new Insets(10));
        coreChart = createChart("Core Temperature (°C)");
        coolantChart = createChart("Coolant Temperature (°C)");

        coreSeries.setName("Core");
        coolantSeries.setName("Coolant");
        coreChart.getData().add(coreSeries);
        coolantChart.getData().add(coolantSeries);

        // prepare threshold lines (they will be drawn as lines; X domain will be updated as time advances)
        cautionLine.setName("Caution");
        criticalLine.setName("Critical");
        coreChart.getData().add(cautionLine);
        coreChart.getData().add(criticalLine);

        Node top = buildTopBar();
        Node center = buildCenter();
        Node bottom = buildControlsWithLog();

        setTop(top);
        setCenter(center);
        setBottom(bottom);

        rodSlider.setShowTickMarks(true);
        rodSlider.setMajorTickUnit(0.5);
        rodSlider.setBlockIncrement(0.01);
        flowSlider.setBlockIncrement(1);
    }

    public double getDtSeconds() { return dtSeconds; }

    public void setContext(SimulationEngine engine, ReactorModel reactor, CoolantModel coolant) {
        this.engine = engine;
        this.reactor = reactor;
        this.coolant = coolant;

        rodSlider.setValue(reactor.getControlRodPosition());
        flowSlider.setValue(coolant.getFlowRate());

        updateRodValueLabel(rodSlider.getValue());
        updateFlowValueLabel(flowSlider.getValue());
        updateStatusLabels();

        rodSlider.valueProperty().addListener((obs, oldV, newV) -> {
            double v = newV.doubleValue();
            if (reactor != null) reactor.setControlRodPosition(v);
            updateRodValueLabel(v);
            updateStatusLabels();
        });

        flowSlider.valueProperty().addListener((obs, oldV, newV) -> {
            double v = newV.doubleValue();
            if (coolant != null) coolant.setFlowRate(v);
            updateFlowValueLabel(v);
            updateStatusLabels();
        });
    }

    public void setController(SimulationController controller) {
        this.controller = controller;
        this.eventLog = controller.getEventLog();

        startBtn.setOnAction(e -> {
            controller.startSimulation();
            statusLabel.setText("Running");
        });
        stopBtn.setOnAction(e -> {
            controller.stopSimulation();
            statusLabel.setText("Stopped");
        });
        resetBtn.setOnAction(e -> {
            controller.resetSimulation();
            resetSimulation();
            statusLabel.setText("Reset");
        });
        saveCsvBtn.setOnAction(e -> controller.exportCsvWithDialog());
        scenariosBtn.setOnAction(e -> openScenariosDialog());
        scramBtn.setOnAction(e -> {
            controller.scram();
            statusLabel.setText("SCRAMMED");
        });
        emCoolantBtn.setOnAction(e -> {
            controller.emergencyInject(controller.getEmergencyInjectionDurationSec(), controller.getEmergencyInjectionFlow());
            statusLabel.setText("Emergency coolant injected");
        });

        // bind log view to controller's event log now that it exists
        if (eventLog != null) {
            Platform.runLater(() -> logView.setItems(eventLog.getObservableLines()));
        }
    }

    private Node buildTopBar() {
        HBox info = new HBox(12, coreTempLabel, new Separator(), coolantTempLabel, new Separator(), timeLabel, new Separator(), statusLabel);
        info.setAlignment(Pos.CENTER_LEFT);
        info.setPadding(new Insets(6));
        return info;
    }

    private Node buildCenter() {
        VBox vbox = new VBox(8);
        vbox.getChildren().addAll(coreChart, coolantChart);
        vbox.setPadding(new Insets(8));
        return vbox;
    }

    private Node buildControlsWithLog() {
        // left: controls
        Label rodLabel = new Label("Control Rod (0=withdrawn,1=inserted)");
        Label flowLabel = new Label("Coolant Flow (kg/s)");

        HBox rodControl = new HBox(8, rodSlider, rodValueLabel);
        rodControl.setAlignment(Pos.CENTER_LEFT);

        HBox flowControl = new HBox(8, flowSlider, flowValueLabel);
        flowControl.setAlignment(Pos.CENTER_LEFT);

        HBox sliders = new HBox(24,
                new VBox(4, rodLabel, rodControl),
                new VBox(4, flowLabel, flowControl)
        );
        sliders.setAlignment(Pos.CENTER_LEFT);

        HBox leftButtons = new HBox(8, startBtn, stopBtn, resetBtn, saveCsvBtn, scenariosBtn);
        leftButtons.setAlignment(Pos.CENTER_LEFT);

        HBox rightButtons = new HBox(8, scramBtn, emCoolantBtn);
        rightButtons.setAlignment(Pos.CENTER_LEFT);

        VBox controls = new VBox(10, sliders, leftButtons, rightButtons);
        controls.setPadding(new Insets(10));

        // right: status + event log
        GridPane statusGrid = new GridPane();
        statusGrid.setHgap(8); statusGrid.setVgap(6);
        statusGrid.add(new Label("Power"), 0, 0); statusGrid.add(powerLabel, 1, 0);
        statusGrid.add(new Label("Reactivity"), 0, 1); statusGrid.add(reactivityLabel, 1, 1);
        statusGrid.add(new Label("Flow"), 0, 2); statusGrid.add(flowLabelSmall, 1, 2);
        statusGrid.add(new Label("Status"), 0, 3); statusGrid.add(statusLabel, 1, 3);

        logView.setPrefHeight(120);

        VBox rightBox = new VBox(6, statusGrid, new Label("Event Log"), logView);
        rightBox.setPadding(new Insets(10));
        rightBox.setPrefWidth(420);

        HBox bottom = new HBox(12, controls, rightBox);
        HBox.setHgrow(rightBox, Priority.ALWAYS);
        return bottom;
    }

    private LineChart<Number, Number> createChart(String title) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Time (s)");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(title);
        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle(title);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setLegendVisible(true);
        chart.setMinHeight(200);
        return chart;
    }

    private void appendPoint(XYChart.Series<Number, Number> series, double t, double value) {
        series.getData().add(new XYChart.Data<>(t, value));
        while (!series.getData().isEmpty()) {
            Number x = series.getData().get(0).getXValue();
            if (t - x.doubleValue() > chartWindowSeconds) {
                series.getData().remove(0);
            } else break;
        }
    }

    public void resetSimulation() {
        time = 0.0;
        coreSeries.getData().clear();
        coolantSeries.getData().clear();
        cautionLine.getData().clear();
        criticalLine.getData().clear();
        coreTempLabel.setText("Core: -- °C");
        coolantTempLabel.setText("Coolant: -- °C");
        timeLabel.setText("t = 0.0 s");
        statusLabel.setText("");
        powerLabel.setText("Power: -- MW");
        reactivityLabel.setText("Reactivity: -- %");
        flowLabelSmall.setText("Flow: -- kg/s");
        if (reactor != null) reactor.setControlRodPosition(1.0);
        if (coolant != null) coolant.setFlowRate(200.0);
        rodSlider.setValue(reactor != null ? reactor.getControlRodPosition() : 1.0);
        flowSlider.setValue(coolant != null ? coolant.getFlowRate() : 200.0);
        updateRodValueLabel(rodSlider.getValue());
        updateFlowValueLabel(flowSlider.getValue());
    }

    @Override
    public void onUpdate(double coreTemp, double coolantTemp) {
        time += dtSeconds;
        coreTempLabel.setText(String.format("Core: %.2f °C", coreTemp));
        coolantTempLabel.setText(String.format("Coolant: %.2f °C", coolantTemp));
        timeLabel.setText(String.format("t = %.1f s", time));
        appendPoint(coreSeries, time, coreTemp);
        appendPoint(coolantSeries, time, coolantTemp);

        // extend threshold lines to current window for coreChart by reading thresholds from controller
        updateThresholdLines(time);

        // keep UI sliders/labels in sync with model (ensures immediate feedback after controller actions)
        if (reactor != null) {
            double rodPos = reactor.getControlRodPosition();
            // only set if different to avoid fighting a user drag
            if (Math.abs(rodSlider.getValue() - rodPos) > 1e-6) {
                rodSlider.setValue(rodPos);
                updateRodValueLabel(rodPos);
            }
        }
        if (coolant != null) {
            double flow = coolant.getFlowRate();
            if (Math.abs(flowSlider.getValue() - flow) > 1e-6) {
                flowSlider.setValue(flow);
                updateFlowValueLabel(flow);
            }
        }

        updateStatusLabels();
    }

    private void updateThresholdLines(double t) {
        double cautionT = controller != null ? controller.getCautionTemp() : 500.0;
        double criticalT = controller != null ? controller.getCriticalTemp() : 700.0;

        cautionLine.getData().clear();
        criticalLine.getData().clear();

        double xStart = Math.max(0, t - chartWindowSeconds);
        cautionLine.getData().add(new XYChart.Data<>(xStart, cautionT));
        cautionLine.getData().add(new XYChart.Data<>(t, cautionT));

        criticalLine.getData().add(new XYChart.Data<>(xStart, criticalT));
        criticalLine.getData().add(new XYChart.Data<>(t, criticalT));
    }

    // methods controller can call to immediately update UI
    public void setRodSliderValue(double v) {
        Platform.runLater(() -> {
            rodSlider.setValue(v);
            updateRodValueLabel(v);
            updateStatusLabels();
        });
    }

    public void setFlowSliderValue(double v) {
        Platform.runLater(() -> {
            flowSlider.setValue(v);
            updateFlowValueLabel(v);
            updateStatusLabels();
        });
    }

    public void setStatusText(String t) {
        Platform.runLater(() -> statusLabel.setText(t));
    }

    private void updateRodValueLabel(double rodValue) {
        double withdrawnPct = Math.round((1.0 - rodValue) * 100.0);
        rodValueLabel.setText(String.format("Rod: %.3f  (withdrawn %.0f%%)", rodValue, withdrawnPct));
        reactivityLabel.setText(String.format("%.0f %%", (1.0 - rodValue) * 100.0));
    }

    private void updateFlowValueLabel(double flow) {
        flowValueLabel.setText(String.format("Flow: %.1f kg/s", flow));
        flowLabelSmall.setText(String.format("%.1f kg/s", flow));
    }

    public void updateStatusLabels() {
        if (reactor != null) {
            double powerW = reactor.getNominalPower() * (1.0 - reactor.getControlRodPosition());
            powerLabel.setText(String.format("%.3f MW", powerW / 1e6));
        }
        if (coolant != null) {
            flowLabelSmall.setText(String.format("%.1f kg/s", coolant.getFlowRate()));
        }
    }

    private void openScenariosDialog() {
        if (controller == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Scenarios");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(8); grid.setPadding(new Insets(10));

        Label spikeLabel = new Label("Reactivity Spike (withdraw rods)");
        TextField spikeDuration = new TextField("10");
        TextField spikeRodPos = new TextField("0");

        Label coolantLabel = new Label("Coolant Failure (cut flow)");
        TextField coolantDuration = new TextField("8");

        grid.add(spikeLabel, 0, 0);
        grid.add(new Label("Duration (s):"), 0, 1);
        grid.add(spikeDuration, 1, 1);
        grid.add(new Label("Rod pos (0..1):"), 0, 2);
        grid.add(spikeRodPos, 1, 2);

        grid.add(new Separator(), 0, 3, 2, 1);

        grid.add(coolantLabel, 0, 4);
        grid.add(new Label("Duration (s):"), 0, 5);
        grid.add(coolantDuration, 1, 5);

        Button runSpike = new Button("Run Reactivity Spike");
        Button runCoolant = new Button("Run Coolant Failure");

        HBox actions = new HBox(8, runSpike, runCoolant);
        actions.setAlignment(Pos.CENTER_LEFT);
        grid.add(actions, 0, 6, 2, 1);

        runSpike.setOnAction(e -> {
            try {
                double dur = Double.parseDouble(spikeDuration.getText());
                double rod = Double.parseDouble(spikeRodPos.getText());
                controller.triggerReactivitySpike(dur, rod);
                statusLabel.setText(String.format("Reactivity spike: %.1fs, rod=%.2f", dur, rod));
            } catch (NumberFormatException ex) {
                statusLabel.setText("Invalid spike inputs");
            }
        });

        runCoolant.setOnAction(e -> {
            try {
                double dur = Double.parseDouble(coolantDuration.getText());
                controller.triggerCoolantFailure(dur);
                statusLabel.setText(String.format("Coolant failure: %.1fs", dur));
            } catch (NumberFormatException ex) {
                statusLabel.setText("Invalid coolant input");
            }
        });

        dialog.getDialogPane().setContent(grid);
        dialog.showAndWait();
    }
}
