package com.repl.asset.master.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.repl.asset.constant.Constants;
import com.repl.asset.exception.ApplicationException;
import com.repl.asset.master.bo.IpAddressBO;
import com.repl.asset.master.model.IpAddressMaster;
import com.repl.asset.master.repository.IpAddressMasterRepository;
import com.repl.asset.message.IResponseCode;

/**
 * IP address master CRUD, over ams_ip_address_master.
 *
 * Its own service rather than another AbstractSpecService subclass: that base class works on
 * a single name, while this row has a name AND an address, and it is the ADDRESS that has to
 * be unique.
 */
@Service
public class IpAddressService {

	@Autowired
	private IpAddressMasterRepository ipAddressMasterRepository;

	/** An IPv4 address: four numbers 0-255 joined by dots, e.g. 192.168.10.55. */
	private static final Pattern IPV4 = Pattern
			.compile("^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$");

	/**
	 * Active AND inactive, everything but deleted.
	 *
	 * Both, because the master screen needs an inactive row visible to switch it back on.
	 * Screens that OFFER an address filter to status = 1 themselves.
	 */
	@Transactional(readOnly = true)
	public List<IpAddressBO> list() {
		return ipAddressMasterRepository.findAll().stream()
				.filter(row -> row.getStatus() != Constants.STATUS_DELETED)
				.sorted(Comparator.comparing(IpAddressMaster::getIpAddress,
						String.CASE_INSENSITIVE_ORDER))
				.map(this::toBO)
				.toList();
	}

	@Transactional
	public IpAddressBO save(IpAddressBO in) throws ApplicationException {
		String address = requireAddress(in);

		IpAddressMaster existing = findIncludingDeleted(address);

		if (existing != null) {
			/*
			 * uk_ip_address is UNIQUE, so a soft-deleted row still occupies that address and a
			 * second insert is impossible. Re-adding an address that was deleted therefore
			 * revives that row — refusing would be wrong, since the screen shows nothing there.
			 */
			if (existing.getStatus() != Constants.STATUS_DELETED) {
				throw new ApplicationException(IResponseCode.EXISTING_ENTITY,
						"\"" + address + "\" already exists.");
			}
			apply(existing, in, address);
			existing.setCreateDate(LocalDate.now());
			existing.setCreateTime(LocalTime.now().withNano(0));
			existing.setRegTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
			return toBO(ipAddressMasterRepository.save(existing));
		}

		IpAddressMaster row = new IpAddressMaster();
		apply(row, in, address);
		// Stamped here, never taken from the request.
		row.setCreateDate(LocalDate.now());
		row.setCreateTime(LocalTime.now().withNano(0));
		row.setRegTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));

		return toBO(ipAddressMasterRepository.save(row));
	}

	@Transactional
	public IpAddressBO update(IpAddressBO in) throws ApplicationException {
		if (in == null || in.getId() == null) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "Nothing to update.");
		}
		String address = requireAddress(in);

		IpAddressMaster row = ipAddressMasterRepository.findById(in.getId())
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That IP address no longer exists."));

		// Ignores the row being edited, so re-saving it unchanged is allowed.
		IpAddressMaster holder = findIncludingDeleted(address);
		if (holder != null && !holder.getId().equals(row.getId())) {
			throw new ApplicationException(IResponseCode.EXISTING_ENTITY,
					holder.getStatus() == Constants.STATUS_DELETED
							? "\"" + address + "\" was previously used and deleted. Add it again "
									+ "from ADD rather than renaming this row onto it."
							: "\"" + address + "\" already exists.");
		}

		apply(row, in, address);
		return toBO(ipAddressMasterRepository.save(row));
	}

	@Transactional
	public void delete(Long id) throws ApplicationException {
		IpAddressMaster row = ipAddressMasterRepository.findById(id)
				.orElseThrow(() -> new ApplicationException(IResponseCode.INVALID_ENTITY,
						"That IP address no longer exists."));

		// Retired, not removed: anything already recording this address still resolves.
		row.setStatus(Constants.STATUS_DELETED);
		ipAddressMasterRepository.save(row);
	}

	/** The fields a form supplies. Status defaults to active when none is sent. */
	private void apply(IpAddressMaster row, IpAddressBO in, String address) {
		row.setIpAddress(address);
		row.setIpName(trimToNull(in.getIp_name()));
		row.setDescription(trimToNull(in.getDescription()));
		row.setStatus(in.getStatus() != null && in.getStatus() == Constants.STATUS_INACTIVE
				? Constants.STATUS_INACTIVE
				: Constants.STATUS_ACTIVE);
	}

	private String requireAddress(IpAddressBO in) throws ApplicationException {
		String address = in == null || in.getIp_address() == null ? "" : in.getIp_address().trim();
		if (address.isEmpty()) {
			throw new ApplicationException(IResponseCode.INVALID_DATA, "IP address is required.");
		}
		if (!IPV4.matcher(address).matches()) {
			throw new ApplicationException(IResponseCode.INVALID_DATA,
					"Enter a valid IP address: four numbers from 0 to 255 separated by dots, e.g. 192.168.10.55.");
		}
		return address;
	}

	/**
	 * The row holding this address, deleted or not, or null.
	 *
	 * Case-insensitive, so a MAC-style address typed in either case counts once. The table's
	 * UNIQUE key is the real guarantee; this exists so the caller gets a readable message
	 * instead of a constraint violation surfacing as a 500.
	 */
	private IpAddressMaster findIncludingDeleted(String address) {
		return ipAddressMasterRepository.findAll().stream()
				.filter(row -> address.equalsIgnoreCase(row.getIpAddress()))
				.findFirst()
				.orElse(null);
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private IpAddressBO toBO(IpAddressMaster row) {
		IpAddressBO bo = new IpAddressBO();
		bo.setId(row.getId());
		bo.setIp_name(row.getIpName());
		bo.setIp_address(row.getIpAddress());
		bo.setDescription(row.getDescription());
		bo.setCreate_date(row.getCreateDate());
		bo.setCreate_time(row.getCreateTime());
		bo.setStatus(row.getStatus());
		return bo;
	}
}
