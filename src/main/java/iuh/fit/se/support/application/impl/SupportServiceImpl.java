package iuh.fit.se.support.application.impl;

import iuh.fit.se.support.api.dto.CreateSupportRequest;
import iuh.fit.se.support.api.dto.SupportResponse;
import iuh.fit.se.support.application.SupportService;
import iuh.fit.se.support.domain.SupportRequest;
import iuh.fit.se.support.repository.SupportRequestRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SupportServiceImpl implements SupportService {

    private final SupportRequestRepository repository;
    private final SimpMessagingTemplate messagingTemplate;

    public SupportServiceImpl(SupportRequestRepository repository, SimpMessagingTemplate messagingTemplate) {
        this.repository = repository;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public SupportResponse create(CreateSupportRequest request) {
        SupportRequest entity = new SupportRequest(request.message(), request.tableCode(), request.qrSessionId());
        entity = repository.save(entity);
        SupportResponse response = map(entity);
        messagingTemplate.convertAndSend("/topic/waiter/support-request", response);
        return response;
    }

    @Override
    public SupportResponse get(Long id) {
        SupportRequest entity = repository.findById(id).orElseThrow();
        return map(entity);
    }

    @Override
    public List<SupportResponse> listByTable(String tableCode) {
        return repository.findByTableCodeOrderByCreatedAtDesc(tableCode).stream().map(this::map).collect(Collectors.toList());
    }

    @Override
    public List<SupportResponse> listAll() {
        return repository.findAll().stream().map(this::map).collect(Collectors.toList());
    }

    @Override
    public SupportResponse assign(Long id, Long staffId) {
        SupportRequest entity = repository.findById(id).orElseThrow();
        entity.setAssignedToStaffId(staffId);
        entity.setStatus(iuh.fit.se.support.domain.SupportRequestStatus.ASSIGNED);
        repository.save(entity);
        return map(entity);
    }

    @Override
    public SupportResponse updateStatus(Long id, iuh.fit.se.support.domain.SupportRequestStatus status) {
        SupportRequest entity = repository.findById(id).orElseThrow();
        entity.setStatus(status);
        repository.save(entity);
        return map(entity);
    }

    private SupportResponse map(SupportRequest r) {
        return new SupportResponse(r.getId(), r.getMessage(), r.getTableCode(), r.getQrSessionId(), r.getStatus(), r.getAssignedToStaffId(), r.getCreatedAt(), r.getUpdatedAt());
    }
}
