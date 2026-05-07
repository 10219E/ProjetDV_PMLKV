/// <reference types="cypress" />
/// <reference path="../../../../../../cypress/support/component.ts" />
import { MyProfile } from './my-profile';
import { HttpClientModule } from '@angular/common/http';
import { RouterTestingModule } from '@angular/router/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { UserService } from '../../../../services/user.service';
import { AuthService } from '../../../../services/auth.service';
import { MigrationService } from '../../../../services/migration.service';
import { ActivatedRoute } from '@angular/router';

describe('MyProfile Component', () => {
  beforeEach(() => {
    cy.viewport(1280, 800);
  });

  const mountProfile = (roleId: number = 1, balance: number = 10) => {
    const mockUser = {
      matricule: 'M123',
      firstName: 'John',
      lastName: 'Doe',
      email: 'john.doe@example.com',
      birthDate: '1990-01-01',
      roleId: roleId,
      level: 'B',
      isActive: true,
      account: { balance: balance, status: balance < 0 ? 'debt' : 'clear' },
      penalties: [],
      sites: [{ siteId: 1, siteName: 'Site Central' }]
    };

    cy.mount(MyProfile, {
      imports: [HttpClientModule, RouterTestingModule, BrowserAnimationsModule],
      providers: [
        {
          provide: UserService,
          useValue: {
            getUserById: () => of(mockUser),
            updateUserInBackend: () => of({}),
            updateUserPenaltyAndAccount: () => of({})
          }
        },
        {
          provide: AuthService,
          useValue: {
            isAuthenticated: () => true,
            login: () => of({}),
            logout: () => {}
          }
        },
        {
          provide: MigrationService,
          useValue: {
            migrateToVip: () => of({})
          }
        },
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of({ get: () => 'M123' })
          }
        }
      ]
    });
  };

  it('should display the user profile information accurately', () => {
    mountProfile(1); // Regular user
    cy.contains(/John Doe/i).should('be.visible');
    cy.contains(/M123/i).should('be.visible');
    cy.contains(/john.doe@example.com/i).should('be.visible');
    cy.contains(/Niveau B/i).should('be.visible');
    cy.contains(/Actif/i).should('be.visible');
  });

  it('should show the "Apurer" button and debt amount when user has a debt', () => {
    mountProfile(1, -25.50); // User with debt
    cy.contains(/-25.50/i).should('be.visible');
    cy.contains(/Apurer/i).should('be.visible');
  });

  it('should open the password change modal', () => {
    mountProfile(1);
    cy.get('button').contains(/Changer le mot de passe/i).click();
    cy.get('h3').contains(/Changer le mot de passe/i).should('be.visible');
    cy.get('input').should('have.length.at.least', 3);
  });

  it('should show VIP upgrade button for eligible users', () => {
    mountProfile(1); // Role 1 is eligible
    cy.contains(/Devenir Membre VIP/i).should('be.visible');
  });

  it('should hide account details for Admins', () => {
    mountProfile(7); // Admin role
    cy.contains(/Administrateur/i).should('be.visible');
    cy.contains(/Mon Compte/i).should('not.exist');
  });
});

