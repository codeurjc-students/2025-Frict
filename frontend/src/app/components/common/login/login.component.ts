import {Component, OnInit, ViewChild, ElementRef, NgZone} from '@angular/core';
import { AuthService } from '../../../services/auth.service';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { NgIf, NgOptimizedImage } from '@angular/common';
import {environment} from '../../../../environments/environment';

declare const google: any; // Google variable (from index.html)

@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    NgOptimizedImage,
    NgIf,
  ],
  templateUrl: './login.component.html',
  standalone: true,
  styleUrl: './login.component.css'
})
export class LoginComponent implements OnInit {

  loginForm: FormGroup;
  showPassword: boolean = false;
  showErrorMessage: boolean = false;

  // HTML div for Google button
  @ViewChild('googleBtn', { static: true }) googleBtn!: ElementRef;

  constructor(
    private authService: AuthService,
    private router: Router,
    private fb: FormBuilder,
    private ngZone: NgZone    // Navigate after Google callback
  ) {
    this.loginForm = this.fb.nonNullable.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.initGoogleButton();
  }

  private initGoogleButton() {
    console.log('Client ID:', environment.googleClientId);
    // Check if Google script has already loaded
    if (typeof google === 'undefined' || !this.googleBtn) {
      console.warn('Google script not loaded');
      return;
    }

    const clientId = environment.googleClientId;
    if (!clientId) {
      console.error('CRITICAL ERROR: Google Client ID is undefined in environment!');
      return;
    }

    google.accounts.id.initialize({
      client_id: environment.googleClientId,
      use_fedcm_for_prompt: false,
      callback: (resp: any) => this.handleGoogleLogin(resp)
    });

    google.accounts.id.renderButton(this.googleBtn.nativeElement, {
      theme: 'outline',
      size: 'large',
      type: 'standard',
      width: '365'
    });
  }

  private handleGoogleLogin(response: any) {
    const googleToken = response.credential;
    this.authService.googleLogin(googleToken).subscribe({
        next: () => {
          // ngZone for keeping navigation running properly when redirecting outside Angular when logging in
          this.ngZone.run(() => {
            this.router.navigate(['/']);
          });
        },
        error: (err) => {
          console.error('Error Google Login', err);
          this.showErrorMessage = true;
        }
      });
  }

  onSubmit() {
    if (this.loginForm.invalid) return;

    const { username, password } = this.loginForm.getRawValue();
    this.authService.login(username, password).subscribe({
      next: () => {
        this.router.navigate(['/']);
      },
      error: () => {
        this.showErrorMessage = true;
      }
    });
  }

  public logout() {
    this.authService.logout();
  }

  public togglePassword() {
    this.showPassword = !this.showPassword;
  }
}
