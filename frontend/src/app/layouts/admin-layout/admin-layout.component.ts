import { Component } from '@angular/core';
import {FooterComponent} from '../../components/common/footer/footer.component';
import {NavbarComponent} from '../../components/common/navbar/navbar.component';
import {RouterOutlet} from '@angular/router';

@Component({
  selector: 'app-admin-layout',
  imports: [
    FooterComponent,
    NavbarComponent,
    RouterOutlet
  ],
  templateUrl: './admin-layout.component.html',
  styleUrl: './admin-layout.component.css'
})
export class AdminLayoutComponent {

}
