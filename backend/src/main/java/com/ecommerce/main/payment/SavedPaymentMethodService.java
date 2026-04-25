package com.ecommerce.main.payment;

import com.ecommerce.main.user.User;
import com.ecommerce.main.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SavedPaymentMethodService {

    private final SavedPaymentMethodRepository repository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<SavedPaymentMethodResponse> getMyMethods(String email) {
        return repository.findByUserEmailOrderByIsDefaultDescCreatedAtDesc(email)
                .stream().map(SavedPaymentMethodResponse::from).toList();
    }

    public SavedPaymentMethodResponse add(String email, SavedPaymentMethodRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isFirst = repository.findByUserEmailOrderByIsDefaultDescCreatedAtDesc(email).isEmpty();

        if (req.isDefault() || isFirst) {
            repository.clearDefaultByUserEmail(email);
        }

        SavedPaymentMethod m = SavedPaymentMethod.builder()
                .user(user)
                .type(req.getType())
                .label(req.getLabel())
                .cardLast4(req.getCardLast4())
                .cardBrand(req.getCardBrand())
                .stripePaymentMethodId(req.getStripePaymentMethodId())
                .paypalEmail(req.getPaypalEmail())
                .walletAddress(req.getWalletAddress())
                .chainId(req.getChainId())
                .chainName(req.getChainName())
                .isDefault(isFirst || req.isDefault())
                .build();

        return SavedPaymentMethodResponse.from(repository.save(m));
    }

    public void delete(String email, Long id) {
        SavedPaymentMethod m = repository.findByIdAndUserEmail(id, email)
                .orElseThrow(() -> new RuntimeException("Ödeme yöntemi bulunamadı."));
        repository.delete(m);
    }

    public SavedPaymentMethodResponse setDefault(String email, Long id) {
        repository.clearDefaultByUserEmail(email);
        SavedPaymentMethod m = repository.findByIdAndUserEmail(id, email)
                .orElseThrow(() -> new RuntimeException("Ödeme yöntemi bulunamadı."));
        m.setDefault(true);
        return SavedPaymentMethodResponse.from(repository.save(m));
    }
}
