import {Component, Input} from '@angular/core';
import {NgIf} from '@angular/common';
import {ProgressSpinner} from 'primeng/progressspinner';

//Handles the loading, the error and the no elements skeletons
@Component({
  selector: 'app-loading',
  standalone: true,
  imports: [
    NgIf,
    ProgressSpinner
  ],
  templateUrl: './loading.component.html',
  styleUrl: './loading.component.css'
})
export class LoadingComponent {

  @Input() loading: boolean = false;

  @Input() error: boolean = false;

  @Input() numElements: number = 0;

  @Input() elementsType: string = 'elementos';

  @Input() loadingText: string = 'Cargando, por favor espera...';

  @Input() errorText: string = 'Ha ocurrido un error inesperado';
}
