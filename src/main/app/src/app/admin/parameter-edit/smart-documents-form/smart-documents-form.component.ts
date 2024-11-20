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

  allSmartDocumentTemplateGroup: SmartDocumentsTemplateGroup[] = [];
  informationObjectTypes: GeneratedType<"RestInformatieobjecttype">[] = [];
  zaakTypeTemplateMappings: DocumentsTemplateGroup[] = [];

  constructor(
    private smartDocumentsService: SmartDocumentsService,
    private informatieObjectenService: InformatieObjectenService,
  ) {
    effect(() => {
      this.allSmartDocumentTemplateGroup =
        this.allSmartDocumentTemplateGroupsQuery.data() || [];

      this.informationObjectTypes =
        this.informationObjectTypesQuery.data() || [];

      this.zaakTypeTemplateMappings =
        this.zaakTypeTemplateMappingsQuery.data() || [];

      this.dataSource.data = this.addParentIds(
        this.mergeSelectedTemplates(
          this.allSmartDocumentTemplateGroup,
          this.zaakTypeTemplateMappings,
        ),
      );

      console.log("Tree data", this.dataSource.data);
    });
  }

  ngOnInit() {
    // Emit form validity changes to the parent
    this.formGroup.statusChanges.subscribe((status) => {
      this.formValidityChanged.emit(this.formGroup.valid);
    });
  }

  allSmartDocumentTemplateGroupsQuery = injectQuery(() => ({
    queryKey: ["allSmartDocumentTemplateGroupsQuery"],
    queryFn: () =>
      firstValueFrom(
        this.smartDocumentsService.getAllSmartDocumentsTemplateGroups(),
      ),
  }));

  zaakTypeTemplateMappingsQuery = injectQuery(() => ({
    queryKey: ["zaakTypeTemplateMappingsQuery", this.zaakTypeUuid],
    queryFn: () =>
      firstValueFrom(
        this.smartDocumentsService.getZaakTypeTemplatesMappings(
          this.zaakTypeUuid,
        ),
      ),
  }));

  informationObjectTypesQuery = injectQuery(() => ({
    queryKey: ["informationObjectTypesQuery", this.zaakTypeUuid],
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
      parentId: node.parentId,
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

  addParentIds(nodes: any[], parentId: string | null = null): any[] {
    return nodes.map((node) => {
      const newNode = {
        ...node, // Spread the original node to keep other properties
        parentId: parentId, // Add the parentId (for the root it's null, for children, it's the parent's id)
        templates: node.templates
          ? this.addParentIds(node.templates, node.id)
          : [], // Recursively assign parentId to children
      };
      return newNode;
    });
  }

  hasSelectedInformationObjectType(id: any): boolean {
    const nodeHasSelectedInformationObjectType = this.dataSource.data
      .find((node) => node.id === id)
      ?.templates.some((_node) => _node.informatieObjectTypeUUID !== "");

    return !!nodeHasSelectedInformationObjectType;
  }

  onNodeChange(node: any): void {
    const id = node.id;
    const parentId = node.parentId;
    const informatieObjectTypeUUID = node.informatieObjectTypeUUID;

    const findAndUpdateNode = (
      _nodes: any[],
      _parentId: string | null,
    ): boolean => {
      for (const currentNode of _nodes) {
        if (currentNode.id === id && currentNode.parentId === _parentId) {
          currentNode.informatieObjectTypeUUID = informatieObjectTypeUUID;
          return true;
        } else if (currentNode.templates) {
          if (findAndUpdateNode(currentNode.templates, _parentId)) return true;
        }
      }
      return false;
    };

    if (findAndUpdateNode(this.dataSource.data, parentId)) {
      // this.dataSource.data = [...this.dataSource.data]; // Trigger re-render
      console.log("Tree updated", this.dataSource.data);
    } else {
      console.error("Node not found:", { id, parentId });
    }
  }

  private mergeSelectedTemplates = (
    allTemplatesObject: SmartDocumentsTemplateGroup[],
    selectedTemplatesObject: DocumentsTemplateGroup[],
  ) => {
    return allTemplatesObject.map((smartDocumentTemplateGroup) => {
      const templates = smartDocumentTemplateGroup.templates.map(
        (smartDocumentTemplate) => {
          const selectedTemplates =
            selectedTemplatesObject.find(
              ({ id }) => id === smartDocumentTemplateGroup.id,
            )?.templates ?? [];

          const informatieObjectTypeUUID =
            selectedTemplates.find(({ id }) => id === smartDocumentTemplate.id)
              ?.informatieObjectTypeUUID ?? "";

          return {
            ...smartDocumentTemplate,
            informatieObjectTypeUUID,
          } satisfies GeneratedType<"RestMappedSmartDocumentsTemplate">;
        },
      );

      return {
        ...smartDocumentTemplateGroup,
        templates,
      };
    });
  };

  public storeSmartDocumentsConfig(): Observable<never> {
    console.log("Saving storeSmartDocumentsConfig", this.dataSource.data);
    return this.smartDocumentsService.storeTemplatesMapping(
      this.zaakTypeUuid,
      this.dataSource.data,
    );
  }
}
