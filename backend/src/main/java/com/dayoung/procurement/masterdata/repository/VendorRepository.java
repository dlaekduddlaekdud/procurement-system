package com.dayoung.procurement.masterdata.repository;

import com.dayoung.procurement.masterdata.domain.Vendor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendor, Long> {

	Optional<Vendor> findByCode(String code);

	boolean existsByCode(String code);

	boolean existsByBusinessRegistrationNumber(String businessRegistrationNumber);
}
