// Evaluator agent - monitors energy consumption and flags inefficiency
// Based on room-heating-scenario.md expectations

// Expected energy to heat from 15°C to 20°C with closed window
// With 1 min simulated per tick, ~8 ticks needed, 1 kW heater:
// Expected ≈ 8 * (1 kW * 1/60 hr) ≈ 0.133 kWh
// Using 0.15 kWh as threshold with small margin
expected_energy(0.15).

// Flag to track if warning has been issued
warning_issued(false).

// Initial goal
!start.

// Setup plan - focus on the heater artifact
+!start : true
    <- .print("Evaluator agent started. Monitoring energy consumption.");
       .print("Expected energy threshold: 0.15 kWh").
    //    focus(heater).

// Reactive plan: warn when energy consumption exceeds expected level
+energyConsumed(E) : expected_energy(Expected) & E > Expected & warning_issued(false)
    <- -warning_issued(false);
       +warning_issued(true);
       .print("*** WARNING: Energy consumption (", E, " kWh) exceeds expected level (", Expected, " kWh) ***");
       .send(heating_controller, achieve, provideAccount);
       .send(jane, achieve, provideAccount).

+account(Account)[source(heating_controller)] : true
    <- .print("Received account from heating controller: ", Account);
       +account_heating_agent(Account).
    //    !evaluate_account(Account).

+account(Account)[source(jane)] : true
    <- .print("Received account from jane: ", Account);
       +account_jane(Account).

+account_heating_agent(A1) : account_jane(A2)
    <- !evaluate_accounts.

+account_heating_agent(A1) : true
    <- .print("Waiting for jane's account...").

+account_jane(_) : account_heating_agent(_)
    <- !evaluate_accounts.

+account_jane(A1) : true
    <- .print("Waiting for heating agent's account...").

+!evaluate_accounts : account_jane(A1) & account_heating_agent(A2)
    <- .print("Requesting LLM evaluation of account...");
       judgeAccount(A1, A2, Evaluation);
       .print("=== LLM EVALUATION ===");
       .print(Evaluation);
       .print("======================");
       .print("Sending evaluation to heating_controller");
       .send(heating_controller, tell, account_evaluation(Evaluation)).

// Plan to evaluate the account using an LLM-as-judge
+!evaluate_account(AccountStr) : true
    <- .print("Requesting LLM evaluation of account...");
       judgeAccount(AccountStr, Evaluation);
       .print("=== LLM EVALUATION ===");
       .print(Evaluation);
       .print("======================");
       .print("Sending evaluation to heating_controller");
       .send(heating_controller, tell, account_evaluation(Evaluation)).

{ include("$jacamoJar/templates/common-cartago.asl") }
