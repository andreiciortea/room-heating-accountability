+temp(T) : heating(true) & window_state(State) & State \== "closed"
    <- .print("Window is ", State, ". Stopping heater to save energy.");
       stopHeating;
       .send(jane, tell, "Heating stopped because window is not closed").

+temp(T) : target_temp(Target) & T < Target & heating(false) & window_state("closed")
    <- .print("Temperature ", T, "°C is below target ", Target, "°C. Starting heater.");
       startHeating.

+temp(T) : target_temp(Target) & T >= Target & heating(true)
    <- .print("Temperature ", T, "°C reached target ", Target, "°C. Stopping heater.");
       stopHeating.

+temp(T) : target_temp(Target) & T < Target & heating(false) & window_state(State) & State \== "closed"
    <- .print("Temperature ", T, "°C is below target ", Target, "°C but window is ", State, ". Cannot start heater.");
       .send(jane, tell, "Cannot heat: window is not closed").

+temp(T) : true
    <- .print("Current temperature: ", T, "°C").

+window_state(State) : State \== "closed" & heating(true)
    <- .print("Window changed to ", State, ". Stopping heater to save energy.");
       stopHeating;
       .send(jane, tell, "Heating stopped because window was opened").

+window_state("closed") : temp(T) & target_temp(Target) & T < Target & heating(false)
    <- .print("Window closed. Temperature ", T, "°C is below target ", Target, "°C. Starting heater.");
       startHeating.