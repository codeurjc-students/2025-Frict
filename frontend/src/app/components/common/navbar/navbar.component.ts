import {Component, computed, inject, OnInit, signal, ViewChild, WritableSignal} from '@angular/core';
import {IsActiveMatchOptions, NavigationStart, Router, RouterLink, RouterLinkActive} from '@angular/router';
import {filter} from 'rxjs';
import {DatePipe, NgClass, NgOptimizedImage, NgTemplateOutlet} from '@angular/common';
import {Button} from 'primeng/button';
import {Drawer} from 'primeng/drawer';
import {PrimeTemplate} from 'primeng/api';
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
import {Popover} from 'primeng/popover';
import {NotificationService} from '../../../services/notification.service';
import {Notification} from '../../../models/notification.model';
import {HttpClient} from '@angular/common/http';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    RouterLink,
    RouterLinkActive,
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

  protected authService = inject(AuthService);
  protected orderService = inject(OrderService);
  private categoryService = inject(CategoryService);
  protected productService = inject(ProductService);
  protected notificationService = inject(NotificationService);
  private router = inject(Router);
  private http = inject(HttpClient);

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
    this.manualToggleState.update(() => {
      const newMap = new Map<number, boolean>();
      newMap.set(categoryId, !currentState);
      return newMap;
    });
  }

  closeDrawerDelayed(): void {
    setTimeout(() => this.visible = false, 200);
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
    this.router.events.pipe(
      filter(e => e instanceof NavigationStart)
    ).subscribe(() => this.closeDrawerDelayed());

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

  goToNotification(notif: Notification) {
    const route = this.authService.isUser() ? '/notifications' : '/admin/notifications';
    this.notifPopover.hide();
    this.router.navigate([route], {
      queryParams: { notifId: notif.id },
      state: { notification: notif }
    });
  }
}
