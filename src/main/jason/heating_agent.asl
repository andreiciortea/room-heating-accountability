// Heating agent - controls a heater to maintain target temperature

// Initial beliefs
target_temp(20).

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

// experiment mode: full accountability loop (build account, send to evaluator)
+!provideAccount : run_mode("experiment") & temp_management_skill(SkillPath)
    <- .print("Received request from energy_evaluator to provide an account");
       getAccount(SkillPath, Account);
       .send(energy_evaluator, tell, account(Account)).

// replay_corrective: bypass LLM judge+patcher, hot-swap from pre-patched skill file
+!provideAccount : run_mode("replay_corrective") & patched_skill_path(PatchedPath) & temp_management_skill(SkillPath)
    <- .print("replay_corrective: loading pre-patched skill from ", PatchedPath);
       loadPatchedSkill(PatchedPath, Success, FixedPlans);
       if (Success) {
           .print("*** Pre-patched skill loaded. Hot-swapping plans. ***");
           .relevant_plans({ +temp(T) }, _, LL);
           .remove_plan(LL);
           .add_plan(FixedPlans);
           .print("*** Hot-swap complete. ***");
       } else {
           .print("*** Failed to load pre-patched skill ***");
       }.

// replay_preventive: skill already compiled in at startup, ignore accountability trigger
+!provideAccount : run_mode("replay_preventive")
    <- .print("replay_preventive: accountability trigger ignored (patched skill already active)").

// fallback if run_mode belief is missing (treated as experiment)
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
