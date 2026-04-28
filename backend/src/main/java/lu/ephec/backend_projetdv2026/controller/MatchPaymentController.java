package lu.ephec.backend_projetdv2026.controller;

import lu.ephec.backend_projetdv2026.models.MatchPayments;
import lu.ephec.backend_projetdv2026.dto.MatchPaymentDto;
import lu.ephec.backend_projetdv2026.services.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/payments")
public class MatchPaymentController {

	private final PaymentService paymentService;
	private static final Logger logger = LoggerFactory.getLogger(MatchPaymentController.class);

	public MatchPaymentController(PaymentService paymentService) {
		this.paymentService = paymentService;
	}

	@PostMapping(produces = "application/json")
	public ResponseEntity<MatchPayments> createPayment(@RequestBody MatchPaymentDto dto) {
		logger.info("[MATCH PAYMENT CONTROLLER] Create payment request received: matchId={} userId={} amount={} status={}",
				dto != null ? dto.getMatchId() : null,
				dto != null ? dto.getUserMatricule() : null,
				dto != null ? dto.getAmount() : null,
				dto != null ? dto.getStatus() : null);

		try {
			MatchPayments toSave = dto.toEntity();
			MatchPayments saved = paymentService.newPayment(toSave);
			return ResponseEntity.status(HttpStatus.CREATED).body(saved);
		} catch (ResponseStatusException ex) {
			logger.warn("[MATCH PAYMENT CONTROLLER] Error creating payment: {}", ex.getReason());
			throw ex;
		} catch (Exception ex) {
			logger.error("[MATCH PAYMENT CONTROLLER] Unexpected error creating payment", ex);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
		}
	}

	@PutMapping(value = "/{id}", produces = "application/json")
	public ResponseEntity<MatchPayments> updatePayment(@PathVariable Integer id, @RequestBody MatchPaymentDto dto) {
		logger.info("[MATCH PAYMENT CONTROLLER] Update payment request received: id={}", id);
		try {
			MatchPayments updateEntity = dto.toEntity();
			// ensure the id path param is used
			updateEntity.setTr(id);
			return paymentService.updatePayment(id, updateEntity)
					.map(p -> ResponseEntity.ok(p))
					.orElseGet(() -> ResponseEntity.notFound().build());
		} catch (ResponseStatusException ex) {
			logger.warn("[MATCH PAYMENT CONTROLLER] Error updating payment: {}", ex.getReason());
			throw ex;
		} catch (Exception ex) {
			logger.error("[MATCH PAYMENT CONTROLLER] Unexpected error updating payment", ex);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
		}
	}

}
