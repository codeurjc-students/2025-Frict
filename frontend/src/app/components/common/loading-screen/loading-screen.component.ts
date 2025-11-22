import {Component, EventEmitter, Input, Output} from '@angular/core';
import {ProgressSpinner} from 'primeng/progressspinner';
import {Router, RouterLink} from '@angular/router';
import {NgIf} from '@angular/common';

@Component({
  selector: 'app-loading-screen',
  imports: [
    ProgressSpinner,
    RouterLink,
    NgIf
  ],
  templateUrl: './loading-screen.component.html',
  styleUrl: './loading-screen.component.css'
})
export class LoadingScreenComponent {

  constructor(private router: Router) {}

  @Input() loading: boolean = true;

  @Input() error: boolean = false;

  @Output() tryReload = new EventEmitter<void>();

  @Input() loadingText: string = 'Cargando, por favor espera...';

  @Input() errorText: string = 'Ha ocurrido un error inesperado';

  reloadPage() {
    this.router.navigate([this.router.url]);
    this.tryReload.emit();
  }
}
