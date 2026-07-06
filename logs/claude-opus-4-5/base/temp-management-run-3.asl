+temp(T) : target_temp(Target) & T < Target & heating(false) & window_state("closed")
    <- .print("Temperature ", T, "°C is below target ", Target, "°C. Starting heater.");
       startHeating.

+temp(T) : target_temp(Target) & T < Target & heating(false) & not window_state("closed")
    <- .print("Temperature ", T, "°C is below target ", Target, "°C. Closing window before starting heater.");
       close;
       startHeating.

+temp(T) : target_temp(Target) & T >= Target & heating(true)
    <- .print("Temperature ", T, "°C reached target ", Target, "°C. Stopping heater.");
       stopHeating.

+temp(T) : heating(true) & not window_state("closed")
    <- .print("Warning: Heating while window is not closed. Closing window to prevent heat loss.");
       close.

+temp(T) : true
    <- .print("Current temperature: ", T, "°C").

+window_state(State) : State \== "closed" & heating(true)
    <- .print("Warning: Window changed to ", State, " while heating. Closing window.");
       close.