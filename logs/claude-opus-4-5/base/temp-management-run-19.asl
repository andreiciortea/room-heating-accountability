+temp(T) : target_temp(Target) & T < Target & heating(false) & window_state("closed")
    <- .print("Temperature ", T, "°C is below target ", Target, "°C. Starting heater.");
       startHeating.

+temp(T) : target_temp(Target) & T < Target & heating(false) & window_state("tilted")
    <- .print("Temperature ", T, "°C is below target ", Target, "°C. Closing window before starting heater.");
       close;
       startHeating.

+temp(T) : target_temp(Target) & T >= Target & heating(true)
    <- .print("Temperature ", T, "°C reached target ", Target, "°C. Stopping heater.");
       stopHeating.

+temp(T) : heating(true) & window_state("tilted")
    <- .print("Window is tilted while heating. Closing window to prevent heat loss.");
       close.

+window_state("tilted") : heating(true)
    <- .print("Window opened while heating. Closing window to prevent heat loss.");
       close.

+temp(T) : true
    <- .print("Current temperature: ", T, "°C").