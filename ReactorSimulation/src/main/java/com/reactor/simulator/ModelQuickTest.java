package com.reactor.simulator;

public class ModelQuickTest {
    public static void main(String[] args) throws InterruptedException {
        ReactorModel reactor = new ReactorModel(300.0, 1.0e7, 5.0e4, 500.0, 1.0e5);
        CoolantModel coolant = new CoolantModel(290.0, 1.0e4, 4184.0, 290.0, 1.0e5);
        double dt = 0.5;
        for (int i = 0; i < 100; i++) {
            reactor.update(dt, coolant.getCoolantTemp());
            coolant.update(dt, reactor.getCoreTemp());
            if (i % 10 == 0) {
                System.out.printf("t=%.1fs | Core=%.2f °C | Coolant=%.2f °C%n", i*dt, reactor.getCoreTemp(), coolant.getCoolantTemp());
            }
            Thread.sleep(50);
        }
    }
}
