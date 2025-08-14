import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component'; // Corrected: import AppComponent

bootstrapApplication(AppComponent, appConfig) // Corrected: bootstrap AppComponent
  .catch((err) => console.error(err));
