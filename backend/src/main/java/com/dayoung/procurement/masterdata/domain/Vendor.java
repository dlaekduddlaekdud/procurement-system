package com.dayoung.procurement.masterdata.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "vendor")
public class Vendor {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 30)
	private String code;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(name = "business_registration_number", nullable = false, unique = true, length = 30)
	private String businessRegistrationNumber;

	@Column(name = "representative_name", length = 100)
	private String representativeName;

	@Column(name = "contact_name", length = 100)
	private String contactName;

	@Column(name = "contact_email", length = 255)
	private String contactEmail;

	@Column(name = "contact_phone", length = 30)
	private String contactPhone;

	@Column(length = 255)
	private String address;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	protected Vendor() {
	}

	public Vendor(String code, String name, String businessRegistrationNumber) {
		this.code = code;
		this.name = name;
		this.businessRegistrationNumber = businessRegistrationNumber;
	}

	public Long getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public String getBusinessRegistrationNumber() {
		return businessRegistrationNumber;
	}

	public String getRepresentativeName() {
		return representativeName;
	}

	public String getContactName() {
		return contactName;
	}

	public String getContactEmail() {
		return contactEmail;
	}

	public String getContactPhone() {
		return contactPhone;
	}

	public String getAddress() {
		return address;
	}

	public boolean isActive() {
		return active;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
