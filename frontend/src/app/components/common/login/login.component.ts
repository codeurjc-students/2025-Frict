import {Component, OnInit, ViewChild, ElementRef, NgZone} from '@angular/core';
import { AuthService } from '../../../services/auth.service';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { NgIf, NgOptimizedImage } from '@angular/common';
import {environment} from '../../../../environments/environment';
import {GoogleAuthComponent} from '../google-auth/google-auth.component';
import {CustomValidators} from '../../../utils/customValidators.util';
import {UserService} from '../../../services/user.service';

@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    NgOptimizedImage,
    NgIf,
    GoogleAuthComponent,
  ],
  templateUrl: './login.component.html',
  standalone: true,
  styleUrl: './login.component.css'
})
export class LoginComponent {

  loginForm: FormGroup;
  showPassword: boolean = false;
  showErrorMessage: boolean = false;

  get usernameControl() { return this.loginForm.get('username'); }

  constructor(
    private authService: AuthService,
    private userService: UserService,
    private router: Router,
    private fb: FormBuilder
  ) {
    this.loginForm = this.fb.nonNullable.group({
      username: ['', Validators.required, [CustomValidators.createUsernameExistsValidator(this.userService)]],
      password: ['', Validators.required]
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

  public togglePassword() {
    this.showPassword = !this.showPassword;
  }
}
