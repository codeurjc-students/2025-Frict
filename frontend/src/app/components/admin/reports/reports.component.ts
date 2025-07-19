import { Component } from '@angular/core';
import {NavbarComponent} from "../navbar/navbar.component";
import {FooterComponent} from '../../common/footer/footer.component';

@Component({
  selector: 'app-reports',
  imports: [
    NavbarComponent,
    FooterComponent
  ],
  templateUrl: './reports.component.html',
  styleUrl: './reports.component.css'
})
export class ReportsComponent {

}
