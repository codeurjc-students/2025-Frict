import { Component } from '@angular/core';
import {RouterLink} from '@angular/router';
import {NgIf} from '@angular/common';
import {LoginInfo} from '../../../models/loginInfo.model';
import {AuthService} from '../../../services/auth.service';

@Component({
  selector: 'app-footer',
  imports: [
    RouterLink,
    NgIf
  ],
  templateUrl: './footer.component.html',
  styleUrl: './footer.component.css'
})
export class FooterComponent {
  constructor(protected authService: AuthService) {}
}
