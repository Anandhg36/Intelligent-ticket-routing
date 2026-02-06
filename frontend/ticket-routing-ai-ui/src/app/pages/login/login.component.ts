import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Router } from '@angular/router';

// If you use Angular's proxy, set API_BASE = '/api'
const API_BASE = 'http://localhost:8010';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
})
export class LoginComponent implements OnInit {
  private fb = inject(FormBuilder);
  private http = inject(HttpClient);
  private router = inject(Router);

  loading = false;
  error: string | null = null;

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    remember: [false],
  });

  ngOnInit(): void {
    const saved = localStorage.getItem('auth_email');
    if (saved) {
      this.form.patchValue({ email: saved, remember: true });
    }
  }

  get f() {
    return this.form.controls;
  }

  onSubmit(): void {
    this.error = null;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const email = this.f.email.value!.trim();
    const password = this.f.password.value!;
    const remember = !!this.form.value.remember;

    // Backend expects POST with query params (no JSON body)
    const params = new HttpParams().set('email', email).set('password', password);

    this.loading = true;

    this.http.post(`${API_BASE}/login`, null, { params }).subscribe({
      next: () => {
        this.loading = false;

        if (remember) {
          localStorage.setItem('auth_email', email);
        } else {
          localStorage.removeItem('auth_email');
        }

        this.router.navigateByUrl('/dashboard');
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        const detail =
          (err.error && (err.error.detail || err.error.message)) ||
          err.message ||
          'Login failed';
        this.error = typeof detail === 'string' ? detail : 'Login failed';
      },
    });
  }
}