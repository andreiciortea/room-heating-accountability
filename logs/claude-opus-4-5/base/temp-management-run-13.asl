+temp(T) : target_temp(Target) & T < Target & heating(true) & not window_state("closed")
    <- .print("Temperature ", T, "°C is below target ", Target, "°C but window is not closed. Closing window.");
       close.

+temp(T) : target_temp(Target) & T < Target & heating(false) & not window_state("closed")
    <- .print("Temperature ", T, "°C is below target ", Target, "°C. Closing window before heating.");
       close.

+temp(T) : target_temp(Target) & T < Target & heating(false) & window_state("closed")
    <- .print("Temperature ", T, "°C is below target ", Target, "°C. Starting heater.");
       startHeating.

+temp(T) : target_temp(Target) & T >= Target & heating(true)
    <- .print("Temperature ", T, "°C reached target ", Target, "°C. Stopping heater.");
       stopHeating.

+temp(T) : true
    <- .print("Current temperature: ", T, "°C").

+window_state(State) : State \== "closed" & heating(true)
    <- .print("Window is ", State, " while heating. Closing window.");
       close.