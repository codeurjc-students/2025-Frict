import {Component, effect, OnInit} from '@angular/core';
import {ActivatedRoute, NavigationEnd, Router, RouterOutlet} from '@angular/router';
import {Title} from '@angular/platform-browser';
import {filter, map, mergeMap} from 'rxjs';
import {AuthService} from './services/auth.service';
import {OrderService} from './services/order.service';
import {ConfirmDialog} from 'primeng/confirmdialog';
import {Toast} from 'primeng/toast';
import {UiService} from './utils/ui.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ConfirmDialog, Toast],
  templateUrl: './app.component.html',
  standalone: true,
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {

  constructor(
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private orderService: OrderService,
    private authService: AuthService,
    private uiService: UiService
  ) {
    effect(() => {
      const isLoggedIn = this.authService.isLogged();
      if (isLoggedIn) {
        this.orderService.syncItemsCount();
      } else {
        this.orderService.setItemsCount(0);
      }
    });
  }

  ngOnInit() {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      map(() => {
        let route = this.activatedRoute;
        while (route.firstChild) route = route.firstChild;
        return route;
      }),
      mergeMap(route => route.data)
    ).subscribe(data => {
      if (data['title']) this.uiService.setPageTitle(data['title']);
      if (data['icon']) this.uiService.setFavicon(data['icon']);
    });
  }
}
