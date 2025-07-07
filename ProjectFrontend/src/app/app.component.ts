import {Component, OnInit} from '@angular/core';
import {RouterOutlet} from '@angular/router'; //Es el componente en el que se integra el contenido del router, e importa RouterLink por ser usado en los botones de nav
import { initFlowbite } from 'flowbite';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  title = 'ProjectFrontend';

  ngOnInit(): void {
    initFlowbite();
  }

}
