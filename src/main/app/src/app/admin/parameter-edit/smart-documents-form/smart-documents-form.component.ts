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

  informationObjectTypes: Informatieobjecttype[] = [];

  constructor(
    private smartDocumentsService: SmartDocumentsService,
    private informatieObjectenService: InformatieObjectenService,
  ) {
    console.log("SmartDocumentsFormComponent constructor called");

    effect(() => {
      this.dataSource.data =
        this.allSmartDocumentTemplateGroupsQuery.data() || [];
      console.log("this.dataSource.data:", this.dataSource.data);
    });

    effect(() => {
      this.informationObjectTypes =
        this.informationObjectTypesQuery.data() || [];
      console.log("this.informationObjectTypes:", this.informationObjectTypes);
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

  allSmartDocumentTemplateGroupsQuery = injectQuery(() => ({
    queryKey: ["allSmartDocumentTemplateGroupsQuery"],
    queryFn: () =>
      firstValueFrom(
        this.smartDocumentsService.getAllSmartDocumentsTemplateGroups(),
      ),
  }));

  templateMappingsQuery = injectQuery(() => ({
    queryKey: ["templateMappingsQuery", this.zaakTypeUuid],
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

  dataSource = new MatTreeFlatDataSource(this.treeControl, this.treeFlattener);

  hasChild2 = (_: number, { templates = [] }: SmartDocumentsTemplateGroup) =>
    !!templates.length;

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

  hasChild = (_: number, node: { expandable: boolean }) => node.expandable;
}
