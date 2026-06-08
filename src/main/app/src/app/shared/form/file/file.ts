/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { AsyncPipe, NgIf } from "@angular/common";
import {
  ChangeDetectorRef,
  Component,
  computed,
  effect,
  ElementRef,
  input,
  numberAttribute,
  OnDestroy,
  OnInit,
  signal,
  viewChild,
} from "@angular/core";
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
} from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { TranslatePipe } from "@ngx-translate/core";
import { lastValueFrom, takeUntil } from "rxjs";
import { ConfiguratieService } from "../../../configuratie/configuratie.service";
import { FileDragAndDropDirective } from "../../directives/file-drag-and-drop.directive";
import { CapitalizeFirstLetterPipe } from "../../pipes/capitalizeFirstLetter.pipe";
import { SingleInputFormField } from "../BaseFormField";

@Component({
  selector: "zac-file",
  templateUrl: "./file.html",
  standalone: true,
  imports: [
    AsyncPipe,
    CapitalizeFirstLetterPipe,
    FileDragAndDropDirective,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    NgIf,
    ReactiveFormsModule,
    TranslatePipe,
  ],
})
export class ZacFile<
    Form extends Record<string, AbstractControl>,
    Key extends keyof Form,
  >
  extends SingleInputFormField<Form, Key, File>
  implements OnInit, OnDestroy
{
  protected allowedFileTypes = input<string[]>([]);
  protected maxFileSizeMB = input(0, { transform: numberAttribute });
  protected fileInput = viewChild<ElementRef>("fileInput");

  protected fileSize = computed(() => {
    const maxFileSizeMB = this.maxFileSizeMB();
    if (maxFileSizeMB) return Promise.resolve(maxFileSizeMB);

    return lastValueFrom(this.configuratieService.readMaxFileSizeMB());
  });

  protected displayControl = this.formBuilder.control<string | null>(null);

  protected allowedFormats = signal<string[]>([]);

  constructor(
    private readonly changeDetector: ChangeDetectorRef,
    private readonly formBuilder: FormBuilder,
    private readonly configuratieService: ConfiguratieService,
  ) {
    super();

    effect(async () => {
      if (this.allowedFileTypes().length) {
        this.allowedFormats.set(this.allowedFileTypes());
        return;
      }
      const allowedFileTypes = await lastValueFrom(
        this.configuratieService.readAllowedFileTypes(),
      );
      this.allowedFormats.set(
        allowedFileTypes.map((allowedFileType) => allowedFileType.extension),
      );
    });
  }

  ngOnInit() {
    // Subscribe to form control status changes to sync errors
    this.control()
      ?.statusChanges.pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.displayControl.setErrors(this.control()?.errors ?? null);
      });

    this.control()
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe((file) => {
        if (file) {
          return;
        }
        this.displayControl.reset();
      });
  }

  ngOnDestroy() {
    this.reset();
    super.ngOnDestroy();
  }

  protected reset() {
    const nativeElement = this.fileInput()?.nativeElement;
    nativeElement.value = null;
    this.updateInputControls(null);
  }

  protected async droppedFile(files: FileList) {
    if (!files.length) return;
    await this.setFile(files[0]);
  }

  protected async selectedFile(event: Event) {
    if (!event.target) return;
    const fileInput = event.target as HTMLInputElement;
    if (!fileInput.files?.length) return;
    await this.setFile(fileInput.files[0]);
  }

  private async setFile(file: File) {
    this.control()?.setErrors(null);
    this.updateInputControls(file);

    if (!this.isFileTypeAllowed(file)) {
      this.control()?.setErrors({
        fileTypeInvalid: { type: this.getFileExtension(file) },
      });
      return;
    }

    if (!file.size) {
      this.control()?.setErrors({
        fileEmpty: true,
      });
      return;
    }

    if (!(await this.isFileSizeAllowed(file))) {
      this.control()?.setErrors({
        fileTooLarge: { size: this.getFileSizeInMB(file) },
      });
      return;
    }
  }

  private updateInputControls(file: File | null) {
    this.control()?.patchValue(file);
    if (file) {
      this.control()?.markAsDirty();
    }
    this.displayControl.patchValue(
      file ? file.name.replace(`.${this.getFileExtension(file)}`, "") : null,
      { emitEvent: false },
    );
    this.changeDetector.detectChanges();
  }

  private isFileTypeAllowed(file: File) {
    if (!this.allowedFormats().length) return false;
    const extension = this.getFileExtension(file);
    return this.allowedFormats().includes(`.${extension}`);
  }

  private async isFileSizeAllowed(file: File) {
    const fileSizeMB = this.getFileSizeInMB(file);
    const maxFileSizeMB = await this.fileSize();
    return fileSizeMB <= maxFileSizeMB;
  }

  private getFileExtension(file: File) {
    return file.name.split(".").pop()?.toLowerCase() || "";
  }

  private getFileSizeInMB(file: File) {
    return Math.round((file.size / 1024 / 1024) * 100) / 100;
  }
}
