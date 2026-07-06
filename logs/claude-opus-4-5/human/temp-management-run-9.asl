+temp(T) : target_temp(Target) & T < Target & heating(true) & window_state(State) & State \== "closed"
    <- .print("Window is ", State, ". Stopping heater to prevent energy waste.");
       stopHeating;
       .send(jane, tell, heating_stopped_window_open).

+temp(T) : target_temp(Target) & T < Target & heating(false) & window_state(State) & State \== "closed"
    <- .print("Cannot start heating: window is ", State, ". Please close the window.");
       .send(jane, tell, cannot_heat_window_open).

+temp(T) : target_temp(Target) & T < Target & heating(false) & window_state("closed")
    <- .print("Temperature ", T, "°C is below target ", Target, "°C. Starting heater.");
       startHeating.

+temp(T) : target_temp(Target) & T >= Target & heating(true)
    <- .print("Temperature ", T, "°C reached target ", Target, "°C. Stopping heater.");
       stopHeating.

+temp(T) : true
    <- .print("Current temperature: ", T, "°C").