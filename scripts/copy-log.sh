#!/bin/bash

if [ -z "$1" ]; then
    echo "Usage: $0 <run-number> [--scenario base|human] [--model MODEL]"
    echo "  run-number    Run number to append to the archived filename"
    echo "  --scenario    base or human (default: human)"
    echo "  --model       Log model directory (default: claude-opus-4-5)"
    exit 1
fi

RUN_NUMBER=$1
shift

SCENARIO="human"
MODEL="claude-opus-4-5"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --scenario) SCENARIO="$2"; shift 2 ;;
        --model)    MODEL="$2";    shift 2 ;;
        *) echo "Unknown argument: $1"; exit 1 ;;
    esac
done

LOG_SOURCE_DIR="log"
LOGS_DIR="logs/${MODEL}/${SCENARIO}"

mkdir -p "$LOGS_DIR"

if [ -f "${LOG_SOURCE_DIR}/mas-0.log" ]; then
    cp "${LOG_SOURCE_DIR}/mas-0.log" "${LOGS_DIR}/mas-run-${RUN_NUMBER}.log"
    echo "Copied log to ${LOGS_DIR}/mas-run-${RUN_NUMBER}.log"
else
    echo "Warning: ${LOG_SOURCE_DIR}/mas-0.log not found"
fi
