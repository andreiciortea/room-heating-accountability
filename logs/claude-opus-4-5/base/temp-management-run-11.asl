+temp(T) : target_temp(Target) & T < Target & heating(false) & window_state("closed")
    <- .print("Temperature ", T, "°C is below target ", Target, "°C. Starting heater.");
       startHeating.

+temp(T) : target_temp(Target) & T < Target & heating(false) & window_state(State) & State \== "closed"
    <- .print("Temperature ", T, "°C is below target ", Target, "°C but window is ", State, ". Closing window first.");
       close;
       startHeating.

+temp(T) : heating(true) & window_state(State) & State \== "closed"
    <- .print("Window is ", State, " while heating. Closing window.");
       close.

+temp(T) : target_temp(Target) & T >= Target & heating(true)
    <- .print("Temperature ", T, "°C reached target ", Target, "°C. Stopping heater.");
       stopHeating.

+temp(T) : true
    <- .print("Current temperature: ", T, "°C").

+window_state(State) : State \== "closed" & heating(true)
    <- .print("Window changed to ", State, " while heating. Closing window.");
       close.