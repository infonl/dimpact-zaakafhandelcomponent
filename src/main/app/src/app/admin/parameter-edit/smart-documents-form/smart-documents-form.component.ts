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
import {
  DocumentsTemplateGroup,
  SmartDocumentsService,
  SmartDocumentsTemplateGroup,
} from "../../smart-documents.service";

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

  allSmartDocumentTemplateGroups: SmartDocumentsTemplateGroup[] = [];
  currentTemplateMappings: DocumentsTemplateGroup[] = [];
  informationObjectTypes: GeneratedType<"RestInformatieobjecttype">[] = [];

  newTemplateMappings: DocumentsTemplateGroup[] = [];

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
      ? this.addParentIdToTemplates(this.currentTemplateMappingsQuery.data())
      : [];

    this.informationObjectTypes = this.informationObjectTypesQuery.data() || [];

    this.newTemplateMappings = this.addTemplateMappings(
      this.allSmartDocumentTemplateGroups,
      this.getAllTemplateMappingFromTree(this.currentTemplateMappings),
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

  handleNodeChange(node: any): void {
    const id = node.id;
    const parentGroupId = node.parentGroupId;
    const informatieObjectTypeUUID = node.informatieObjectTypeUUID;

    const adjustFlatNode = (_nodes: any[], _parentId: string | null): boolean =>
      _nodes.some((currentNode) => {
        if (currentNode.id === id && currentNode.parentGroupId === _parentId) {
          currentNode.informatieObjectTypeUUID = informatieObjectTypeUUID;
          return true;
        }

        return currentNode.templates
          ? adjustFlatNode(currentNode.templates, _parentId)
          : false;
      });

    if (adjustFlatNode(this.dataSource.data, parentGroupId)) {
      console.log("Tree updated; Template: '", node.name, this.dataSource.data);
    } else {
      console.error("Node not found !!!!", { id, parentGroupId });
    }
  }

  public saveSmartDocumentsMapping(): Observable<never> {
    const onlyMaopedTemplates = this.onlyMappedTemplates(
      this.addTemplateMappings(
        this.newTemplateMappings,
        this.getAllTemplateMappingFromTree(this.dataSource.data),
      ),
    );

    console.log("Storing SmartDocuments Mapping", onlyMaopedTemplates);
    return this.smartDocumentsService.storeTemplatesMapping(
      this.zaakTypeUuid,
      onlyMaopedTemplates,
    );
  }

  private onlyMappedTemplates = (data) => {
    return data
      .map((group) => {
        const templates = (group.templates || [])
          .filter((template) => template.informatieObjectTypeUUID)
          .map(({ parentGroupId, ...template }) => template);

        const groups = group.groups
          ? this.onlyMappedTemplates(group.groups)
          : [];

        if (templates.length || groups.length) {
          const { parentGroupId, ...cleanedGroup } = group;
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

  private flattenGroupsToRoot = (data) => {
    return data.flatMap((item) => {
      const rootGroup = {
        id: item.id,
        name: item.name,
        templates: item.templates.map((template) => ({
          ...template,
          parentGroupId: item.id,
        })),
      };

      const flattenedGroups = item.groups.flatMap((group) => {
        const flattenedSubGroups = group.groups
          ? this.flattenGroupsToRoot(group.groups)
          : [];

        return [
          {
            ...group,
            templates: group.templates.map((template) => ({
              ...template,
              parentGroupId: group.id,
            })),
          },
          ...flattenedSubGroups,
        ];
      });

      return [rootGroup, ...flattenedGroups];
    });
  };

  private addTemplateMappings = (data, uuidsToAdd) => {
    const assignInformatieObjectTypeUUID = (items) =>
      items.map((item) => {
        const updatedItem = {
          ...item,
          templates: (item.templates || []).map((template) => {
            const matchedUUID = uuidsToAdd.find(
              (uuidItem) =>
                uuidItem.id === template.id &&
                uuidItem.parentGroupId === template.parentGroupId,
            );

            return {
              ...template,
              informatieObjectTypeUUID: matchedUUID?.informatieObjectTypeUUID,
            };
          }),
          groups: item.groups
            ? assignInformatieObjectTypeUUID(item.groups)
            : [],
        };

        return updatedItem;
      });

    return assignInformatieObjectTypeUUID(data);
  };

  private getAllTemplateMappingFromTree = (data) => {
    return data.flatMap((item) => {
      const itemTemplates = item.templates
        ? item.templates.map((template) => ({
            id: template.id,
            parentGroupId: template.parentGroupId,
            informatieObjectTypeUUID: template.informatieObjectTypeUUID,
          }))
        : [];

      const groupTemplates = item.groups
        ? item.groups.flatMap((group) => [
            ...group.templates.map((template) => ({
              id: template.id,
              parentGroupId: template.parentGroupId,
              informatieObjectTypeUUID: template.informatieObjectTypeUUID,
            })),

            ...this.getAllTemplateMappingFromTree(group.groups || []),
          ])
        : [];

      return [...itemTemplates, ...groupTemplates];
    });
  };

  private addParentIdToTemplates = (data) => {
    return data.map((item) => {
      const templates =
        item.templates?.map((template) => ({
          ...template,
          parentGroupId: item.id,
        })) || [];

      const groups =
        item.groups?.map((group) => ({
          ...group,
          templates:
            group.templates?.map((template) => ({
              ...template,
              parentGroupId: group.id,
            })) || [],
          groups: group.groups ? this.addParentIdToTemplates(group.groups) : [],
        })) || [];

      return {
        ...item,
        templates,
        groups,
      };
    });
  };
}
