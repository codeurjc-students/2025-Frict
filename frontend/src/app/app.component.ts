import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, NavigationEnd, Router, RouterOutlet} from '@angular/router';
import {Title} from '@angular/platform-browser';
import {filter, map, mergeMap} from 'rxjs';
import {AuthService} from './services/auth.service';
import {LoginInfo} from './models/loginInfo.model';
import {OrderService} from './services/order.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  standalone: true,
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {

  constructor(
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private title: Title,
    private orderService: OrderService
  ) {
  }

  ngOnInit() {
    this.initItemsCount();
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      map(() => {
        let route = this.activatedRoute;
        while (route.firstChild) route = route.firstChild;
        return route;
      }),
      mergeMap(route => route.data)
    ).subscribe(data => {
      if (data['title']) this.title.setTitle(data['title']);
      if (data['icon']) this.setIcon(data['icon']);
    });
  }

  private setIcon(iconUrl: string) {
    const link: HTMLLinkElement =
      document.querySelector("link[rel~='icon']") || document.createElement('link');
    link.rel = 'icon';
    link.href = iconUrl;
    document.head.appendChild(link);
  }

  //Init signal variable in OrderService which contains the number of items in cart (useful for navbar cart items count)
  private initItemsCount() {
    this.orderService.getUserCartSummary().subscribe({ //The elements do not matter, but totalElements page field does
      next: (summary) => {
        console.log(summary);
        this.orderService.setItemsCount(summary.totalItems);
      }
    })
  }
}
