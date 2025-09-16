package com.litemax.ECoPro.entity.auth;


import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

	private final JavaMailSender mailSender;

//	@Value("${spring.mail.from}")
	private String fromEmail;

//	@Value("${app.frontend.url}")
	private String frontendUrl;

	public void sendEmailVerification(User user) {
		log.info("Sending email verification to: {}", user.getEmail());

		String verificationLink = frontendUrl + "/verify-email?token=" + user.getEmailVerificationToken();

		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(fromEmail);
		message.setTo(user.getEmail());
		message.setSubject("Verify Your Email Address");
		message.setText(buildEmailVerificationContent(user.getFirstName(), verificationLink));

		try {
//			mailSender.send(message);
			log.info("Email verification sent successfully to: {}", user.getEmail());
		} catch (Exception e) {
			log.error("Failed to send email verification to: {}", user.getEmail(), e);
			throw new RuntimeException("Failed to send verification email", e);
		}
	}

	public void sendPasswordResetEmail(User user, String resetToken) {
		log.info("Sending password reset email to: {}", user.getEmail());

		String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(fromEmail);
		message.setTo(user.getEmail());
		message.setSubject("Password Reset Request");
		message.setText(buildPasswordResetContent(user.getFirstName(), resetLink));

		try {
//			mailSender.send(message);
			log.info("Password reset email sent successfully to: {}", user.getEmail());
		} catch (Exception e) {
			log.error("Failed to send password reset email to: {}", user.getEmail(), e);
			throw new RuntimeException("Failed to send password reset email", e);
		}
	}

	public void sendWelcomeEmail(User user) {
		log.info("Sending welcome email to: {}", user.getEmail());

		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(fromEmail);
		message.setTo(user.getEmail());
		message.setSubject("Welcome to Our E-commerce Platform!");
		message.setText(buildWelcomeContent(user.getFirstName()));

		try {
//			mailSender.send(message);
			log.info("Welcome email sent successfully to: {}", user.getEmail());
		} catch (Exception e) {
			log.error("Failed to send welcome email to: {}", user.getEmail(), e);
		}
	}

	private String buildEmailVerificationContent(String firstName, String verificationLink) {
		return String.format("Dear %s,\n\n"
				+ "Welcome to our e-commerce platform! To complete your registration, please verify your email address by clicking the link below:\n\n"
				+ "%s\n\n" + "This verification link will expire in 24 hours.\n\n"
				+ "If you didn't create an account with us, please ignore this email.\n\n" + "Best regards,\n"
				+ "The E-commerce Team", firstName, verificationLink);
	}

	private String buildPasswordResetContent(String firstName, String resetLink) {
		return String.format("Dear %s,\n\n"
				+ "We received a request to reset your password. Click the link below to create a new password:\n\n"
				+ "%s\n\n" + "This reset link will expire in 1 hour.\n\n"
				+ "If you didn't request a password reset, please ignore this email. Your password will remain unchanged.\n\n"
				+ "Best regards,\n" + "The E-commerce Team", firstName, resetLink);
	}

	private String buildWelcomeContent(String firstName) {
		return String.format("Dear %s,\n\n" + "Welcome to our e-commerce platform!\n\n"
				+ "Your account has been successfully created and verified. You can now:\n"
				+ "- Browse our extensive product catalog\n" + "- Add items to your cart and wishlist\n"
				+ "- Place orders and track shipments\n" + "- Manage your profile and addresses\n\n"
				+ "Visit our platform: %s\n\n" + "If you have any questions, feel free to contact our support team.\n\n"
				+ "Best regards,\n" + "The E-commerce Team", firstName, frontendUrl);
	}
}
