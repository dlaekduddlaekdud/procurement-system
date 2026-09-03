package com.dayoung.procurement.masterdata.repository;

import com.dayoung.procurement.masterdata.domain.Item;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {

	Optional<Item> findByCode(String code);

	boolean existsByCode(String code);
}
