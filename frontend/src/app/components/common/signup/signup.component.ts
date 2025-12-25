import {Component} from '@angular/core';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {AuthService} from '../../../services/auth.service';
import {Router, RouterLink} from '@angular/router';
import {UserService} from '../../../services/user.service';
import {NgOptimizedImage} from '@angular/common';

@Component({
  selector: 'app-signup',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    NgOptimizedImage
  ],
  templateUrl: './signup.component.html',
  standalone: true,
  styleUrl: './signup.component.css'
})
export class SignupComponent {

  registerForm: FormGroup;
  showPassword = false;
  selectedImage: File | null = null;

  constructor(private fb: FormBuilder, private authService: AuthService, private userService: UserService, private router: Router) {
    this.registerForm = this.fb.nonNullable.group({
      name: ['', Validators.required],
      username: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required]],
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
        this.router.navigate(['/error']);
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
