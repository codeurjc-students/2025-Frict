package com.tfg.backend.event;

import com.tfg.backend.model.Registry;
import com.tfg.backend.service.RegistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RegistryEventListener {

    private final RegistryService registryService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRegistryEvent(RegistryEvent event) {

        Registry registry = event.getRegistry();
        Registry.Metadata meta = registry.getMetadata();
        Registry.Metrics metrics = registry.getMetrics();

        String primaryEntityId = switch (meta.getEntityType()) {
            case SHOP -> meta.getShopId();
            case PRODUCT -> meta.getProductId();
            case USER -> meta.getUserId();
            case ORDER -> meta.getOrderId();
            default -> null;
        };

        Double newTotal = registryService.calculateNextTotal(
                meta.getEntityType(),
                primaryEntityId,
                meta.getDataType(),
                metrics.getValue()
        );

        metrics.setTotal(newTotal);
        registryService.save(registry);
    }
}