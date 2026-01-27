# Room Heating Accountability

A multi-agent system demonstrating how accountability can support self-improvement in a home heating management scenario. The project is built with JaCaMo and uses LLMs for evaluating agent accounts and improving Jason plans.

## Overview

This project models a Swiss home heating scenario where:
- A **heating agent** manages a room heater to maintain target temperatures
- An **evaluator agent** monitors energy consumption and flags inefficiencies
- When efficiency issues are detected, an **LLM-as-judge** analyzes the heating agent's account and provides feedback
- The feedback is used to automatically **patch, improve, and relload the agent's plans**

The scenario simulates a living room with a tilted window that causes significant heat loss, demonstrating how agents can be held accountable for inefficient behavior and improve through LLM-guided feedback.

## Prerequisites

- **Java 21** or later
- **Gradle** (uses wrapper, no separate installation needed)
- An **LLM API key** from one of: Anthropic, OpenAI, or Google Gemini

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
    │   ├── heating_agent.asl      # Heating controller agent
    │   ├── evaluator_agent.asl    # Energy monitoring agent
    │   └── skills/
    │       └── temp-management.asl # Temperature control plans
    │
    └── cartago/               # Artifacts (Java)
        └── org/hyperagents/demo/
            ├── Heater.java        # Heater simulation
            ├── Window.java        # Window state management
            ├── LLMJudge.java      # LLM evaluation artifact
            ├── AccountBuilder.java # Accountability report builder
            └── ASLPatcher.java    # LLM-powered plan patcher
```

## How It Works

1. The **heating agent** monitors room temperature and controls the heater to reach the 20°C target
2. The **window** starts tilted, causing significant heat loss (61% energy waste)
3. The **evaluator agent** detects when energy consumption exceeds the expected threshold (0.15 kWh)
4. An **accountability report** is generated, including the agent's beliefs, actions, and skill plans
5. The **LLM judge** artifact uses an LLM to analyze the report and identifies blind spots, corrective actions, and prescriptive actions
6. The **ASL patcher** artifact uses LLM feedback to improve the agent's temperature management skill (a set of plans)
7. The improved skill is loaded, and the agent continues with better behavior
