/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FlatTreeControl } from "@angular/cdk/tree";
import { Component, effect, Input } from "@angular/core";
import { FormBuilder, FormGroup } from "@angular/forms";
import {
  MatTreeFlatDataSource,
  MatTreeFlattener,
} from "@angular/material/tree";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { firstValueFrom, Observable } from "rxjs";
import { InformatieObjectenService } from "src/app/informatie-objecten/informatie-objecten.service";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import {
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
    private formBuilder: FormBuilder,
  ) {
    effect(() => this.prepareDatasource());

    this.formGroup = this.formBuilder.group({});
  }

  private prepareDatasource() {
    const allSmartDocumentTemplateGroups: GeneratedType<"RestSmartDocumentsTemplateGroup">[] =
      this.smartDocumentsService.addParentIdsToMakeTemplatesUnique(
        this.allSmartDocumentTemplateGroupsQuery.data(),
      );

    this.currentTemplateMappings =
      this.smartDocumentsService.addParentIdsToMakeTemplatesUnique(
        this.currentTemplateMappingsQuery.data(),
      );

    const informationObjectTypesQueryData =
      this.informationObjectTypesQuery.data();
    if (informationObjectTypesQueryData) {
      this.informationObjectTypes = informationObjectTypesQueryData;
    }

    this.newTemplateMappings = this.smartDocumentsService.addTemplateMappings(
      allSmartDocumentTemplateGroups,
      this.smartDocumentsService.getTemplateMappings(
        this.currentTemplateMappings,
      ),
    );

    this.dataSource.data = this.smartDocumentsService.flattenNestedGroups(
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

  private _transformer = (node: Record<string, unknown>, level: number) => {
    return {
      ...node,
      name: String(node.name),
      level: level,
      expandable: Boolean(
        "templates" in node &&
          Array.isArray(node.templates) &&
          node.templates.length,
      ),
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
    (node) => node.templates as Record<string, unknown>[],
  );

  dataSource = new MatTreeFlatDataSource(this.treeControl, this.treeFlattener);

  hasChild = (_: number, node: { expandable: boolean }) => node.expandable;

  hasSelected(id: string): boolean {
    const templates = this.dataSource.data.find(
      (node) => node.id === id,
    )?.templates;

    if (!templates) return false;

    return (templates as { informatieObjectTypeUUID: string }[]).some(
      (templateNode) => templateNode.informatieObjectTypeUUID,
    );
  }

  handleMatTreeNodeChange({
    id,
    parentGroupId,
    informatieObjectTypeUUID,
  }: FlatNode & MappedSmartDocumentsTemplateWithParentId): void {
    let nodeUpdated = false;

    this.dataSource.data.forEach((node) => {
      (
        node.templates as {
          id: string;
          parentGroupId: string;
          informatieObjectTypeUUID: string;
        }[]
      )?.forEach((templateNode) => {
        if (
          templateNode.id === id &&
          templateNode.parentGroupId === parentGroupId
        ) {
          templateNode.informatieObjectTypeUUID = informatieObjectTypeUUID;
          nodeUpdated = true;
        }
      });
    });

    if (!nodeUpdated) {
      throw new Error(
        `Node not found: ${JSON.stringify({ id, parentGroupId })}`,
      );
    }
  }

  public saveSmartDocumentsMapping(): Observable<never> {
    return this.smartDocumentsService.storeTemplatesMapping(
      this.zaakTypeUuid,
      this.smartDocumentsService.getOnlyMappedTemplates(
        this.newTemplateMappings,
      ),
    );
  }
}
