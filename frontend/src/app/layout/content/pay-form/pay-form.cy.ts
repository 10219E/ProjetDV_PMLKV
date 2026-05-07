/// <reference types="cypress" />
/// <reference path="../../../../../cypress/support/component.ts" />
import { PayFormComponent } from './pay-form';
import { HttpClientModule } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

describe('PayFormComponent', () => {
  beforeEach(() => {
    cy.viewport(1000, 600);
  });

  const mountPayForm = (amount: number = 25.50) => {
    cy.mount(PayFormComponent, {
      imports: [HttpClientModule, BrowserAnimationsModule],
      componentProperties: {
        amount: amount
      }
    }).then((wrapper) => {
      wrapper.fixture.detectChanges();
    });
  };

  it('should display the correct amount to pay', () => {
    mountPayForm(42.00);
    cy.contains(/Montant à payer/i).should('be.visible');
    // The number pipe format '1.0-2' and localization might add spaces or different symbols
    cy.contains(/42/i).should('be.visible');
    cy.get('span').contains(/€/i).should('be.visible');
  });

  it('should show error for invalid card number', () => {
    mountPayForm();

    // Type partial card number
    cy.get('input').type('1234').blur();

    // Trigger pay
    cy.get('button').contains(/Payer/i).click();

    // Check validation @if
    cy.contains(/doit contenir 16 chiffres/i).should('be.visible');
  });

  it('should emit "paid" event after a successful mock payment', () => {
    mountPayForm(25.50);

    const paidSpy = cy.spy().as('paidSpy');

    // Use the component instance from the mount result wrapper
    cy.mount(PayFormComponent, {
      imports: [HttpClientModule, BrowserAnimationsModule],
      componentProperties: { amount: 25.50 }
    }).then((wrapper) => {
       wrapper.component.paid.subscribe(paidSpy);
       wrapper.fixture.detectChanges();
    });

    // Fill valid 16-digit card
    cy.get('input').type('1234567812345678');

    // Click pay
    cy.get('button').contains(/Payer/i).click();

    // Check loading state
    cy.get('button').contains(/Paiement.../i).should('be.disabled');

    // Wait for the mock 800ms delay in component logic
    cy.get('@paidSpy', { timeout: 2000 }).should('have.been.calledWith', {
      amount: 25.50,
      cardLast4: '5678'
    });
  });

  it('should emit "cancelled" when clicking Annuler', () => {
    const cancelSpy = cy.spy().as('cancelSpy');

    cy.mount(PayFormComponent, {
      imports: [HttpClientModule, BrowserAnimationsModule],
      componentProperties: { amount: 25.50 }
    }).then((wrapper) => {
       wrapper.component.cancelled.subscribe(cancelSpy);
       wrapper.fixture.detectChanges();
    });

    cy.get('button').contains(/Annuler/i).click();
    cy.get('@cancelSpy').should('have.been.called');
  });
});
