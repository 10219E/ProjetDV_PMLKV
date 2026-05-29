package lu.ephec.backend_projetdv2026.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialRecordDto {
    private LocalDateTime paymentDate;
    private Double amount;
    private String userFullName;
    private String siteName;
    private String paymentMethod;
    private String status;
}
