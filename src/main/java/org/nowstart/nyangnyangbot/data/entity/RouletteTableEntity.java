package org.nowstart.nyangnyangbot.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "roulette_table")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RouletteTableEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Setter
    private String title;
    @Setter
    private String command;
    @Setter
    private Long pricePerRound;
    @Setter
    private boolean active;
    @Setter
    private Integer version;
    @Setter
    private Integer highRoundThreshold;

    public void activate() {
        active = true;
        version = version == null ? 1 : version + 1;
    }

    public void deactivate() {
        active = false;
    }
}
