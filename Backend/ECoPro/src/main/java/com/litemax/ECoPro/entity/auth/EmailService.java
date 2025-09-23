package com.litemax.ECoPro.entity.auth;



import com.litemax.ECoPro.entity.order.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

	private final JavaMailSender mailSender;

	@Value("${spring.mail.from}")
	private String fromEmail;

	@Value("${app.frontend.url}")
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
			mailSender.send(message);
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
			mailSender.send(message);
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
			mailSender.send(message);
			log.info("Welcome email sent successfully to: {}", user.getEmail());
		} catch (Exception e) {
			log.error("Failed to send welcome email to: {}", user.getEmail(), e);
		}
	}
    // --- ORDER STATUS CONFIRMATION ---
    public void sendOrderConfirmation(String toEmail, Order order) {
        log.info("Sending order confirmation email to: {}", toEmail);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Order Confirmation - Order #" + order.getId());
        message.setText(buildOrderConfirmationContent(order));

        try {
            mailSender.send(message);
            log.info("Order confirmation sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send order confirmation to: {}", toEmail, e);
            throw new RuntimeException("Failed to send order confirmation email", e);
        }
    }
    // --- ORDER STATUS UPDATE ---
    public void sendOrderStatusUpdate(String toEmail, Order order, Order.OrderStatus oldStatus, Order.OrderStatus newStatus) {
        log.info("Sending order status update email to: {}", toEmail);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Order Status Update - Order #" + order.getId());
        message.setText(buildOrderStatusUpdateContent(order, oldStatus, newStatus));

        try {
            mailSender.send(message);
            log.info("Order status update sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send order status update to: {}", toEmail, e);
            throw new RuntimeException("Failed to send order status update email", e);
        }
    }
    // --- ORDER CANCELLATION ---
    public void sendOrderCancellation(String toEmail, Order order) {
        log.info("Sending order cancellation email to: {}", toEmail);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Order Cancelled - Order #" + order.getId());
        message.setText(buildOrderCancellationContent(order));

        try {
            mailSender.send(message);
            log.info("Order cancellation email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send order cancellation to: {}", toEmail, e);
            throw new RuntimeException("Failed to send order cancellation email", e);
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

    private String buildOrderConfirmationContent(Order order) {
        return String.format(
                "Dear Customer,\n\n" +
                        "Thank you for your order!\n\n" +
                        "Order ID: %s\n" +
                        "Total Amount: %s\n\n" +
                        "We will notify you once your order has been shipped.\n\n" +
                        "Best regards,\n" +
                        "The E-commerce Team",
                order.getId(),
                order.getTotalAmount()  // assuming you have this field
        );
    }

    private String buildOrderStatusUpdateContent(Order order, Order.OrderStatus oldStatus, Order.OrderStatus newStatus) {
        return String.format(
                "Dear Customer,\n\n" +
                        "Your order #%s has been updated.\n\n" +
                        "Previous Status: %s\n" +
                        "Current Status: %s\n\n" +
                        "You can log in to your account for more details.\n\n" +
                        "Best regards,\n" +
                        "The E-commerce Team",
                order.getId(),
                oldStatus,
                newStatus
        );
    }

    private String buildOrderCancellationContent(Order order) {
        return String.format(
                "Dear Customer,\n\n" +
                        "We regret to inform you that your order #%s has been cancelled.\n\n" +
                        "If you have already made a payment, a refund will be processed within 5–7 business days.\n\n" +
                        "We apologize for any inconvenience caused.\n\n" +
                        "Best regards,\n" +
                        "The E-commerce Team",
                order.getId()
        );
    }


}
