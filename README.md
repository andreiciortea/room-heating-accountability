# Room Heating Accountability

A multi-agent system demonstrating how accountability can support self-improvement in a home heating management scenario. The project is built with JaCaMo and uses LLMs for evaluating agent accounts and improving Jason plans.

## Overview

This project models a Swiss home heating scenario where:
- A **heating agent (accountor)** manages a room heater to maintain target temperatures
- An **evaluator agent (accountee)** monitors energy consumption and flags inefficiencies
- When efficiency issues are detected, the evaluator agent uses an **LLM-as-judge** to analyze the heating agent's account and provide feedback
- The heating agent uses an LLM to process the feedback and automatically **patch, improve, and reload** its skill for managing the temperature

The scenario simulates a living room with a tilted window that causes significant heat loss, demonstrating how agents can be held accountable for inefficient behavior and improve through LLM-guided feedback. The project implements also an extended version of the scenario in which a human decided to leave the window open for a bird to fly out.

## Prerequisites

- **Java 21** or later
- **Gradle** (uses wrapper, no separate installation needed)
- An **LLM API key** from one of: Anthropic, OpenAI, or Google Gemini

## Project Structure

```
room-heating-accountability/
├── build.gradle.kts           # Gradle build configuration
├── room_heating.jcm           # JaCaMo multi-agent system configuration
├── .env                       # LLM configuration (API keys, model)
├── logging.properties         # Logging configuration
├── room-heating-scenario.md   # Detailed scenario documentation
│
└── src/main/
    ├── jason/                 # Agent behavior (AgentSpeak)
    │   ├── heating_agent.asl           # Heating controller agent (accountor)
    │   ├── evaluator_agent.asl         # Energy monitoring agent (accountee)
    │   ├── evaluator_agent_human.asl   # Energy monitoring agent for the extended scenario (accountee)
    │   ├── human_agent.asl             # Proxy for the human accountor
    │   └── skills/
    │       └── temp-management.asl # Temperature control plans
    │
    └── cartago/               # Artifacts (Java)
        └── org/hyperagents/demo/
            ├── Heater.java         # Heater simulation
            ├── Window.java         # Window state management
            ├── LLMJudge.java       # LLM evaluation artifact
            ├── AccountBuilder.java # Accountability report builder
            └── ASLPatcher.java     # LLM-powered plan patcher
```

## Configuration

### Environment Variables

Copy the example environment file and configure your API key:

```bash
cp .env.example .env
```

Edit `.env` with your settings:

```properties
# LLM Provider: anthropic, openai, or gemini
LLM_PROVIDER=anthropic

# Model name (provider-specific)
LLM_MODEL=claude-sonnet-4-5-20250929

# API Keys (set the one matching your provider)
ANTHROPIC_API_KEY=your-anthropic-key
OPENAI_API_KEY=your-openai-key
GOOGLE_API_KEY=your-google-key
```

**Configuration priority** (highest to lowest):
1. System environment variables
2. `.env` file values
3. Default values in code

### Logging

JaCaMo's logging configuration is in `logging.properties`.

## Running the agents

```bash
./gradlew runAgents
```

This launches the JaCaMo multi-agent system with:
- `heating_controller` - manages the heater to reach target temperature
- `energy_evaluator` - monitors energy consumption and triggers LLM evaluation

The simulation uses accelerated time (1 real second = 60 simulated seconds).

To run the extended scenario with a human accountor:

```bash
./gradlew runAgentsHuman
```

In addition to the above agents, this launches a third agent:
- `jane` — a proxy for the human accountor with a predefined account and behavior (e.g., closing the window if informed by the `heating_controller` that the window needs to be closed to continue heating)
