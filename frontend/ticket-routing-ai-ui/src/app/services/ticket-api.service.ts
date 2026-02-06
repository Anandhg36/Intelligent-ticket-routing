import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface TicketDto {
  id: number;
  ticketNumber: string;
  subject: string;
  status: 'OPEN' | 'IN_PROGRESS' | 'RESOLVED';
  priority: 'LOW' | 'MEDIUM' | 'HIGH';
  assignedTeamName?: string | null;
  requesterName?: string;
  requesterEmail?: string;
  createdAt?: string;
}

export interface TeamDto {
  id: number;
  name: string;
}

@Injectable({ providedIn: 'root' })
export class TicketApiService {
  private base = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  getTeams(): Observable<TeamDto[]> {
    return this.http.get<TeamDto[]>(`${this.base}/teams`);
  }

  getTickets(filters?: { team?: string; status?: string; search?: string }): Observable<TicketDto[]> {
    let params = new HttpParams();

    if (filters?.team) params = params.set('team', filters.team);
    if (filters?.status) params = params.set('status', filters.status);
    if (filters?.search) params = params.set('search', filters.search);

    return this.http.get<TicketDto[]>(`${this.base}/tickets`, { params });
  }

  createTicket(body: { subject: string; requesterEmail: string; requesterName?: string }): Observable<TicketDto> {
    return this.http.post<TicketDto>(`${this.base}/tickets`, body);
  }
}
