import { Component, EventEmitter, Input, Output } from "@angular/core";
import { FormGroup } from "@angular/forms";

@Component({
  selector: "smart-documents-form-group",
  templateUrl: "./smart-documents-form-group.component.html",
})
export class SmartDocumentsFormGroupComponent {
  @Input() formGroup: FormGroup; // FormGroup passed from parent
  @Output() formValidityChanged = new EventEmitter<boolean>();

  constructor() {
    console.log("SmartDocumentsFormGroupComponent constructor called");
  }

  ngOnInit() {
    if (this.formGroup) {
      console.log("FormGroup initialized in SmartDocumentsFormGroupComponent");
    }

    // Emit form validity changes to the parent
    this.formGroup.statusChanges.subscribe((status) => {
      console.log("Form status in child changed:", status);
      this.formValidityChanged.emit(this.formGroup.valid);
    });
  }
}
