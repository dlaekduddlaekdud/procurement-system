package com.dayoung.procurement.masterdata.repository;

import com.dayoung.procurement.masterdata.domain.Warehouse;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

	Optional<Warehouse> findByCode(String code);

	boolean existsByCode(String code);
}
