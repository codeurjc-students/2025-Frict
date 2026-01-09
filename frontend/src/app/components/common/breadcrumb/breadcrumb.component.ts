import {Component, inject, OnDestroy, OnInit} from '@angular/core';
import {Breadcrumb} from 'primeng/breadcrumb';
import {MenuItem} from 'primeng/api';
import {filter, Subscription} from 'rxjs';
import {ActivatedRoute, NavigationEnd, Router} from '@angular/router';

@Component({
  selector: 'app-breadcrumb',
  imports: [
    Breadcrumb
  ],
  templateUrl: './breadcrumb.component.html',
  styleUrl: './breadcrumb.component.css'
})
export class BreadcrumbComponent implements OnInit, OnDestroy {
  breadcrumbs: MenuItem[] = [];
  home: MenuItem = { icon: 'pi pi-home', routerLink: '/' };

  private router = inject(Router);
  private activatedRoute = inject(ActivatedRoute);
  private menuSub!: Subscription;

  ngOnInit(): void {
    // Initial breadcrumbs
    this.breadcrumbs = this.createBreadcrumbs(this.activatedRoute.root);

    // Listen to route changes
    this.menuSub = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => {
        this.breadcrumbs = this.createBreadcrumbs(this.activatedRoute.root);
      });
  }

  ngOnDestroy(): void {
    if (this.menuSub) this.menuSub.unsubscribe();
  }

  private createBreadcrumbs(route: ActivatedRoute, url: string = '', breadcrumbs: MenuItem[] = []): MenuItem[] {
    const children: ActivatedRoute[] = route.children;

    if (children.length === 0) {
      return breadcrumbs;
    }

    for (const child of children) {
      const routeURL: string = child.snapshot.url.map(segment => segment.path).join('/');

      if (routeURL !== '') {
        url += `/${routeURL}`;
      }

      const label = child.snapshot.data['breadcrumb'];

      if (label) {
        breadcrumbs.push({
          label: label,
          routerLink: url
        });
      }

      return this.createBreadcrumbs(child, url, breadcrumbs);
    }

    return breadcrumbs;
  }
}
