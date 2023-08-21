# Dimpact Zaakafhandelcomponent (ZAC)

This repository contains the source code of the "zaakafhandelcomponent" (ZAC) developed for [Dimpact](https://www.dimpact.nl/).

It was initially developed by Atos. Starting July 2023 the development of ZAC was taken over by INFO, a partner of Lifely.

## License

This software is licensed under the [EUPL](LICENSE.md).

We use [SPDX](https://spdx.dev/) license identifiers in source code files.
When adding a new source code file or modifying an existing one, please update the SPDX license identifier accordingly:

### Adding a new source code file

For most source code files (e.g. `.ts`, `.js` and `.java` files) please add the following SPDX license identifier to the top of the file:

```
/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
```

For other file types (e.g. `.html` and `.xml` files) please add the following SPDX license identifier to the top of the file:

```
 <!--
  ~ SPDX-FileCopyrightText: 2023 Lifely
  ~ SPDX-License-Identifier: EUPL-1.2+
  -->
```

Finally for e.g. `.sh` files please add:

```
#
# SPDX-FileCopyrightText: 2023 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#
```

Tip: configure your IDE to automatically add these headers to new source code files.
For example, in IntelliJ IDEA please follow the instructions on https://www.jetbrains.com/help/idea/copyright.html.

### Modifying an existing source code file

Please update the SPDX license identifier to the top of the file by adding a `, <YEAR> Lifely` to
the `SPDX-FileCopyrightText` identifier. E.g.:

```
/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
```
