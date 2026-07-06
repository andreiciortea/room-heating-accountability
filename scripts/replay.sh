#!/bin/bash
set -euo pipefail

SKILL_FILE="src/main/jason/skills/temp-management.asl"
BACKUP_FILE="${SKILL_FILE}.bak"
SKILL_COPY_ACTIVE=false

usage() {
    cat <<EOF
Usage: $0 --mode MODE --scenario SCENARIO [--run N | --skill PATH] [--out DIR]

Modes:
  experiment          Full accountability loop (LLM judge + patcher). No --run/--skill needed.
  replay_corrective   Start with base skill; hot-swap pre-patched skill at the accountability trigger.
  replay_preventive   Start with pre-patched skill compiled in; accountability trigger is ignored.

Scenarios:
  base    Base scenario  (runs room_heating.jcm)
  human   Human scenario (runs room_heating_human.jcm)

Skill selection (required for replay modes):
  --run N             Resolve logs/<model>/<scenario>/temp-management-run-N.asl
  --skill PATH        Explicit path to the patched .asl file
  --model NAME        Log model directory (default: claude-opus-4-5)

Options:
  --out DIR           Archive mas-0.log (and effective skill for replay modes) into DIR after the run

Examples:
  $0 --mode experiment --scenario base
  $0 --mode replay_corrective --scenario base --run 1
  $0 --mode replay_corrective --scenario human --skill logs/claude-opus-4-5/human/temp-management-run-5.asl
  $0 --mode replay_preventive --scenario base --run 3 --out results/
EOF
    exit 1
}

MODE=""
SCENARIO=""
RUN=""
SKILL_PATH=""
MODEL="claude-opus-4-5"
OUT_DIR=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --mode)     MODE="$2";      shift 2 ;;
        --scenario) SCENARIO="$2";  shift 2 ;;
        --run)      RUN="$2";       shift 2 ;;
        --skill)    SKILL_PATH="$2"; shift 2 ;;
        --model)    MODEL="$2";     shift 2 ;;
        --out)      OUT_DIR="$2";   shift 2 ;;
        -h|--help)  usage ;;
        *) echo "Unknown argument: $1"; usage ;;
    esac
done

# Validate required args
[[ -z "$MODE" ]]     && { echo "Error: --mode is required"; usage; }
[[ -z "$SCENARIO" ]] && { echo "Error: --scenario is required"; usage; }

case "$MODE" in
    experiment|replay_corrective|replay_preventive) ;;
    *) echo "Error: unknown mode '$MODE'"; usage ;;
esac

case "$SCENARIO" in
    base)  GRADLE_TASK="replayAgents" ;;
    human) GRADLE_TASK="replayAgentsHuman" ;;
    *) echo "Error: unknown scenario '$SCENARIO'"; usage ;;
esac

# Resolve skill path for replay modes
if [[ "$MODE" == "replay_corrective" || "$MODE" == "replay_preventive" ]]; then
    if [[ -n "$SKILL_PATH" ]]; then
        : # explicit path provided
    elif [[ -n "$RUN" ]]; then
        SKILL_PATH="logs/${MODEL}/${SCENARIO}/temp-management-run-${RUN}.asl"
    else
        echo "Error: replay modes require --run N or --skill PATH"
        usage
    fi
    [[ ! -f "$SKILL_PATH" ]] && { echo "Error: skill file not found: $SKILL_PATH"; exit 1; }
    echo "Using patched skill: $SKILL_PATH"
fi

# Cleanup handler — restores temp-management.asl if we swapped it in for replay_preventive
cleanup() {
    if [[ "$SKILL_COPY_ACTIVE" == "true" ]]; then
        echo ""
        echo "Restoring original skill file..."
        cp "$BACKUP_FILE" "$SKILL_FILE"
        echo "Restored $SKILL_FILE"
    fi
    if [[ -n "$OUT_DIR" && -f "log/mas-0.log" ]]; then
        mkdir -p "$OUT_DIR"
        RUN_LABEL="${MODE}-${SCENARIO}${RUN:+-run${RUN}}"
        cp "log/mas-0.log" "${OUT_DIR}/mas-${RUN_LABEL}.log"
        echo "Archived log to ${OUT_DIR}/mas-${RUN_LABEL}.log"
        if [[ "$MODE" != "experiment" && -n "$SKILL_PATH" ]]; then
            cp "$SKILL_PATH" "${OUT_DIR}/skill-${RUN_LABEL}.asl"
            echo "Archived skill to ${OUT_DIR}/skill-${RUN_LABEL}.asl"
        fi
    fi
}
trap cleanup EXIT

# For replay_preventive: swap the patched skill in before JaCaMo starts
if [[ "$MODE" == "replay_preventive" ]]; then
    echo "replay_preventive: copying patched skill to $SKILL_FILE"
    cp "$SKILL_FILE" "$BACKUP_FILE"
    cp "$SKILL_PATH" "$SKILL_FILE"
    SKILL_COPY_ACTIVE=true
fi

# Build Gradle args
GRADLE_ARGS=("-Pmode=$MODE" "-Pscenario=$SCENARIO")
[[ -n "$SKILL_PATH" ]] && GRADLE_ARGS+=("-Pskill=$SKILL_PATH")
[[ -n "$MODEL" ]]      && GRADLE_ARGS+=("-Pmodel=$MODEL")

echo "Starting: ./gradlew $GRADLE_TASK ${GRADLE_ARGS[*]}"
echo "Close the JaCaMo application when done."
echo ""

./gradlew "$GRADLE_TASK" "${GRADLE_ARGS[@]}"
