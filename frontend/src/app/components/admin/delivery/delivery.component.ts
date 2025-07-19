import { Component } from '@angular/core';
import {NavbarComponent} from "../navbar/navbar.component";
import {FooterComponent} from '../../common/footer/footer.component';

@Component({
  selector: 'app-delivery',
  imports: [
    NavbarComponent,
    FooterComponent
  ],
  templateUrl: './delivery.component.html',
  styleUrl: './delivery.component.css'
})
export class DeliveryComponent {

}
