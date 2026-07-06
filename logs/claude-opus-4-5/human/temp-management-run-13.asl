+temp(T) : target_temp(Target) & T < Target & heating(false) & window_state("closed")
    <- .print("Temperature ", T, "°C is below target ", Target, "°C. Starting heater.");
       startHeating.

+temp(T) : target_temp(Target) & T < Target & heating(false) & not window_state("closed")
    <- .print("Temperature ", T, "°C is below target ", Target, "°C but window is not closed. Cannot start heating.");
       .send(jane, tell, "Heating delayed: window is open. Please close the window to enable heating.").

+temp(T) : target_temp(Target) & T >= Target & heating(true)
    <- .print("Temperature ", T, "°C reached target ", Target, "°C. Stopping heater.");
       stopHeating.

+temp(T) : heating(true) & not window_state("closed")
    <- .print("Window is open while heating. Stopping heater to prevent energy waste.");
       .send(jane, tell, "Heating stopped: window is open. Heat would escape and waste energy.");
       stopHeating.

+temp(T) : true
    <- .print("Current temperature: ", T, "°C").

+window_state(State) : heating(true) & not State == "closed"
    <- .print("Window changed to ", State, " while heating. Stopping heater.");
       .send(jane, tell, "Heating stopped: window was opened. Please close window to resume heating.");
       stopHeating.

+window_state("closed") : target_temp(Target) & temp(T) & T < Target & heating(false)
    <- .print("Window closed. Temperature ", T, "°C is below target ", Target, "°C. Starting heater.");
       startHeating.