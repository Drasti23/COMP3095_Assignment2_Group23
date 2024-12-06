package ca.gbc.approvalservice.service;

import ca.gbc.approvalservice.dto.ApprovalRequest;
import ca.gbc.approvalservice.dto.ApprovalResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import java.util.List;
import java.util.Optional;

public interface ApprovalService {
    String createApproval(ApprovalRequest approvalRequest,boolean isApproved);
    List<ApprovalResponse> getAllApprovals();

    @CircuitBreaker(name = "eventServiceCircuitBreaker", fallbackMethod = "eventServiceFallback")
    String geteventinfo(String eventId);

    Optional<ApprovalResponse> getApprovalById(Long id);
    Optional<ApprovalResponse> updateApproval(Long id, ApprovalRequest approvalRequest);
    boolean deleteApproval(Long id);
}
