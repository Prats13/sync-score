package com.syncscore.v1.repo;

import com.syncscore.v1.domain.PublicProfile;
import com.syncscore.v1.domain.SyncTier;
import com.syncscore.v1.domain.VerificationLabel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PublicProfileRepository extends JpaRepository<PublicProfile, UUID> {
    Optional<PublicProfile> findByAgencyId(UUID agencyId);
    Optional<PublicProfile> findBySlug(String slug);

    interface BrowseRow {
        String getSlug();
        UUID getAgencyId();
        String getAgencyName();
        String getNiche();
        String getWebsiteUrl();
        String getBookingUrl();
        Integer getTotalScore();
        SyncTier getTier();
        VerificationLabel getVerificationLabel();
    }

    @Query("""
            select
              pp.slug as slug,
              ap.id as agencyId,
              ap.name as agencyName,
              ap.niche as niche,
              ap.websiteUrl as websiteUrl,
              ap.bookingUrl as bookingUrl,
              sr.totalScore as totalScore,
              sr.tier as tier,
              pp.verificationLabel as verificationLabel
            from PublicProfile pp
            join com.syncscore.v1.domain.AgencyProfile ap on ap.id = pp.agencyId
            left join com.syncscore.v1.domain.ScoreResult sr on sr.id = pp.latestScoreResultId
            where pp.isPublic = true
              and (:tier is null or sr.tier = :tier)
            order by sr.totalScore desc nulls last, ap.name asc
            """)
    List<BrowseRow> browsePublic(@Param("tier") SyncTier tier);
}
