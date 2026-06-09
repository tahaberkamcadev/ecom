package com.tahaberkamcadev.payment_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tahaberkamcadev.payment_service.repository.ProcessedEventRepository;

@ExtendWith(MockitoExtension.class)
class ProcessedEventServiceTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private ProcessedEventService processedEventService;

    @Test
    void markIfNew_returnsTrueForNewEvent() {
        UUID eventId = UUID.randomUUID();
        when(processedEventRepository.insertIfAbsent(eq(eventId), eq("payment_completed"), any())).thenReturn(1);

        assertThat(processedEventService.markIfNew(eventId, "payment_completed")).isTrue();
    }

    @Test
    void markIfNew_returnsFalseForDuplicate() {
        UUID eventId = UUID.randomUUID();
        when(processedEventRepository.insertIfAbsent(eq(eventId), eq("payment_completed"), any())).thenReturn(0);

        assertThat(processedEventService.markIfNew(eventId, "payment_completed")).isFalse();
    }
}
