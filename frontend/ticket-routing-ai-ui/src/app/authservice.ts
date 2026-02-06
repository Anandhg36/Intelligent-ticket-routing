import { Injectable } from "@angular/core";

@Injectable({ providedIn: 'root' })
export class AuthService {
  private static readonly USER_ID_KEY = 'user_id';

  setUserId(id: number): void {
    localStorage.setItem(AuthService.USER_ID_KEY, String(id));
  }

  getUserId(): number | null {
    const raw = localStorage.getItem(AuthService.USER_ID_KEY);
    if (!raw) return null;
    const num = Number(raw);
    return Number.isNaN(num) ? null : num;
  }

  clearUserId(): void {
    localStorage.removeItem(AuthService.USER_ID_KEY);
  }
}
