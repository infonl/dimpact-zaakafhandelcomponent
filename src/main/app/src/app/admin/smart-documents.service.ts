/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { map } from "rxjs";
import {
  PostBody,
  PutBody,
  ZacHttpClient,
} from "../shared/http/zac-http-client";
import { GeneratedType } from "../shared/utils/generated-types";

export interface TemplateMapping {
  id: string;
  parentGroupId?: string;
  informatieObjectTypeUUID?: string;
}

@Injectable({ providedIn: "root" })
export class SmartDocumentsService {
  constructor(private readonly zacHttpClient: ZacHttpClient) {}

  getAllSmartDocumentsTemplateGroups() {
    return this.zacHttpClient.GET(
      "/rest/zaakafhandelparameters/smartdocuments-templates",
    );
  }

  getTemplatesMapping(zaakafhandelUUID: string) {
    return this.zacHttpClient
      .GET(
        "/rest/zaakafhandelparameters/{zaakafhandelUUID}/smartdocuments-templates-mapping",
        { path: { zaakafhandelUUID } },
      )
      .pipe(map((data) => this.flattenGroups(this.convertApiData(data))));
  }

  private convertApiData(
    data: GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[],
  ): GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[] {
    return data.map((group) => ({
      ...group,
      templates: group.templates || undefined,
      groups: group.groups ? this.convertApiData(group.groups) : undefined,
    }));
  }

  storeTemplatesMapping(
    zaakafhandelUUID: string,
    templateGroups: GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[],
  ) {
    const body = this.convertToApiFormat(templateGroups);
    return this.zacHttpClient.POST(
      "/rest/zaakafhandelparameters/{zaakafhandelUUID}/smartdocuments-templates-mapping",
      body,
      { path: { zaakafhandelUUID } },
    );
  }

  private convertToApiFormat(
    groups: GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[],
  ): PostBody<"/rest/zaakafhandelparameters/{zaakafhandelUUID}/smartdocuments-templates-mapping"> {
    return groups.map((group) => ({
      ...group,
      templates:
        group.templates?.map((template) => ({
          ...template,
          informatieObjectTypeUUID: template.informatieObjectTypeUUID!,
        })) || null,
      groups: group.groups ? this.convertToApiFormat(group.groups) : null,
    }));
  }

  getTemplateGroup(
    body: PutBody<"/rest/zaakafhandelparameters/smartdocuments-template-group">,
    templateName: string,
    informatieObjectTypeUUID: string,
  ) {
    return this.zacHttpClient
      .PUT("/rest/zaakafhandelparameters/smartdocuments-template-group", body)
      .pipe(
        map((data) =>
          this.createSingleTemplateGroup(
            data,
            templateName,
            informatieObjectTypeUUID,
          ),
        ),
      );
  }

  private createSingleTemplateGroup(
    templateGroup: GeneratedType<"RestSmartDocumentsTemplateGroup">,
    templateName: string,
    informatieObjectTypeUUID: string,
  ): GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[] {
    return [
      {
        id: templateGroup.id,
        name: templateGroup.name,
        groups: null,
        templates: [
          {
            id: templateGroup.templates!.find(
              ({ name }) => name === templateName,
            )!.id,
            name: templateName,
            informatieObjectTypeUUID,
          },
        ],
      },
    ];
  }

  // Get only groups that have mapped templates
  getOnlyMappedTemplates(
    data: GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[],
  ): GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[] {
    return data
      .map((group) => {
        const templates =
          group.templates?.filter((t) => t.informatieObjectTypeUUID) || [];
        const groups = this.getOnlyMappedTemplates(group.groups ?? []);

        return templates.length || groups.length
          ? { ...group, templates, groups }
          : null;
      })
      .filter(
        Boolean,
      ) as GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[];
  }

  // Flatten nested groups into a simple array
  flattenGroups(
    groups: GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[],
  ) {
    const result: {
      id: string;
      name: string;
      templates: GeneratedType<"RestMappedSmartDocumentsTemplate">[];
    }[] = [];

    const traverse = (
      group: GeneratedType<"RestMappedSmartDocumentsTemplateGroup">,
    ) => {
      if (group.templates?.length) {
        result.push({
          id: group.id,
          name: group.name,
          templates: group.templates,
        });
      }
      group.groups?.forEach(traverse);
    };

    groups.forEach(traverse);
    return result;
  }

  // Add UUID mappings to templates
  addTemplateMappings(
    groups: GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[],
    mappings: TemplateMapping[],
  ): GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[] {
    return groups.map((group) => ({
      ...group,
      templates: group.templates?.map((template) => ({
        ...template,
        informatieObjectTypeUUID:
          mappings.find(
            ({ id, parentGroupId }) =>
              id === template.id && parentGroupId === group.id,
          )?.informatieObjectTypeUUID || "",
      })),
      groups: group.groups
        ? this.addTemplateMappings(group.groups, mappings)
        : [],
    }));
  }

  // Extract all template mappings from groups
  getTemplateMappings(
    data: GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[],
  ) {
    const result: TemplateMapping[] = [];

    const extract = (
      group: GeneratedType<"RestMappedSmartDocumentsTemplateGroup">,
    ) => {
      group.templates?.forEach((template) => {
        result.push({
          id: template.id,
          parentGroupId: group.id,
          informatieObjectTypeUUID: template.informatieObjectTypeUUID,
        });
      });
      group.groups?.forEach(extract);
    };

    data.forEach(extract);
    return result;
  }

  // Add parent IDs to templates for uniqueness
  addParentIdsToTemplates(
    data?: GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[],
  ): GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[] {
    if (!data) return [];

    return data.map((group) => ({
      ...group,
      templates: group.templates?.map((template) => ({
        ...template,
        parentGroupId: group.id,
      })),
      groups: this.addParentIdsToTemplates(group.groups ?? []),
    }));
  }
}
