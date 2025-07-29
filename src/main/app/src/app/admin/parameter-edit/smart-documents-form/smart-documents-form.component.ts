/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FlatTreeControl } from "@angular/cdk/tree";
import { Component, effect, Input } from "@angular/core";
import { FormBuilder } from "@angular/forms";
import {
  MatTreeFlatDataSource,
  MatTreeFlattener,
} from "@angular/material/tree";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { firstValueFrom } from "rxjs";
import { InformatieObjectenService } from "src/app/informatie-objecten/informatie-objecten.service";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import {
  SmartDocumentsService,
  TemplateMapping,
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
  @Input({ required: true }) zaakTypeUuid!: string;

  formGroup = this.formBuilder.group({});

  currentTemplateMappings: GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[] =
    [];
  informationObjectTypes: GeneratedType<"RestInformatieobjecttype">[] = [];

  newTemplateMappings: GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[] =
    [];

  constructor(
    private smartDocumentsService: SmartDocumentsService,
    private informatieObjectenService: InformatieObjectenService,
    private formBuilder: FormBuilder,
  ) {
    effect(() => this.prepareDatasource());
  }

  private prepareDatasource() {
    const allSmartDocumentTemplateGroups: GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[] =
      this.smartDocumentsService.addParentIdsToTemplates(
        this.convertApiData(this.allSmartDocumentTemplateGroupsQuery.data()),
      );

    this.currentTemplateMappings =
      this.smartDocumentsService.addParentIdsToTemplates(
        this.convertApiData(this.currentTemplateMappingsQuery.data()),
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

    this.dataSource.data = this.smartDocumentsService.flattenGroups(
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

  private convertApiData(
    data?: GeneratedType<
      | "RestSmartDocumentsTemplateGroup"
      | "RestMappedSmartDocumentsTemplateGroup"
    >[],
  ): GeneratedType<"RestMappedSmartDocumentsTemplateGroup">[] {
    if (!data) return [];
    return data.map((group) => ({
      ...group,
      templates:
        group.templates?.map((template) => ({
          ...template,
          informatieObjectTypeUUID:
            "informatieObjectTypeUUID" in template
              ? template.informatieObjectTypeUUID
              : "",
        })) || undefined,
      groups: group.groups ? this.convertApiData(group.groups) : undefined,
    }));
  }

  hasSelected(id: string) {
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
  }: TemplateMapping) {
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
          templateNode.informatieObjectTypeUUID =
            informatieObjectTypeUUID || "";
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

  public saveSmartDocumentsMapping() {
    return this.smartDocumentsService.storeTemplatesMapping(
      this.zaakTypeUuid,
      this.smartDocumentsService.getOnlyMappedTemplates(
        this.newTemplateMappings,
      ),
    );
  }
}
