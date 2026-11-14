/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FormulierDefinitieID } from "../../admin/model/formulier-definitie";
import { FormulierDefinitie } from "../../admin/model/formulieren/formulier-definitie";
import { TaakRechten } from "../../policy/model/taak-rechten";
import { GeneratedType } from "../../shared/utils/generated-types";
import { TaakStatus } from "./taak-status.enum";
import { Taakinformatie } from "./taakinformatie";

export class Taak {
  id: string;
  naam: string;
  toelichting: string;
  creatiedatumTijd: string;
  toekenningsdatumTijd: string;
  fataledatum: string;
  behandelaar?: GeneratedType<"RestUser">;
  groep: GeneratedType<"RestGroup">;
  zaakUuid: string;
  zaakIdentificatie: string;
  zaaktypeOmschrijving: string;
  status: TaakStatus;
  formulierDefinitieId: FormulierDefinitieID;
  formulierDefinitie: FormulierDefinitie;
  formioFormulier: any;
  tabellen: Record<string, string[]>;
  taakdata: Record<string, string>;
  taakinformatie: Taakinformatie;
  taakdocumenten: string[];
  rechten: TaakRechten;
}
