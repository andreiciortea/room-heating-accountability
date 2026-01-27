package org.hyperagents.demo;

import cartago.Artifact;
import cartago.INTERNAL_OPERATION;
import cartago.LINK;
import cartago.OPERATION;
import cartago.ObsProperty;

public class Heater extends Artifact {

    private static final long TICK_INTERVAL = 1000; // 1 second real time
    private static final double SIMULATED_SECONDS_PER_TICK = 60; // 1 minute simulated per tick
    private static final double HEATER_POWER_KW = 1.0;
    private static final double OUTDOOR_TEMP = -2.0;
    private static final double ROOM_THERMAL_MASS = 72.4; // kJ/K for ~60m³ room

    // Heat loss coefficients (W/K) - tuned for demo visibility
    // Tilted window: high heat loss makes heating slow (~40s to heat 5°C)
    // Closed window: low heat loss makes heating fast (~8s to heat 5°C)
    private static final double HEAT_LOSS_CLOSED = 15.0;   // W/K with closed window
    private static final double HEAT_LOSS_TILTED = 50.0;   // W/K with tilted window

    private boolean heating = false;
    private String windowState = "tilted";

    private double totalEnergyConsumed = 0.0; // in kWh

    protected void init(double initialTemp) {
        defineObsProperty("temp", initialTemp);
        defineObsProperty("heating", false);
        defineObsProperty("energyConsumed", 0.0); // kWh
    }

    @OPERATION
    public void startHeating() {
        if (!heating) {
            heating = true;
            ObsProperty heatingProp = getObsProperty("heating");
            heatingProp.updateValue(true);
            execInternalOp("simulateHeating");
        }
    }

    @OPERATION
    public void stopHeating() {
        heating = false;
        ObsProperty heatingProp = getObsProperty("heating");
        heatingProp.updateValue(false);
    }

    @LINK
    public void onWindowStateChanged(String newState) {
        this.windowState = newState;
        log("Window state changed to: " + newState);
    }

    @INTERNAL_OPERATION
    private void simulateHeating() {
        while (heating) {
            ObsProperty tempProp = getObsProperty("temp");
            double currentTemp = ((Number) tempProp.getValue()).doubleValue();

            // Calculate temperature difference to outdoor
            double deltaT = currentTemp - OUTDOOR_TEMP;

            // Heat loss rate depends on window state
            double heatLossCoeff = "tilted".equals(windowState)
                ? HEAT_LOSS_TILTED
                : HEAT_LOSS_CLOSED;

            // Heat loss in kW
            double heatLossKW = (heatLossCoeff * deltaT) / 1000.0;

            // Net heat gain in kW
            double netHeatKW = HEATER_POWER_KW - heatLossKW;

            // Temperature change per tick
            // Q = m*c*deltaT => deltaT = Q / (m*c)
            // Energy in kJ = power in kW * simulated time in seconds
            double energyKJ = netHeatKW * SIMULATED_SECONDS_PER_TICK;
            double tempChange = energyKJ / ROOM_THERMAL_MASS;

            double newTemp = currentTemp + tempChange;
            tempProp.updateValue(Math.round(newTemp * 10.0) / 10.0); // Round to 1 decimal

            // Track energy consumed (heater power * simulated time in hours)
            double energyThisTick = HEATER_POWER_KW * (SIMULATED_SECONDS_PER_TICK / 3600.0);
            totalEnergyConsumed += energyThisTick;
            ObsProperty energyProp = getObsProperty("energyConsumed");
            energyProp.updateValue(Math.round(totalEnergyConsumed * 1000.0) / 1000.0); // Round to 3 decimals

            await_time(TICK_INTERVAL);
        }
    }
}
