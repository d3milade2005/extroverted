package com.cityvibe.admin.model;

/*
 * =====================================================
 * COMMENTED OUT - FOR FUTURE KYC IMPLEMENTATION
 * =====================================================
 * This entity is prepared but not yet active.
 * Uncomment when ready to implement promoter verification/KYC.
 * =====================================================
 */

/*
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing promoter/host verification and KYC documentation.
 * Used for verifying event hosts before they can sell tickets or create official events.
 *
 * @author CityVibe Team
 * @version 1.0
 * /
@Entity
@Table(name = "promoter_verifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoterVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // =====================================================
    // Promoter Details
    // =====================================================

    @Column(name = "promoter_id", unique = true, nullable = false)
    private UUID promoterId;

    // =====================================================
    // Business Information
    // =====================================================

    @Column(name = "business_name", length = 255)
    private String businessName;

    @Column(name = "business_type", length = 100)
    private String businessType;

    @Column(name = "business_registration_number", length = 100)
    private String businessRegistrationNumber;

    // =====================================================
    // KYC Documents
    // =====================================================

    @Column(name = "id_document_type", length = 50)
    private String idDocumentType; // Could be enum: NATIONAL_ID, DRIVERS_LICENSE, etc.

    @Column(name = "id_document_number", length = 100)
    private String idDocumentNumber;

    @Column(name = "id_document_url", columnDefinition = "TEXT")
    private String idDocumentUrl;

    @Column(name = "proof_of_address_url", columnDefinition = "TEXT")
    private String proofOfAddressUrl;

    @Column(name = "business_registration_url", columnDefinition = "TEXT")
    private String businessRegistrationUrl;

    // =====================================================
    // Social Verification (Alternative to KYC)
    // =====================================================

    @Column(name = "instagram_handle", length = 100)
    private String instagramHandle;

    @Column(name = "twitter_handle", length = 100)
    private String twitterHandle;

    @Column(name = "facebook_page", length = 255)
    private String facebookPage;

    @Column(name = "website_url", length = 255)
    private String websiteUrl;

    // =====================================================
    // Verification Status
    // =====================================================

    @Column(name = "verification_status", nullable = false, length = 50)
    @Builder.Default
    private String verificationStatus = "PENDING"; // PENDING, APPROVED, REJECTED, RESUBMISSION_REQUIRED

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    // =====================================================
    // Rejection Details
    // =====================================================

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejected_by")
    private UUID rejectedBy;

    // =====================================================
    // Additional Information
    // =====================================================

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    // =====================================================
    // Metadata
    // =====================================================

    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // =====================================================
    // Helper Methods
    // =====================================================

    /**
     * Approve this verification
     * /
    public void approve(UUID adminId) {
        this.verificationStatus = "APPROVED";
        this.verifiedBy = adminId;
        this.verifiedAt = LocalDateTime.now();
    }

    /**
     * Reject this verification
     * /
    public void reject(UUID adminId, String reason) {
        this.verificationStatus = "REJECTED";
        this.rejectedBy = adminId;
        this.rejectedAt = LocalDateTime.now();
        this.rejectionReason = reason;
    }

    /**
     * Request resubmission with notes
     * /
    public void requestResubmission(String adminNotes) {
        this.verificationStatus = "RESUBMISSION_REQUIRED";
        this.adminNotes = adminNotes;
    }

    /**
     * Check if verification is pending
     * /
    public boolean isPending() {
        return "PENDING".equals(verificationStatus);
    }

    /**
     * Check if verification is approved
     * /
    public boolean isApproved() {
        return "APPROVED".equals(verificationStatus);
    }

    /**
     * Check if all required documents are uploaded
     * /
    public boolean hasAllDocuments() {
        return idDocumentUrl != null &&
               proofOfAddressUrl != null;
    }

    /**
     * Check if social verification is provided (alternative to KYC)
     * /
    public boolean hasSocialVerification() {
        return (instagramHandle != null || twitterHandle != null ||
                facebookPage != null || websiteUrl != null);
    }
}
*/

// =====================================================
// WHEN TO UNCOMMENT THIS CLASS:
// =====================================================
// 1. Uncomment the migration V4__create_promoter_verifications.sql
// 2. Uncomment this entire class
// 3. Create corresponding DTOs in the dto package
// 4. Implement PromoterVerificationService
// 5. Add API endpoints in PromoterVerificationController
// 6. Update AdminAction to include promoter verification actions
// =====================================================