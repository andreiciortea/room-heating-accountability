+temp(T) : target_temp(Target) & T < Target & heating(true) & window_state(State) & State \== "closed"
    <- .print("Window is ", State, ". Stopping heater to save energy.");
       stopHeating;
       .send(jane, tell, heating_blocked_by_open_window).

+temp(T) : target_temp(Target) & T < Target & heating(false) & window_state(State) & State \== "closed"
    <- .print("Temperature ", T, "°C is below target ", Target, "°C but window is ", State, ". Cannot start heating.");
       .send(jane, tell, heating_needed_but_window_open).

+temp(T) : target_temp(Target) & T < Target & heating(false) & window_state("closed")
    <- .print("Temperature ", T, "°C is below target ", Target, "°C. Starting heater.");
       startHeating.

+temp(T) : target_temp(Target) & T >= Target & heating(true)
    <- .print("Temperature ", T, "°C reached target ", Target, "°C. Stopping heater.");
       stopHeating.

+temp(T) : true
    <- .print("Current temperature: ", T, "°C").