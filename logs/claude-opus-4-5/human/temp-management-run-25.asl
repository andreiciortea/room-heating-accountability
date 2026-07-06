+temp(T) : target_temp(Target) & T < Target & heating(true) & not window_state("closed")
    <- .print("Window is open while heating. Stopping heater to save energy.");
       stopHeating;
       .send(jane, tell, "Heating suspended because window is not closed").

+temp(T) : target_temp(Target) & T < Target & heating(false) & window_state("closed")
    <- .print("Temperature ", T, "°C is below target ", Target, "°C. Starting heater.");
       startHeating.

+temp(T) : target_temp(Target) & T >= Target & heating(true)
    <- .print("Temperature ", T, "°C reached target ", Target, "°C. Stopping heater.");
       stopHeating.

+temp(T) : true
    <- .print("Current temperature: ", T, "°C").

+window_state("closed") : temp(T) & target_temp(Target) & T < Target & heating(false)
    <- .print("Window closed. Temperature ", T, "°C is below target ", Target, "°C. Starting heater.");
       startHeating;
       .send(jane, tell, "Heating resumed now that window is closed").