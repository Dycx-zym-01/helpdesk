package com.example.helpdesk.repository;

import com.example.helpdesk.domain.TicketProcessingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TicketProcessingRecordRepository extends JpaRepository<TicketProcessingRecord, Long> {

    Optional<TicketProcessingRecord> findFirstByTicketIdAndEndedAtIsNullOrderByStartedAtDesc(Long ticketId);

    List<TicketProcessingRecord> findByTicketIdIn(Collection<Long> ticketIds);
}
