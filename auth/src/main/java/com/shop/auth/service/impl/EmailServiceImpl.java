package com.shop.auth.service.impl;

import com.shop.auth.service.EmailService;
import com.shop.auth.utils.MaskingUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendOtp(String toEmail, String toName, String otp, int expiryMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Your Account Verification OTP");
        message.setText(buildOtpEmailBody(toName, otp, expiryMinutes));

        mailSender.send(message);
        log.info("OTP email sent to=[{}]", MaskingUtil.maskEmail(toEmail));
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String toName, String resetLink, int expiryMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Password Reset Request");
        message.setText(buildPasswordResetEmailBody(toName, resetLink, expiryMinutes));

        mailSender.send(message);
        log.info("Password reset email sent to=[{}]", MaskingUtil.maskEmail(toEmail));
    }

    private String buildOtpEmailBody(String name, String otp, int expiryMinutes) {
        return "Dear " + name + ",\n\n"
             + "Your one-time verification code is:\n\n"
             + "    " + otp + "\n\n"
             + "This code expires in " + expiryMinutes + " minutes.\n"
             + "Do NOT share this code with anyone.\n\n"
             + "If you did not request this, please ignore this email.\n\n"
             + "Regards,\nShop Platform";
    }

    private String buildPasswordResetEmailBody(String name, String resetLink, int expiryMinutes) {
        return "Dear " + name + ",\n\n"
             + "We received a request to reset your password.\n\n"
             + "Click the link below to set a new password. "
             + "This link expires in " + expiryMinutes + " minutes and can only be used once.\n\n"
             + "    " + resetLink + "\n\n"
             + "If you did not request a password reset, you can safely ignore this email.\n"
             + "Your password will not be changed.\n\n"
             + "Regards,\nShop Platform Security Team";
    }
}
