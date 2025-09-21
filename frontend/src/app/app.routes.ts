import { Routes } from '@angular/router';
import {ClientHomeComponent} from './components/client/client-home/client-home.component';
import {ErrorComponent} from './components/common/error/error.component';
import {LoginComponent} from './components/common/login/login.component';

export const routes: Routes = [
  //Rutas del lado del cliente
  { path: '', component: ClientHomeComponent },

  //Rutas comunes
  { path: 'error', component: ErrorComponent },
  { path: 'login', component: LoginComponent },

  //Rutas inexistentes
  {path: '**', redirectTo: 'login'},
];
