import {Component, ViewChild, OnInit, signal, WritableSignal} from '@angular/core';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { Router, RouterLink, RouterLinkActive, IsActiveMatchOptions } from '@angular/router';
import {NgIf, NgOptimizedImage, NgTemplateOutlet} from '@angular/common';
import { Button } from 'primeng/button';
import { Drawer } from 'primeng/drawer';
import { MenuItem, PrimeTemplate } from 'primeng/api';
import { FormsModule } from '@angular/forms';
import { Menu } from 'primeng/menu';
import { AuthService } from '../../../services/auth.service';
import { LoginInfo } from '../../../models/loginInfo.model';
import { OrderService } from '../../../services/order.service';
import {Category} from '../../../models/category.model';
import {CategoryService} from '../../../services/category.service';
import {Avatar} from 'primeng/avatar';
import {InputGroup} from 'primeng/inputgroup';
import {InputText} from 'primeng/inputtext';

interface CategoryUI {
  icon?: string;
  label?: string;
}

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    FontAwesomeModule,
    RouterLink,
    RouterLinkActive,
    NgIf,
    NgOptimizedImage,
    Button,
    Drawer,
    PrimeTemplate,
    FormsModule,
    Menu,
    Avatar,
    InputGroup,
    InputText,
    NgTemplateOutlet
  ],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css'
})
export class NavbarComponent implements OnInit {

  constructor(
    protected authService: AuthService,
    protected orderService: OrderService,
    private categoryService: CategoryService,
    private router: Router
  ) {}

  @ViewChild('drawerRef') drawerRef!: Drawer;
  @ViewChild('menu') menu!: Menu;

  visible: boolean = false;
  searchBarInput: string = '';
  items: MenuItem[] | undefined;
  loggedUserInfo!: LoginInfo;

  isCategoriesExpanded: WritableSignal<boolean> = signal(true);
  isAccountExpanded: WritableSignal<boolean> = signal(true);
  manualToggleState = signal<Map<number, boolean>>(new Map());

  shouldExpand(categoryId: number, isRouteActive: boolean): boolean {
    const manualState = this.manualToggleState().get(categoryId);

    if (manualState !== undefined) {
      return manualState;
    }

    return isRouteActive;
  }

  toggleSubmenu(categoryId: number, isRouteActive: boolean) {
    const currentState = this.shouldExpand(categoryId, isRouteActive);

    // Actualizamos el mapa invirtiendo el estado actual
    this.manualToggleState.update(map => {
      const newMap = new Map(map);
      newMap.set(categoryId, !currentState);
      return newMap;
    });
  }

  toggleCategories() {
    this.isCategoriesExpanded.update((v) => !v);
  }

  toggleAccount() {
    this.isAccountExpanded.update((v) => !v);
  }

  //If a label is defined for this category name, then use it. Otherwise, use the category name from the backend. Else, use "Category" label instead.
  // Same for icons
  // noinspection JSNonASCIINames
  private readonly categoryConfig: Record<string, CategoryUI> = {
    'Ordenadores': { icon: 'pi pi-desktop' /*, label: 'Gaming' */ }, //Prints the category with both custom icon and label
    'Componentes': { icon: 'pi pi-database' },
    'Periféricos': { icon: 'pi pi-wifi' },
    'Telefonía y Wearables': { icon: 'pi pi-bolt' },
    'Hogar y Conectividad': { icon: 'pi pi-mobile' }, // Prints the category with the custom icon and the category name
  };

  public adminItems = [
    { label: 'Productos', icon: 'pi pi-desktop',   link: 'products' },
    { label: 'Pedidos',   icon: 'pi pi-box',       link: 'orders' },
    { label: 'Informes',  icon: 'pi pi-chart-bar', link: 'reports' },
    { label: 'Reparto',   icon: 'pi pi-truck',     link: 'delivery' },
    { label: 'Usuarios',  icon: 'pi pi-users',     link: 'users' }
  ];

  // Settings for routerLinkActive to distinguish between query params
  public readonly routerOptions: IsActiveMatchOptions = {
    matrixParams: 'ignored',
    queryParams: 'exact',
    paths: 'exact',
    fragment: 'ignored',
  };

  public categories : Category[] = [];

  public getCategoryUI(categoryName: string): CategoryUI | undefined {
    const config = this.categoryConfig[categoryName];

    if (!config) {
      return { icon: 'pi pi-tag', label: categoryName ?? 'Categoría' }
    }
    else {
      return { icon: config.icon ?? 'pi pi-tag', label: config.label ?? categoryName ?? 'Categoría' }
    }
  }

  ngOnInit() {
    this.items = [
      { label: 'Notificación 1', icon: 'pi pi-bell' },
      { label: 'Notificación 2', icon: 'pi pi-bell' }
    ];

    this.categoryService.getAllCategories().subscribe({
      next: (list) => {
        this.categories = list.categories;
        console.log(this.categories);
      }
    })

    this.authService.getLoginInfo().subscribe({
      next: (loginInfo) => {
        this.loggedUserInfo = loginInfo;
      }
    });
  }

  protected logout() {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/login'])
    });
  }
}
