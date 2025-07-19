import { Component } from '@angular/core';
import {RouterLink} from "@angular/router";
import {FooterComponent} from '../footer/footer.component';
import {NavbarComponent} from '../../admin/navbar/navbar.component';

@Component({
  selector: 'app-profile',
  imports: [
    RouterLink,
    NavbarComponent,
    FooterComponent
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent {

}
