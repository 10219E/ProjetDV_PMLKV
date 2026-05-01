import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { NavMenu } from '../../nav-menu/nav-menu';
import { HomeAccountHeader } from '../../header/header';
import { MatchService } from '../../../../services/match.service';
import { MatchDto } from '../../../../api/model/matchDto';
import { InfoService } from '../../../../services/info.service';
import { SiteInfo } from '../../../../api/model/siteInfo';
import {FieldService} from '../../../../services/field.service';
import {FieldDto} from "../../../../api/model/fieldDto";

@Component({
  selector: 'app-join-public-match',
  standalone: true,
  imports: [CommonModule, NavMenu, HomeAccountHeader],
  templateUrl: './join-public-match.html'
})
export class JoinPublicMatch implements OnInit {
  matches: Array<MatchDto & { siteName?: string }> = [];
  loading = false;
  error: string | null = null;

  constructor(
	private route: ActivatedRoute,
	private router: Router,
	private matchService: MatchService,
	private infoService: InfoService,
  private fieldService: FieldService,
	private cd: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
	// load public matches with status open
	this.loadMatches();
  }

  private loadMatches(): void {
    this.loading = true;
    this.error = null;
    try {
      this.matchService.getMatchesByTypeAndStatus('public', 'open').subscribe({
        next: (data: any) => {
          const all = Array.isArray(data) ? data : [];
          // keep only matches from tomorrow (exclude today and past matches)
          const tomorrow = this.getTomorrowIsoDate();
          const filtered: MatchDto[] = all.filter((m: MatchDto) => {
            const dateOnly = (m?.matchDate ?? '').split('T')[0];
            return dateOnly >= tomorrow;
          });

          // First, get all fields to build a fieldId to siteId map
          this.fieldService.fetchAllFields().subscribe({
            next: (fields: FieldDto[]) => {
              const fieldToSiteMap: Record<number, number> = {};
              (fields || []).forEach(f => {
                if (f?.fieldId != null && f?.siteId != null) {
                  fieldToSiteMap[f.fieldId] = f.siteId;
                }
              });

              // Then get all sites to build a siteId to siteName map
              this.infoService.getSites().subscribe({
                next: (sites: SiteInfo[]) => {
                  const siteMap: Record<number, string> = {};
                  (sites || []).forEach(s => {
                    if (s?.siteId != null) {
                      const name = (s.name ?? '').toString().trim();
                      siteMap[s.siteId] = name.length > 0 ? name : '—';
                    }
                  });

                  // Now attach siteName to each match using the fieldToSiteMap
                  this.matches = filtered.map(m => {
                    const siteId = m?.fieldId != null ? fieldToSiteMap[m.fieldId] : null;
                    const siteName = siteId != null ? (siteMap[siteId] ?? '—') : '—';
                    return { ...m, siteName } as MatchDto & { siteName?: string };
                  });

                  this.loading = false;
                  this.cd.detectChanges();
                },
                error: (err) => {
                  console.error('Error fetching sites:', err);
                  // If site fetch fails, still show matches with fallback
                  this.matches = filtered.map(m => ({ ...m, siteName: '—' } as MatchDto & { siteName?: string }));
                  this.loading = false;
                  this.cd.detectChanges();
                }
              });
            },
            error: (err) => {
              console.error('Error fetching fields:', err);
              // If field fetch fails, still show matches with fallback
              this.matches = filtered.map(m => ({ ...m, siteName: '—' } as MatchDto & { siteName?: string }));
              this.loading = false;
              this.cd.detectChanges();
            }
          });
        },
        error: (err: any) => {
          console.error('Failed to load public matches', err);
          this.error = err?.message || 'Erreur lors du chargement des matchs publics.';
          this.matches = [];
          this.loading = false;
          this.cd.detectChanges();
        }
      });
    } catch (e) {
      this.loading = false;
      this.error = 'Erreur interne.';
      this.cd.detectChanges();
    }
  }

  // Placeholder action when user clicks Join. Real implementation should call backend or navigate to match details.
  joinMatch(m: MatchDto) {
	console.log('Join match', m);
	// navigate to match detail or open join flow
	const userId = this.route.snapshot.paramMap.get('userId');
	if (userId && m && m.matchId != null) {
	  this.router.navigate(['/home', userId, 'match', String(m.matchId)]).catch(() => {});
	}
  }

  viewMatch(m: MatchDto) {
	console.log('View match', m);
	// implement view behavior: for now navigate to match detail route if available
	const userId = this.route.snapshot.paramMap.get('userId');
	if (userId && m && m.matchId != null) {
	  this.router.navigate(['/home', userId, 'match', String(m.matchId)]).catch(() => {});
	}
  }

  formatTime(t?: any): string {
	if (!t) return '';
	// LocalTime from API typically has { hour?: number, minute?: number }
	const hour = (t && (t.hour ?? t.Hour)) ?? null;
	const minute = (t && (t.minute ?? t.Minute)) ?? 0;
	if (hour == null) {
	  // fallback to string representation
	  try { return String(t); } catch { return ''; }
	}
	return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
  }

  // Return tomorrow's date in local YYYY-MM-DD format (used to filter matches)
  private getTomorrowIsoDate(): string {
	const d = new Date();
	d.setDate(d.getDate() + 1);
	const yyyy = d.getFullYear();
	const mm = String(d.getMonth() + 1).padStart(2, '0');
	const dd = String(d.getDate()).padStart(2, '0');
	return `${yyyy}-${mm}-${dd}`;
  }
}


