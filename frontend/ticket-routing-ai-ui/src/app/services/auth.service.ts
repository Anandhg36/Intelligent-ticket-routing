import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs/operators';

const API_BASE = 'http://127.0.0.1:8000'; // FastAPI base

export interface SignupDTO { email: string; password: string; fullName: string; }
export interface LoginDTO { email: string; password: string; }
export interface AuthResponse { access_token: string; }

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);

  signup(data: SignupDTO) {
    return this.http.post(`${API_BASE}/auth/signup`, data);
  }

  login(data: LoginDTO) {
    return this.http.post<AuthResponse>(`${API_BASE}/auth/login`, data).pipe(
      tap(res => localStorage.setItem('token', res.access_token))
    );
  }

  logout(){ localStorage.removeItem('token'); }
  isAuthed(){ return !!localStorage.getItem('token'); }
}
