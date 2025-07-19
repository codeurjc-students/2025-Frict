import { Component } from '@angular/core';
import {NavbarComponent} from '../navbar/navbar.component';
import {FooterComponent} from '../../common/footer/footer.component';

@Component({
  selector: 'app-shops',
  imports: [
    NavbarComponent,
    FooterComponent
  ],
  templateUrl: './shops.component.html',
  standalone: true,
  styleUrl: './shops.component.css'
})
export class ShopsComponent {

}
