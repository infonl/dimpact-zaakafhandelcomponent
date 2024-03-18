import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  OnInit,
  ViewChild,
  inject,
  viewChild,
} from "@angular/core";
import { FormComponent } from "../../model/form-component";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { FileFormField } from "../file/file-form-field";
import { MatInputModule } from "@angular/material/input";
import { MatIconModule } from "@angular/material/icon";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { FileDragAndDropDirective } from "../../../directives/file-drag-and-drop.directive";
import { Observable, map, tap } from "rxjs";
import { MatMiniFabButton } from "@angular/material/button";
import { AsyncPipe, CommonModule } from "@angular/common";

export const UploadStatus = {
  SELECT_FILE: "SELECT_FILE",
  SELECTED: "SELECTED",
};

@Component({
  standalone: true,
  selector: "zac-file",
  styles: [
    `
      ::ng-deep .file-field .mat-mdc-form-field-subscript-wrapper {
        margin-top: -16px;
      }
    `,
  ],
  template: `
    <div DropZone (onFileDropped)="selectFile($event)">
      <mat-form-field
        appearance="fill"
        subscriptSizing="dynamic"
        class="file-field full-width"
        (click)="fileInput.click()"
      >
        <mat-label>{{ data.label | translate }}</mat-label>
        <input
          [value]="fileName$ | async"
          [id]="data.id + '_filefield'"
          [readonly]="true"
          [required]="data.required"
          matInput
        />

        @if (status === 'SELECT_FILE') {
        <button mat-mini-fab matSuffix type="button">
          <mat-icon>upload</mat-icon>
        </button>
        } @if (status === 'SELECTED') {
        <button mat-mini-fab matSuffix (click)="reset($event)" type="button">
          <mat-icon>delete</mat-icon>
        </button>
        }

        <mat-hint>
          @if ((data.formControl.invalid && data.formControl.touched) ||
          data.uploadError) {
          <div class="error">
            {{ getErrorMessage() }}
          </div>
          } @if (data.hint) {
          <div>{{ data.hint?.label }}</div>
          }
        </mat-hint>

        <input
          [formControl]="data.formControl"
          hidden
          type="file"
          [accept]="data.getAllowedFileTypes()"
          #fileInput
          id="uploadFile"
          (change)="handleChange($event)"
          name="uploadFile"
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
  ],
})
export class FileInputComponent extends FormComponent implements OnInit {
  fileInput = viewChild<ElementRef>("fileInput");
  translate = inject(TranslateService);
  changeDetector = inject(ChangeDetectorRef);

  data: FileFormField;
  status = UploadStatus.SELECT_FILE;

  triggerResetFromParent$: Observable<void>;
  fileName$: Observable<string>;
  controlValue$: Observable<File>;

  ngOnInit() {
    this.triggerResetFromParent$ = this.data.reset$.pipe(
      tap(() => this.reset()),
    );
    this.fileName$ = this.data.formControl.valueChanges.pipe(
      map(() => {
        if (this.data.formControl.value === null) {
          return null;
        }
        return this.fileInput().nativeElement.files[0].name;
      }),
    );
  }

  reset($event?: MouseEvent) {
    if ($event) {
      $event.stopPropagation();
    }
    this.status = UploadStatus.SELECT_FILE;
    this.fileInput().nativeElement.value = null;
    this.data.formControl.setValue(null);
    this.changeDetector.detectChanges();
    this.data.fileUploaded$.next(null);
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
      this.data.fileUploaded$.next(file.name);
      this.status = UploadStatus.SELECTED;
      this.data.formControl.setValue(file);
      this.data.formControl.updateValueAndValidity();
      this.changeDetector.detectChanges();
    }
  }

  handleChange(event: Event) {
    const file = (event.target as HTMLInputElement).files[0];
    this.selectFile(file);
  }

  validateFile(file: File): boolean {
    this.data.uploadError = null;
    if (!file) {
      this.data.formControl.setValue(null);
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
    return true;
  }
}
