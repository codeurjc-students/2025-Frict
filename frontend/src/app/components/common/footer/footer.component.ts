import { Component } from '@angular/core';
import {RouterLink} from '@angular/router';
import {NgIf} from '@angular/common';
import {LoginInfo} from '../../../models/loginInfo.model';

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
  protected loginInfo: LoginInfo = {isLogged: true, id: 0, name: '', username: '', admin: false};

}
