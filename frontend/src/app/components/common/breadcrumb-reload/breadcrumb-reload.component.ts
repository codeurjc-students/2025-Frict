import { Component, inject, OnInit, Output, EventEmitter, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Breadcrumb } from 'primeng/breadcrumb';
import { MenuItem } from 'primeng/api';
import { filter } from 'rxjs';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { Button } from 'primeng/button';
import {BreadcrumbService} from '../../../utils/breadcrumb.service';
import {AuthService} from '../../../services/auth.service';

@Component({
  selector: 'app-breadcrumb-reload',
  standalone: true,
  imports: [Breadcrumb, Button],
  templateUrl: './breadcrumb-reload.component.html',
  styleUrl: 'breadcrumb-reload.component.css'
})
export class BreadcrumbReloadComponent implements OnInit {

  @Output() onReload = new EventEmitter<void>();

  home: MenuItem = { label: 'Inicio', routerLink: '/' };

  private router = inject(Router);
  private activatedRoute = inject(ActivatedRoute);
  private destroyRef = inject(DestroyRef);
  public breadcrumbService = inject(BreadcrumbService);
  public authService = inject(AuthService);

  ngOnInit(): void {
    this.configureHomeNode();
    this.updateBreadcrumbs();

    this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => {
        this.configureHomeNode();
        this.updateBreadcrumbs();
      });
  }

  triggerReload(): void {
    this.onReload.emit();
  }

  private configureHomeNode(): void {
    const isStaff = this.authService.isAdmin() || this.authService.isManager() || this.authService.isDriver();
    this.home = {
      label: 'Inicio',
      routerLink: isStaff ? '/admin' : '/'
    };
  }

  private updateBreadcrumbs(): void {
    const items = this.createBreadcrumbs(this.activatedRoute.root);
    this.breadcrumbService.setBaseBreadcrumbs(items, this.router.url);
  }

  private createBreadcrumbs(route: ActivatedRoute, url: string = '', breadcrumbs: MenuItem[] = []): MenuItem[] {
    const children: ActivatedRoute[] = route.children;
    if (children.length === 0) return breadcrumbs;

    for (const child of children) {
      const routeURL: string = child.snapshot.url.map(segment => segment.path).join('/');
      if (routeURL !== '') url += `/${routeURL}`;

      const label = child.snapshot.data['breadcrumb'];

      if (label && label !== 'Inicio') {
        breadcrumbs.push({ label, routerLink: url });
      }

      return this.createBreadcrumbs(child, url, breadcrumbs);
    }
    return breadcrumbs;
  }
}
