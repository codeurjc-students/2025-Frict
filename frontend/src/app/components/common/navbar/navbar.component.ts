import {Component, ViewChild} from '@angular/core';
import {FontAwesomeModule} from '@fortawesome/angular-fontawesome';
import {Router, RouterLink, RouterLinkActive} from '@angular/router';
import {NgIf, NgOptimizedImage} from '@angular/common';
import {Avatar} from 'primeng/avatar';
import {Button} from 'primeng/button';
import {Drawer} from 'primeng/drawer';
import {StyleClass} from 'primeng/styleclass';
import {MenuItem, PrimeTemplate} from 'primeng/api';
import {FormsModule} from '@angular/forms';
import {Menu} from 'primeng/menu';
import {AuthService} from '../../../services/auth.service';
import {LoginInfo} from '../../../models/loginInfo.model';
import {OrderService} from '../../../services/order.service';



@Component({
  selector: 'app-navbar',
  imports: [
    FontAwesomeModule,
    RouterLink,
    NgIf,
    NgOptimizedImage,
    Avatar,
    Button,
    Drawer,
    StyleClass,
    PrimeTemplate,
    FormsModule,
    Menu,
    RouterLinkActive
  ],
  templateUrl: './navbar.component.html',
  standalone: true,
  styleUrl: './navbar.component.css'
})
export class NavbarComponent {

  constructor(protected authService: AuthService,
              protected orderService: OrderService,
              protected router: Router) {}

  @ViewChild('drawerRef') drawerRef!: Drawer;
  @ViewChild('menu') menu!: Menu;

  closeCallback(e: any): void {
    this.drawerRef.close(e);
  }

  visible: boolean = false;
  searchBarInput: string = '';
  items: MenuItem[] | undefined;
  loggedUserInfo!: LoginInfo;

  ngOnInit() {
    //window.addEventListener('scroll', () => this.menu.hide());
    this.items = [
      {
        label: 'Notificación 1',
        icon: 'pi pi-bell'
      },
      {
        label: 'Notificación 2',
        icon: 'pi pi-bell'
      }
    ];
    this.authService.getLoginInfo().subscribe({
      next: (loginInfo) => {
        this.loggedUserInfo = loginInfo;
      }
    })
  }

  protected logout() {
    this.authService.logout().subscribe({
      next: () => {
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.router.navigate(['/error']);
      }
    });
  }
}
