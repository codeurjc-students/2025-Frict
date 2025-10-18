import {Component} from '@angular/core';
import {AuthService} from '../../../services/auth.service';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {Router, RouterLink} from '@angular/router';
import {FaIconComponent} from '@fortawesome/angular-fontawesome';
import {faCircleInfo} from '@fortawesome/free-solid-svg-icons';
import {NgIf, NgOptimizedImage} from '@angular/common';
import {faGoogle} from '@fortawesome/free-brands-svg-icons';

@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    FaIconComponent,
    NgOptimizedImage,
    NgIf,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {

  protected readonly faGoogle = faGoogle;

  loginForm: FormGroup;
  showPassword : boolean = false;
  showErrorMessage: boolean = false;

  constructor(private authService: AuthService,
              private router: Router,
              private fb: FormBuilder) { //Pending: form fields
    this.loginForm = this.fb.nonNullable.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });
  }

  //As login request could read logged user data before the service refreshes it, that request is made within this method.
  onSubmit() {
    const { username, password } = this.loginForm.getRawValue();
    this.authService.login(username, password).subscribe({ //Tries to log in with provided credentials and retrieves logged user basic login info within the service
      next: () => {
        this.router.navigate(['/']); //It may be the previous route before accessing login page
      },
      error: () => {
        this.showErrorMessage = true;
      }
    })
  }

  public logout() {
    this.authService.logout();
  }

  public togglePassword() {
    this.showPassword = !this.showPassword;
  }

  protected readonly faCircleInfo = faCircleInfo;
}
