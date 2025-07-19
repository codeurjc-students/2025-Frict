import {Component, OnInit} from '@angular/core';
import {NavigationEnd, Router, RouterOutlet} from '@angular/router'; //Es el componente en el que se integra el contenido del router, e importa RouterLink por ser usado en los botones de nav
import { initFlowbite } from 'flowbite';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  standalone: true,
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {

  title = 'ProjectFrontend';

  constructor(private router: Router) {}

  ngOnInit(): void {
    this.router.events.subscribe((event: any) => {
      if (event instanceof NavigationEnd) {
        initFlowbite();
      }
    });
  }

}
