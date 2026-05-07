/// <reference types="cypress" />
/// <reference path="../../../../../cypress/support/component.ts" />
import { NavMenu } from './nav-menu';
import { HttpClientModule } from '@angular/common/http';
import { RouterTestingModule } from '@angular/router/testing';
import { AuthService } from '../../../services/auth.service';
import { UserService } from '../../../services/user.service';
import { NavService } from '../../../services/nav.service';
import { of } from 'rxjs';

describe('NavMenu Component', () => {
  beforeEach(() => {
    cy.viewport(1280, 720);
  });

  const mountNav = (isAdmin: boolean) => {
    const mockUser = {
      matricule: 'M123',
      roleId: isAdmin ? 7 : 1,
      account: { balance: 10, status: 'ok' },
      penalties: []
    };

    // Mock NavService to emit the visible state
    const navServiceMock = {
      visible$: of(true), // Ensure it's visible for the test
      toggle: () => {},
      close: () => {}
    };

    cy.mount(NavMenu, {
      imports: [
        HttpClientModule,
        RouterTestingModule.withRoutes([{ path: 'dashboard', component: NavMenu }])
      ],
      providers: [
        {
          provide: AuthService,
          useValue: {
            isAuthenticated: () => true,
            logout: () => {}
          }
        },
        {
          provide: UserService,
          useValue: {
            getCurrentUser: () => of(mockUser)
          }
        },
        { provide: NavService, useValue: navServiceMock }
      ]
    }).then((wrapper) => {
      // Use the internal component instance to set the URL or trigger check
      const component = wrapper.component;
      // We simulate a non-landing page URL by setting a custom property or mocking the router property
      Object.defineProperty(component['router'], 'url', { get: () => '/dashboard' });
      // manually re-run auth check because ngOnInit already ran at mount
      component['checkAuthentication']();
    });

    cy.wait(50);
  };

  it('should show all options for a regular User (Non-Admin)', () => {
    mountNav(false);

    // Desktop view checks
    cy.get('aside').within(() => {
      cy.contains('Mon Profil').should('be.visible');
      cy.contains('Mes Matchs').should('be.visible');
      cy.contains('Mes Invitations').should('be.visible');
      cy.contains('Rejoindre Match Public').should('be.visible');
      cy.contains('Créer Match Privé').should('be.visible');
      cy.contains('Reglages').should('be.visible');
      cy.contains('Se Déconnecter').should('be.visible');
    });
  });

  it('should hide user-specific matches and invitations for an Admin', () => {
    mountNav(true);

    // Filter to ensure we target the desktop menu
    cy.get('aside').should('be.visible').within(() => {
      cy.contains(/Mon Profil/i).should('be.visible');
      cy.contains(/Panneau Admin/i).should('be.visible');
      cy.contains(/Créer Match Public/i).should('be.visible');

      // These should NOT exist for admin based on @if logic in html
      cy.contains(/Mes Matchs/i).should('not.exist');
      cy.contains(/Mes Invitations/i).should('not.exist');
      cy.contains(/Rejoindre Match Public/i).should('not.exist');

      // Specifically check that "Reglages" (user version) is not present
      cy.contains(/Reglages/i).should('not.exist');
    });
  });

  it('should show restriction popup when user has debt', () => {
    const debtUser = {
      matricule: 'M123',
      roleId: 1,
      account: { balance: -50, status: 'debt' },
      penalties: []
    };

    cy.mount(NavMenu, {
      imports: [HttpClientModule, RouterTestingModule],
      providers: [
        { provide: AuthService, useValue: { isAuthenticated: () => true } },
        { provide: UserService, useValue: { getCurrentUser: () => of(debtUser) } },
        NavService
      ]
    });

    // Strategy: Click "Créer Match Privé" and check @if (showRestrictionPopup)
    cy.contains('Créer Match Privé').click();
    cy.get('h3').contains('Attention').should('be.visible');
    cy.get('p').contains('-50 €').should('be.visible');
    cy.get('button').contains('OK').click();
    cy.get('h3').should('not.exist');
  });
});


