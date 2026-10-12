# Manual PDF generator

Generates the PDF versions of the [ZAC-gebruikershandleiding](../ZAC-gebruikershandleiding/ZAC-gebruikershandleiding.md)
and the [inrichting-zaakafhandelcomponent](../inrichting-zaakafhandelcomponent/inrichting-zaakafhandelcomponent.md) manuals
from their Markdown, with headless Chromium.

## In the pipeline

The [Generate manual PDFs](../../../.github/workflows/generate-manual-pdfs.yml) workflow runs:

- for every ZAC release and hotfix, from the release job in the Build, test & deploy workflow.
  It attaches `ZAC-V<major>.<minor>-gebruikershandleiding.pdf` and `ZAC-V<major>.<minor>-inrichting-zaakafhandelcomponent.pdf`
  to the GitHub release.
- for every pull request that changes one of these manuals. It uploads the PDFs as a workflow artifact, so that you can
  review them. Do not commit the PDFs.
- on demand, with 'Run workflow'. Fill in a release tag to (re)attach the PDFs to an existing release.

## Locally

```sh
cd docs/manuals/pdf-generator
npm ci --ignore-scripts
npx playwright install chromium
./generate-manual-pdfs.sh 5.10 ../../../build/manual-pdfs
```
