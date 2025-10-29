import { Component } from '@angular/core';
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
  styleUrl: './signup.component.css'
})
export class SignupComponent {

  registerForm: FormGroup;
  showPassword = false;
  selectedPhoto: File | null = null;

  constructor(private fb: FormBuilder, private authService: AuthService, private userService: UserService, private router: Router) {
    this.registerForm = this.fb.nonNullable.group({
      name: ['', Validators.required],
      username: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
      address: ['', Validators.required]
    });
  }

  togglePassword() {
    this.showPassword = !this.showPassword;
  }

  onSubmit() {
    this.authService.register(this.registerForm.value).subscribe({
      next: (response) => { //Backend returns some fields, one of them being the id of the user created
        if (this.selectedPhoto) { this.uploadUserImage(response.id, this.selectedPhoto);}
        else{ this.router.navigate(["/login"]); }
      },
      error: () => {
        this.router.navigate(['/error'])
      }
    })
  }

  private uploadUserImage(id: string, selectedPhoto: File) {
    this.userService.uploadUserImage(id, selectedPhoto).subscribe({
      next: () => {
        this.router.navigate([`/login`]);
      },
      error: () => {
        this.router.navigate(['/error']);
        alert('Error subiendo la imagen.');
      }
    })
  }

  changeSelectedPhoto(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input && input.files && input.files.length) {
      this.selectedPhoto = input.files[0];
    }
  }
}
