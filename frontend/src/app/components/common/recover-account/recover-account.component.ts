import {Component} from '@angular/core';
import {CommonModule, NgOptimizedImage} from '@angular/common';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {RouterModule} from '@angular/router';
import {AuthService} from '../../../services/auth.service';
import {CustomValidators} from '../../../utils/customValidators.util';
import {UserService} from '../../../services/user.service';

@Component({
  selector: 'app-recover-password',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    NgOptimizedImage
  ],
  templateUrl: './recover-account.component.html',
  styleUrl: './recover-account.component.css'
})
export class RecoverAccountComponent {

  recoverForm: FormGroup;
  loading: boolean = false;
  isEmailSent: boolean = false; // To chenge the view after success
  errorMessage: string = '';

  get usernameControl() { return this.recoverForm.get('username'); }

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private userService: UserService
  ) {
    this.recoverForm = this.fb.group({
      username: ['', [Validators.required], [CustomValidators.createUsernameExistsValidator(this.userService)]]
    });
  }

  onSubmit() {
    if (this.recoverForm.invalid) return;

    this.loading = true;
    this.errorMessage = '';

    const username = this.recoverForm.get('username')?.value;

    // Llamada al servicio (Simulada o real)
    this.authService.initPasswordRecovery(username).subscribe({
      next: () => {
        this.loading = false;
        this.isEmailSent = true;
      },
      error: (err) => {
        this.loading = false;
        console.error(err);
        this.errorMessage = 'No se ha podido procesar la solicitud. Verifica el usuario.';
      }
    });
  }
}
