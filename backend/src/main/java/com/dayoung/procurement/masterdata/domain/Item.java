package com.dayoung.procurement.masterdata.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "item")
public class Item {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 50)
	private String code;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(length = 255)
	private String specification;

	@Column(nullable = false, length = 20)
	private String unit;

	@Column(name = "standard_unit_price", nullable = false, precision = 19, scale = 2)
	private BigDecimal standardUnitPrice;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	protected Item() {
	}

	public Item(String code, String name, String specification, String unit, BigDecimal standardUnitPrice) {
		this.code = code;
		this.name = name;
		this.specification = specification;
		this.unit = unit;
		this.standardUnitPrice = standardUnitPrice;
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

	public String getSpecification() {
		return specification;
	}

	public String getUnit() {
		return unit;
	}

	public BigDecimal getStandardUnitPrice() {
		return standardUnitPrice;
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
