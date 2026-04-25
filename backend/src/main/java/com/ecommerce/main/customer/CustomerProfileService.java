package com.ecommerce.main.customer;

import com.ecommerce.main.user.User;
import com.ecommerce.main.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerProfileService {

    private final CustomerProfileRepository customerProfileRepository;
    private final UserRepository userRepository;

    public CustomerProfile getMyProfile(String email) {
        return customerProfileRepository.findByUserEmail(email)
                .orElseGet(() -> createEmptyProfile(email));
    }

    public CustomerProfile getById(Long id) {
        return customerProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Profile not found: " + id));
    }

    public Page<CustomerProfile> getAll(Pageable pageable) {
        return customerProfileRepository.findAll(pageable);
    }

    public CustomerProfile updateMyProfile(String email, CustomerProfileRequest request) {
        CustomerProfile profile = customerProfileRepository.findByUserEmail(email)
                .orElseGet(() -> createEmptyProfile(email));
        applyUpdate(profile, request);
        return customerProfileRepository.save(profile);
    }

    private CustomerProfile createEmptyProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        CustomerProfile profile = CustomerProfile.builder().user(user).build();
        return customerProfileRepository.save(profile);
    }

    private void applyUpdate(CustomerProfile profile, CustomerProfileRequest request) {
        if (request.getGender() != null) profile.setGender(request.getGender());
        if (request.getAge() != null) profile.setAge(request.getAge());
        if (request.getCity() != null) profile.setCity(request.getCity());
        if (request.getMembershipType() != null) profile.setMembershipType(request.getMembershipType());
        if (request.getSatisfactionLevel() != null) profile.setSatisfactionLevel(request.getSatisfactionLevel());
    }
}
