package com.nspay.finance;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnore;
@Data
@EqualsAndHashCode(callSuper=true)
@Entity
@Table(name="CUSTOMER_PROFILES", uniqueConstraints=@UniqueConstraint(columnNames="userId"))
public class CustomerProfile extends OwnedRecord {
    private String email;
    private String phone;
    private String country;
    private LocalDate dateOfBirth;
    private String occupation;
    private String onboardingStatus = "NOT_STARTED";
}
