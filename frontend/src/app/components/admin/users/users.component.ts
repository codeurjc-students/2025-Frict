import { Component } from '@angular/core';
import {NavbarComponent} from '../navbar/navbar.component';
import {FooterComponent} from '../../common/footer/footer.component';

@Component({
  selector: 'app-users',
  imports: [
    NavbarComponent,
    FooterComponent
  ],
  templateUrl: './users.component.html',
  styleUrl: './users.component.css'
})
export class UsersComponent {

}
