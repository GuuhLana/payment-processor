package com.glrtech.payment.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.glrtech.payment.api.dto.CreatePaymentRequest;
import com.glrtech.payment.api.dto.PaymentEventResponse;
import com.glrtech.payment.api.dto.PaymentResponse;
import com.glrtech.payment.domain.Payment;
import com.glrtech.payment.domain.PaymentEvent;
import com.glrtech.payment.domain.PaymentEventType;
import com.glrtech.payment.domain.PaymentStatus;
import com.glrtech.payment.exception.IdempotencyConflictException;
import com.glrtech.payment.exception.PaymentNotFoundException;
import com.glrtech.payment.gateway.PaymentGatewayResolver;
import com.glrtech.payment.messaging.PaymentEventPublisher;
import com.glrtech.payment.messaging.event.PaymentRequestedMessage;
import com.glrtech.payment.repository.PaymentEventRepository;
import com.glrtech.payment.repository.PaymentRepository;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final PaymentEventPublisher publisher;
    private final PaymentGatewayResolver gatewayResolver;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentEventRepository paymentEventRepository,
            PaymentEventPublisher publisher,
            PaymentGatewayResolver gatewayResolver) {
        this.paymentRepository = paymentRepository;
        this.paymentEventRepository = paymentEventRepository;
        this.publisher = publisher;
        this.gatewayResolver = gatewayResolver;
    }

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        gatewayResolver.resolve(request.gateway()); // fail fast on an unknown gateway (400), instead of
                                                     // burning retries and landing the message in the DLQ

        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            return replayOrConflict(existing.get(), request);
        }

        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .amount(request.amount())
                .currency(request.currency())
                .gateway(request.gateway())
                .status(PaymentStatus.PENDING)
                .idempotencyKey(request.idempotencyKey())
                .build();

        try {
            payment = paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException raceLoser) {
            // Another request won the race on the unique idempotency_key constraint.
            return paymentRepository.findByIdempotencyKey(request.idempotencyKey())
                    .map(PaymentResponse::from)
                    .orElseThrow(() -> raceLoser);
        }

        recordEvent(payment.getId(), PaymentEventType.PAYMENT_REQUESTED, "Payment created with status PENDING");

        publisher.publishPaymentRequested(new PaymentRequestedMessage(
                payment.getId(), payment.getAmount(), payment.getCurrency(),
                payment.getGateway(), payment.getIdempotencyKey()));

        return PaymentResponse.from(payment);
    }

    private PaymentResponse replayOrConflict(Payment existing, CreatePaymentRequest request) {
        boolean sameRequest = existing.getAmount().compareTo(request.amount()) == 0
                && existing.getCurrency().equals(request.currency())
                && existing.getGateway().equals(request.gateway());

        if (!sameRequest) {
            throw new IdempotencyConflictException(
                    "idempotencyKey '" + request.idempotencyKey() + "' was already used with different payment data");
        }
        return PaymentResponse.from(existing);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID id) {
        return paymentRepository.findById(id)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new PaymentNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<PaymentEventResponse> getEvents(UUID id) {
        if (!paymentRepository.existsById(id)) {
            throw new PaymentNotFoundException(id);
        }
        return paymentEventRepository.findByPaymentIdOrderByCreatedAtAsc(id).stream()
                .map(PaymentEventResponse::from)
                .toList();
    }

    private void recordEvent(UUID paymentId, PaymentEventType type, String details) {
        paymentEventRepository.save(PaymentEvent.builder()
                .paymentId(paymentId)
                .eventType(type)
                .details(details)
                .build());
    }
}
