package org.nowstart.nyangnyangbot.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DonationEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String donationType;
    @ManyToOne
    private ChannelEntity channel;
    @ManyToOne
    private ChannelEntity donatorChannel;
    private Long payAmount;
    @Lob
    private String donationText;
    @Lob
    private String emojisJson;
}
