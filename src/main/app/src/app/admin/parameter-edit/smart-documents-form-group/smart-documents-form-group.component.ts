import { Component, EventEmitter, OnInit, Output } from "@angular/core";
import { FormBuilder, FormGroup, Validators } from "@angular/forms";

@Component({
  selector: "smart-documents-form-group",
  templateUrl: "./smart-documents-form-group.component.html",
  //   styleUrls: ["./smart-documents-form-group.component.less"],
})
export class SmartDocumentsFormGroupComponent implements OnInit {
  formGroup: FormGroup;

  @Output() formValidityChanged = new EventEmitter<boolean>();

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    this.createForm();
  }

  createForm() {
    this.formGroup = this.fb.group({
      documentTitle: ["", [Validators.required]],
      documentDescription: ["", [Validators.required]],
    });

    this.formValidityChanged.emit(this.formGroup.valid);

    this.formGroup.statusChanges.subscribe(() => {
      this.formValidityChanged.emit(this.formGroup.valid);
    });
  }

  get isValid(): boolean {
    return this.formGroup.valid;
  }
}
