import {Component} from '@angular/core';
import {RouterOutlet} from '@angular/router';
import {NavbarComponent} from '../../components/common/navbar/navbar.component';
import {FooterComponent} from '../../components/common/footer/footer.component';

@Component({
  selector: 'app-user-layout',
  imports: [
    RouterOutlet,
    NavbarComponent,
    FooterComponent
  ],
  templateUrl: './user-layout.component.html',
  styleUrl: './user-layout.component.css'
})
export class UserLayoutComponent {

}
