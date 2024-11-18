import { Component, effect, EventEmitter, Input, Output } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { firstValueFrom } from "rxjs";
import { InformatieObjectenService } from "src/app/informatie-objecten/informatie-objecten.service";
import {
  DocumentsTemplate,
  DocumentsTemplateGroup,
  SmartDocumentsService,
  SmartDocumentsTemplateGroup,
} from "../../smart-documents.service";
import { FlatTreeControl } from "@angular/cdk/tree";
import {
  MatTreeFlatDataSource,
  MatTreeFlattener,
} from "@angular/material/tree";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { Informatieobjecttype } from "src/app/informatie-objecten/model/informatieobjecttype";

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
  informationObjectTypes: Informatieobjecttype[] = [];
  zaakTypeTemplateMappings: DocumentsTemplateGroup[] = [];

  constructor(
    private smartDocumentsService: SmartDocumentsService,
    private informatieObjectenService: InformatieObjectenService,
  ) {
    effect(() => {
      this.allSmartDocumentTemplateGroup =
        this.allSmartDocumentTemplateGroupsQuery.data() || [];
      console.log("full group list:", this.dataSource.data);

      this.informationObjectTypes =
        this.informationObjectTypesQuery.data() || [];

      this.zaakTypeTemplateMappings =
        this.zaakTypeTemplateMappingsQuery.data() || [];
      console.log("zaakTypeTemplateMappings:", this.zaakTypeTemplateMappings);

      (this.dataSource.data = this.mergeSelectedTemplates(
        this.allSmartDocumentTemplateGroup,
        this.zaakTypeTemplateMappings,
      )),
        console.log("mergeSelectedTemplates:", this.dataSource.data);
    });
  }

  ngOnInit() {
    if (this.formGroup) {
    }

    // Emit form validity changes to the parent
    this.formGroup.statusChanges.subscribe((status) => {
      console.log("Form status in child changed:", status);
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
    console.log(node);
    return {
      id: node.id,
      name: node.name,
      informatieObjectTypeUUID: node.informatieObjectTypeUUID || undefined,
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

  private mergeSelectedTemplates = (
    allTemplatesObject,
    selectedTemplatesObject,
  ) => {
    const mapSelectedTemplates = selectedTemplatesObject.reduce((acc, item) => {
      acc[item.id] = item.templates.reduce((innerAcc, template) => {
        innerAcc[template.id] = template.informatieObjectTypeUUID;
        return innerAcc;
      }, {});
      return acc;
    }, {});

    const updateTemplates = (templates, selectedTemplates) => {
      return templates.map((template) => ({
        ...template,
        ...(selectedTemplates[template.id] && {
          informatieObjectTypeUUID: selectedTemplates[template.id],
        }),
      }));
    };

    return allTemplatesObject.map((item) => ({
      ...item,
      templates: updateTemplates(
        item.templates,
        mapSelectedTemplates[item.id] || {},
      ),
    }));
  };
}
