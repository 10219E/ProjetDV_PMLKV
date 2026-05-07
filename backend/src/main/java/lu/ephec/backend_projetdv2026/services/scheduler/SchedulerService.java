package lu.ephec.backend_projetdv2026.services.scheduler;

import lu.ephec.backend_projetdv2026.models.Match;
import lu.ephec.backend_projetdv2026.repo.JPAMatchRepo;
import lu.ephec.backend_projetdv2026.repo.JPAMatchPlayersRepo;
import lu.ephec.backend_projetdv2026.repo.JPAMatchPaymentsRepo;
import lu.ephec.backend_projetdv2026.models.MatchPlayers;
import lu.ephec.backend_projetdv2026.models.MatchPayments;
import lu.ephec.backend_projetdv2026.services.PaymentService;
import lu.ephec.backend_projetdv2026.models.UserPenalties;
import lu.ephec.backend_projetdv2026.repo.JPAUserAccountsRepo;
import lu.ephec.backend_projetdv2026.repo.JPAUserPenaltiesRepo;
import lu.ephec.backend_projetdv2026.models.UserAccounts;
import java.time.LocalDateTime;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class SchedulerService {

	private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);
	private final Lock schedulerLock = new ReentrantLock();

	private final JPAMatchRepo jpaMatchRepo;
	private final JPAMatchPlayersRepo jpaMatchPlayersRepo;
	private final JPAMatchPaymentsRepo jpaMatchPaymentsRepo;
	private final PaymentService paymentService;
	private final PenaltyHelperService penaltyHelperService;
	private final JPAUserAccountsRepo jpaUserAccountsRepo;
	private final JPAUserPenaltiesRepo jpaUserPenaltiesRepo;

	public SchedulerService(JPAMatchRepo jpaMatchRepo, JPAMatchPlayersRepo jpaMatchPlayersRepo, JPAMatchPaymentsRepo jpaMatchPaymentsRepo, PaymentService paymentService, PenaltyHelperService penaltyHelperService, JPAUserAccountsRepo jpaUserAccountsRepo, JPAUserPenaltiesRepo jpaUserPenaltiesRepo) {
		this.jpaMatchRepo = jpaMatchRepo;
		this.jpaMatchPlayersRepo = jpaMatchPlayersRepo;
		this.jpaMatchPaymentsRepo = jpaMatchPaymentsRepo;
		this.paymentService = paymentService;
		this.penaltyHelperService = penaltyHelperService;
		this.jpaUserAccountsRepo = jpaUserAccountsRepo;
		this.jpaUserPenaltiesRepo = jpaUserPenaltiesRepo;
	}

	///SCHEDULER RUNS EVERY 5 MIN + LOCK CHECK TO AVOID CONFLICTS WITH MULTIPLE INSTANCES

	//CHECK IF PUBLIC OR PRIVATE MATCH IS FULLY BOOKED AND SETS TO PUBLIC - CLOSED OR PRIVATE - CONFIRMED
	@Scheduled(cron = "0 0/5 * * * *")
	public void confirmMatchPayment() {
		if (!schedulerLock.tryLock()) {
			logger.warn("[Service - Scheduler] confirmMatches is already running, skipping this execution");
			return;
		}
		try {
			logger.info("[Service - Scheduler] Running player count check to confirm match");

			// Initialize counter for updated matches
			int updatedCount = 0;

			//get all matches that are open (public) or awaiting (private)
			List<Match> privmatches = jpaMatchRepo.findByTypeAndPrivStatus("private", "awaiting");
			List<Match> pubmatches = jpaMatchRepo.findByTypeAndPubStatus("public", "open");
			List<Match> matches = new ArrayList<>();
			matches.addAll(privmatches);
			matches.addAll(pubmatches);

			for (Match match : matches) {
				//get all players for the match
				List<MatchPlayers> matchPlayers = jpaMatchPlayersRepo.findByMatch_MatchId(match.getMatchId());
				String matchstatus = match.getType().equals("private") ? match.getPrivStatus() : match.getPubStatus();
				//check each player for matchPlayers if payment (match payments) is clear for the match and user (I could also check MatchPlayer confirmed
				//but checking the actual payment is more accurate as it reflects the financial status, while MatchPlayer status could be out of sync due to various reasons)
				if (matchPlayers.size() == match.getMaxPlayers()) {
					boolean allPaid = true;
					for (MatchPlayers mp : matchPlayers) {
						//check if user is null (can happen in public matches if no player assigned yet)
						if (mp.getUser() == null) {
							allPaid = false;
							break;
						}

						List<MatchPayments> payments = paymentService.fetchByUser(mp.getUser().getMatricule());

						boolean playerPaid = false;
						for (MatchPayments payment : payments) {
							//check for user if he paid for match (status clear) and (payment date not null)
							if (payment.getStatus().equals("clear") && payment.getPaymentDate() != null) {
								playerPaid = true;
								break;
							}
						}

						if (!playerPaid) {
							allPaid = false;
							break;
						}
					}

					if (allPaid) {
						// Update match status if all players have paid
						if (match.getType().equals("private")) {
							match.setPrivStatus("confirmed");
						} else {
							match.setPubStatus("closed");
						}
						updatedCount++;
					} else {
						// Restore original status if not all players have paid
						if (match.getType().equals("private")) {
							match.setPrivStatus(matchstatus);
						} else {
							match.setPubStatus(matchstatus);
						}
					}

					jpaMatchRepo.save(match);
				}
			}

			// Log the number of matches updated
			logger.info("[Service - Scheduler] Player count check to confirm match completed. {} matches were updated.", updatedCount);
		} finally {
			schedulerLock.unlock();
		}
	}

	//MARKS MATCHES AS COMPLETED WHEN THEIR END DATETIME HAS ELAPSED AND THEIR STATUS INDICATES THEY WERE CONFIRMED/CLOSED
	//PREVIOUS MATCH PLAYERS ARE DELETED FROM THE PREVIOUS MATCH
	//EXCEPTION FOR PUBLIC MATCHES - WE ALSO MARK OPENED AS COMPLETED AS WE DON'T HAVE ENOUGH TRAFFIC TO JUSTIFY CANCELLING EACH PUBLIC MATCH INDIVIDUALLY
	@Scheduled(cron = "0 0/5 * * * *")
	public void markElapsedMatchesCompleted() {
		if (!schedulerLock.tryLock()) {
			logger.warn("[Service - Scheduler] markElapsedMatchesCompleted is already running, skipping this execution");
			return;
		}

		try {
			logger.info("[Service - Scheduler] Running mark elapsed matches completed");

			LocalDateTime now = LocalDateTime.now();

			// Process matches in batches to reduce transaction size
			List<Match> matches = jpaMatchRepo.findAll();
			int batchSize = 10;
			int totalUpdated = 0;

			for (int i = 0; i < matches.size(); i += batchSize) {
				List<Match> batch = matches.subList(i, Math.min(i + batchSize, matches.size()));
				if (!batch.isEmpty()) {
					int updated = processMatchBatch(batch, now);
					totalUpdated += updated;
				}
			}

			logger.info("[Service - Scheduler] Completed mark elapsed matches completed — {} matches updated in total", totalUpdated);
		} finally {
			schedulerLock.unlock();
		}
	}

	//PRIVATE MATCH WITH PRIV STATUS 'AWAITING' AND MATCH DATE TOMORROW -> CANCELLED + NEW PUBLIC MATCH CREATED WITH SAME DATETIME, APPROVED PLAYERS COPIED AS APPROVED, OTHERS (NULL) AS PENDING.
	//PREVIOUS MATCH PLAYERS ARE DELETED FROM THE PREVIOUS MATCH
	@Scheduled(cron = "0 0/5 * * * *")
	public void convertAwaitingPrivateMatchesScheduledForTomorrow() {
		if (!schedulerLock.tryLock()) {
			logger.warn("[Service - Scheduler] convertAwaitingPrivateMatchesScheduledForTomorrow is already running, skipping this execution");
			return;
		}

		try {
			logger.info("[Service - Scheduler] Running converting incomplete private matches to public matches");

			LocalDate tomorrow = LocalDate.now().plusDays(1);
			List<Match> matches = jpaMatchRepo.findAll();

			int created = 0;
			for (Match m : matches) {
				try {
					if (m == null) continue;
					if (!"private".equals(m.getType())) continue;
					if (m.getMatchDate() == null) continue;
					if (!m.getMatchDate().isEqual(tomorrow)) continue;
					if (!"awaiting".equals(m.getPrivStatus())) continue;

					created += processPrivateMatchConversion(m);
				} catch (Exception ex) {
					logger.error("[Service - Scheduler] Error converting match id {}: {}", m.getMatchId(), ex.getMessage(), ex);
				}
			}

			logger.info("[Service - Scheduler] Completed private to public conversion run — {} new public matches created", created);
		} finally {
			schedulerLock.unlock();
		}
	}



	//APPLIES DEBT STATUS TO NEGATIVE BALANCE USERS AND CREATES A PENALTY IF THEY DON'T HAVE AN ACTIVE UNPAID_BALANCE PENALTY
	@Scheduled(cron = "0 0/5 * * * *")
	public void applyPenaltiesForDebtors() {
		if (!schedulerLock.tryLock()) {
			logger.warn("[Service - Scheduler] applyPenaltiesForDebtors is already running, skipping this execution");
			return;
		}

		try {
			logger.info("[Service - Scheduler] Running applyPenaltiesForDebtors");

			List<UserAccounts> debtors = jpaUserAccountsRepo.findAllDebtorsWithDetails();
			int penalized = 0;

			// Process debtors in batches
			int batchSize = 5;
			for (int i = 0; i < debtors.size(); i += batchSize) {
				List<UserAccounts> batch = debtors.subList(i, Math.min(i + batchSize, debtors.size()));
				penalized += processDebtorBatch(batch);
			}

			logger.info("[Service - Scheduler] Completed applyPenaltiesForDebtors — {} users penalized", penalized);
		} finally {
			schedulerLock.unlock();
		}
	}

	//CHECKS PENALTY EXPIRATION AND INACTIVATES THEM
	@Scheduled(cron = "0 0/5 * * * *")
	public void expireFinishedPenalties() {
		if (!schedulerLock.tryLock()) {
			logger.warn("[Service - Scheduler] expireFinishedPenalties is already running, skipping this execution");
			return;
		}

		try {
			logger.info("[Service - Scheduler] Running expireFinishedPenalties");

			LocalDateTime now = LocalDateTime.now();
			List<UserPenalties> active = jpaUserPenaltiesRepo.findAllByIsActiveTrue();
			int expired = 0;
			for (UserPenalties p : active) {
				try {
					if (p == null || p.getEndDate() == null) continue;
					if (now.isAfter(p.getEndDate())) {
						p.setIsActive(false);
						jpaUserPenaltiesRepo.save(p);
						expired++;
						logger.info("[Service - Scheduler] Expired penalty {} for user {}", p.getTr(), p.getUser() != null ? p.getUser().getMatricule() : "<null>");
					}
				} catch (Exception ex) {
					logger.error("[Service - Scheduler] Failed processing penalty {} : {}", p != null ? p.getTr() : "<null>", ex.getMessage(), ex);
				}
			}

			logger.info("[Service - Scheduler] Completed expireFinishedPenalties — {} penalties expired", expired);
		} finally {
			schedulerLock.unlock();
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	protected int processPrivateMatchConversion(Match m) {
		// Cancel the private match
		m.setPrivStatus("cancelled");
		jpaMatchRepo.save(m);

		// Create new public match
		Match pub = new Match();
		pub.setField(m.getField());
		pub.setType("public");
		pub.setMatchDate(m.getMatchDate());
		pub.setStartTime(m.getStartTime());
		pub.setEndTime(m.getEndTime());
		pub.setMinPlayers(m.getMinPlayers());
		pub.setMaxPlayers(m.getMaxPlayers());
		pub.setPricing(m.getPricing());
		pub.setOrganiser(null);
		pub.setPrivStatus(null);
		pub.setPubStatus("open");

		Match savedPub = jpaMatchRepo.save(pub);

		// Copy players
		List<MatchPlayers> players = jpaMatchPlayersRepo.findByMatch_MatchId(m.getMatchId());
		for (MatchPlayers p : players) {
			MatchPlayers np = new MatchPlayers();
			np.setMatch(savedPub);
			np.setPlayerRole(p.getPlayerRole());
			if ("approved".equals(p.getStatus()) && p.getUser() != null) {
				np.setUser(p.getUser());
				np.setStatus("approved");
			} else {
				np.setUser(null);
				np.setStatus("pending");
			}
			jpaMatchPlayersRepo.save(np);
		}

		// Calculate organiser debt
		double amountPerInvite = m.getPricing() / (double) Math.max(1, m.getMaxPlayers());
		double totalDebt = 0.0;
		if (m.getOrganiser() != null && m.getOrganiser().getMatricule() != null) {
			for (MatchPlayers p : players) {
				if ("p1".equalsIgnoreCase(p.getPlayerRole())) continue;

				if (p.getUser() == null) {
					totalDebt -= amountPerInvite;
					continue;
				}

				String userId = p.getUser().getMatricule();
				var payOpt = jpaMatchPaymentsRepo.findByUser_MatriculeAndMatch_MatchId(userId, m.getMatchId());
				if (payOpt.isPresent()) {
					MatchPayments pay = payOpt.get();
					String status = pay.getStatus();
					if (status == null || !(status.equals("clear") || status.equals("cancelled"))) {
						totalDebt -= amountPerInvite;
					}
				} else {
					totalDebt -= amountPerInvite;
				}
			}

			if (totalDebt > 0) {
				MatchPlayers p1 = players.stream().filter(pp -> "p1".equalsIgnoreCase(pp.getPlayerRole())).findFirst().orElse(null);
				boolean p1IsOrganiser = false;
				if (p1 != null && p1.getUser() != null && m.getOrganiser() != null && m.getOrganiser().getMatricule() != null) {
					p1IsOrganiser = p1.getUser().getMatricule().equals(m.getOrganiser().getMatricule());
				}

				if (!p1IsOrganiser) {
					logger.warn("[Service - Scheduler] Organiser mismatch for match {}: organiser matricule {} does not match p1 matricule {}. Skipping applying debt/penalty.", m.getMatchId(),
							m.getOrganiser() != null ? m.getOrganiser().getMatricule() : "<null>",
							p1 != null && p1.getUser() != null ? p1.getUser().getMatricule() : "<null>");
				} else {
					String organiserId = m.getOrganiser().getMatricule();
					try {
						paymentService.updateAccountStatus(organiserId, "debt", totalDebt);
						logger.info("[Service - Scheduler] Applied debt {} to organiser {} for cancelled private match {}", totalDebt, organiserId, m.getMatchId());

						// Create penalty
						try {
							UserPenalties penalty = new UserPenalties();
							penalty.setUser(m.getOrganiser());
							penalty.setMatchId(m.getMatchId());
							penalty.setReason("insufficient_players");
							LocalDateTime start = LocalDateTime.now();
							penalty.setStartDate(start);
							penalty.setEndDate(start.plusWeeks(1));
							penalty.setIsActive(true);
							penalty.setDescription("[Service - Scheduler] Penalty applied due to unpaid invites for match " + m.getMatchId());
							penaltyHelperService.createPenaltyNewTransaction(penalty);
							logger.info("[Service - Scheduler] Applied penalty to organiser {} for match {} (expires {})", organiserId, m.getMatchId(), penalty.getEndDate());
						} catch (Exception ex) {
							logger.error("[Service - Scheduler] Failed to create penalty for organiser {}: {}", organiserId, ex.getMessage(), ex);
						}
					} catch (Exception ex) {
						logger.error("[Service - Scheduler] Failed to apply debt to organiser {} for match {}: {}", m.getOrganiser().getMatricule(), m.getMatchId(), ex.getMessage(), ex);
					}
				}
			}

			// Transfer payments
			List<MatchPayments> oldPayments = jpaMatchPaymentsRepo.findByMatch_MatchId(m.getMatchId());
			for (MatchPayments pay : oldPayments) {
				try {
					if ("clear".equals(pay.getStatus())) {
						pay.setMatch(savedPub);
						jpaMatchPaymentsRepo.save(pay);
						logger.debug("[Service - Scheduler] Moved cleared payment tr {} to new match {}", pay.getTr(), savedPub.getMatchId());
					} else {
						pay.setStatus("cancelled");
						jpaMatchPaymentsRepo.save(pay);
						logger.debug("[Service - Scheduler] Cancelled payment tr {} for old match {}", pay.getTr(), m.getMatchId());
					}
				} catch (Exception ex) {
					logger.error("[Service - Scheduler] Failed to transfer/cancel payment tr {} for match {}: {}", pay.getTr(), m.getMatchId(), ex.getMessage(), ex);
				}
			}

			// After transferring payments, delete all MatchPlayers entries for the old (cancelled) match
			try {
				jpaMatchPlayersRepo.deleteByMatch_MatchId(m.getMatchId());
				logger.debug("[Service - Scheduler] Deleted MatchPlayers for cancelled match {}", m.getMatchId());
			} catch (Exception ex) {
				logger.error("[Service - Scheduler] Failed to delete MatchPlayers for cancelled match {}: {}", m.getMatchId(), ex.getMessage(), ex);
			}
		}

		logger.info("[Service - Scheduler] Converted private match {} to public match {}", m.getMatchId(), savedPub.getMatchId());
		return 1;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	protected int processMatchBatch(List<Match> batch, LocalDateTime now) {
		int updated = 0;
		for (Match m : batch) {
			try {
				if (m.getMatchDate() == null || m.getEndTime() == null) continue;

				LocalDateTime matchEnd = LocalDateTime.of(m.getMatchDate(), m.getEndTime());
				boolean elapsed = !matchEnd.isAfter(now);

				if (!elapsed) continue;

				boolean shouldComplete = false;
				if ("private".equals(m.getType()) && "confirmed".equals(m.getPrivStatus())) {
					shouldComplete = true;
				}//WORKAROUND INCLUDING OPEN AS WE DON'T HAVE ENOUGH TRAFFIC TO JUSTIFY CANCELLING EACH PUBLIC MATCH
				if ("public".equals(m.getType()) && ("closed".equals(m.getPubStatus()) || "open".equals(m.getPubStatus()))) {
					shouldComplete = true;
				}
				// Safe check for null values
				if ((m.getPrivStatus() != null && "confirmed".equals(m.getPrivStatus())) ||
						(m.getPubStatus() != null && ("closed".equals(m.getPubStatus()) || "open".equals(m.getPubStatus())))) {
					shouldComplete = true;
				}

				if (shouldComplete) {
					if ("private".equals(m.getType())) {
						m.setPrivStatus("completed");
						updated++;
					} else if ("public".equals(m.getType())) {
						m.setPubStatus("completed");
						updated++;
					} else {
						//m.setPrivStatus("completed");
						//m.setPubStatus("completed");
					}


					jpaMatchRepo.save(m);
					// When a match is completed, remove all associated MatchPlayers entries
					try {
						jpaMatchPlayersRepo.deleteByMatch_MatchId(m.getMatchId());
						logger.debug("[Service - Scheduler] Deleted MatchPlayers for completed match {}", m.getMatchId());
					} catch (Exception ex) {
						logger.error("[Service - Scheduler] Failed to delete MatchPlayers for completed match {}: {}", m.getMatchId(), ex.getMessage(), ex);
					}
					logger.debug("[Service - Scheduler] Marked match {} as completed", m.getMatchId());
				}
			} catch (Exception ex) {
				logger.error("[Service - Scheduler] Error processing match id {}: {}", m.getMatchId(), ex.getMessage(), ex);
			}
		}
		logger.info("[Service - Scheduler] Batch processed — {} matches updated", updated);
		return updated;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	protected int processDebtorBatch(List<UserAccounts> batch) {
		int penalized = 0;
		for (UserAccounts ua : batch) {
			try {
				if (ua == null || ua.getUser() == null) continue;
				Double balance = ua.getBalance();
				if (balance == null || balance >= 0) continue;
				String userId = ua.getUser().getMatricule();

				boolean hasUnpaidBalance = jpaUserPenaltiesRepo.existsActivePenaltyByReason(userId, "unpaid_balance", LocalDateTime.now());

				if (hasUnpaidBalance) {
					logger.debug("[Service - Scheduler] User {} already has active unpaid_balance penalty, skipping", userId);
					continue;
				}

				UserPenalties penalty = new UserPenalties();
				penalty.setUser(ua.getUser());
				penalty.setMatchId(null);
				penalty.setReason("unpaid_balance");
				LocalDateTime start = LocalDateTime.now();
				penalty.setStartDate(start);
				penalty.setEndDate(start.plusYears(5));
				penalty.setIsActive(true);
				penalty.setDescription("Penalty for unpaid balance (auto-applied by scheduler)");

				penaltyHelperService.createPenaltyNewTransaction(penalty);
				penalized++;
				logger.info("[Service - Scheduler] Applied unpaid_balance penalty to user {} (expires {})", userId, penalty.getEndDate());
			} catch (Exception ex) {
				logger.error("[Service - Scheduler] Failed to apply unpaid_balance penalty to user {}: {}", ua.getUser() != null ? ua.getUser().getMatricule() : "<null>", ex.getMessage(), ex);
			}
		}
		return penalized;
	}
}