/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TaakStuurGegevens } from "./taak-stuur-gegevens";
import {GeneratedType} from "../../shared/utils/generated-types";

export class HumanTaskData {
  planItemInstanceId: string;
  groep: GeneratedType<'RestGroup'>;
  medewerker: GeneratedType<'RestUser'>;
  fataledatum: string;
  toelichting: string;
  taakdata: {};
  taakStuurGegevens: TaakStuurGegevens;
}
