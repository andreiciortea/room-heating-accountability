package org.hyperagents.demo;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.OpFeedbackParam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AccountBuilder extends Artifact {

    private static final String ACCOUNT_TEMPLATE = """    
        Here is my account of what happened.

        ACTIONS:

        I've started heating to reach target temperature.

        MY BELIEFS:

        When I started heating, I had the following beliefs:

        temp(15).
        target_temp(20).
        window_state("tilted").
        heating(false).

        INITIAL ENVIRONMENT STATE:

        When I started heating, the living room was in the following state:
        
        temp(15).
        window_state("tilted").
        heating(false).

        CURRENT ENVIRONMENT STATE:

        temp(16.8).
        window_state("tilted").
        heating(true).

        SKILLS:

        %s
        """;

    @OPERATION
    public void getAccount(String skillPath, OpFeedbackParam<String> account) {
        try {
            String skillContent = readSkillFile(skillPath);
            String builtAccount = String.format(ACCOUNT_TEMPLATE, skillContent);
            account.set(builtAccount);
            log("Built account with skill from: " + skillPath);
        } catch (IOException e) {
            log("Error reading skill file: " + e.getMessage());
            account.set("Error: Could not read skill file at " + skillPath);
        }
    }

    private String readSkillFile(String skillPath) throws IOException {
        Path path = Path.of(skillPath);
        if (!Files.exists(path)) {
            throw new IOException("Skill file not found: " + skillPath);
        }
        return Files.readString(path);
    }
}
