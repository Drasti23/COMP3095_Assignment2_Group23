package ca.gbc.bookingservice.service;

import ca.gbc.bookingservice.dto.BookingRequest;
import ca.gbc.bookingservice.dto.BookingResponse;
import ca.gbc.bookingservice.model.Booking;
import ca.gbc.bookingservice.repository.BookingRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RestTemplate restTemplate;

    @Value("${service.room.url}")
    private String updateServiceUrl;

    @Value("${service.room.availability.url}")
    private String roomServiceUrl;

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    @Retry(name = "roomServiceRetry", fallbackMethod = "roomServiceFallback")
    @CircuitBreaker(name = "roomServiceCircuitBreaker", fallbackMethod = "roomServiceFallback")
    public BookingResponse createBooking(BookingRequest bookingRequest) {
        if (isRoomAvailable(bookingRequest.roomId())) {
            Booking booking = new Booking();
            booking.setRoomId(bookingRequest.roomId());
            booking.setUserName(bookingRequest.userName());
            booking.setStartTime(bookingRequest.startTime());
            booking.setEndTime(bookingRequest.endTime());

            Booking savedBooking = bookingRepository.save(booking);
            updateRoomAvailability(bookingRequest.roomId(), false);

            String eventMessage = String.format("Booking created: { id: %s, roomId: %s }",
                    savedBooking.getId(), savedBooking.getRoomId());
            kafkaTemplate.send("booking-events", eventMessage);
            log.info("Booking event published to Kafka: {}", eventMessage);
            log.info("Successfully created booking for roomId: {}", bookingRequest.roomId());

            return new BookingResponse(
                    savedBooking.getId(),
                    savedBooking.getRoomId(),
                    savedBooking.getUserName(),
                    savedBooking.getStartTime(),
                    savedBooking.getEndTime(),
                    bookingRequest.purpose(),
                    true
            );
        } else {
            throw new IllegalStateException("Room is not available for booking.");
        }
    }

    @Retry(name = "roomServiceRetry", fallbackMethod = "roomServiceFallback")
    @CircuitBreaker(name = "roomServiceCircuitBreaker", fallbackMethod = "roomServiceFallback")
    private boolean isRoomAvailable(String roomId) {
        String availabilityUrl = roomServiceUrl + "/" + roomId;
        try {
            return restTemplate.getForObject(availabilityUrl, Boolean.class);
        } catch (Exception e) {
            log.error("Error checking room availability: {}", e.getMessage());
            throw new RuntimeException("Room Service is unavailable.");
        }
    }

    // Fallback method for Room Service Unavailability
    public BookingResponse roomServiceFallback(String roomId, Throwable throwable) {
        log.error("Room Service is unavailable. Error: {}", throwable.getMessage());
        return new BookingResponse("Room Service Unavailable", null, null, null, null, null, false);
    }

    @Retry(name = "roomServiceRetry", fallbackMethod = "updateRoomFallback")
    @CircuitBreaker(name = "roomServiceCircuitBreaker", fallbackMethod = "updateRoomFallback")
    public void updateRoomAvailability(String roomId, boolean available) {
        String url = String.format("%s/%s/availability?available=%b", updateServiceUrl, roomId, available);
        try {
            restTemplate.put(url, null);
        } catch (Exception e) {
            updateRoomFallback(roomId, available, e);
        }
    }

    // Fallback method for Room Availability Update failure
    public void updateRoomFallback(String roomId, boolean available, Throwable throwable) {
        log.error("Failed to update room availability for roomId: {}. Error: {}", roomId, throwable.getMessage());
    }

    @Override
    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(booking -> new BookingResponse(
                        booking.getId(),
                        booking.getRoomId(),
                        booking.getUserName(),
                        booking.getStartTime(),
                        booking.getEndTime(),
                        null, // purpose can be added here if needed
                        true
                ))
                .collect(Collectors.toList());
    }

    @Override
    public BookingResponse getBookingById(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        return new BookingResponse(
                booking.getId(),
                booking.getRoomId(),
                booking.getUserName(),
                booking.getStartTime(),
                booking.getEndTime(),
                null, // purpose can be added here if needed
                true
        );
    }

    @Override
    public void deleteBooking(String bookingId) {
        bookingRepository.deleteById(bookingId);
    }

    @Override
    public Boolean bookingExists(String id) {
        log.debug("Checking if booking exists with ID: {}", id);
        return bookingRepository.findById(id).isPresent();
    }
}
