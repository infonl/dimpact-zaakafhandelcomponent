#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Generates the PDF files of the ZAC manuals for a ZAC version.
# Usage: generate-manual-pdfs.sh <version, e.g. 5.10> <output directory>

set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <version, e.g. 5.10> <output directory>" >&2
  exit 1
fi

version="$1"
outputDirectory="$(mkdir -p "$2" && cd "$2" && pwd)"
generatorDirectory="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
manualsDirectory="$(dirname "$generatorDirectory")"

cd "$generatorDirectory"
node render-pdf.mjs \
  "$manualsDirectory/ZAC-gebruikershandleiding/ZAC-gebruikershandleiding.md" \
  "$outputDirectory/ZAC-V${version}-gebruikershandleiding.pdf" \
  "ZAC Gebruikershandleiding"
node render-pdf.mjs \
  "$manualsDirectory/inrichting-zaakafhandelcomponent/inrichting-zaakafhandelcomponent.md" \
  "$outputDirectory/ZAC-V${version}-inrichting-zaakafhandelcomponent.pdf" \
  "Inrichting Zaakafhandelcomponent"
