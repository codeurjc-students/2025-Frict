import { Routes } from '@angular/router';
import {HomeComponent} from './components/home/home.component';
import {LoginComponent} from './components/login/login.component';

export const routes: Routes = [
  {path: '', component: HomeComponent}, //La ruta '' será la que se muestre por defecto en el router cuando se inicia la aplicación. Si no se define, el router no mostrará nada dentro de su componente
  {path: 'login', component: LoginComponent},
];
