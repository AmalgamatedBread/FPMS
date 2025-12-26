package com.fpms.fpms_backend.repository;

import com.fpms.fpms_backend.model.entities.ApprovalRequest;
import com.fpms.fpms_backend.model.entities.Faculty;
import com.fpms.fpms_backend.model.entities.PortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    List<ApprovalRequest> findBySubmittedBy(Faculty submittedBy);
    List<ApprovalRequest> findByReviewedBy(Faculty reviewedBy);
    List<ApprovalRequest> findByItem(PortfolioItem item);
    List<ApprovalRequest> findByStatus(ApprovalRequest.ApprovalStatus status);

    @Query("SELECT ar FROM ApprovalRequest ar WHERE ar.status = 'PENDING' AND ar.item.portfolio.department = :department")
    List<ApprovalRequest> findPendingByDepartment(@Param("department") com.fpms.fpms_backend.model.entities.Department department);

    @Query("SELECT ar FROM ApprovalRequest ar WHERE ar.status = 'PENDING'")
    List<ApprovalRequest> findAllPending();
}