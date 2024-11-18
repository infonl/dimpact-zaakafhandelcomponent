/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NestedTreeControl } from "@angular/cdk/tree";
import { Component, Input, effect } from "@angular/core";
import { MatTreeNestedDataSource } from "@angular/material/tree";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { Observable, firstValueFrom } from "rxjs";
import { InformatieObjectenService } from "src/app/informatie-objecten/informatie-objecten.service";
import {
  DocumentsTemplateGroup,
  SmartDocumentsService,
  SmartDocumentsTemplate,
  SmartDocumentsTemplateGroup,
} from "../../smart-documents.service";
import {GeneratedType} from "../../../shared/utils/generated-types";

function getSelectableGroup(
  original: SmartDocumentsTemplateGroup,
  selection?: DocumentsTemplateGroup,
): DocumentsTemplateGroup {
  return {
    ...original,
    groups:
      original.groups &&
      getSelectableGroups(original.groups, selection?.groups ?? []),
    templates:
      original.templates &&
      getSelectableTemplates(original.templates, selection?.templates ?? []),
  };
}

function getSelectableTemplates(
  original: SmartDocumentsTemplate[],
  selection: GeneratedType<'RestMappedSmartDocumentsTemplate'>[],
): GeneratedType<'RestMappedSmartDocumentsTemplate'>[] {
  return original.map((template) => ({
    ...template,
    informatieObjectTypeUUID:
      selection.find(({ id }) => id === template.id)
        ?.informatieObjectTypeUUID ?? "",
  }));
}

export function getSelectableGroups(
  original: SmartDocumentsTemplateGroup[],
  selection: DocumentsTemplateGroup[],
): DocumentsTemplateGroup[] {
  return original.map((group) =>
    getSelectableGroup(
      group,
      selection.find(({ id }) => id === group.id),
    ),
  );
}

export function filterOutUnselected(
  group: DocumentsTemplateGroup,
): DocumentsTemplateGroup | undefined {
  const groups = group.groups?.map(filterOutUnselected).filter(Boolean);
  const templates = group.templates?.filter(
    ({ informatieObjectTypeUUID }) => informatieObjectTypeUUID?.length,
  );
  return groups?.length || templates?.length
    ? {
        ...group,
        groups,
        templates,
      }
    : undefined;
}

@Component({
  selector: "smart-documents-tree",
  styleUrl: "./smart-documents-tree.component.less",
  templateUrl: "./smart-documents-tree.component.html",
})
export class SmartDocumentsTreeComponent {
  @Input() zaaktypeUuid: string;
  treeControl = new NestedTreeControl<DocumentsTemplateGroup>(
    ({ groups = [], templates = [] }) => [...groups, ...templates],
  );
  informatieObjectTypes = injectQuery(() => ({
    queryKey: ["informatieObjectTypes", this.zaaktypeUuid],
    queryFn: () =>
      firstValueFrom(
        this.informatieObjectenService.listInformatieobjecttypes(
          this.zaaktypeUuid,
        ),
      ),
  }));
  allSmartDocumentTemplates = injectQuery(() => ({
    queryKey: ["all smart documents"],
    queryFn: () =>
      firstValueFrom(
        this.smartDocumentsService.getAllSmartDocumentsTemplates(),
      ),
  }));
  templateMappings = injectQuery(() => ({
    queryKey: [
      "smart documents template mapping for zaaktype",
      this.zaaktypeUuid,
    ],
    queryFn: () =>
      firstValueFrom(
        this.smartDocumentsService.getTemplatesMapping(this.zaaktypeUuid),
      ),
  }));
  dataSource = new MatTreeNestedDataSource<DocumentsTemplateGroup>();
  parameters: any;

  zaaktypeVertrouwelijkheid(selectedUuid: string): string {
    return selectedUuid
      ? this.informatieObjectTypes
          .data()
          .find(({ uuid }) => uuid === selectedUuid).vertrouwelijkheidaanduiding
      : null;
  }

  constructor(
    private smartDocumentsService: SmartDocumentsService,
    private informatieObjectenService: InformatieObjectenService,
  ) {
    effect(() => {
      this.dataSource.data = getSelectableGroups(
        this.allSmartDocumentTemplates.data() || [],
        this.templateMappings.data() || [],
      );
    });
  }

  hasChild = (
    _: number,
    { groups = [], templates = [] }: DocumentsTemplateGroup,
  ) => !!groups.length || !!templates.length;

  save(): Observable<never> {
    return this.smartDocumentsService.storeTemplatesMapping(
      this.zaaktypeUuid,
      this.dataSource.data.map(filterOutUnselected).filter(Boolean),
    );
  }
}
