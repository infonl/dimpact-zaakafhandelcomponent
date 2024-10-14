/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { catchError, map, Observable } from "rxjs";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { ZacHttpClient } from "../shared/http/zac-http-client";

interface BaseTemplate {
  id: string;
  name: string;
}

export type SmartDocumentsTemplate = BaseTemplate;

export interface DocumentsTemplate extends BaseTemplate {
  informatieObjectTypeUUID: string;
}

interface BaseGroup<T extends BaseTemplate> {
export type SmartDocumentsTemplate = BaseTemplate;

export interface DocumentsTemplate extends BaseTemplate {
  informatieObjectTypeUUID: string;
}

interface BaseGroup<T extends BaseTemplate> {
  id: string;
  name: string;
  groups?: BaseGroup<T>[];
  templates?: T[];
}

export type SmartDocumentsTemplateGroup = BaseGroup<SmartDocumentsTemplate>;
  groups?: BaseGroup<T>[];
  templates?: T[];
}

export type SmartDocumentsTemplateGroup = BaseGroup<SmartDocumentsTemplate>;

export type DocumentsTemplateGroup = BaseGroup<DocumentsTemplate>;

export interface RootObject extends DocumentsTemplateGroup {}
export type DocumentsTemplateGroup = BaseGroup<DocumentsTemplate>;

export interface RootObject extends DocumentsTemplateGroup {}

@Injectable({ providedIn: "root" })
export class SmartDocumentsService {
  constructor(
    private zacHttp: ZacHttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {}

  listTemplates(): Observable<SmartDocumentsTemplateGroup[]> {
    return this.zacHttp
      .GET("/rest/zaakafhandelparameters/document-templates")
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  getTemplatesMapping(
    zaakafhandelUUID: string,
  ): Observable<DocumentsTemplateGroup[]> {
    return this.zacHttp
      .GET(
        "/rest/zaakafhandelparameters/{zaakafhandelUUID}/document-templates",
        {
          pathParams: {
            path: {
              zaakafhandelUUID,
            },
          },
        },
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  getTemplatesMappingFlat(
    zaakafhandelUUID: string,
  ): Observable<DocumentsTemplateGroup[]> {
    return this.zacHttp
      .GET(
        "/rest/zaakafhandelparameters/{zaakafhandelUUID}/document-templates",
        {
          pathParams: {
            path: {
              zaakafhandelUUID,
            },
          },
        },
      )
      .pipe(
        map((data) => {
          const flattened = data.map(this.flattenObject).flat();
          return flattened;
        }),
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  storeTemplatesMapping(
    zaakafhandelUUID: string,
    templates: DocumentsTemplateGroup[],
  ) {
    return this.zacHttp
      .POST(
        "/rest/zaakafhandelparameters/{zaakafhandelUUID}/document-templates",
        templates,
        {
          pathParams: {
            path: {
              zaakafhandelUUID,
            },
          },
        },
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }
}
