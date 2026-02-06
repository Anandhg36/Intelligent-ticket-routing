import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { TicketAssignPopupComponent } from './ticket-assign-popup.component';


type TicketStatus = 'Open' | 'In Progress' | 'Resolved';
type TicketPriority = 'High' | 'Medium' | 'Low';

interface Message {
  by: 'Customer' | 'Agent' | 'AI';
  text: string;
  at: string;
}

interface TimelineItem {
  title: string;
  at: string;
  meta?: string;
}

interface Ticket {
  ticketNumber: string;
  subject: string;
  requesterName: string;
  requesterEmail: string;
  createdDate: string;
  status: TicketStatus;
  priority: TicketPriority;
  assignedTeam?: string;
  category?: string;
  aiSuggestions: { team: string; confidence: number }[];
  similarTickets: { id: string; subject: string }[];
  timeline: TimelineItem[];
  conversation: Message[];
}

type ListTab = 'All' | 'Open' | 'Resolved';

@Component({
  selector: 'app-ticket-listing',
  standalone: true,
  imports: [CommonModule, FormsModule, TicketAssignPopupComponent],
  templateUrl: './ticket-listing.component.html',
  styleUrls: ['./ticket-listing.component.css'],
})
export class TicketListingComponent implements OnInit {
  // Keep these because HTML expects them
  tab: ListTab = 'All';
  searchTerm = '';

  allTickets: Ticket[] = [];
  filteredTickets: Ticket[] = []; // HTML still uses this

  selectedTicket: Ticket | null = null;
  replyText = '';
  // Reassign modal state
  showReassignModal = false;

  // Static teams list (can later come from API)
  availableTeams = [
    'Cluster Architecture',
    'Containers and Workloads',
    'Networking',
    'Cluster Administration',
    'Configuration and Security',
    'Scheduling and Resource management',
    'Storage'
  ]

  // API endpoint
  private readonly API_URL = 'http://localhost:8080/api/tickets';

  constructor(private route: ActivatedRoute) { }

  ngOnInit(): void {
    // 🔹 React to team filter changes
    this.route.queryParamMap.subscribe(params => {
      const team = params.get('team');
      this.loadTickets(team);
    });
  }


  // FETCH ONLY: no filtering logic, just show all
  private async loadTickets(team?: string | null) {
    try {
      let url = this.API_URL;

      // 🔹 Append team query param only if present
      if (team) {
        url += `?teamName=${encodeURIComponent(team)}`;
      }

      const res = await fetch(url);
      if (!res.ok) throw new Error(`Tickets API failed: ${res.status}`);

      const data = await res.json();

      this.allTickets = data;
      this.filteredTickets = this.allTickets; // UI binding unchanged
    } catch (e) {
      console.error('Failed to load tickets from backend', e);
      this.allTickets = [];
      this.filteredTickets = [];
    }
  }



  private mapStatus(s: any): TicketStatus {
    const v = String(s ?? '').toUpperCase();
    if (v === 'RESOLVED') return 'Resolved';
    if (v === 'IN_PROGRESS') return 'In Progress';
    return 'Open';
  }

  private mapPriority(p: any): TicketPriority {
    const v = String(p ?? '').toUpperCase();
    if (v === 'HIGH') return 'High';
    if (v === 'MEDIUM') return 'Medium';
    return 'Low';
  }

  // Stats - HTML expects these
  get totalTicketsCount(): number {
    return this.allTickets.length;
  }

  get pendingTicketsCount(): number {
    return this.allTickets.filter((t) => t.status !== 'Resolved').length;
  }

  get solvedTicketsCount(): number {
    return this.allTickets.filter((t) => t.status === 'Resolved').length;
  }

  // Ticket selection - HTML expects these
  selectTicket(t: Ticket) {
    this.selectedTicket = t;
  }

  closeDetail() {
    this.selectedTicket = null;
  }

  postReply() {
    if (!this.selectedTicket) return;
    const msg = this.replyText.trim();
    if (!msg) return;

    this.selectedTicket.conversation = [
      ...this.selectedTicket.conversation,
      { by: 'Agent', text: msg, at: 'Now' },
    ];

    this.replyText = '';
  }

  // -----------------------------
  // DUMMY METHODS (so HTML doesn't break)
  // -----------------------------


  onSearchChange() {
    // no search for now
  }

  setTab(tab: ListTab) {
    this.tab = tab;
    this.applyFilters();
  }

  applyFilters() {
    const term = this.searchTerm.trim().toLowerCase();

    this.filteredTickets = this.allTickets.filter(t => {
      const matchesSearch =
        !term ||
        t.ticketNumber?.toLowerCase().includes(term) ||
        t.subject?.toLowerCase().includes(term) ||
        t.requesterName?.toLowerCase().includes(term) ||
        t.requesterEmail?.toLowerCase().includes(term);

      const matchesTab =
        this.tab === 'All' ||
        (this.tab === 'Resolved' && t.status === 'Resolved') ||
        (this.tab === 'Open' && t.status !== 'Resolved');

      return matchesSearch && matchesTab;
    });
  }

  getConfidenceClass(confidence: number): string {
    if (confidence >= 80) {
      return 'high';
    }
    if (confidence >= 50) {
      return 'medium';
    }
    return 'low';
  }

  openReassignModal() {
    this.showReassignModal = true;
  }

  closeReassignModal() {
    this.showReassignModal = false;
  }

  submitReassignment(payload: {
    aiAssignedTeam: string;
    humanAssignedTeam: string;
    aiSuggestedWrong: boolean;
    teamReview: string;
  }) {
    if (!this.selectedTicket?.ticketNumber) {
      console.error('No ticket selected');
      return;
    }

    const url = `http://localhost:8080/api/tickets/${this.selectedTicket.ticketNumber}/activities/reassign`;

    fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        aiAssignedTeam: payload.aiAssignedTeam,
        humanAssignedTeam: payload.humanAssignedTeam,
        aiSuggestedWrong: payload.aiSuggestedWrong,
        teamReview: payload.teamReview
      })
    })
      .then(res => {
        if (!res.ok) {
          throw new Error(`Reassignment failed: ${res.status}`);
        }
        return res.json();
      })
      .then(() => {
        // ✅ update UI
        this.selectedTicket!.assignedTeam = payload.humanAssignedTeam;

        this.closeReassignModal();
      })
      .catch(err => {
        console.error('Failed to submit reassignment', err);
      });
  }





}
