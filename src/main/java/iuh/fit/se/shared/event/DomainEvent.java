package iuh.fit.se.shared.event;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Getter
public abstract class DomainEvent {

    private final Instant occurredOn = Instant.now();
    private final String eventId = UUID.randomUUID().toString();
}
