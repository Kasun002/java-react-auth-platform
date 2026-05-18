-- Supports the OTP cleanup job DELETE: WHERE expires_at < :cutoff
-- Without this index, the cleanup query would do a full table scan as the table grows.
CREATE INDEX idx_otp_expires_at ON otp_verification(expires_at);
