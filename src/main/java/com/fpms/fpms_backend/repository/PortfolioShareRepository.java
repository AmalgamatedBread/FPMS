package com.fpms.fpms_backend.repository;

import com.fpms.fpms_backend.model.entities.Faculty;
import com.fpms.fpms_backend.model.entities.Portfolio;
import com.fpms.fpms_backend.model.entities.PortfolioShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioShareRepository extends JpaRepository<PortfolioShare, Long> {
    List<PortfolioShare> findBySharedWith(Faculty sharedWith);
    List<PortfolioShare> findByPortfolio(Portfolio portfolio);
    boolean existsByPortfolioAndSharedWith(Portfolio portfolio, Faculty sharedWith);

    @Query("SELECT ps FROM PortfolioShare ps WHERE ps.sharedWith = :faculty AND ps.portfolio.type = 'DEPARTMENT'")
    List<PortfolioShare> findSharedDepartmentPortfolios(@Param("faculty") Faculty faculty);
}