import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Router } from '@angular/router';

/** TODO: move to environments */
const API_BASE = 'http://localhost:8000';

interface SignupResponse {
  message: string;
  data: unknown;
}

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.css'],
})
export class SignupComponent {
  private fb = inject(FormBuilder);
  private http = inject(HttpClient);
  private router = inject(Router);

  loading = false;
  error: string | null = null;
  success = false;

  form = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    canvastoken: ['', [Validators.required]],
  });

  get f() {
    return this.form.controls;
  }

  // /** Generate an API token for the user (client-side) */
  // private generateToken(): string {
  //   try {
  //     // modern browsers
  //     // @ts-ignore - TS lib may not include randomUUID in some configs
  //     if (crypto?.randomUUID) return crypto.randomUUID();
  //   } catch {}
  //   // fallback
  //   return (
  //     Math.random().toString(36).slice(2) +
  //     '-' +
  //     Date.now().toString(36) +
  //     '-' +
  //     Math.random().toString(36).slice(2)
  //   ).toUpperCase();
  // }

  onSubmit(): void {
    this.error = null;
    this.success = false;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload = {
      name: this.f.name.value!.trim(),
      email: this.f.email.value!.trim(),
      password: this.f.password.value!,
      api_token: this.f.canvastoken.value!.trim()
    };

    this.loading = true;

    this.http
      .post<SignupResponse>(`${API_BASE}/signup`, payload)
      .subscribe({
        next: (res) => {
          console.log("Registration successful!")
          this.loading = false;
          this.success = true;
          setTimeout(() => this.router.navigateByUrl('/login'), 500);
        },
        error: (err: HttpErrorResponse) => {
          this.loading = false;
          const detail =
            (err.error && (err.error.detail || err.error.message)) ||
            err.message ||
            'Signup failed';
          this.error = typeof detail === 'string' ? detail : 'Signup failed';
        },
      });
  }
}