import {Component, Input, ViewChild} from '@angular/core';
import {FontAwesomeModule} from '@fortawesome/angular-fontawesome';
import {RouterLink} from '@angular/router';
import {NgIf, NgOptimizedImage} from '@angular/common';
import {LoginInfo} from '../../../models/loginInfo.model';
import {Avatar} from 'primeng/avatar';
import {Button} from 'primeng/button';
import {Drawer} from 'primeng/drawer';
import {StyleClass} from 'primeng/styleclass';
import {MenuItem, PrimeTemplate} from 'primeng/api';
import {FormsModule} from '@angular/forms';
import {Menu} from 'primeng/menu';
import {AuthService} from '../../../services/auth.service';



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
    Menu
  ],
  templateUrl: './navbar.component.html',
  standalone: true,
  styleUrl: './navbar.component.css'
})
export class NavbarComponent {

  constructor(protected authService: AuthService) {}

  @ViewChild('drawerRef') drawerRef!: Drawer;
  @ViewChild('menu') menu!: Menu;

  closeCallback(e: any): void {
    this.drawerRef.close(e);
  }

  visible: boolean = false;
  searchBarInput: string = '';
  items: MenuItem[] | undefined;

  ngOnInit() {
    window.addEventListener('scroll', () => this.menu.hide());
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
  }
}
