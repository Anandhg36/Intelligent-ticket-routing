import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },

  { path: 'signup', loadComponent: () =>
      import('./pages/signup/signup.component').then(m => m.SignupComponent) },

  { path: 'login', loadComponent: () =>
      import('./pages/login/login.component').then(m => m.LoginComponent) },

 // app.routes.ts
 {
    path: 'dashboard',
    loadComponent: () =>
      import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'find-classmate' },
      {
        path: 'ticket-listing',
        loadComponent: () =>
          import('./pages/dashboard/ticket-listing/ticket-listing.component')
            .then(m => m.TicketListingComponent),
      },
      
    ],
  },


  { path: '**', redirectTo: 'login' }
];
