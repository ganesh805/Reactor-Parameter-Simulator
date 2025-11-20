🔥 Reactor Parameter Simulator
A Real-Time Nuclear Reactor Thermal & Safety Simulation Engine

This project simulates the thermal behavior of a nuclear reactor core using real physics equations, interactive controls, accident scenarios, and built-in safety systems such as SCRAM and Emergency Coolant Injection.

It behaves like a simplified version of a real reactor control room dashboard used in power plants.

📌 Table of Contents

Overview

Features

How the Physics Works

Formulas Explained

Architecture

UI Overview

Scenarios

Safety System

Technologies Used

Installation

Run Instructions

Screenshots

Future Enhancements

Author

🚀 Overview

The Reactor Parameter Simulator models how a reactor core heats and cools based on:

Control Rod Position (0 = withdrawn, 1 = inserted)

Coolant Flow Rate (kg/s)

Nominal Power (MW)

Heat Transfer to Coolant

Safety Thresholds (Caution & Critical)

The simulation runs in real-time, updating physical parameters at each time step (dt seconds).

The UI shows:

Core Temperature Graph

Coolant Temperature Graph

Power Output

Reactivity (%)

System Status

Event Log (warnings, SCRAM, emergency actions)

Scenario triggers (reactivity spike, coolant failure)

This is a professional engineering simulator, not a toy project.

⭐ Features

🔹 Real-Time Simulation Engine

Custom physics loop

Dynamically updates temperatures and power

Adjustable timestep (dt)

🔹 Physics-Based Thermal Modeling

Heat generation from reactor power

Heat removal via coolant

Conduction & convection simulation

Core–Coolant thermal exchange

🔹 Dashboard UI (JavaFX)

Dual live charts (Core & Coolant temperature)

Real-time numeric indicators

Control sliders (Rod, Coolant Flow)

Status indicators

Event Log viewer

🔹 Safety Systems

SCRAM (Rapid reactor shutdown)

Emergency Coolant Injection (ECCS)

Auto-Shutdown at Critical Temperature

Caution & Critical Threshold Lines

🔹 Accident Scenarios

Reactivity Spike

Coolant Pump Failure

Combined Events

Auto recovery

🔹 Data Logging

CSV Export (time, core temp, coolant temp)

Real-time event logging with timestamps

🧠 How the Physics Works

The simulator uses a 0D thermal model (lumped mass model).
This means:

Core heat is treated as a single-node system

Coolant heat is treated separately

Heat is exchanged between core ↔ coolant

This is similar to standard textbook nuclear engineering models.

📐 Formulas Explained
1. Reactor Power Generation

Power is controlled by Rod Position:

powerGenerated = nominalPower * (1 - rodPosition)


Rod = 1.0 → fully inserted → shuts down reaction

Rod = 0.0 → fully withdrawn → maximum power

2. Heat Removed by Coolant
qRemoved = flowRate * coolantSpecificHeat * (coolantTemp - sinkTemp)


Where:

flowRate = coolant flow (kg/s)

coolantSpecificHeat = 4184 J/kg·K (for water)

sinkTemp = ambient coolant sink temperature (e.g., 290°C)

More flow → more heat removal → reactor cools.

3. Net Heat
qNet = powerGenerated - heatRemoved


If qNet > 0 → temperature rises
If qNet < 0 → temperature falls

4. Core Temperature Update
coreTemp += (qNet / (coreMass * coreSpecificHeat)) * dt


Where:

coreMass → kg

coreSpecificHeat → J/kg·K

dt → simulation time step

5. Coolant Temperature Update
coolantTemp += (heatTransferred / (coolantMass * coolantSpecificHeat)) * dt

🧱 Architecture

Clean MVC (Model–View–Controller) Structure:

src/main/java/com/reactor/simulator/
│── Main.java                 → App entry point

│── Dashboard.java            → JavaFX UI (View)

│── SimulationEngine.java     → Real-time update loop

│── SimulationController.java → Safety logic + coordination

│── ReactorModel.java         → Core physics

│── CoolantModel.java         → Coolant physics

│── ScenarioManager.java      → Accident scenarios

│── EventLog.java             → Observable log system

│── SimulationListener.java   → Listener interface

└── utils/


This structure is production-grade, not academic.

🖥️ UI Overview

The dashboard displays:

Live Graphs

Core Temperature vs Time

Coolant Temperature vs Time

Caution & Critical threshold lines

Controls

Rod slider

Coolant flow slider

Start / Stop

Reset

SCRAM

Emergency Coolant

Save CSV

Scenario Dialog

Indicators

Core Temperature

Coolant Temperature

Power (MW)

Reactivity (%)

Coolant Flow (kg/s)

System Status

Event Log

⚠️ Scenarios

1. Reactivity Spike

Rods rapidly withdraw (e.g., rod=0 for 10 seconds)

Sudden heat surge

Used to simulate transient anomalies

2. Coolant Failure

Pump failure (flow=0)

Causes runaway temperature rise

3. Combined Accident

Spike + coolant failure → dangerous conditions

Triggers automatic SCRAM

🔒 Safety System
1. Caution Temperature

Default: 500°C

UI status becomes WARNING

Event logged

2. Critical Temperature

Default: 700°C

Automatic:

SCRAM

Emergency Coolant Injection

Engine stop

Event logged

3. SCRAM (Manual/Auto)

Inserts control rods fully

rodPosition = 1.0


Power = 0

Temperature drops

4. Emergency Coolant Injection (ECCS)

Flow temporarily boosted to high value (e.g., 1000 kg/s)

Rapid cooldown

🧪 Technologies Used

Java 21+

JavaFX 21

Maven

Object-Oriented Design (OOP)

Real-time simulation patterns

Event-driven programming

CSV export

Observer Pattern (Listeners)

📦 Installation

Clone the repository:

git clone https://github.com/ganesh805/Reactor-Parameter-Simulator
cd Reactor-Parameter-Simulator


Build the project:

mvn clean compile

▶️ Run Instructions
Run using Maven
mvn javafx:run

Or Run with Java

(If packaged into a JAR)

java -jar ReactorSimulator.jar

📸 Screenshots

<img width="1041" height="852" alt="Screenshot 2025-11-20 190307" src="https://github.com/user-attachments/assets/29f5e3b0-1454-460a-a242-3529fcc51726" />

<img width="1252" height="1030" alt="Screenshot 2025-11-20 144007" src="https://github.com/user-attachments/assets/d94ab5b3-af45-4784-9145-3bb5560ab2e9" />

<img width="1377" height="959" alt="Screenshot 2025-11-20 180959" src="https://github.com/user-attachments/assets/b96587b1-e820-456a-bb65-77d53b86c317" />

<img width="1377" height="959" alt="Screenshot 2025-11-20 180926" src="https://github.com/user-attachments/assets/b7502b37-4302-4846-bb71-747f7c84d5bf" />


Dashboard

Temperature Graphs

🌱 Future Enhancements

Multi-reactor simulation

Pressure modeling

Better graph styling

Network communication

AI-based anomaly detection

Reactor state saving/loading

👤 Author

Karanam Ganesh
Java Developer | Simulation Systems Enthusiast
GitHub: https://github.com/ganesh805
Gmail: ganeshkaranam629@gmail.com
