import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import {AuthService} from '../../../services/auth.service';
import {LoginResponse} from '../../../models/loginResponse.model';

@Component({
  selector: 'app-login',
  imports: [
    CommonModule,
    ReactiveFormsModule
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {

  loginForm: FormGroup;
  showPassword = false;


  constructor(private fb: FormBuilder, private authService: AuthService, private router: Router) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
  }

  onSubmit() {
    console.log('Botón iniciar sesión pulsado')
    if (this.loginForm.valid) {
      const { email, password } = this.loginForm.value;
      this.authService.login(email, password).subscribe({
        next: (response: LoginResponse) => {
          console.log('Frontend: User authenticated:', response);

          // Aquí accedes a user y token desde la respuesta
          localStorage.setItem('USER', JSON.stringify(response.user));
          this.authService.saveToken(response.token);

          this.router.navigate(['/']);
        },
        error: (error: any) => {
          console.error('Frontend: Error en el login', error);
          alert(error.error?.error || 'Frontend: Login error');
        }
      });
    }
  }

  togglePassword() {
    this.showPassword = !this.showPassword;
  }
}
