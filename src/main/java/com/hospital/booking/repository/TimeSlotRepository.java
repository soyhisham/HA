package com.hospital.booking.repository;

import com.hospital.booking.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

/** Database access for the time slots an admin defines for each doctor. */
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    // upcoming slots of one doctor, earliest first
    List<TimeSlot> findByDoctorIdAndSlotDateGreaterThanEqualOrderBySlotDateAscStartTimeAsc(Long doctorId, LocalDate from);

    // removes every slot of a doctor (used when the doctor is deleted); must run inside a transaction
    void deleteByDoctorId(Long doctorId);
}
