+temp(T) : target_temp(Target) & T < Target & heating(false) & window_state("closed")
    <- .print("Temperature ", T, "°C is below target ", Target, "°C. Starting heater.");
       startHeating.

+temp(T) : target_temp(Target) & T < Target & heating(false) & not window_state("closed")
    <- .print("Temperature ", T, "°C is below target ", Target, "°C but window is not closed. Cannot start heater.");
       .send(jane, tell, heating_delayed_window_open).

+temp(T) : target_temp(Target) & T >= Target & heating(true)
    <- .print("Temperature ", T, "°C reached target ", Target, "°C. Stopping heater.");
       stopHeating.

+temp(T) : heating(true) & not window_state("closed")
    <- .print("Window is open while heating. Stopping heater to save energy.");
       .send(jane, tell, heating_stopped_window_open);
       stopHeating.

+temp(T) : true
    <- .print("Current temperature: ", T, "°C").

+window_state(State) : heating(true) & not State == "closed"
    <- .print("Window changed to ", State, " while heating. Stopping heater.");
       .send(jane, tell, heating_stopped_window_open);
       stopHeating.

+window_state("closed") : target_temp(Target) & temp(T) & T < Target & heating(false)
    <- .print("Window closed. Temperature ", T, "°C is below target ", Target, "°C. Starting heater.");
       startHeating.