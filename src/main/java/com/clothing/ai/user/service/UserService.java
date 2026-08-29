package com.clothing.ai.user.service;

import com.clothing.ai.common.exception.*;
import com.clothing.ai.user.dto.AuthDtos.*;
import com.clothing.ai.user.dto.AddressDtos.*;
import com.clothing.ai.user.entity.Address;
import com.clothing.ai.user.entity.User;
import com.clothing.ai.user.repository.AddressRepository;
import com.clothing.ai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    @Lazy private final AuthService authService;

    @Transactional(readOnly = true)
    public User getOrThrow(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User","id",id));
    }

    @Transactional
    public UserResponse updateProfile(UUID id, UserResponse req) {
        User u = getOrThrow(id);
        if (req.firstName() != null) u.setFirstName(req.firstName());
        if (req.lastName() != null) u.setLastName(req.lastName());
        if (req.phone() != null) u.setPhone(req.phone());
        if (req.avatarUrl() != null) u.setAvatarUrl(req.avatarUrl());
        if (req.heightCm() != null) u.setHeightCm(req.heightCm());
        if (req.weightKg() != null) u.setWeightKg(req.weightKg());
        if (req.gender() != null && !req.gender().isBlank()) u.setGender(User.Gender.valueOf(req.gender()));
        return authService.toUserResponse(u);
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> listAddresses(UUID userId) {
        return addressRepository.findByUserId(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AddressResponse createAddress(UUID userId, AddressRequest req) {
        User u = getOrThrow(userId);
        Address a = Address.builder()
                .user(u).label(req.label()).fullName(req.fullName()).phone(req.phone())
                .line1(req.line1()).line2(req.line2()).city(req.city())
                .stateProvince(req.stateProvince()).postalCode(req.postalCode()).country(req.country())
                .defaultAddress(req.defaultAddress())
                .build();
        if (req.defaultAddress()) clearDefaults(userId);
        a = addressRepository.save(a);
        return toResponse(a);
    }

    @Transactional
    public AddressResponse updateAddress(UUID userId, UUID addrId, AddressRequest req) {
        Address a = addressRepository.findById(addrId).orElseThrow(() -> new ResourceNotFoundException("Address","id",addrId));
        if (!a.getUser().getId().equals(userId)) throw new ForbiddenException("Not your address");
        a.setLabel(req.label()); a.setFullName(req.fullName()); a.setPhone(req.phone());
        a.setLine1(req.line1()); a.setLine2(req.line2()); a.setCity(req.city());
        a.setStateProvince(req.stateProvince()); a.setPostalCode(req.postalCode()); a.setCountry(req.country());
        a.setDefaultAddress(req.defaultAddress());
        if (req.defaultAddress()) clearDefaults(userId);
        return toResponse(a);
    }

    @Transactional
    public void deleteAddress(UUID userId, UUID addrId) {
        Address a = addressRepository.findById(addrId).orElseThrow(() -> new ResourceNotFoundException("Address","id",addrId));
        if (!a.getUser().getId().equals(userId)) throw new ForbiddenException("Not your address");
        addressRepository.delete(a);
    }

    private void clearDefaults(UUID userId) {
        addressRepository.findByUserId(userId).forEach(a -> a.setDefaultAddress(false));
    }

    private AddressResponse toResponse(Address a) {
        return new AddressResponse(a.getId(), a.getLabel(), a.getFullName(), a.getPhone(),
                a.getLine1(), a.getLine2(), a.getCity(), a.getStateProvince(),
                a.getPostalCode(), a.getCountry(), a.isDefaultAddress());
    }
}
