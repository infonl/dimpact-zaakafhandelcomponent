/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FlatTreeControl } from "@angular/cdk/tree";
import { Component, effect, EventEmitter, Input, Output } from "@angular/core";
import { FormGroup } from "@angular/forms";
import {
  MatTreeFlatDataSource,
  MatTreeFlattener,
} from "@angular/material/tree";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { firstValueFrom, Observable } from "rxjs";
import { InformatieObjectenService } from "src/app/informatie-objecten/informatie-objecten.service";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { SmartDocumentsService } from "../../smart-documents.service";

type SmartDocumentsTemplateWithParentId =
  GeneratedType<"RestSmartDocumentsTemplate"> & {
    parentGroupId: string;
  };
type SmartDocumentsTemplateGroupWithParentId = Omit<
  GeneratedType<"RestSmartDocumentsTemplateGroup">,
  "groups" | "templates"
> & {
  groups?: SmartDocumentsTemplateGroupWithParentId[] | null;
  templates?: SmartDocumentsTemplateWithParentId[] | null;
};

type MappedSmartDocumentsTemplateWithParentId =
  GeneratedType<"RestMappedSmartDocumentsTemplate"> & {
    parentGroupId?: string | null;
  };
type MappedSmartDocumentsTemplateGroupWithParentId = Omit<
  GeneratedType<"RestMappedSmartDocumentsTemplateGroup">,
  "groups" | "templates"
> & {
  groups?: MappedSmartDocumentsTemplateGroupWithParentId[] | null;
  templates?: MappedSmartDocumentsTemplateWithParentId[] | null;
};

type PlainTemplateMappings = Omit<
  MappedSmartDocumentsTemplateWithParentId,
  "name"
>;

type MappedSmartDocumentsTemplateFlattenedGroupWithParentId = Omit<
  MappedSmartDocumentsTemplateGroupWithParentId,
  "groups"
>;

interface FlatNode {
  expandable: boolean;
  name: string;
  level: number;
}

@Component({
  selector: "smart-documents-form",
  templateUrl: "./smart-documents-form.component.html",
  styleUrl: "./smart-documents-form.component.less",
})
export class SmartDocumentsFormComponent {
  @Input() formGroup: FormGroup;
  @Input() zaakTypeUuid: string;
  @Output() formValidityChanged = new EventEmitter<boolean>();

  allSmartDocumentTemplateGroups: GeneratedType<"RestSmartDocumentsTemplateGroup">[] =
    [];
  currentTemplateMappings: MappedSmartDocumentsTemplateGroupWithParentId[] = [];
  informationObjectTypes: GeneratedType<"RestInformatieobjecttype">[] = [];

  newTemplateMappings: MappedSmartDocumentsTemplateGroupWithParentId[] = [];

  constructor(
    private smartDocumentsService: SmartDocumentsService,
    private informatieObjectenService: InformatieObjectenService,
  ) {
    effect(() => this.prepareDatasource());
  }

  ngOnInit() {
    this.formGroup.statusChanges.subscribe(() => {
      this.formValidityChanged.emit(this.formGroup.valid);
    });
  }

  private prepareDatasource() {
    this.allSmartDocumentTemplateGroups =
      this.allSmartDocumentTemplateGroupsQuery.data()
        ? this.addParentIdToTemplates(
            this.allSmartDocumentTemplateGroupsQuery.data(),
          )
        : [];

    this.currentTemplateMappings = this.currentTemplateMappingsQuery.data()
      ? (this.addParentIdToTemplates(
          this.currentTemplateMappingsQuery.data(),
        ) as MappedSmartDocumentsTemplateGroupWithParentId[])
      : [];

    this.informationObjectTypes = this.informationObjectTypesQuery.data() || [];

    this.newTemplateMappings = this.addTemplateMappings(
      this.allSmartDocumentTemplateGroups,
      this.getAllTemplateMappingsFromTree(this.currentTemplateMappings),
    );

    this.dataSource.data = this.flattenGroupsToRoot(this.newTemplateMappings);

    if (this.dataSource.data.length) {
      console.log("Current Tree Data", this.dataSource.data);
    }
  }

  allSmartDocumentTemplateGroupsQuery = injectQuery(() => ({
    queryKey: ["allSmartDocumentTemplateGroupsQuery"],
    refetchOnWindowFocus: false,
    queryFn: () =>
      firstValueFrom(
        this.smartDocumentsService.getAllSmartDocumentsTemplateGroups(),
      ),
  }));

  currentTemplateMappingsQuery = injectQuery(() => ({
    queryKey: ["currentTemplateMappingsQuery", this.zaakTypeUuid],
    refetchOnWindowFocus: false,
    queryFn: () =>
      firstValueFrom(
        this.smartDocumentsService.getTemplatesMapping(this.zaakTypeUuid),
      ),
  }));

  informationObjectTypesQuery = injectQuery(() => ({
    queryKey: ["informationObjectTypesQuery", this.zaakTypeUuid],
    refetchOnWindowFocus: false,
    queryFn: () =>
      firstValueFrom(
        this.informatieObjectenService.listInformatieobjecttypes(
          this.zaakTypeUuid,
        ),
      ),
  }));

  private _transformer = (node: any, level: number) => {
    return {
      id: node.id,
      name: node.name,
      parentGroupId: node.parentGroupId,
      informatieObjectTypeUUID: node.informatieObjectTypeUUID,
      level: level,
      expandable: !!node.templates && node.templates.length > 0,
    };
  };

  treeControl = new FlatTreeControl<FlatNode>(
    (node) => node.level,
    (node) => node.expandable,
  );

  treeFlattener = new MatTreeFlattener(
    this._transformer,
    (node) => node.level,
    (node) => node.expandable,
    (node) => node.templates,
  );

  dataSource = new MatTreeFlatDataSource(this.treeControl, this.treeFlattener);

  hasChild = (_: number, node: { expandable: boolean }) => node.expandable;

  hasSelected(id: string): boolean {
    const nodeHasSelectedInformationObjectType = this.dataSource.data
      .find((node) => node.id === id)
      ?.templates.some((_node) => _node.informatieObjectTypeUUID);

    return !!nodeHasSelectedInformationObjectType;
  }

  getSelectedClass(node: string): string {
    return this.hasSelected(node) ? "active" : "default";
  }

  handleNodeChange({
    id,
    parentGroupId,
    informatieObjectTypeUUID,
  }: FlatNode & MappedSmartDocumentsTemplateWithParentId): void {
    let nodeUpdated = false;

    this.dataSource.data.forEach(
      (
        node: FlatNode & MappedSmartDocumentsTemplateFlattenedGroupWithParentId,
      ) => {
        node.templates.forEach((templateNode) => {
          if (
            templateNode.id === id &&
            templateNode.parentGroupId === parentGroupId
          ) {
            templateNode.informatieObjectTypeUUID = informatieObjectTypeUUID;
            nodeUpdated = true;
          }
        });
      },
    );

    if (!nodeUpdated) {
      throw new Error(
        `Node not found: ${JSON.stringify({ id, parentGroupId })}`,
      );
    }
  }

  public saveSmartDocumentsMapping(): Observable<never> {
    const onlyMaopedTemplates = this.onlyMappedTemplates(
      this.addTemplateMappings(
        this.newTemplateMappings,
        this.getAllTemplateMappingsFromTree(this.dataSource.data),
      ),
    );

    return this.smartDocumentsService.storeTemplatesMapping(
      this.zaakTypeUuid,
      onlyMaopedTemplates,
    );
  }

  private onlyMappedTemplates = (
    data: MappedSmartDocumentsTemplateGroupWithParentId[],
  ): GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[] => {
    return data
      .map((group) => {
        const templates = (group.templates || [])
          .filter((template) => template.informatieObjectTypeUUID)
          .map(({ parentGroupId, ...template }) => template);

        const groups = group.groups
          ? this.onlyMappedTemplates(group.groups)
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

  private flattenGroupsToRoot = (
    data: MappedSmartDocumentsTemplateGroupWithParentId[],
  ): MappedSmartDocumentsTemplateFlattenedGroupWithParentId[] => {
    return data.flatMap((item) => {
      const rootGroup = {
        id: item.id,
        name: item.name,
        templates: item.templates || [],
      };

      const flattenedGroups =
        item.groups?.flatMap((group) => {
          const flattenedSubGroups = group.groups
            ? this.flattenGroupsToRoot(group.groups)
            : [];

          return [
            {
              ...group,
              templates: group.templates || [],
            },
            ...flattenedSubGroups,
          ];
        }) || [];

      return [rootGroup, ...flattenedGroups];
    });
  };

  private addTemplateMappings = (
    groups: GeneratedType<"RestSmartDocumentsTemplateGroup">[],
    uuidsToAdd: PlainTemplateMappings[],
  ): MappedSmartDocumentsTemplateGroupWithParentId[] => {
    const updateTemplate = (template, uuidsToAdd) => {
      const matchedUUID = uuidsToAdd.find(
        (uuidItem) =>
          uuidItem.id === template.id &&
          uuidItem.parentGroupId === template.parentGroupId,
      );

      return {
        ...template,
        informatieObjectTypeUUID: matchedUUID?.informatieObjectTypeUUID,
      };
    };

    const updateGroup = (group) => ({
      ...group,
      templates: (group.templates || []).map((template) =>
        updateTemplate(template, uuidsToAdd),
      ),
      groups: group.groups ? assignInformatieObjectTypeUUID(group.groups) : [],
    });

    const assignInformatieObjectTypeUUID = (groups) => groups.map(updateGroup);

    return assignInformatieObjectTypeUUID(groups);
  };

  private getAllTemplateMappingsFromTree = (
    data:
      | MappedSmartDocumentsTemplateGroupWithParentId[]
      | MappedSmartDocumentsTemplateFlattenedGroupWithParentId[],
  ): PlainTemplateMappings[] => {
    console.log("data", data);
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

          ...this.getAllTemplateMappingsFromTree(group.groups || []),
        ]) ?? [];

      return [...itemTemplates, ...groupTemplates];
    });
  };

  private addParentIdToTemplates = <
    T extends
      | GeneratedType<"RestSmartDocumentsTemplateGroup">
      | GeneratedType<"RestMappedSmartDocumentsTemplateGroup">,
  >(
    data: T[],
  ): (Omit<T, "templates"> & {
    templates: (T["templates"][number] & { parentGroupId: string })[];
  })[] => {
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
