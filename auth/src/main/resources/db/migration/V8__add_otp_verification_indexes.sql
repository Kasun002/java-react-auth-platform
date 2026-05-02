-- Composite index: covers countByUserAndCreatedAtAfter (resend rate-limit query)
CREATE INDEX idx_otp_user_created ON otp_verification(user_id, created_at);

-- Partial index: covers findTopByUserAndUsedFalseOrderByCreatedAtDesc (hot path on every verify)
-- Scoped to unused records only — significantly reduces index size as records age out.
CREATE INDEX idx_otp_user_unused ON otp_verification(user_id, created_at DESC) WHERE used = false;
