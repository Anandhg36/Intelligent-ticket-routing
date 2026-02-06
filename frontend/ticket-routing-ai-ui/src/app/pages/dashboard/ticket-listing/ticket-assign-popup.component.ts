import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-ticket-assign-popup',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ticket-assign-popup.component.html',
  styleUrls: ['./ticket-assign-popup.component.css']
})
export class TicketAssignPopupComponent {
  @Input() ticket: any;
  @Input() teams: string[] = [];

  @Output() close = new EventEmitter<void>();
  @Output() submit = new EventEmitter<{
    aiAssignedTeam: string;
    humanAssignedTeam: string;
    aiSuggestedWrong: boolean;
    teamReview: string;
  }>();

  selectedTeam = '';
  aiSuggestedWrong = false;
  reviewText = '';

  submitForm() {
    if (!this.selectedTeam) {
      alert('Please select a team');
      return;
    }

    this.submit.emit({
      aiAssignedTeam: this.ticket?.assignedTeamName || '',
      humanAssignedTeam: this.selectedTeam,
      aiSuggestedWrong: this.aiSuggestedWrong,
      teamReview: this.reviewText.trim()
    });
  }

  closeModal() {
    this.close.emit();
  }
}
