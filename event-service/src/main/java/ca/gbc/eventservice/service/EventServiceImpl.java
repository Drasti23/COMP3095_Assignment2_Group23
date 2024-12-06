package ca.gbc.eventservice.service;

import ca.gbc.eventservice.dto.EventRequest;
import ca.gbc.eventservice.dto.EventResponse;
import ca.gbc.eventservice.model.Event;
import ca.gbc.eventservice.repository.EventRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final RestTemplate restTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // Injecting the service URLs from the application.properties
    @Value("${booking-service.url}")
    private String bookingServiceUrl;

    @Value("${user-service.url}")
    private String userServiceUrl;

    @Override
    @CircuitBreaker(name = "bookingServiceCircuitBreaker", fallbackMethod = "bookingServiceFallback")
    public String createEvent(EventRequest eventRequest) {
        // Step 1: Check if the booking ID is valid (exists)
        String bookingId = eventRequest.BookingId();

        // Use a RestTemplate to call the booking service
        Boolean exists = restTemplate.getForObject(bookingServiceUrl + bookingId, Boolean.class);

        if (Boolean.FALSE.equals(exists)) {
            return "Booking not found with ID: " + bookingId;
        } else {
            // Step 2: Check if the organizer is authorized
            String organizerEmail = eventRequest.organizer();  // Assuming this is the email
            if (!isOrganizerAuthorized(organizerEmail, eventRequest.location())) {
                return "Organizer is not authorized to create event in location: " + eventRequest.location();
            } else {
                Event event = mapRequestToEvent(eventRequest);
                Event savedEvent = eventRepository.save(event);
                String eventMessage = String.format(" Student Event created: "+savedEvent.getId()+" & userId: "+savedEvent.getOrganizer()+" & bookingId: "+savedEvent.getBookingId());
                kafkaTemplate.send("event-created",eventMessage);
                return "Event created successfully with ID: " + event.getId();
            }
        }
    }

    // Fallback method for the booking service circuit breaker
    public String bookingServiceFallback(EventRequest eventRequest, Throwable throwable) {
        log.error("Fallback executed for booking service. Error: {}", throwable.getMessage());
        return "Error checking booking service. Please try again later.";
    }

    @Override
    @CircuitBreaker(name = "userServiceCircuitBreaker", fallbackMethod = "userServiceFallback")
    public boolean isOrganizerAuthorized(String organizerId, String eventLocation) {
        String url = userServiceUrl + "role/" + organizerId;

        String role = restTemplate.getForObject(url, String.class);

        // Faculty can create events with any location
        assert role != null;
        if (role.equals("faculty")) {
            return true;
        }

        // Students can only create events if the location is "George Brown College"
        return role.equals("student") && eventLocation.equalsIgnoreCase("George Brown College");
    }

    // Fallback method for the user service circuit breaker
    public boolean userServiceFallback(String organizerId, String eventLocation, Throwable throwable) {
        log.error("Fallback executed for user service. Error: {}", throwable.getMessage());
        return false; // Return false if the user service fails
    }

    @Override
    public List<EventResponse> getAllEvents() {
        log.debug("Fetching all events.");
        List<Event> events = eventRepository.findAll();
        return events.stream().map(this::mapToEventResponse).toList();
    }

    @Override
    public EventResponse getEventById(String id) {
        log.debug("Fetching event by ID: " + id);
        Optional<Event> event = eventRepository.findById(id);
        return event.map(this::mapToEventResponse).orElse(null);
    }

    @Override
    public void deleteEvent(String id) {
        log.debug("Deleting event with ID: " + id);
        eventRepository.deleteById(id);
    }

    @Override
    public void updateEventdate(String id, String newDate) {
        log.debug("Updating event date for event ID: " + id);
        Optional<Event> eventOptional = eventRepository.findById(id);
        if (eventOptional.isPresent()) {
            Event event = eventOptional.get();
            event.setDate(newDate);  // Update the date of the event
            eventRepository.save(event);
        }
    }

    // Helper methods to map between entities and DTOs
    private EventResponse mapToEventResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getBookingId(),
                event.getName(),
                event.getDescription(),
                event.getOrganizer(),
                event.getDate(),
                event.getLocation()
        );
    }

    private Event mapRequestToEvent(EventRequest request) {
        return Event.builder()
                .BookingId(request.BookingId())
                .name(request.name())
                .description(request.description())
                .organizer(request.organizer())
                .date(request.date())
                .location(request.location())
                .build();
    }
}
