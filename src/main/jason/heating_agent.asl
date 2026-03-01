// Heating agent - controls a heater to maintain target temperature

// Initial beliefs
target_temp(20).
heating(true).

temp_management_skill("src/main/jason/skills/temp-management.asl").

// Initial goal
!start.

// Setup plan - link artifacts and focus on the heater
+!start : true
    <- .print("Heating agent started. Target temperature: 20°C");
       lookupArtifact("window", WindowId);
       lookupArtifact("heater", HeaterId);
       linkArtifacts(WindowId, "heater", HeaterId);
       .print("Linked window to heater");
       focus(WindowId);
       focus(HeaterId).

+!provideAccount : temp_management_skill(SkillPath)
    <- .print("Received request from energy_evaluator to provide an account");
       getAccount(SkillPath, Account);
       .send(energy_evaluator, tell, account(Account)).

+account_evaluation(Evaluation) : true
    <- .print("Received accont evaluation:\n", Evaluation);
       !patch_skill(Evaluation).

// Plan to patch the agent skill based on evaluation feedback
+!patch_skill(Evaluation) : temp_management_skill(SkillPath) & human_comm(HumanComm)
    <- .print("Attempting to patch temperature management skill...");
       patchAgentSkill(SkillPath, Evaluation, HumanComm, Success, FixedPlans);
       if (Success) {
           .print("*** Skill successfully patched! ***");
           .print("Removing skill plans");
           .relevant_plans({ +temp(T) }, _, LL);
           .remove_plan(LL);
           .print("Adding fixed skill plans");
           .add_plan(FixedPlans);
       } else {
           .print("*** Failed to patch skill ***");
       }.


{ include("skills/temp-management.asl") }
{ include("$jacamoJar/templates/common-cartago.asl") }
