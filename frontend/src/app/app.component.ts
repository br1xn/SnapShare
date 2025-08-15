import { Component } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class AppComponent {
  selectedFiles: File[] = [];
  uploadMessage: string = '';
  uploadedOtp: string = '';
  downloadOtpInput: string = '';
  downloadMessage: string = '';
  showDownloadSection: boolean = false;
  downloading: boolean = false;
  progress: number = 0;

  private BACKEND_URL = window.location.origin.includes('localhost')
    ? 'http://localhost:8080'
    : 'https://snapshare-backend-eah4.onrender.com';

  constructor(private http: HttpClient) {}

  onFileSelected(event: any) {
    const newFiles = event.target.files;
      if (newFiles) {
        this.selectedFiles = Array.from(newFiles);
    }
  }

  triggerFileSelect() {
    const fileInput = document.getElementById('fileInput') as HTMLInputElement;
    fileInput.click();
  }

  onFileDrop(event: DragEvent) {
    event.preventDefault();
    const files = event.dataTransfer?.files;
    if (files) {
      this.selectedFiles = Array.from(files);
    }
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
  }

  removeFile(fileToRemove: File) {
    this.selectedFiles = this.selectedFiles.filter(file => file !== fileToRemove);
  }

  getFileSize(size: number): string {
    const units = ['Bytes', 'KB', 'MB', 'GB'];
    let i = 0;
    while (size >= 1024 && i < units.length - 1) {
      size /= 1024;
      i++;
    }
    return `${size.toFixed(2)} ${units[i]}`;
  }

  getTotalSize(): string {
    const totalBytes = this.selectedFiles.reduce((sum, file) => sum + file.size, 0);
    return this.getFileSize(totalBytes);
  }

  async uploadFiles() {
    if (this.selectedFiles.length === 0) {
      this.uploadMessage = 'Please select at least one file first.';
      return;
    }

    this.uploadMessage = 'Creating shared bundle...';
    this.uploadedOtp = '';
    this.downloadMessage = '';

    try {
        const bundleRes: any = await this.http.post(`${this.BACKEND_URL}/api/files/create_bundle`, {}).toPromise();
        const otp = bundleRes.otp;

        const CHUNK_SIZE_LIMIT = 100 * 1024 * 1024; // 100MB

        for (const file of this.selectedFiles) {
            if (file.size > CHUNK_SIZE_LIMIT) {
                await this.uploadChunkedFile(file, otp);
            } else {
                await this.uploadSingleFile(file, otp);
            }
        }

        this.uploadMessage = `All uploads complete! OTP is generated.`;
        this.uploadedOtp = otp;
        this.selectedFiles = [];

    } catch (err) {
        const httpError = err as HttpErrorResponse;
        this.uploadMessage = `Upload failed: ${httpError.error?.error || 'Server error'}.`;
        console.error('Upload error:', httpError);
    }
  }

  private uploadSingleFile(file: File, otp: string) {
    const formData = new FormData();
    formData.append('file', file);
    this.uploadMessage = `Uploading "${file.name}"...`;

    return new Promise((resolve, reject) => {
        this.http.post(`${this.BACKEND_URL}/api/files/upload_single_file/${otp}`, formData).toPromise().then(
            () => resolve(true),
            (err) => reject(err)
        );
    });
  }

  private uploadChunkedFile(file: File, otp: string) {
    const CHUNK_SIZE = 5 * 1024 * 1024; // 5MB chunks
    const totalChunks = Math.ceil(file.size / CHUNK_SIZE);
    const uploadId = Date.now().toString() + Math.random().toString(36).substring(2);

    return new Promise(async (resolve, reject) => {
        for (let i = 0; i < totalChunks; i++) {
            const chunk = file.slice(i * CHUNK_SIZE, (i + 1) * CHUNK_SIZE);
            const chunkFormData = new FormData();
            chunkFormData.append('chunk', chunk, file.name);
            chunkFormData.append('fileName', file.name);
            chunkFormData.append('chunkIndex', i.toString());
            chunkFormData.append('totalChunks', totalChunks.toString());
            chunkFormData.append('uploadId', uploadId);

            this.uploadMessage = `Uploading "${file.name}" chunk ${i + 1} of ${totalChunks}...`;

            try {
                await this.http.post(`${this.BACKEND_URL}/api/files/chunked_upload/${otp}`, chunkFormData).toPromise();
            } catch (err) {
                this.uploadMessage = `Chunk upload failed for "${file.name}".`;
                console.error('Chunk upload error:', err);
                reject(err);
                return;
            }
        }

        this.http.post(`${this.BACKEND_URL}/api/files/finalize_upload/${otp}`, { uploadId }).toPromise().then(
            () => resolve(true),
            (err) => reject(err)
        );
    });
  }

  startDownload() {
    this.downloading = true;
    let currentProgress = 0;
    const interval = setInterval(() => {
      currentProgress += 10;
      this.progress = currentProgress;
      if (currentProgress >= 100) {
        clearInterval(interval);
        this.downloadFile();
      }
    }, 100);
  }

  downloadFile() {
    if (!this.downloadOtpInput.trim()) {
      this.downloadMessage = 'Please enter a valid OTP.';
      this.downloading = false;
      return;
    }

    this.downloadMessage = 'Downloading...';
    this.uploadMessage = '';
    this.uploadedOtp = '';

    this.http.get(`${this.BACKEND_URL}/api/files/download/${this.downloadOtpInput}`, { observe: 'response', responseType: 'blob' })
      .subscribe({
        next: (res) => {
          const blob = res.body;
          const contentDisposition = res.headers.get('Content-Disposition');
          let filename = 'downloaded_files.zip';

          if (contentDisposition) {
            const filenameMatch = contentDisposition.match(/filename="(.+)"/);
            if (filenameMatch && filenameMatch.length > 1) {
              filename = filenameMatch[1];
            }
          }

          const a = document.createElement('a');
          const objectUrl = URL.createObjectURL(blob as Blob);
          a.href = objectUrl;
          a.download = filename;
          a.click();
          URL.revokeObjectURL(objectUrl);
          this.downloadMessage = `Download of "${filename}" successful!`;
          this.downloading = false;
        },
        error: (err: HttpErrorResponse) => {
          this.downloadMessage = `Download failed. Invalid OTP or file expired.`;
          console.error('Download error:', err);
          this.downloading = false;
        }
      });
  }
}
