package lu.ephec.backend_projetdv2026.controller;

import lu.ephec.backend_projetdv2026.models.MatchPayments;
import lu.ephec.backend_projetdv2026.dto.MatchPaymentDto;
import java.util.List;
import java.util.stream.Collectors;
import lu.ephec.backend_projetdv2026.services.PaymentService;
import lu.ephec.backend_projetdv2026.services.MatchService;
import lu.ephec.backend_projetdv2026.models.MatchPlayers;
import lu.ephec.backend_projetdv2026.dto.MatchDto;
import lu.ephec.backend_projetdv2026.dto.FieldDto;
import lu.ephec.backend_projetdv2026.dto.SiteDto;
import lu.ephec.backend_projetdv2026.dto.UserProfileDto;
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
	private final MatchService matchService;
	private static final Logger logger = LoggerFactory.getLogger(MatchPaymentController.class);

	public MatchPaymentController(PaymentService paymentService, MatchService matchService) {
		this.paymentService = paymentService;
		this.matchService = matchService;
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

	@GetMapping (value = "/invites/{userId}", produces = "application/json")
	public ResponseEntity<List<PendingInviteDetails>> getPendingWithDetailsPaymentsByUser(@PathVariable String userId) {
		logger.info("[MATCH PAYMENT CONTROLLER] Get pending payments details for match user request received: userId={}", userId);
		try {
			List<MatchPayments> payments = paymentService.fetchPendingByUser(userId);

			List<PendingInviteDetails> result = payments.stream().map(p -> {
				MatchPaymentDto payDto = MatchPaymentDto.fromEntity(p);

				MatchDto matchDto = null;
				FieldDto fieldDto = null;
				SiteDto siteDto = null;
				UserProfileDto organiserDto = null;

				if (p.getMatch() != null) {
					try { matchDto = MatchDto.from(p.getMatch()); } catch (Exception ignored) {}
					if (p.getMatch().getField() != null) {
						try { fieldDto = FieldDto.from(p.getMatch().getField()); } catch (Exception ignored) {}
						if (p.getMatch().getField().getSite() != null) {
							try { siteDto = SiteDto.from(p.getMatch().getField().getSite()); } catch (Exception ignored) {}
						}
					}
					if (p.getMatch().getOrganiser() != null) {
						try { organiserDto = UserProfileDto.from(p.getMatch().getOrganiser(), null, null); } catch (Exception ignored) {}
					}
				}

				List<UserProfileDto> participants = null;
				Integer occupancy = null;
				Integer remainingSlots = null;
				if (p.getMatch() != null && p.getMatch().getMatchId() != null) {
					try {
						List<MatchPlayers> players = matchService.fetchAllForMatch(p.getMatch().getMatchId());
						participants = players.stream().map(mp -> mp.getUser() != null ? UserProfileDto.from(mp.getUser(), null, null) : null).collect(Collectors.toList());
						long occ = players.stream().filter(mp -> "approved".equals(mp.getStatus())).count();
						occupancy = (int) occ;
						if (p.getMatch().getMaxPlayers() != null) {
							remainingSlots = p.getMatch().getMaxPlayers() - occupancy;
						}
					} catch (ResponseStatusException ignored) {
						// no players found -> keep participants null
					}
				}

				PendingInviteDetails dto = new PendingInviteDetails();
				dto.setPayment(payDto);
				dto.setMatch(matchDto);
				dto.setField(fieldDto);
				dto.setSite(siteDto);
				dto.setOrganiser(organiserDto);
				dto.setParticipants(participants);
				dto.setOccupancy(occupancy);
				dto.setRemainingSlots(remainingSlots);
				return dto;
			}).collect(Collectors.toList());

			return ResponseEntity.ok(result);
		} catch (ResponseStatusException ex) {
			logger.warn("[MATCH PAYMENT CONTROLLER] Error fetching pending payments details for match user {}: {}", userId, ex.getReason());
			throw ex;
		} catch (Exception ex) {
			logger.error("[MATCH PAYMENT CONTROLLER] Unexpected error fetching pending payments details for match user {}", userId, ex);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
		}
	}

	// Composite DTO reusing existing DTOs for the controller response
	public static class PendingInviteDetails {
		private MatchPaymentDto payment;
		private MatchDto match;
		private FieldDto field;
		private SiteDto site;
		private UserProfileDto organiser;
		private List<UserProfileDto> participants;
		private Integer occupancy;
		private Integer remainingSlots;

		public MatchPaymentDto getPayment() { return payment; }
		public void setPayment(MatchPaymentDto payment) { this.payment = payment; }
		public MatchDto getMatch() { return match; }
		public void setMatch(MatchDto match) { this.match = match; }
		public FieldDto getField() { return field; }
		public void setField(FieldDto field) { this.field = field; }
		public SiteDto getSite() { return site; }
		public void setSite(SiteDto site) { this.site = site; }
		public UserProfileDto getOrganiser() { return organiser; }
		public void setOrganiser(UserProfileDto organiser) { this.organiser = organiser; }
		public List<UserProfileDto> getParticipants() { return participants; }
		public void setParticipants(List<UserProfileDto> participants) { this.participants = participants; }
		public Integer getOccupancy() { return occupancy; }
		public void setOccupancy(Integer occupancy) { this.occupancy = occupancy; }
		public Integer getRemainingSlots() { return remainingSlots; }
		public void setRemainingSlots(Integer remainingSlots) { this.remainingSlots = remainingSlots; }
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
