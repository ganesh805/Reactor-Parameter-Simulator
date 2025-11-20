package com.reactor.simulator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class CSVExporter {
    private final File file;
    private BufferedWriter writer;
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ISO_INSTANT;

    public CSVExporter(File file) {
        this.file = file;
    }

    public void open() throws IOException {
        writer = new BufferedWriter(new FileWriter(file, false));
        writer.write("# Reactor simulation export\n");
        writer.write("# Generated: " + timeFmt.format(Instant.now().atOffset(ZoneOffset.UTC)) + " UTC\n");
        writer.write("time_s,core_temp_c,coolant_temp_c\n");
        writer.flush();
    }

    public synchronized void writeLine(double timeSeconds, double coreTemp, double coolantTemp) throws IOException {
        if (writer == null) open();
        writer.write(String.format("%.6f,%.6f,%.6f%n", timeSeconds, coreTemp, coolantTemp));
    }

    public synchronized void close() {
        if (writer == null) return;
        try { writer.flush(); writer.close(); } catch (IOException ignored) {}
        writer = null;
    }

    public File getFile() { return file; }
}
