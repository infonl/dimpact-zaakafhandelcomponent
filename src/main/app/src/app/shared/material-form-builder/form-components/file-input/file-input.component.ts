/*
 * SPDX-FileCopyrightText: 024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AsyncPipe } from "@angular/common";
import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  OnInit,
  inject,
  viewChild,
} from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatMiniFabButton } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { Observable, tap } from "rxjs";
import { FileDragAndDropDirective } from "../../../directives/file-drag-and-drop.directive";
import { FormComponent } from "../../model/form-component";
import { FileFormField } from "../file/file-form-field";
import { MatFileInput } from "./file-input-control";

@Component({
  standalone: true,
  selector: "zac-file",
  styles: [
    `
      ::ng-deep .file-field .mat-mdc-form-field-subscript-wrapper {
        margin-top: -16px;
      }

      input[type="file"] {
        position: absolute;
        inset: 0;
        padding-top: var(
          --mat-form-field-filled-with-label-container-padding-top
        );
        padding-bottom: var(
          --mat-form-field-filled-with-label-container-padding-bottom
        );
      }

      input[type="file"].empty {
        opacity: 0;
      }

      ::file-selector-button {
        display: none;
      }
    `,
  ],
  template: `
    <div DropZone (onFileDropped)="handleDrop($event)">
      <mat-form-field
        appearance="fill"
        subscriptSizing="dynamic"
        class="file-field full-width"
      >
        <mat-label>{{ data.label | translate }}</mat-label>

        @if (!data.formControl.value) {
          <button
            mat-mini-fab
            matSuffix
            type="button"
            (click)="fileInput.click()"
          >
            <mat-icon>upload</mat-icon>
          </button>
        }
        @if (data.formControl.value) {
          <button mat-mini-fab matSuffix (click)="reset($event)" type="button">
            <mat-icon>delete</mat-icon>
          </button>
        }

        <mat-hint>
          @if (
            (data.formControl.invalid && data.formControl.touched) ||
            data.uploadError
          ) {
            <div class="error">
              {{ getErrorMessage() }}
            </div>
          }
          @if (data.hint) {
            <div>{{ data.hint?.label }}</div>
          }
        </mat-hint>

        <input
          type="file"
          [accept]="data.getAllowedFileTypes()"
          #fileInput
          [required]="data.required"
          id="uploadFile"
          (change)="handleChange($event)"
          name="uploadFile"
          matFileInput
          [class.empty]="!data.formControl.value"
          (click)="$event.stopPropagation()"
        />
      </mat-form-field>
    </div>
  `,
  imports: [
    MatInputModule,
    MatIconModule,
    MatMiniFabButton,
    FormsModule,
    FileDragAndDropDirective,
    ReactiveFormsModule,
    TranslateModule,
    AsyncPipe,
    MatFileInput,
  ],
})
export class FileInputComponent extends FormComponent implements OnInit {
  fileInput = viewChild<ElementRef<HTMLInputElement>>("fileInput");
  translate = inject(TranslateService);
  changeDetector = inject(ChangeDetectorRef);

  data: FileFormField;

  triggerResetFromParent$: Observable<void>;
  controlValue$: Observable<File>;

  ngOnInit() {
    this.triggerResetFromParent$ = this.data.reset$.pipe(
      tap(() => this.reset()),
    );
  }

  reset($event?: MouseEvent) {
    if ($event) {
      $event.stopPropagation();
    }
    this.data.formControl.setValue("");
    this.changeDetector.detectChanges();
  }

  getErrorMessage(): string {
    if (this.data.uploadError) {
      return this.data.uploadError;
    }
    return super.getErrorMessage();
  }

  selectFile(file: File): void {
    const validated = this.validateFile(file);
    if (validated) {
      this.data.formControl.setValue(file);
      this.data.formControl.updateValueAndValidity();
      this.changeDetector.detectChanges();
    }
  }

  handleChange(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files[0];
    this.selectFile(file);
  }

  handleDrop(files: FileList) {
    this.fileInput().nativeElement.files = files;
    this.selectFile(files[0]);
  }

  validateFile(file: File): boolean {
    this.data.uploadError = null;
    if (!file) {
      this.data.formControl.setValue("");
      this.changeDetector.detectChanges();
      return false;
    }

    if (!this.data.isBestandstypeToegestaan(file)) {
      this.data.uploadError = `Het bestandstype is niet toegestaan (${this.data.getBestandsextensie(
        file,
      )})`;
      return false;
    }

    if (!this.data.isBestandsgrootteToegestaan(file)) {
      this.data.uploadError = `Het bestand is te groot (${this.data.getBestandsgrootteMB(
        file,
      )}MB)`;
      return false;
    }

    if (!file.size) {
      this.data.uploadError = "Het bestand is leeg";
      this.data.formControl.setErrors({
        emptyFile: true,
      });
      return false;
    }

    return true;
  }
}
