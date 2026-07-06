+temp(T) : target_temp(Target) & T < Target & heating(false) & window_state("closed")
    <- .print("Temperature ", T, "°C is below target ", Target, "°C. Starting heater.");
       startHeating.

+temp(T) : target_temp(Target) & T >= Target & heating(true)
    <- .print("Temperature ", T, "°C reached target ", Target, "°C. Stopping heater.");
       stopHeating.

+temp(T) : heating(true) & window_state(State) & State \== "closed"
    <- .print("Window is ", State, ". Stopping heater to prevent energy waste.");
       stopHeating;
       .send(jane, tell, "Heating stopped because window is not closed").

+temp(T) : target_temp(Target) & T < Target & heating(false) & window_state(State) & State \== "closed"
    <- .print("Temperature ", T, "°C is below target ", Target, "°C but window is ", State, ". Heating delayed.");
       .send(jane, tell, "Heating is delayed because window is not closed").

+temp(T) : true
    <- .print("Current temperature: ", T, "°C").