#!/bin/bash

if [ -z "$1" ]; then
    echo "Usage: $0 <run-number>"
    exit 1
fi

RUN_NUMBER=$1
SKILL_FILE="src/main/jason/skills/temp-management.asl"
BACKUP_FILE="${SKILL_FILE}.bak"
LOGS_DIR="logs/claude-opus-4-5/human"

# Ensure logs directory exists
mkdir -p "$LOGS_DIR"

# Copy current skill file to logs with run number
cp "$SKILL_FILE" "${LOGS_DIR}/temp-management-run-${RUN_NUMBER}.asl"
echo "Copied skill to ${LOGS_DIR}/temp-management-run-${RUN_NUMBER}.asl"

# Restore from backup
cp "$BACKUP_FILE" "$SKILL_FILE"
echo "Restored skill from backup"
