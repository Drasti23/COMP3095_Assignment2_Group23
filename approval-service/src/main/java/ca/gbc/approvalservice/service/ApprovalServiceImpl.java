package ca.gbc.approvalservice.service;

import ca.gbc.approvalservice.dto.ApprovalRequest;
import ca.gbc.approvalservice.dto.ApprovalResponse;
import ca.gbc.approvalservice.model.Approval;
import ca.gbc.approvalservice.repository.ApprovalRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final RestTemplate restTemplate;

    // URL for EventService
    @Value("${event-service.url}")
    private String eventServiceUrl;  // Event service URL from application.properties or application.yml

    @Override
    public String createApproval(ApprovalRequest approvalRequest,boolean approved) {
        String id = approvalRequest.eventId();
        if(geteventinfo(id).isEmpty())
        {
            return "Event not found";
        }
        else{
            Approval approval = mapRequestToApproval(approvalRequest);
            if(approved){
                approval.setStatus("Approved");
            }
            else{
                approval.setStatus("Rejected");
            }
            approvalRepository.save(approval);
            return "Review added in database successfully";
        }
    }




    @Override
    public List<ApprovalResponse> getAllApprovals() {
        log.debug("Fetching all approvals.");
        List<Approval> approvals = approvalRepository.findAll();
        return approvals.stream().map(this::mapToApprovalResponse).toList();
    }

    @Override
    public String geteventinfo(String eventId) {
        return restTemplate.getForObject(eventServiceUrl + eventId, String.class);
    }

    @Override
    public Optional<ApprovalResponse> getApprovalById(Long id) {
        log.debug("Fetching approval by ID: " + id);
        Optional<Approval> approval = approvalRepository.findById(id);
        return approval.map(this::mapToApprovalResponse);
    }

    @Override
    public Optional<ApprovalResponse> updateApproval(Long id, ApprovalRequest approvalRequest) {
        log.debug("Updating approval for ID: " + id);
        Optional<Approval> approvalOptional = approvalRepository.findById(id);
        if (approvalOptional.isPresent()) {
            Approval approval = approvalOptional.get();
            approval.setNotes(approvalRequest.notes());
            approvalRepository.save(approval);
            return Optional.of(mapToApprovalResponse(approval));
        }
        return Optional.empty();
    }

    @Override
    public boolean deleteApproval(Long id) {
        log.debug("Deleting approval with ID: " + id);
        if (approvalRepository.existsById(id)) {
            approvalRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // Helper methods to map between entities and DTOs

    private ApprovalResponse mapToApprovalResponse(Approval approval) {
        return new ApprovalResponse(
                approval.getId(),
                approval.getEventId(),
                approval.getApprover(),
                approval.getStatus(),
                approval.getNotes()
        );
    }

    private Approval mapRequestToApproval(ApprovalRequest request) {
        return Approval.builder()
                .id(request.id())
                .eventId(request.eventId())
                .approver(request.approver())
                .status("Pending")  // Default to "Pending" status when created
                .notes(request.notes())
                .build();
    }
}