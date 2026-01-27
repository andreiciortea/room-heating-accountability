package org.hyperagents.demo;

import cartago.ARTIFACT_INFO;
import cartago.Artifact;
import cartago.OPERATION;
import cartago.OUTPORT;
import cartago.ObsProperty;
import cartago.OperationException;

@ARTIFACT_INFO(
    outports = {
        @OUTPORT(name = "heater")
    })
public class Window extends Artifact {

    protected void init(String initialState) {
        defineObsProperty("window_state", initialState);
        // Note: Cannot notify heater on init since link is not yet established
        // The agent must link artifacts and then check initial state
    }

    @OPERATION
    public void tilt() {
        ObsProperty stateProp = getObsProperty("window_state");
        stateProp.updateValue("tilted");
        log("Window tilted");
        try {
            execLinkedOp("heater", "onWindowStateChanged", "tilted");
        } catch (OperationException e) {
            e.printStackTrace();
        }
    }

    @OPERATION
    public void close() {
        ObsProperty stateProp = getObsProperty("window_state");
        stateProp.updateValue("closed");
        log("Window closed");
        try {
            execLinkedOp("heater", "onWindowStateChanged", "closed");
        } catch (OperationException e) {
            e.printStackTrace();
        }
    }

    @OPERATION
    public void notifyState() {
        ObsProperty stateProp = getObsProperty("window_state");
        String currentState = (String) stateProp.getValue();
        log("Notifying linked artifact of current state: " + currentState);
        try {
            execLinkedOp("heater", "onWindowStateChanged", currentState);
        } catch (OperationException e) {
            e.printStackTrace();
        }
    }
}
