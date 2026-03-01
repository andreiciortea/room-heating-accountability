!start.

+!start : true
    <- .print("Hello world from a simulated human agent!").

+!provideAccount : true
    <- .print("Received request from energy_evaluator to provide an account");
       .send(energy_evaluator, tell, account("I left the window open because there is a bird in the living room. Maybe she will fly out.")).

+!kqml_received(heating_controller, Performative, Content, MsgId) : true
    <- .print("Received message from heating_controller: ", Performative, " ", Content);
       .print("Closing window!");
       close.

{ include("$jacamoJar/templates/common-cartago.asl") }