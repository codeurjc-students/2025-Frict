import {Component, OnInit} from '@angular/core';
import {CommonModule, NgOptimizedImage} from '@angular/common';
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {ActivatedRoute, Router, RouterModule} from '@angular/router';

// PrimeNG
import {InputOtpModule} from 'primeng/inputotp';

import {AuthService} from '../../../services/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    RouterModule,
    NgOptimizedImage,
    InputOtpModule
  ],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.css'
})
export class ResetPasswordComponent implements OnInit {

  currentStep: number = 1;

  username: string = '';
  otpCode: string = ''; // Linked to p-inputOtp

  passwordForm: FormGroup;

  loading: boolean = false;
  errorMessage: string = '';
  showPassword: boolean = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private authService: AuthService
  ) {
    this.passwordForm = this.fb.group({
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordsMatchValidator });
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.username = params['username'] || '';
    });
  }

  verifyOtp() {
    if (this.otpCode.length !== 6) return;

    this.loading = true;
    this.errorMessage = '';

    this.authService.verifyOtp(this.username, this.otpCode).subscribe({
      next: (isValid) => {
        this.loading = false;
        if (isValid) {
          this.currentStep = 2; //Go to password section
        } else {
          this.errorMessage = 'El código es incorrecto o ha expirado.';
        }
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Error al verificar el código.';
      }
    });
  }

  onSubmitPassword() {
    if (this.passwordForm.invalid) return;

    this.loading = true;
    const newPassword = this.passwordForm.get('password')?.value;

    this.authService.resetPassword(this.username, this.otpCode, newPassword).subscribe({
      next: () => {
        this.router.navigate(['/login']);
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'No se pudo cambiar la contraseña. Inténtalo de nuevo.';
      }
    });
  }

  // Utils
  togglePassword() {
    this.showPassword = !this.showPassword;
  }

  passwordsMatchValidator(group: FormGroup) {
    const pass = group.get('password')?.value;
    const confirm = group.get('confirmPassword')?.value;
    return pass === confirm ? null : { notMatching: true };
  }
}
