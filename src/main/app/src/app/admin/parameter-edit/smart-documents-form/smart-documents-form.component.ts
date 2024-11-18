import { Component, EventEmitter, Input, Output } from "@angular/core";
import { FormGroup } from "@angular/forms";

@Component({
  selector: "smart-documents-form",
  templateUrl: "./smart-documents-form.component.html",
})
export class SmartDocumentsFormComponent {
  @Input() formGroup: FormGroup;
  @Output() formValidityChanged = new EventEmitter<boolean>();

  constructor() {
    console.log("SmartDocumentsFormComponent constructor called");
  }

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
