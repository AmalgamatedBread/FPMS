package com.fpms.fpms_backend.repository;

import com.fpms.fpms_backend.model.entities.Department;
import com.fpms.fpms_backend.model.entities.Faculty;
import com.fpms.fpms_backend.model.entities.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    List<Portfolio> findByOwner(Faculty owner);

    List<Portfolio> findByOwnerAndType(Faculty owner, Portfolio.PortfolioType type);

    List<Portfolio> findByDepartmentAndType(Department department, Portfolio.PortfolioType type);

    List<Portfolio> findByDepartment(Department department);

    boolean existsByNameAndOwner(String name, Faculty owner);

    @Query("SELECT p FROM Portfolio p WHERE p.owner = :owner AND p.type = 'PERSONAL'")
    List<Portfolio> findPersonalPortfolios(@Param("owner") Faculty owner);

    @Query("SELECT p FROM Portfolio p WHERE p.department = :department AND p.type = 'DEPARTMENT'")
    List<Portfolio> findDepartmentPortfolios(@Param("department") Department department);

    @Query("SELECT p FROM Portfolio p WHERE p.type = 'COLLEGE'")
    List<Portfolio> findCollegePortfolios();

    @Query("SELECT p FROM Portfolio p LEFT JOIN FETCH p.items WHERE p.id = :id")
    Optional<Portfolio> findByIdWithItems(@Param("id") Long id);
}