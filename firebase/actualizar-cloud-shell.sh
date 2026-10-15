#!/usr/bin/env bash
set -euo pipefail
CIVITA_ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
# Ejecutar en Cloud Shell, con la cuenta que administra el proyecto.
gcloud config set project civita-5cc42
python3 "$CIVITA_ROOT/firebase/initialize.py" --diagnose
python3 -m unittest discover -s "$CIVITA_ROOT/firebase/tests"
# Primero se compilan y prueban las reglas contra emuladores locales.
cd "$CIVITA_ROOT/tests/firebase"
npm ci --ignore-scripts --no-audit --no-fund
npm test
cd "$CIVITA_ROOT"
python3 firebase/initialize.py --apply --enable-email --configure-android --output "$HOME/civita-resultado"
printf '%s\n' 'Firestore actualizado. Las evidencias requieren Cloud Storage habilitado y storage.rules publicadas.'
