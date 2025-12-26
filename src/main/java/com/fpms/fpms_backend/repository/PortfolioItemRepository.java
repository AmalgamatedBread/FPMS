package com.fpms.fpms_backend.repository;

import com.fpms.fpms_backend.model.entities.Faculty;
import com.fpms.fpms_backend.model.entities.Portfolio;
import com.fpms.fpms_backend.model.entities.PortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {

    List<PortfolioItem> findByPortfolio(Portfolio portfolio);

    List<PortfolioItem> findByPortfolioAndParentFolderIsNull(Portfolio portfolio);

    List<PortfolioItem> findByParentFolder(PortfolioItem parentFolder);

    List<PortfolioItem> findByUploadedBy(Faculty uploadedBy);

    List<PortfolioItem> findByStatus(String status);

    @Query("SELECT pi FROM PortfolioItem pi WHERE pi.portfolio.id = :portfolioId")
    List<PortfolioItem> findByPortfolioId(@Param("portfolioId") Long portfolioId);

    @Query("SELECT pi FROM PortfolioItem pi WHERE pi.portfolio.id = :portfolioId AND pi.parentFolder IS NULL")
    List<PortfolioItem> findByPortfolioIdAndParentFolderIsNull(@Param("portfolioId") Long portfolioId);

    @Query("SELECT pi FROM PortfolioItem pi WHERE pi.parentFolder.id = :folderId")
    List<PortfolioItem> findByParentFolderId(@Param("folderId") Long folderId);

    @Query("SELECT pi FROM PortfolioItem pi WHERE pi.portfolio = :portfolio AND pi.status = 'PENDING'")
    List<PortfolioItem> findPendingItems(@Param("portfolio") Portfolio portfolio);

    @Query("SELECT pi FROM PortfolioItem pi WHERE pi.uploadedBy = :faculty AND pi.status = 'PENDING'")
    List<PortfolioItem> findPendingItemsByFaculty(@Param("faculty") Faculty faculty);

    @Query("SELECT COUNT(pi) FROM PortfolioItem pi WHERE pi.portfolio = :portfolio AND pi.isFolder = false")
    Long countFilesByPortfolio(@Param("portfolio") Portfolio portfolio);

    // NEW METHODS FOR PERSONAL DOCUMENTS
    @Query("SELECT pi FROM PortfolioItem pi WHERE pi.uploadedBy = :user AND pi.portfolio IS NULL")
    List<PortfolioItem> findPersonalDocumentsByUser(@Param("user") Faculty user);

    // Or if you want to keep the existing method name but fix the query:
    @Query("SELECT pi FROM PortfolioItem pi WHERE pi.uploadedBy = :user AND pi.portfolio IS NULL AND pi.isFolder = false")
    List<PortfolioItem> findPersonalFilesByUser(@Param("user") Faculty user);

    @Query("SELECT pi FROM PortfolioItem pi WHERE pi.portfolio IS NULL AND pi.uploadedBy.facultyId = :userId")
    List<PortfolioItem> findByUserIdWithoutPortfolio(@Param("userId") Long userId);

    List<PortfolioItem> findByPortfolioIdAndParentFolderId(Long portfolioId, Long parentFolderId);

    // For user documents (portfolio is null)
    List<PortfolioItem> findByUploadedByAndPortfolioIsNull(Faculty uploadedBy);


}