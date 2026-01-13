import {Component} from '@angular/core';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {AuthService} from '../../../services/auth.service';
import {Router, RouterLink} from '@angular/router';
import {UserService} from '../../../services/user.service';
import {NgIf, NgOptimizedImage} from '@angular/common';
import {GoogleAuthComponent} from '../google-auth/google-auth.component';
import {CustomValidators} from '../../../utils/customValidators.util';

@Component({
  selector: 'app-signup',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    NgOptimizedImage,
    GoogleAuthComponent,
    NgIf
  ],
  templateUrl: './signup.component.html',
  standalone: true,
  styleUrl: './signup.component.css'
})
export class SignupComponent {

  registerForm: FormGroup;
  showPassword = false;
  selectedImage: File | null = null;

  get usernameControl() { return this.registerForm.get('username'); }
  get emailControl() { return this.registerForm.get('email'); }

  constructor(private fb: FormBuilder,
              private authService: AuthService,
              private userService: UserService,
              private router: Router) {

    this.registerForm = this.fb.nonNullable.group({
      name: ['', Validators.required],
      username: ['', Validators.required, [CustomValidators.createUsernameValidator(this.userService)]],
      email: ['', [Validators.required, Validators.email], [CustomValidators.createEmailValidator(this.userService)]],
      password: ['', Validators.required]
    });
  }

  togglePassword() {
    this.showPassword = !this.showPassword;
  }

  onSubmit() {
    this.authService.signup(this.registerForm.value).subscribe({
      next: (loginInfo) => { //Backend returns some fields, one of them being the id of the user created
        if (this.selectedImage) { this.uploadUserImage(loginInfo.id, this.selectedImage);}
        else{ this.router.navigate(["/login"]); }
      },
      error: () => {
        this.router.navigate(['/error'])
      }
    })
  }

  private uploadUserImage(userId: string, selectedImage: File) {
    this.userService.uploadUserImage(userId, selectedImage).subscribe({
      next: () => {
        this.router.navigate([`/login`]);
      },
      error: () => {
        alert('Error subiendo la imagen.');
      }
    })
  }

  changeSelectedImage(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input && input.files && input.files.length) {
      this.selectedImage = input.files[0];
    }
  }
}
