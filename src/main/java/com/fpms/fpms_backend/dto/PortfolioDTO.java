package com.fpms.fpms_backend.dto;

import com.fpms.fpms_backend.model.entities.Portfolio;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioDTO {
    private Long id;
    private String name;
    private String description;
    private Portfolio.PortfolioType type;
    private String ownerName;
    private String departmentName;
    private LocalDateTime createdAt;
    private int itemCount;

    public static PortfolioDTO fromEntity(Portfolio portfolio) {
        PortfolioDTO dto = new PortfolioDTO();
        dto.setId(portfolio.getId());
        dto.setName(portfolio.getName());
        dto.setDescription(portfolio.getDescription());
        dto.setType(portfolio.getType());
        dto.setOwnerName(portfolio.getOwner().getFirstName() + " " + portfolio.getOwner().getLastName());
        dto.setDepartmentName(portfolio.getDepartment() != null ? portfolio.getDepartment().getDeptName() : "Personal");
        dto.setCreatedAt(portfolio.getCreatedAt());
        dto.setItemCount(portfolio.getItems() != null ? portfolio.getItems().size() : 0);
        return dto;
    }
}