package com.lalitha.sweets.service;

import com.lalitha.sweets.model.Order;
import com.lalitha.sweets.model.OrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class OrderNotificationService {

    @Autowired
    private EmailService emailService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private WhatsAppService whatsAppService;

    @Value("${whatsapp.admin.number}")
    private String adminNumber;

    @Async
    public void sendOrderPlacedNotifications(
            Order order,
            String trackUrl,
            byte[] invoicePdf) {

        // Each notification is isolated so one failure does not stop the others.

        try {
            emailService.sendOrderConfirmation(
                    order,
                    "Order Placed - Lalitha Surya Sweets",
                    trackUrl,
                    invoicePdf
            );
        } catch (Exception e) {
            System.err.println(
                    "Order #" + order.getId()
                    + " confirmation email failed: " + e.getMessage()
            );
        }

        try {
            smsService.sendOrderStatusSms(
                    order,
                    OrderStatus.PLACED,
                    order.getCustomerPhoneSnapshot(),
                    trackUrl
            );
        } catch (Exception e) {
            System.err.println(
                    "Order #" + order.getId()
                    + " SMS failed: " + e.getMessage()
            );
        }

        try {
            whatsAppService.sendWhatsApp(
                    adminNumber,
                    "🚨 *New Paid Order Received!*\n\n" +
                    "🧾 *Order ID:* #" + order.getId() + "\n" +
                    "👤 *Customer:* " + order.getCustomerNameSnapshot() + "\n" +
                    "📞 *Phone:* " + order.getCustomerPhoneSnapshot() + "\n" +
                    "💰 *Amount:* ₹" + order.getTotalAmount() + "\n\n" +
                    "📍 *Address:*\n" + order.getAddress()
            );
        } catch (Exception e) {
            System.err.println(
                    "Order #" + order.getId()
                    + " admin WhatsApp failed: " + e.getMessage()
            );
        }
    }
}
