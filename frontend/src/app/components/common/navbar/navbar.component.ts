import {Component, computed, OnInit, signal, ViewChild, WritableSignal} from '@angular/core';
import {IsActiveMatchOptions, Router, RouterLink, RouterLinkActive} from '@angular/router';
import {NgClass, NgIf, NgOptimizedImage, NgTemplateOutlet} from '@angular/common';
import {Button} from 'primeng/button';
import {Drawer} from 'primeng/drawer';
import {MenuItem, PrimeTemplate} from 'primeng/api';
import {FormsModule} from '@angular/forms';
import {Menu} from 'primeng/menu';
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
    Menu,
    Avatar,
    InputGroup,
    InputText,
    NgTemplateOutlet,
    NgClass,
    Select
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
    private router: Router
  ) {}

  @ViewChild('drawerRef') drawerRef!: Drawer;
  @ViewChild('menu') menu!: Menu;

  visible: boolean = false;
  searchBarInput: string = '';
  items: MenuItem[] | undefined;
  loggedUserInfo!: LoginInfo;

  //Shop mode (global to show all products, local to show only selected shop products)
  private baseScopeOptions = [
    { label: 'Todas', value: 'GLOBAL' },
    { label: 'Tienda', value: 'LOCAL' }
  ];

  public scopeOptions = computed(() => {
    const hasShop = this.authService.hasShopSelected(); // Usamos la señal de tu authService
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


  public adminItems = [
    { label: 'Productos', icon: 'pi pi-desktop',   link: '/admin/products', roles: ['ADMIN'] },
    { label: 'Categorías', icon: 'pi pi-tag',   link: '/admin/categories', roles: ['ADMIN'] },
    { label: 'Informes',  icon: 'pi pi-chart-bar', link: 'reports', roles: ['ADMIN', 'MANAGER', 'DRIVER'] },
    { label: 'Tiendas',   icon: 'pi pi-shop',      link: '/admin/shops', roles: ['ADMIN', 'MANAGER'] },
    { label: 'Pedidos',   icon: 'pi pi-box',       link: '/admin/orders', roles: ['ADMIN', 'MANAGER', 'DRIVER'] },
    { label: 'Reparto',   icon: 'pi pi-truck',     link: 'delivery', roles: ['ADMIN', 'MANAGER', 'DRIVER'] },
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
    this.items = [
      { label: 'Notificación 1', icon: 'pi pi-bell' },
      { label: 'Notificación 2', icon: 'pi pi-bell' }
    ];

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
    }
  }

  canViewItem(item: any): boolean {
    if (item.roles.includes('ADMIN') && this.authService.isAdmin()) return true;
    if (item.roles.includes('MANAGER') && this.authService.isManager()) return true;
    if (item.roles.includes('DRIVER') && this.authService.isDriver()) return true;
    return false;
  }
}
