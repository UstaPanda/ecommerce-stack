package com.ecommerce.main.address;

import com.ecommerce.main.user.User;
import com.ecommerce.main.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserAddressService {

    private final UserAddressRepository addressRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserAddressResponse> getMyAddresses(String email) {
        return addressRepository.findByUserEmailOrderByIsDefaultDescCreatedAtDesc(email)
                .stream().map(UserAddressResponse::from).toList();
    }

    public UserAddressResponse create(String email, UserAddressRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (req.isDefault()) {
            addressRepository.clearDefaultByUserEmail(email);
        }

        // If this is the first address, make it default automatically
        boolean isFirst = addressRepository.findByUserEmailOrderByIsDefaultDescCreatedAtDesc(email).isEmpty();

        UserAddress address = UserAddress.builder()
                .user(user)
                .title(req.getTitle())
                .fullAddress(req.getFullAddress())
                .city(req.getCity())
                .district(req.getDistrict())
                .postalCode(req.getPostalCode())
                .isDefault(isFirst || req.isDefault())
                .build();

        return UserAddressResponse.from(addressRepository.save(address));
    }

    public UserAddressResponse update(String email, Long id, UserAddressRequest req) {
        UserAddress address = addressRepository.findById(id)
                .filter(a -> a.getUser().getEmail().equals(email))
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (req.isDefault()) {
            addressRepository.clearDefaultByUserEmail(email);
        }

        address.setTitle(req.getTitle());
        address.setFullAddress(req.getFullAddress());
        address.setCity(req.getCity());
        address.setDistrict(req.getDistrict());
        address.setPostalCode(req.getPostalCode());
        address.setDefault(req.isDefault());

        return UserAddressResponse.from(addressRepository.save(address));
    }

    public void delete(String email, Long id) {
        UserAddress address = addressRepository.findById(id)
                .filter(a -> a.getUser().getEmail().equals(email))
                .orElseThrow(() -> new RuntimeException("Address not found"));
        addressRepository.delete(address);
    }

    public UserAddressResponse setDefault(String email, Long id) {
        addressRepository.clearDefaultByUserEmail(email);
        UserAddress address = addressRepository.findById(id)
                .filter(a -> a.getUser().getEmail().equals(email))
                .orElseThrow(() -> new RuntimeException("Address not found"));
        address.setDefault(true);
        return UserAddressResponse.from(addressRepository.save(address));
    }
}
