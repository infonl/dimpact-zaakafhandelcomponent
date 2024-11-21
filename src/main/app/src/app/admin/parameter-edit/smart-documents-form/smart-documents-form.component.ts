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
import { Observable, firstValueFrom } from "rxjs";
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
})
export class SmartDocumentsFormComponent {
  @Input() formGroup: FormGroup;
  @Input() zaakTypeUuid: string;
  @Output() formValidityChanged = new EventEmitter<boolean>();

  allSmartDocumentTemplateGroups: SmartDocumentsTemplateGroup[] = [];
  currentStoredZaakTypeTemplateGroups: DocumentsTemplateGroup[] = [];
  informationObjectTypes: GeneratedType<"RestInformatieobjecttype">[] = [];

  newStoredZaakTypeTemplateGroups: any[] = [];

  constructor(
    private smartDocumentsService: SmartDocumentsService,
    private informatieObjectenService: InformatieObjectenService,
  ) {
    effect(() => {
      this.allSmartDocumentTemplateGroups =
        this.allSmartDocumentTemplateGroupsQuery.data()
          ? this.addParentIdToTemplates(
              this.allSmartDocumentTemplateGroupsQuery.data(),
            )
          : [];

      this.currentStoredZaakTypeTemplateGroups =
        this.zaakTypeTemplateMappingsQuery.data()
          ? this.addParentIdToTemplates(
              this.zaakTypeTemplateMappingsQuery.data(),
            )
          : [];

      this.informationObjectTypes =
        this.informationObjectTypesQuery.data() || [];

      const onlyInformationTypeUUIDs = this.convertToIdAndUUIDMestedGroupsArray(
        this.currentStoredZaakTypeTemplateGroups,
      );

      this.newStoredZaakTypeTemplateGroups = JSON.parse(
        JSON.stringify(this.allSmartDocumentTemplateGroups),
      );

      this.newStoredZaakTypeTemplateGroups = this.addObjectUUIDsToTemplate(
        this.newStoredZaakTypeTemplateGroups,
        onlyInformationTypeUUIDs,
      );

      this.dataSource.data = JSON.parse(
        JSON.stringify(
          this.flattenGroupsToRoot(this.newStoredZaakTypeTemplateGroups),
        ),
      );

      if (this.dataSource.data.length) {
        console.log("Current Tree Data", this.dataSource.data);
      }
    });
  }

  ngOnInit() {
    this.formGroup.statusChanges.subscribe(() => {
      this.formValidityChanged.emit(this.formGroup.valid);
    });
  }

  allSmartDocumentTemplateGroupsQuery = injectQuery(() => ({
    queryKey: ["allSmartDocumentTemplateGroupsQuery"],
    refetchOnWindowFocus: false,
    queryFn: () =>
      firstValueFrom(
        this.smartDocumentsService.getAllSmartDocumentsTemplateGroups(),
      ),
  }));

  zaakTypeTemplateMappingsQuery = injectQuery(() => ({
    queryKey: ["zaakTypeTemplateMappingsQuery", this.zaakTypeUuid],
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

  hasSelectedInformationObjectType(id: any): boolean {
    const nodeHasSelectedInformationObjectType = this.dataSource.data
      .find((node) => node.id === id)
      ?.templates.some((_node) => _node.informatieObjectTypeUUID !== "");

    return !!nodeHasSelectedInformationObjectType;
  }

  onNodeChange(node: any): void {
    const id = node.id;
    const parentGroupId = node.parentGroupId;
    const informatieObjectTypeUUID = node.informatieObjectTypeUUID;

    const adjustFlatNode = (
      _nodes: any[],
      _parentId: string | null,
    ): boolean => {
      for (const currentNode of _nodes) {
        if (currentNode.id === id && currentNode.parentGroupId === _parentId) {
          currentNode.informatieObjectTypeUUID = informatieObjectTypeUUID;
          return true;
        } else if (currentNode.templates) {
          if (adjustFlatNode(currentNode.templates, _parentId)) return true;
        }
      }
      return false;
    };

    if (adjustFlatNode(this.dataSource.data, parentGroupId)) {
      console.log("Tree updated; Template: '", node.name, this.dataSource.data);
    } else {
      console.error("Node not found !!!!", { id, parentGroupId });
    }
  }

  public saveSmartDocumentsMapping(): Observable<never> {
    const justUUIDs = this.convertToIdAndUUIDArrayFlat(this.dataSource.data);
    const newStoreWithInformationObjectTypeUUIDs =
      this.addObjectUUIDsToTemplate(
        this.newStoredZaakTypeTemplateGroups,
        justUUIDs,
      );

    console.log(newStoreWithInformationObjectTypeUUIDs);
    const selectedTemplates = this.stripUndefinedTemplates(
      this.addObjectUUIDsToTemplate(
        this.newStoredZaakTypeTemplateGroups,
        justUUIDs,
      ),
    );

    console.log("Storing SmartDocuments Mapping", selectedTemplates);
    return this.smartDocumentsService.storeTemplatesMapping(
      this.zaakTypeUuid,
      selectedTemplates,
    );
  }

  private stripUndefinedTemplates = (data) => {
    return data
      .map((group) => {
        const templates = group.templates
          ?.filter((template) => template.informatieObjectTypeUUID)
          .map(({ parentGroupId, ...template }) => template);

        const groups = group.groups
          ? this.stripUndefinedTemplates(group.groups)
          : [];

        if (templates?.length || groups?.length) {
          const { parentGroupId, ...cleanedGroup } = group;
          return {
            ...cleanedGroup,
            templates,
            groups,
          };
        }

        return undefined;
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

      const flattenedGroups = item.groups.flatMap((group) => ({
        ...group,
        templates: group.templates.map((template) => ({
          ...template,
          parentGroupId: group.id,
        })),
      }));

      return [rootGroup, ...flattenedGroups];
    });
  };

  private addObjectUUIDsToTemplate = (data, UUIDsToAdd) => {
    const assignInformatieObjectTypeUUID = (items) =>
      items.map((item) => ({
        ...item,
        templates: item.templates?.map((template) => ({
          ...template,
          informatieObjectTypeUUID: UUIDsToAdd.find(
            (uuidItem) =>
              uuidItem.id === template.id &&
              uuidItem.parentGroupId === template.parentGroupId,
          )?.informatieObjectTypeUUID,
        })),
        groups: item.groups
          ? assignInformatieObjectTypeUUID(item.groups)
          : undefined,
      }));

    return assignInformatieObjectTypeUUID(data);
  };

  private convertToIdAndUUIDArrayFlat = (data) => {
    return data.flatMap((item) =>
      item.templates.map((template) => ({
        id: template.id,
        parentGroupId: template.parentGroupId,
        informatieObjectTypeUUID: template.informatieObjectTypeUUID,
      })),
    );
  };

  private convertToIdAndUUIDMestedGroupsArray = (data) => {
    return data.flatMap((item) => {
      // Check if 'groups' exists
      const groupTemplates = item.groups
        ? item.groups.flatMap((group) =>
            group.templates.map((template) => ({
              id: template.id,
              parentGroupId: template.parentGroupId,
              informatieObjectTypeUUID: template.informatieObjectTypeUUID,
            })),
          )
        : []; // If 'groups' is not present, return an empty array

      // Handle 'templates' directly at the item level
      const itemTemplates = item.templates
        ? item.templates.map((template) => ({
            id: template.id,
            parentGroupId: template.parentGroupId,
            informatieObjectTypeUUID: template.informatieObjectTypeUUID,
          }))
        : []; // If 'templates' is not present, return an empty array

      // Combine both arrays
      return [...groupTemplates, ...itemTemplates];
    });
  };

  private addParentIdToTemplates = (data) => {
    const assignParentGroupId = (items) =>
      items.map((item) => ({
        ...item,
        templates: item.templates?.map((template) => ({
          ...template,
          parentGroupId: item.id,
        })),
        groups: item.groups ? assignParentGroupId(item.groups) : undefined,
      }));

    return assignParentGroupId(data);
  };
}
