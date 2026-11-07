/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

export class DocumentCreationData {
  public zaakUuid: string;
  public taskId: string;
  public smartDocumentsTemplateGroupId: string;
  public smartDocumentsTemplateId: string;
  public title: string;
  public description?: string;
  public author: string;
  public creationDate: string;
}
