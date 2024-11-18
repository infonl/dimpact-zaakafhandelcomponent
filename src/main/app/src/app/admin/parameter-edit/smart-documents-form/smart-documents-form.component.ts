import { Component, effect, EventEmitter, Input, Output } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { firstValueFrom } from "rxjs";
import { InformatieObjectenService } from "src/app/informatie-objecten/informatie-objecten.service";
import {
  SmartDocumentsService,
  SmartDocumentsTemplateGroup,
} from "../../smart-documents.service";
import { FlatTreeControl } from "@angular/cdk/tree";
import {
  MatTreeFlatDataSource,
  MatTreeFlattener,
} from "@angular/material/tree";

@Component({
  selector: "smart-documents-form",
  templateUrl: "./smart-documents-form.component.html",
})
export class SmartDocumentsFormComponent {
  @Input() formGroup: FormGroup;
  @Input() zaakTypeUuid: string;
  @Output() formValidityChanged = new EventEmitter<boolean>();

  constructor(
    private smartDocumentsService: SmartDocumentsService,
    private informatieObjectenService: InformatieObjectenService,
  ) {
    console.log("SmartDocumentsFormComponent constructor called");

    effect(() => {
      this.dataSource.data = this.allSmartDocumentTemplates.data() || [];
    });
  }

  private _transformer = (node: SmartDocumentsTemplateGroup, level: number) => {
    return {
      expandable: !!node.templates && node.templates.length > 0,
      id: node.id,
      name: node.name,
      level: level,
    };
  };

  treeControl = new FlatTreeControl<SmartDocumentsTemplateGroup>(
    (node) => 0,
    (node) => true,
  );

  treeFlattener = new MatTreeFlattener(
    this._transformer,
    (node) => node.level,
    (node) => node.expandable,
    (node) => node.templates,
  );

  allSmartDocumentTemplates = injectQuery(() => ({
    queryKey: ["all smart documents"],
    queryFn: () => firstValueFrom(this.smartDocumentsService.listTemplates()),
  }));

  templateMappings = injectQuery(() => ({
    queryKey: [
      "smart documents template mapping for zaaktype",
      this.zaakTypeUuid,
    ],
    queryFn: () =>
      firstValueFrom(
        this.smartDocumentsService.getTemplatesMappingFlat(this.zaakTypeUuid),
      ),
  }));

  dataSource = new MatTreeFlatDataSource(this.treeControl, this.treeFlattener);

  ngOnInit() {
    if (this.formGroup) {
      console.log("FormGroup initialized in SmartDocumentsFormComponent");
    }

    // Emit form validity changes to the parent
    this.formGroup.statusChanges.subscribe((status) => {
      console.log("Form status in child changed:", status);
      this.formValidityChanged.emit(this.formGroup.valid);
    });
  }
}
