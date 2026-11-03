#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# Writes the 'Handleidingen' wiki page: for every GitHub release with manual PDFs attached, newest first,
# a link to the release with its release date, and a link to each of its PDFs.
# The page is rebuilt from all releases, so a rerun or a missed run leaves no gaps.
# Usage: write-wiki-manuals-page.sh <repository, e.g. infonl/dimpact-zaakafhandelcomponent> <output file>

set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <repository, e.g. infonl/dimpact-zaakafhandelcomponent> <output file>" >&2
  exit 1
fi

repository="$1"
outputFile="$2"

{
  echo "Handleidingen van de zaakafhandelcomponent (ZAC) als PDF, per release. De nieuwste release staat bovenaan."
  echo
  echo "De [Generate manual PDFs](https://github.com/${repository}/actions/workflows/generate-manual-pdfs.yml)"
  echo "workflow werkt deze pagina bij elke release bij. Pas deze pagina niet met de hand aan."
  echo
  gh api --paginate "repos/${repository}/releases" --jq '
    .[]
    | select((.draft | not) and (.prerelease | not))
    | select(any(.assets[]; .name | endswith(".pdf")))
    | "## [\(.tag_name)](\(.html_url)) (\(.published_at[0:10]))\n",
      (.assets
        | map(select(.name | endswith(".pdf")))
        | sort_by(.name)[]
        | "- [\(if (.name | contains("gebruikershandleiding")) then "Gebruikershandleiding"
              elif (.name | contains("inrichting")) then "Inrichting zaakafhandelcomponent"
              else .name end) (\(.name))](\(.browser_download_url))"),
      ""'
} > "$outputFile"
