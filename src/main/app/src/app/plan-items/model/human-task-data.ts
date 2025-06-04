/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../shared/utils/generated-types";
import { TaakStuurGegevens } from "./taak-stuur-gegevens";

/**
 * @deprecated - use the `GeneratedType`
 */
export class HumanTaskData {
  planItemInstanceId: string;
  groep: GeneratedType<"RestGroup">;
  medewerker: GeneratedType<"RestUser">;
  fataledatum: string;
  toelichting: string;
  taakdata: Record<string, unknown>;
  taakStuurGegevens: TaakStuurGegevens;
}
