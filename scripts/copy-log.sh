#!/bin/bash

if [ -z "$1" ]; then
    echo "Usage: $0 <run-number>"
    exit 1
fi

RUN_NUMBER=$1
LOG_SOURCE_DIR="log"
LOGS_DIR="logs/claude-opus-4-5/human"

# Ensure logs directory exists
mkdir -p "$LOGS_DIR"

# Copy mas log file with run number
if [ -f "${LOG_SOURCE_DIR}/mas-0.log" ]; then
    cp "${LOG_SOURCE_DIR}/mas-0.log" "${LOGS_DIR}/mas-run-${RUN_NUMBER}.log"
    echo "Copied log to ${LOGS_DIR}/mas-run-${RUN_NUMBER}.log"
else
    echo "Warning: ${LOG_SOURCE_DIR}/mas-0.log not found"
fi
