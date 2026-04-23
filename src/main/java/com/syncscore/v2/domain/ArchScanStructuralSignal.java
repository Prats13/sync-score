package com.syncscore.v2.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "arch_scan_structural_signals")
public class ArchScanStructuralSignal {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "architecture_scan_id", nullable = false)
    private UUID architectureScanId;

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_type", nullable = false, length = 50)
    private StructuralSignalType signalType;

    @Column(name = "value_numeric", precision = 12, scale = 2)
    private BigDecimal valueNumeric;

    @Column(name = "value_label", length = 50)
    private String valueLabel;

    @Column(name = "confidence_contribution", precision = 6, scale = 2)
    private BigDecimal confidenceContribution;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ArchScanStructuralSignal() {}

    public ArchScanStructuralSignal(UUID architectureScanId, StructuralSignalType signalType,
                                    BigDecimal valueNumeric, String valueLabel, BigDecimal confidenceContribution) {
        this.architectureScanId = architectureScanId;
        this.signalType = signalType;
        this.valueNumeric = valueNumeric;
        this.valueLabel = valueLabel;
        this.confidenceContribution = confidenceContribution;
    }

    public UUID getId() { return id; }
    public UUID getArchitectureScanId() { return architectureScanId; }
    public StructuralSignalType getSignalType() { return signalType; }
    public BigDecimal getValueNumeric() { return valueNumeric; }
    public String getValueLabel() { return valueLabel; }
    public BigDecimal getConfidenceContribution() { return confidenceContribution; }
    public Instant getCreatedAt() { return createdAt; }
}