import { bootstrapApplication, provideClientHydration } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { provideRouter } from '@angular/router';
import { routes } from './app/app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

const authInterceptor = (req: any, next: any) => {
  const token = localStorage.getItem('token');
  if (token) req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` }});
  return next(req);
};

bootstrapApplication(AppComponent, {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideClientHydration(),
  ]
}).catch(err => console.error(err));
