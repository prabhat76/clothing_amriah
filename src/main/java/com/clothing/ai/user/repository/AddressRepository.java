package com.clothing.ai.user.repository;

import com.clothing.ai.user.entity.Address;
import com.clothing.ai.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {
    List<Address> findByUser(User user);
    List<Address> findByUserId(UUID userId);
}
