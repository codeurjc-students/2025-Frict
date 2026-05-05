import {Component, computed, OnInit, signal, ViewChild, WritableSignal} from '@angular/core';
import {IsActiveMatchOptions, Router, RouterLink, RouterLinkActive} from '@angular/router';
import {NgClass, NgIf, NgOptimizedImage, NgTemplateOutlet} from '@angular/common';
import {Button} from 'primeng/button';
import {Drawer} from 'primeng/drawer';
import {MenuItem, PrimeTemplate} from 'primeng/api';
import {FormsModule} from '@angular/forms';
import {AuthService} from '../../../services/auth.service';
import {LoginInfo} from '../../../models/loginInfo.model';
import {OrderService} from '../../../services/order.service';
import {Category} from '../../../models/category.model';
import {CategoryService} from '../../../services/category.service';
import {Avatar} from 'primeng/avatar';
import {InputGroup} from 'primeng/inputgroup';
import {InputText} from 'primeng/inputtext';
import {ProductService, SearchScope} from '../../../services/product.service';
import {Select} from 'primeng/select';
import { Popover } from 'primeng/popover';
import {NotificationService} from '../../../services/notification.service';
import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    RouterLink,
    RouterLinkActive,
    NgIf,
    NgOptimizedImage,
    Button,
    Drawer,
    PrimeTemplate,
    FormsModule,
    Avatar,
    InputGroup,
    InputText,
    NgTemplateOutlet,
    NgClass,
    Select,
    Popover,
    DatePipe
  ],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css'
})
export class NavbarComponent implements OnInit {

  constructor(
    protected authService: AuthService,
    protected orderService: OrderService,
    private categoryService: CategoryService,
    protected productService: ProductService,
    protected notificationService: NotificationService,
    private router: Router,
    private http: HttpClient
  ) {}

  @ViewChild('drawerRef') drawerRef!: Drawer;
  @ViewChild('notifPopover') notifPopover!: Popover;

  visible: boolean = false;
  searchBarInput: string = '';
  loggedUserInfo!: LoginInfo;

  //Shop mode (global to show all products, local to show only selected shop products)
  private baseScopeOptions = [
    { label: 'Global', value: 'GLOBAL' },
    { label: 'Tienda seleccionada', value: 'LOCAL' }
  ];

  public scopeOptions = computed(() => {
    const hasShop = this.authService.hasShopSelected();
    return this.baseScopeOptions.map(opt => ({
      ...opt,
      disabled: opt.value === 'LOCAL' && !hasShop
    }));
  });

  isCategoriesExpanded: WritableSignal<boolean> = signal(true);
  isAccountExpanded: WritableSignal<boolean> = signal(true);
  manualToggleState = signal<Map<number, boolean>>(new Map());

  public onScopeChange(newValue: SearchScope | null) {
    if (newValue) {
      this.productService.setSearchScope(newValue);
    }
  }

  shouldExpand(categoryId: number, isRouteActive: boolean): boolean {
    const manualState = this.manualToggleState().get(categoryId);

    if (manualState !== undefined) {
      return manualState;
    }

    return isRouteActive;
  }

  toggleSubmenu(categoryId: number, isRouteActive: boolean) {
    const currentState = this.shouldExpand(categoryId, isRouteActive);

    // Invert current state
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


  public adminItems = [
    { label: 'Productos', icon: 'pi pi-desktop',   link: '/admin/products', roles: ['ADMIN'] },
    { label: 'Categorías', icon: 'pi pi-tag',   link: '/admin/categories', roles: ['ADMIN'] },
    { label: 'Informes',  icon: 'pi pi-chart-bar', link: 'reports', roles: ['ADMIN'] },
    { label: 'Tiendas',   icon: 'pi pi-shop',      link: '/admin/shops', roles: ['ADMIN', 'MANAGER'] },
    { label: 'Pedidos',   icon: 'pi pi-box',       link: '/admin/orders', roles: ['ADMIN', 'MANAGER'] },
    { label: 'Reparto',   icon: 'pi pi-truck',     link: '/admin/delivery', roles: ['DRIVER'] },
    { label: 'Camiones',   icon: 'pi pi-truck',     link: '/admin/trucks', roles: ['ADMIN'] },
    { label: 'Usuarios',  icon: 'pi pi-users',     link: '/admin/users', roles: ['ADMIN'] }
  ];

  // Settings for routerLinkActive to distinguish between query params
  public readonly routerOptions: IsActiveMatchOptions = {
    matrixParams: 'ignored',
    queryParams: 'exact',
    paths: 'exact',
    fragment: 'ignored',
  };

  public categories : Category[] = [];

  ngOnInit() {
    this.categoryService.getAllCategories().subscribe({
      next: (list) => {
        this.categories = list;
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

  protected search() {
    if (this.searchBarInput.length > 0){
      this.router.navigate(['/search'], {
        queryParams: {
          query: this.searchBarInput
        }
      });
      this.searchBarInput = '';
    }
  }

  canViewItem(item: any): boolean {
    if (item.roles.includes('ADMIN') && this.authService.isAdmin()) return true;
    if (item.roles.includes('MANAGER') && this.authService.isManager()) return true;
    if (item.roles.includes('DRIVER') && this.authService.isDriver()) return true;
    return false;
  }

  triggerTest() {
    this.notificationService.triggerTest().subscribe({
      error: (err) => console.error('Error in test:', err)
    });
  }
}
