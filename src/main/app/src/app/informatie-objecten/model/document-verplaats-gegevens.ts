/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

export class DocumentVerplaatsGegevens {
  public nieuweZaakID;

  constructor(
    public documentUUID: string,
    public documentTitel: string,
    public documentTypeUUID: string,
    public bron: string,
  ) {}
}
