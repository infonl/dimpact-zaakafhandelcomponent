/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import {
  booleanAttribute,
  Component,
  Input,
  OnInit,
  ViewChild,
  ElementRef,
  ChangeDetectorRef,
} from "@angular/core";
import { AbstractControl, FormGroup, Validators } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import {
  HttpClient,
  HttpEvent,
  HttpEventType,
  HttpHeaders,
} from "@angular/common/http";
import { Subscription } from "rxjs";
import { FormHelper } from "../helpers";
import { FoutAfhandelingService } from "../../../fout-afhandeling/fout-afhandeling.service";

enum UploadStatus {
  SELECTEER_BESTAND = "SELECTEER_BESTAND",
  BEZIG = "BEZIG",
  GEREED = "GEREED",
}

@Component({
  selector: "zac-file",
  templateUrl: "./file.html",
  styleUrls: ["./file.less"],
})
export class ZacFile<
  Form extends Record<string, AbstractControl>,
  Key extends keyof Form,
> implements OnInit
{
  @Input({ required: true }) key!: Key & string;
  @Input({ required: true }) form!: FormGroup<Form>;
  @Input({ transform: booleanAttribute }) readonly = false;
  @Input() label?: string;
  @Input() uploadURL?: string;
  @Input() allowedFileTypes?: string[];
  @Input() maxFileSizeMB?: number;
  @ViewChild("fileInput") fileInput!: ElementRef;

  protected control?: AbstractControl;
  protected progress = 0;
  protected subscription?: Subscription;
  protected status: string = UploadStatus.SELECTEER_BESTAND;
  protected uploadError?: string;

  constructor(
    private readonly translateService: TranslateService,
    private readonly http: HttpClient,
    private readonly foutAfhandelingService: FoutAfhandelingService,
    private readonly changeDetector: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.control = this.form.get(String(this.key))!;
  }

  protected get required() {
    return this.control?.hasValidator(Validators.required) ?? false;
  }

  protected getErrorMessage = () => {
    if (this.uploadError) return this.uploadError;
    return FormHelper.getErrorMessage(this.control, this.translateService);
  };

  protected uploadFile(file: File) {
    this.uploadError = undefined;
    if (!file) {
      this.updateInput("");
      return;
    }

    if (!this.isBestandstypeToegestaan(file)) {
      this.uploadError = `Het bestandstype is niet toegestaan (${this.getBestandsextensie(file)})`;
      this.control?.setErrors({ file: true });
      return;
    }

    if (!this.isBestandsgrootteToegestaan(file)) {
      this.uploadError = `Het bestand is te groot (${this.getBestandsgrootteMB(file)}MB)`;
      this.control?.setErrors({ file: true });
      return;
    }

    if (!file.size) {
      this.uploadError = "Het bestand is leeg";
      this.control?.setErrors({ file: true });
      return;
    }

    if (!this.uploadURL) {
      this.uploadError = "Upload URL is niet geconfigureerd";
      this.control?.setErrors({ file: true });
      return;
    }

    this.subscription = this.createRequest(file).subscribe({
      next: (event: HttpEvent<unknown>) => {
        switch (event.type) {
          case HttpEventType.Sent:
            this.status = UploadStatus.BEZIG;
            break;
          case HttpEventType.ResponseHeader:
            break;
          case HttpEventType.UploadProgress:
            this.progress = Math.round(
              (event.loaded / (event.total ?? event.loaded)) * 100
            );
            this.updateInput(`${file.name} | ${this.progress}%`);
            break;
          case HttpEventType.Response:
            this.fileInput.nativeElement.value = null;
            this.updateInput(file.name);
            this.status = UploadStatus.GEREED;
            setTimeout(() => {
              this.progress = 0;
            }, 1500);
        }
      },
      error: (error) => {
        this.status = UploadStatus.SELECTEER_BESTAND;
        this.fileInput.nativeElement.value = null;
        this.uploadError = this.foutAfhandelingService.getFout(error);
      },
    });
  }

  protected reset($event?: MouseEvent) {
    $event?.stopPropagation();
    if (this.subscription && !this.subscription.closed) {
      this.subscription.unsubscribe();
    }
    this.status = UploadStatus.SELECTEER_BESTAND;
    this.fileInput.nativeElement.value = null;
    this.updateInput("");
  }

  protected droppedFile(files: FileList): void {
    if (!files.length) return;
    this.uploadFile(files[0]);
  }

  private createRequest(file: File) {
    this.updateInput(file.name);
    const formData = new FormData();
    formData.append("filename", file.name);
    formData.append("filesize", file.size.toString());
    formData.append("type", file.type);
    formData.append("file", file, file.name);
    const httpHeaders = new HttpHeaders();
    httpHeaders.append("Content-Type", "multipart/form-data");
    httpHeaders.append("Accept", "application/json");
    return this.http.post(this.uploadURL!, formData, {
      reportProgress: true,
      observe: "events",
      headers: httpHeaders,
    });
  }

  private updateInput(inputValue: string) {
    this.control?.setValue(inputValue as unknown as File);
    this.changeDetector.detectChanges();
  }

  private isBestandstypeToegestaan(file: File): boolean {
    if (!this.allowedFileTypes?.length) return true;
    const extension = this.getBestandsextensie(file);
    return this.allowedFileTypes.includes(extension);
  }

  private isBestandsgrootteToegestaan(file: File): boolean {
    if (!this.maxFileSizeMB) return true;
    const fileSizeMB = this.getBestandsgrootteMB(file);
    return fileSizeMB <= this.maxFileSizeMB;
  }

  private getBestandsextensie(file: File): string {
    return file.name.split(".").pop()?.toLowerCase() || "";
  }

  private getBestandsgrootteMB(file: File): number {
    return Math.round((file.size / 1024 / 1024) * 100) / 100;
  }
}
