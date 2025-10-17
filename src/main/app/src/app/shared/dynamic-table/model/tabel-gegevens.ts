/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../utils/generated-types";

export class TabelGegevens {
  aantalPerPagina: number;
  pageSizeOptions: number[];
  werklijstRechten: GeneratedType<"RestWerklijstRechten">;
}
