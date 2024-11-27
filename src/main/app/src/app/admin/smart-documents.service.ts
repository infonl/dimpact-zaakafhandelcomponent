/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { catchError, map, Observable } from "rxjs";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { GeneratedType } from "../shared/utils/generated-types";

export type SmartDocumentsTemplateWithParentId =
  GeneratedType<"RestSmartDocumentsTemplate"> & {
    parentGroupId?: string | null;
  };
export type SmartDocumentsTemplateGroupWithParentId = Omit<
  GeneratedType<"RestSmartDocumentsTemplateGroup">,
  "groups" | "templates"
> & {
  groups?: SmartDocumentsTemplateGroupWithParentId[] | null;
  templates?: SmartDocumentsTemplateWithParentId[] | null;
};

export type MappedSmartDocumentsTemplateWithParentId =
  GeneratedType<"RestMappedSmartDocumentsTemplate"> & {
    parentGroupId?: string | null;
  };
export type MappedSmartDocumentsTemplateGroupWithParentId = Omit<
  GeneratedType<"RestMappedSmartDocumentsTemplateGroup">,
  "groups" | "templates"
> & {
  groups?: MappedSmartDocumentsTemplateGroupWithParentId[] | null;
  templates?: MappedSmartDocumentsTemplateWithParentId[] | null;
};

export type PlainTemplateMappings = Omit<
  MappedSmartDocumentsTemplateWithParentId,
  "name"
>;

export type MappedSmartDocumentsTemplateFlattenedGroupWithParentId = Omit<
  MappedSmartDocumentsTemplateGroupWithParentId,
  "groups"
>;

@Injectable({ providedIn: "root" })
export class SmartDocumentsService {
  constructor(
    private zacHttp: ZacHttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {}

  getAllSmartDocumentsTemplateGroups(): Observable<
    GeneratedType<"RestSmartDocumentsTemplateGroup">[]
  > {
    return this.zacHttp
      .GET("/rest/zaakafhandelparameters/document-templates")
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  getAllSmartDocumentsTemplateGroupsFlat(): Observable<
    GeneratedType<"RestSmartDocumentsTemplateGroup">[]
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
  ): Observable<GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[]> {
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

  getZaakTypeTemplatesMappingsFlat(
    zaakafhandelUUID: string,
  ): Observable<GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[]> {
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
    templates: GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[],
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
   * Flattens a nested RootObject (GeneratedType<"RestMappedSmartDocumentsTemplateGroup">) into an array of group objects,
   * omitting nested groups, and preserving templates.
   * @param {GeneratedType<"RestMappedSmartDocumentsTemplateGroup">} obj - The root object to flatten.
   * @returns {Array<Omit<GeneratedType<"RestMappedSmartDocumentsTemplateGroup">, "groups">>} - The flattened array of groups with templates, excluding nested groups.
   */
  flattenDocumentsTemplateGroup(
    obj: GeneratedType<"RestMappedSmartDocumentsTemplateGroup">,
  ): Array<
    Omit<GeneratedType<"RestMappedSmartDocumentsTemplateGroup">, "groups">
  > {
    const result: Array<
      Omit<GeneratedType<"RestMappedSmartDocumentsTemplateGroup">, "groups">
    > = [];

    function flattenDocumentsTemplateGroup(
      group: GeneratedType<"RestMappedSmartDocumentsTemplateGroup">,
    ) {
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

  getMappedTemplates = (
    data: MappedSmartDocumentsTemplateGroupWithParentId[],
  ): GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[] => {
    return data
      .map((group) => {
        const templates = (group.templates || [])
          .filter((template) => template.informatieObjectTypeUUID)
          .map(({ parentGroupId, ...template }) => template);

        const groups = group.groups
          ? this.getMappedTemplates(group.groups)
          : [];

        if (templates.length || groups.length) {
          const { ...cleanedGroup } = group;
          return {
            ...cleanedGroup,
            templates,
            groups,
          };
        }

        return null;
      })
      .filter(Boolean);
  };

  flattenNestedGroups(
    groups: MappedSmartDocumentsTemplateGroupWithParentId[],
  ): MappedSmartDocumentsTemplateFlattenedGroupWithParentId[] {
    const result = [];

    function traverse(group) {
      const { id, name, templates = [] } = group;
      result.push({ id, name, templates });

      if (group.groups) {
        group.groups.forEach(traverse);
      }
    }

    groups.forEach(traverse);

    return result;
  }

  addTemplateMappings = (
    groups: GeneratedType<"RestSmartDocumentsTemplateGroup">[],
    uuidsToAdd: PlainTemplateMappings[],
  ): MappedSmartDocumentsTemplateGroupWithParentId[] => {
    const updateGroup = (
      group: MappedSmartDocumentsTemplateGroupWithParentId,
    ): MappedSmartDocumentsTemplateGroupWithParentId => ({
      ...group,
      templates: (group.templates || []).map((template) => ({
        ...template,
        informatieObjectTypeUUID: uuidsToAdd.find(
          (uuidItem) =>
            uuidItem.id === template.id &&
            uuidItem.parentGroupId === template.parentGroupId,
        )?.informatieObjectTypeUUID,
      })),
      groups: group.groups ? group.groups.map(updateGroup) : [],
    });

    return groups.map(updateGroup);
  };

  getTemplateMappings = (
    data: MappedSmartDocumentsTemplateGroupWithParentId[],
  ): PlainTemplateMappings[] => {
    return data.flatMap((item) => {
      const itemTemplates =
        item.templates?.map((template) => ({
          id: template.id,
          parentGroupId: template.parentGroupId,
          informatieObjectTypeUUID: template.informatieObjectTypeUUID,
        })) ?? [];

      const groupTemplates =
        item.groups?.flatMap((group) => [
          ...group.templates.map((template) => ({
            id: template.id,
            parentGroupId: template.parentGroupId,
            informatieObjectTypeUUID: template.informatieObjectTypeUUID,
          })),

          ...this.getTemplateMappings(group.groups || []),
        ]) ?? [];

      return [...itemTemplates, ...groupTemplates];
    });
  };

  addParentIdToTemplates = <
    T extends
      | GeneratedType<"RestSmartDocumentsTemplateGroup">
      | GeneratedType<"RestMappedSmartDocumentsTemplateGroup">,
  >(
    data?: T[],
  ): (Omit<T, "templates"> & {
    templates: (T["templates"][number] & { parentGroupId: string })[];
  })[] => {
    if (!data) {
      return [];
    }

    return data.map((group) => {
      return {
        ...group,
        templates: group.templates?.map((template: T["templates"][number]) => ({
          ...template,
          parentGroupId: group.id,
        })),
        groups: group.groups ? this.addParentIdToTemplates(group.groups) : [],
      };
    });
  };
}
