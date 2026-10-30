# ZAC Gebruikershandleiding

The most recent version of the end-user manual is in [ZAC-gebruikershandleiding.md](./ZAC-gebruikershandleiding.md) (Dutch only).

The pipeline generates a PDF version of this manual for every ZAC release and attaches it to the [GitHub release](https://github.com/infonl/dimpact-zaakafhandelcomponent/releases).
The PDFs of older releases are attached to their GitHub releases as well.
See [pdf-generator](../pdf-generator/README.md) to generate the PDF locally.

## Updating the table of contents

The table of contents in the manual is managed by [doctoc](https://github.com/thlorenz/doctoc) and is verified by CI on every pull request that changes a manual file.

Run the update script from any directory:

```sh
docs/manuals/ZAC-gebruikershandleiding/update-toc.sh
```
