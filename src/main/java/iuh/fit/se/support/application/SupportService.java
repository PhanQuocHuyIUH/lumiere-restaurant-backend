package iuh.fit.se.support.application;

import iuh.fit.se.support.api.dto.CreateSupportRequest;
import iuh.fit.se.support.api.dto.SupportResponse;
import java.util.List;

public interface SupportService {
    SupportResponse create(CreateSupportRequest request);
    SupportResponse get(Long id);
    List<SupportResponse> listByTable(String tableCode);
    List<SupportResponse> listAll();
    SupportResponse assign(Long id, Long staffId);
    SupportResponse updateStatus(Long id, iuh.fit.se.support.domain.SupportRequestStatus status);
}
