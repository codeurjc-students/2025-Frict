import { Component } from '@angular/core';
import {NavbarComponent} from "../navbar/navbar.component";
import {FooterComponent} from '../../common/footer/footer.component';

@Component({
  selector: 'app-home',
  imports: [
    NavbarComponent,
    FooterComponent

  ],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent {

}
