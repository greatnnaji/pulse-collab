import { Routes } from '@angular/router';

import { AppShellComponent } from './app-shell/app-shell.component';
import { authGuard } from './auth/auth.guard';
import { AuthLayoutComponent } from './auth/layout/auth-layout.component';
import { LoginComponent } from './auth/login/login.component';
import { RegisterComponent } from './auth/register/register.component';
import { MessageThreadComponent } from './messages/message-thread.component';

export const appRoutes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'app'
  },
  {
    path: 'app',
    component: AppShellComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'groups/:groupId/messages',
        component: MessageThreadComponent
      }
    ]
  },
  {
    path: 'auth',
    component: AuthLayoutComponent,
    children: [
      {
        path: 'login',
        component: LoginComponent
      },
      {
        path: 'register',
        component: RegisterComponent
      },
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'login'
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'app'
  }
];
