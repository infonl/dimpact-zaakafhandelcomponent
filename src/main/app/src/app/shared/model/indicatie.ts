/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../utils/generated-types";

export enum BesluitIndicatie {
  INGETROKKEN = "INGETROKKEN",
}

export type Indicatie =
  | GeneratedType<"DocumentIndicatie">
  | GeneratedType<"RestPersoonIndicaties">
  | GeneratedType<"ZaakIndicatie">
  | BesluitIndicatie;
