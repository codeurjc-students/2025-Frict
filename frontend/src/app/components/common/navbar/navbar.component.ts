import {Component, Input} from '@angular/core';
import {FontAwesomeModule} from '@fortawesome/angular-fontawesome';
import {faBars, faCartShopping, faHouse, faUser, faXmark} from '@fortawesome/free-solid-svg-icons';
import {RouterLink} from '@angular/router';
import {NgIf, NgOptimizedImage} from '@angular/common';
import {AuthService} from '../../../services/auth.service';
import {LoginInfo} from '../../../models/loginInfo.model';



@Component({
  selector: 'app-navbar',
  imports: [
    FontAwesomeModule,
    RouterLink,
    NgIf,
    NgOptimizedImage
  ],
  templateUrl: './navbar.component.html',
  standalone: true,
  styleUrl: './navbar.component.css'
})
export class NavbarComponent {

  protected readonly faBars = faBars;
  protected readonly faCartShopping = faCartShopping;
  protected readonly faHouse = faHouse;
  protected readonly faXmark = faXmark;
  protected readonly faUser = faUser;

  @Input() loginInfo!: LoginInfo;
}
