/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import {
  booleanAttribute,
  ChangeDetectorRef,
  Component,
  ElementRef,
  Input,
  numberAttribute,
  OnDestroy,
  OnInit,
  ViewChild,
} from "@angular/core";
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  Validators,
} from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { lastValueFrom, Subject, takeUntil } from "rxjs";
import { ConfiguratieService } from "../../../configuratie/configuratie.service";
import { FileIcon } from "../../../informatie-objecten/model/file-icon";
import { FormHelper } from "../helpers";

@Component({
  selector: "zac-file",
  templateUrl: "./file.html",
})
export class ZacFile<
    Form extends Record<string, AbstractControl>,
    Key extends keyof Form,
  >
  implements OnInit, OnDestroy
{
  @Input({ required: true }) key!: Key & string;
  @Input({ required: true }) form!: FormGroup<Form>;
  @Input({ transform: booleanAttribute }) readonly = false;
  @Input() label?: string;
  @Input() allowedFileTypes: string[] = [];
  @Input({ transform: numberAttribute }) maxFileSizeMB: number = 0;
  @ViewChild("fileInput") fileInput!: ElementRef;

  protected control?: AbstractControl<File | null>;
  protected displayControl = this.formBuilder.control<string | null>(null);
  private destroy$ = new Subject<void>();

  public allowedFormats = "";

  constructor(
    private readonly translateService: TranslateService,
    private readonly changeDetector: ChangeDetectorRef,
    private readonly formBuilder: FormBuilder,
    private readonly configuratieService: ConfiguratieService,
  ) {}

  async ngOnInit() {
    this.control = this.form.get(String(this.key))!;

    // Subscribe to form control status changes to sync errors
    this.control?.statusChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.displayControl.setErrors(this.control?.errors ?? null);
    });

    if (!this.maxFileSizeMB) {
      this.maxFileSizeMB = await lastValueFrom(
        this.configuratieService.readMaxFileSizeMB(),
      );
    }

    const additionalFileTypes = await lastValueFrom(
      this.configuratieService.readAdditionalAllowedFileTypes(),
    );

    if (!this.allowedFileTypes.length) {
      const defaultFileTypes = FileIcon.fileIcons.map((icon) =>
        icon.getBestandsextensie(),
      );
      this.allowedFileTypes = defaultFileTypes.concat(additionalFileTypes);
    } else {
      this.allowedFileTypes = this.allowedFileTypes.concat(additionalFileTypes);
    }

    this.allowedFormats = this.allowedFileTypes.join(", ");
  }

  ngOnDestroy() {
    this.reset();
    this.destroy$.next();
    this.destroy$.complete();
  }

  protected get required() {
    return this.control?.hasValidator(Validators.required) ?? false;
  }

  protected getErrorMessage = () => {
    return FormHelper.getErrorMessage(this.control, this.translateService);
  };

  protected reset() {
    this.fileInput.nativeElement.value = null;
    this.updateInputControls(null);
  }

  protected droppedFile(files: FileList) {
    if (!files.length) return;
    this.setFile(files[0]);
  }

  protected selectedFile(event: Event) {
    if (!event.target) return;
    const fileInput = event.target as HTMLInputElement;
    if (!fileInput.files?.length) return;
    this.setFile(fileInput.files[0]);
  }

  private setFile(file: File) {
    this.control?.setErrors(null);
    this.updateInputControls(file);

    if (!this.isFileTypeAllowed(file)) {
      this.control?.setErrors({
        fileTypeInvalid: { type: this.getFileExtension(file) },
      });
      return;
    }

    if (!file.size) {
      this.control?.setErrors({
        fileEmpty: true,
      });
      return;
    }

    if (!this.isFileSizeAllowed(file)) {
      this.control?.setErrors({
        fileTooLarge: { size: this.getFileSizeInMB(file) },
      });
      return;
    }
  }

  private updateInputControls(file: File | null) {
    this.control?.patchValue(file, { emitEvent: false });
    this.displayControl.patchValue(
      file ? file.name.replace(`.${this.getFileExtension(file)}`, "") : null,
      { emitEvent: false },
    );
    this.changeDetector.detectChanges();
  }

  private isFileTypeAllowed(file: File) {
    if (!this.allowedFileTypes.length) return false;
    const extension = this.getFileExtension(file);
    return this.allowedFileTypes.includes(`.${extension}`);
  }

  private isFileSizeAllowed(file: File) {
    if (!this.maxFileSizeMB) return true;
    const fileSizeMB = this.getFileSizeInMB(file);
    return fileSizeMB <= this.maxFileSizeMB;
  }

  private getFileExtension(file: File) {
    return file.name.split(".").pop()?.toLowerCase() || "";
  }

  private getFileSizeInMB(file: File) {
    return Math.round((file.size / 1024 / 1024) * 100) / 100;
  }
}
