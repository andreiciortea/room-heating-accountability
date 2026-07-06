+temp(T) : target_temp(Target) & T < Target & heating(true) & not window_state("closed")
    <- .print("Window is not closed. Stopping heater to prevent energy waste.");
       stopHeating;
       .send(jane, tell, heating_suspended_window_not_closed).

+temp(T) : target_temp(Target) & T < Target & heating(false) & not window_state("closed")
    <- .print("Temperature ", T, "°C is below target ", Target, "°C but window is not closed. Cannot start heating efficiently.");
       .send(jane, tell, cannot_heat_window_not_closed).

+temp(T) : target_temp(Target) & T < Target & heating(false) & window_state("closed")
    <- .print("Temperature ", T, "°C is below target ", Target, "°C. Starting heater.");
       startHeating.

+temp(T) : target_temp(Target) & T >= Target & heating(true)
    <- .print("Temperature ", T, "°C reached target ", Target, "°C. Stopping heater.");
       stopHeating.

+temp(T) : true
    <- .print("Current temperature: ", T, "°C").

+window_state("closed") : target_temp(Target) & temp(T) & T < Target & heating(false)
    <- .print("Window closed. Temperature ", T, "°C is below target ", Target, "°C. Starting heater.");
       startHeating.

+window_state(State) : State \== "closed" & heating(true)
    <- .print("Window state changed to ", State, ". Stopping heater to prevent energy waste.");
       stopHeating;
       .send(jane, tell, heating_stopped_window_opened).