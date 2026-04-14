import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

@Component({
  selector: 'app-home-account',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './home-account.component.html',
  styleUrls: ['./home-account.component.css']
})
export class HomeAccountComponent implements OnInit {
  userName = 'Utilisateur';
  isVip = true;
  todayDate = new Date();

  // Dummy data for visual
  upcomingMatches = [
    { site: 'Centre Sportif', date: '12/05/2026', heure: '14:00', status: 'Confirmé' },
    { site: 'Padel Club XY', date: '14/05/2026', heure: '18:30', status: 'En attente' }
  ];

  invitations = [
    { site: 'Padel Arena', date: '20/05/2026', heure: '10:00' }
  ];

  stats = {
    played: 12,
    penalties: 0,
    actives: 1,
    privateOrganized: 5,
    publicParticipation: 7
  };

  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {
    const userId = this.route.snapshot.paramMap.get('userId');
    // Call user data if needed later
    if (userId) {
      this.userName = 'Joueur ' + userId;
    }
  }

  acceptInvite(invite: any) {
    console.log('Accept invite', invite);
  }

  declineInvite(invite: any) {
    console.log('Decline invite', invite);
  }
}
