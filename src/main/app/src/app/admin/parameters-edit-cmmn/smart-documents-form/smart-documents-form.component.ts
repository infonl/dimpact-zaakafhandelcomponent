/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FlatTreeControl } from "@angular/cdk/tree";
import { NgIf } from "@angular/common";
import { Component, effect, Input, OnInit } from "@angular/core";
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
} from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatChipsModule } from "@angular/material/chips";
import { MatDividerModule } from "@angular/material/divider";
import { MatIconModule } from "@angular/material/icon";
import { MatSlideToggleModule } from "@angular/material/slide-toggle";
import {
  MatTreeFlatDataSource,
  MatTreeFlattener,
  MatTreeModule,
} from "@angular/material/tree";
import { TranslateModule } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { firstValueFrom } from "rxjs";
import { InformatieObjectenService } from "src/app/informatie-objecten/informatie-objecten.service";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import {
  SmartDocumentsService,
  TemplateMapping,
} from "../../smart-documents.service";
import { SmartDocumentsFormItemComponent } from "./smart-documents-form-item/smart-documents-form-item.component";

interface FlatNode {
  expandable: boolean;
  name: string;
  level: number;
}

@Component({
  selector: "smart-documents-form",
  templateUrl: "./smart-documents-form.component.html",
  styleUrl: "./smart-documents-form.component.less",
  standalone: true,
  imports: [
    NgIf,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatDividerModule,
    MatIconModule,
    MatSlideToggleModule,
    MatTreeModule,
    TranslateModule,
    SmartDocumentsFormItemComponent,
  ],
})
export class SmartDocumentsFormComponent implements OnInit {
  @Input({ required: true }) zaakTypeUuid!: string;
  @Input({ required: true }) enabledGlobally!: boolean;
  @Input() enabledForZaaktype: boolean = false;

  enabledForZaaktypeForm = new FormGroup({
    enabledForZaaktype: new FormControl<boolean>(false),
  });

  get enabledForZaaktypeValue(): boolean {
    return Boolean(this.enabledForZaaktypeForm.value.enabledForZaaktype);
  }

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

  ngOnInit(): void {
    this.enabledForZaaktypeForm.controls.enabledForZaaktype.setValue(
      this.enabledForZaaktype,
    );
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

  private readonly allSmartDocumentTemplateGroupsQuery = injectQuery(() => ({
    queryKey: ["allSmartDocumentTemplateGroupsQuery"],
    refetchOnWindowFocus: false,
    queryFn: () =>
      firstValueFrom(
        this.smartDocumentsService.getAllSmartDocumentsTemplateGroups(),
      ),
  }));

  private readonly currentTemplateMappingsQuery = injectQuery(() => ({
    queryKey: ["currentTemplateMappingsQuery", this.zaakTypeUuid],
    refetchOnWindowFocus: false,
    queryFn: () =>
      firstValueFrom(
        this.smartDocumentsService.getTemplatesMapping(this.zaakTypeUuid),
      ),
  }));

  private readonly informationObjectTypesQuery = injectQuery(() => ({
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
