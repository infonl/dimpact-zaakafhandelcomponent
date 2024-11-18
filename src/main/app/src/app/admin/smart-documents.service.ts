/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { catchError, map, Observable } from "rxjs";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import {GeneratedType} from "../shared/utils/generated-types";

export type SmartDocumentsTemplate = Omit<GeneratedType<'RestMappedSmartDocumentsTemplate'>, 'informatieObjectTypeUUID'>;

interface BaseGroup<T extends Omit<GeneratedType<'RestMappedSmartDocumentsTemplate'>, 'informatieObjectTypeUUID'>> {
  id: string;
  name: string;
  groups?: BaseGroup<T>[];
  templates?: T[];
}

export type SmartDocumentsTemplateGroup = BaseGroup<SmartDocumentsTemplate>;

export type DocumentsTemplateGroup = BaseGroup<GeneratedType<'RestMappedSmartDocumentsTemplate'>>;

@Injectable({ providedIn: "root" })
export class SmartDocumentsService {
  constructor(
    private zacHttp: ZacHttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {}

  getAllSmartDocumentsTemplates(): Observable<SmartDocumentsTemplateGroup[]> {
    return this.zacHttp
      .GET("/rest/zaakafhandelparameters/document-templates")
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  getAllSmartDocumentsTemplateGroups(): Observable<
    SmartDocumentsTemplateGroup[]
  > {
    return this.zacHttp
      .GET("/rest/zaakafhandelparameters/document-templates")
      .pipe(
        map((data) => data.map(this.flattenDocumentsTemplateGroup).flat()),
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

  getZaakTypeTemplatesMappings(
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
          const flattened = data.map(this.flattenDocumentsTemplateGroup).flat();
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

  /**
   * Flattens a nested RootObject (DocumentsTemplateGroup) into an array of group objects,
   * omitting nested groups, and preserving templates.
   * @param {DocumentsTemplateGroup} obj - The root object to flatten.
   * @returns {Array<Omit<DocumentsTemplateGroup, "groups">>} - The flattened array of groups with templates, excluding nested groups.
   */
  flattenDocumentsTemplateGroup(
    obj: DocumentsTemplateGroup,
  ): Array<Omit<DocumentsTemplateGroup, "groups">> {
    const result: Array<Omit<DocumentsTemplateGroup, "groups">> = [];

    function flattenDocumentsTemplateGroup(group: DocumentsTemplateGroup) {
      result.push({
        id: group.id,
        name: group.name,
        templates: group.templates || [],
      });

      if (group.groups) {
        group.groups.forEach(flattenDocumentsTemplateGroup);
      }
    }

    // Flatten the root object itself
    result.push({
      id: obj.id,
      name: obj.name,
      templates: obj.templates || [],
    });

    if (obj.groups) {
      obj.groups.forEach(flattenDocumentsTemplateGroup);
    }

    return result;
  }
}
