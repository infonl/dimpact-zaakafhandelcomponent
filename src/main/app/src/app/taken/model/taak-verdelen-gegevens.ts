/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

export class TaakVerdelenGegevens {
  screenEventResourceId: string;
  taken: { taakId: string; zaakUuid: string }[];
  behandelaarGebruikersnaam: string;
  groepId: string;
  reden: string;
}
