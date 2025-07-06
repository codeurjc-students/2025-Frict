import { Component } from '@angular/core';
import {RouterLink, RouterOutlet} from '@angular/router'; //Es el componente en el que se integra el contenido del router, e importa RouterLink por ser usado en los botones de nav

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'ProjectFrontend';
}
