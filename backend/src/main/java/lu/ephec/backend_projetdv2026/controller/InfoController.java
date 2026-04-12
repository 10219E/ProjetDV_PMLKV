package lu.ephec.backend_projetdv2026.controller;

import lu.ephec.backend_projetdv2026.dto.InfoControllerDto;
import lu.ephec.backend_projetdv2026.services.FieldService;
import lu.ephec.backend_projetdv2026.services.SiteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class InfoController {

	private final SiteService siteService;
	private final FieldService fieldService;
    private static final Logger logger = LoggerFactory.getLogger(InfoController.class);

	public InfoController(SiteService siteService, FieldService fieldService) {
		this.siteService = siteService;
		this.fieldService = fieldService;
	}

	//SEND COUNTS TO FE
	@GetMapping("/fscount")
	public InfoControllerDto getSitesAndFieldsCount() {
		Integer sites = siteService.countSites();
		Integer fields = fieldService.countFields();
		// log both counts
		logger.info("Sites count: {}, Fields count: {}", sites, fields);
		return new InfoControllerDto(sites, fields);
	}
}
