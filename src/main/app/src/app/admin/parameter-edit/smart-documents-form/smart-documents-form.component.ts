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
  MappedSmartDocumentsTemplateFlattenedGroupWithParentId,
  MappedSmartDocumentsTemplateGroupWithParentId,
  MappedSmartDocumentsTemplateWithParentId,
  SmartDocumentsService,
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

  private prepareDatasource() {
    const allSmartDocumentTemplateGroups: GeneratedType<"RestSmartDocumentsTemplateGroup">[] =
      this.smartDocumentsService.addParentIdToTemplates(
        this.allSmartDocumentTemplateGroupsQuery.data(),
      );

    this.currentTemplateMappings =
      this.smartDocumentsService.addParentIdToTemplates(
        this.currentTemplateMappingsQuery.data(),
      );

    this.informationObjectTypes = this.informationObjectTypesQuery.data();

    this.newTemplateMappings = this.smartDocumentsService.addTemplateMappings(
      allSmartDocumentTemplateGroups,
      this.smartDocumentsService.getTemplateMappings(
        this.currentTemplateMappings,
      ),
    );

    this.dataSource.data =
      this.smartDocumentsService.flattenNestedGroupsToRootGroups(
        this.newTemplateMappings,
      );
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
    const hasInformationObjectType = this.dataSource.data
      .find((node) => node.id === id)
      ?.templates.some((_node) => _node.informatieObjectTypeUUID);

    return !!hasInformationObjectType;
  }

  handleMatTreeNodeChange({
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
    return this.smartDocumentsService.storeTemplatesMapping(
      this.zaakTypeUuid,
      this.smartDocumentsService.getMappedTemplates(this.newTemplateMappings),
    );
  }
}
