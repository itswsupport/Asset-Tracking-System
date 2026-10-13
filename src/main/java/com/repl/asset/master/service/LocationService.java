package com.repl.asset.master.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.repl.asset.exception.ApplicationException;
import com.repl.asset.master.bo.LocationBO;
import com.repl.asset.master.model.LocationMaster;
import com.repl.asset.master.model.PlantMaster;
import com.repl.asset.master.repository.LocationMasterRepository;
import com.repl.asset.master.repository.PlantMasterRepository;
import com.repl.asset.message.IResponseCode;

/**
 * Location master over db_all_master.location_master: list and save.
 *
 * The table is a replica of db_ems_version2.sf_loc_mst's columns, so it carries a plant code
 * (in plant_id) and nothing about the company; both names come from plant_mst by that code.
 */
@Service
public class LocationService {

	@Autowired
	private LocationMasterRepository repository;

	@Autowired
	private PlantMasterRepository plantMasterRepository;

	/** Every location, company -> plant -> location. */
	@Transactional(readOnly = true)
	public List<LocationBO> list() {
		Map<Long, PlantMaster> plants = plantsByCode();
		return repository.findAll().stream()
				.map(row -> toBO(row, plants))
				.sorted(Comparator
						.comparing((LocationBO b) -> nz(b.getCompany_name()))
						.thenComparing(b -> nz(b.getPlant_code()))
						.thenComparing(b -> nz(b.getLocation_name()), String.CASE_INSENSITIVE_ORDER))
				.toList();
	}

	@Transactional
	public LocationBO save(LocationBO in) throws ApplicationException {
		String name = in == null || in.getLocation_name() == null ? "" : in.getLocation_name().trim();
		String code = in == null || in.getPlant_code() == null ? "" : in.getPlant_code().trim();
		if (name.isEmpty() || code.isEmpty()) {
			throw new ApplicationException(IResponseCode.INVALID_DATA,
					"Plant and location name are both required.");
		}

		Integer plantCode;
		try {
			plantCode = Integer.valueOf(code);
		} catch (NumberFormatException e) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Plant code must be a number.");
		}

		// One name per plant, case-insensitively, so "Office" and "OFFICE " are one location.
		boolean taken = repository.findByPlantId(plantCode).stream()
				.anyMatch(r -> r.getLocationName() != null && r.getLocationName().trim().equalsIgnoreCase(name));
		if (taken) {
			throw new ApplicationException(IResponseCode.EXISTING_ENTITY,
					"\"" + name + "\" is already added for plant " + code + ".");
		}

		LocationMaster row = new LocationMaster();
		row.setPlantId(plantCode);
		row.setLocationName(name);
		String hindi = in.getLocation_name_hindi() == null ? "" : in.getLocation_name_hindi().trim();
		row.setLocationNameHindi(hindi.isEmpty() ? null : hindi);
		// reg_by stays null: the backend does not know who is signed in (no token is carried).
		return toBO(repository.save(row), plantsByCode());
	}

	private Map<Long, PlantMaster> plantsByCode() {
		return plantMasterRepository.findAll().stream()
				.filter(p -> p.getPlantCode() != null)
				.collect(Collectors.toMap(PlantMaster::getPlantCode, Function.identity(), (a, b) -> a));
	}

	private String nz(String s) {
		return s == null ? "" : s;
	}

	private LocationBO toBO(LocationMaster row, Map<Long, PlantMaster> plants) {
		LocationBO bo = new LocationBO();
		bo.setId(row.getId());
		bo.setLocation_name(row.getLocationName());
		bo.setLocation_name_hindi(row.getLocationNameHindi());
		if (row.getPlantId() != null) {
			bo.setPlant_code(String.valueOf(row.getPlantId()));
			PlantMaster plant = plants.get(row.getPlantId().longValue());
			if (plant != null) {
				bo.setPlant_name(plant.getPlantName());
				bo.setCompany_name(plant.getCompanyName() == null ? null : plant.getCompanyName().trim());
			}
		}
		return bo;
	}
}
