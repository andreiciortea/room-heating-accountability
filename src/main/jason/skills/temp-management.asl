// Reactive plan: start heating when temperature drops below target
+temp(T) : target_temp(Target) & T < Target & heating(false)
    <- .print("Temperature ", T, "°C is below target ", Target, "°C. Starting heater.");
       startHeating.

// Reactive plan: stop heating when temperature reaches or exceeds target
+temp(T) : target_temp(Target) & T >= Target & heating(true)
    <- .print("Temperature ", T, "°C reached target ", Target, "°C. Stopping heater.");
       stopHeating.

// Inform about temperature changes
+temp(T) : true
    <- .print("Current temperature: ", T, "°C").