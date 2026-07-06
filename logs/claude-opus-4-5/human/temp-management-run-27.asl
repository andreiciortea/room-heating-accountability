+temp(T) : window_state(State) & State \== "closed" & heating(true)
    <- .print("Window is ", State, " while heating. Stopping heater to save energy.");
       stopHeating;
       .send(jane, tell, heating_stopped_window_open).

+temp(T) : target_temp(Target) & T < Target & window_state(State) & State \== "closed" & heating(false)
    <- .print("Temperature ", T, "°C is below target ", Target, "°C but window is ", State, ". Cannot start heating.");
       .send(jane, tell, cannot_heat_window_open).

+temp(T) : target_temp(Target) & T < Target & window_state("closed") & heating(false)
    <- .print("Temperature ", T, "°C is below target ", Target, "°C. Starting heater.");
       startHeating.

+temp(T) : target_temp(Target) & T >= Target & heating(true)
    <- .print("Temperature ", T, "°C reached target ", Target, "°C. Stopping heater.");
       stopHeating.

+temp(T) : true
    <- .print("Current temperature: ", T, "°C").