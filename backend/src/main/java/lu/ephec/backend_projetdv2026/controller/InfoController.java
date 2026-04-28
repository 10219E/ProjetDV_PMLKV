package lu.ephec.backend_projetdv2026.controller;

import lu.ephec.backend_projetdv2026.dto.InfoControllerDto;
import lu.ephec.backend_projetdv2026.services.FieldService;
import lu.ephec.backend_projetdv2026.services.SiteService;
import lu.ephec.backend_projetdv2026.services.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.stream.Collectors;
import lu.ephec.backend_projetdv2026.models.Site;

@RestController
@RequestMapping("/api")
public class InfoController {

	private final SiteService siteService;
	private final FieldService fieldService;
    private static final Logger logger = LoggerFactory.getLogger(InfoController.class);
	private final UserService userService;

	public InfoController(SiteService siteService, FieldService fieldService, UserService userService) {
		this.siteService = siteService;
		this.fieldService = fieldService;
		this.userService = userService;
	}

	//SEND COUNTS TO FE
	@GetMapping(value = "/fscount", produces = "application/json")
	public InfoControllerDto getSitesAndFieldsCount() {
		Integer sites = siteService.countSites();
		Integer fields = fieldService.countFields();
		// log both counts
		logger.info("[INFO CONTROLLER] Sites count: {}, Fields count: {} to display on home page.", sites, fields);
		return new InfoControllerDto(sites, fields);
	}

	//GET ONLY NEEDED INFO SITES
	@GetMapping(value = "/sitelist", produces = "application/json")
	public InfoControllerDto getSites() {
		List<Site> activeSites = siteService.fetchAllActive();
		logger.info("[INFO CONTROLLER] Found {} active sites.", activeSites.size());
		List<InfoControllerDto.SiteInfo> siteInfoList = activeSites.stream()
				.map(site -> new InfoControllerDto.SiteInfo(site.getSiteId(), site.getName(), site.getAddress()))
				.collect(Collectors.toList());
		return new InfoControllerDto(siteInfoList);
	}

	//GET USER MATRICULE
	@GetMapping(value="/identify", produces = "application/json")
	public InfoControllerDto getUserMatByEmail(@RequestParam("email") String email) {
		String matricule = userService.fetchByMail(email).orElseThrow().getMatricule();
		logger.info("[INFO CONTROLLER] Identified user with email {} as matricule {}", email, matricule);
		return new InfoControllerDto(matricule);
	}

}
