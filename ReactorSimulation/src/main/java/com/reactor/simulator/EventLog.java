package com.reactor.simulator;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class EventLog {
    private final ObservableList<String> lines = FXCollections.observableArrayList();
    private final DateTimeFormatter fmt = DateTimeFormatter.ISO_INSTANT;

    public void append(String message) {
        String ts = fmt.format(Instant.now().atOffset(ZoneOffset.UTC));
        String entry = String.format("[%s] %s", ts, message);
        // Ensure UI thread when updating observable list
        Platform.runLater(() -> lines.add(entry));
        System.out.println(entry);
    }

    public ObservableList<String> getObservableLines() {
        return lines;
    }

    public List<String> getLinesSnapshot() {
        return List.copyOf(lines);
    }
}
