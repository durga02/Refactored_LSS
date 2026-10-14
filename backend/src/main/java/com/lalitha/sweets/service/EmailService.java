
package com.lalitha.sweets.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

import com.lalitha.sweets.model.Order;
import com.lalitha.sweets.model.OrderItem;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Value("${app.backend.base-url}")
    private String backendBaseUrl;


    // =========================================================
    // BRAND COLORS
    // =========================================================

    private static final String BRAND_RED = "#7f1d1d";


    // =========================================================
    // COMMON EMAIL WRAPPER
    // =========================================================

    private String wrapEmail(String bodyHtml) {

        return """
        <!DOCTYPE html>
        <html>
        <head>

            <meta charset="UTF-8">

            <meta
                name="viewport"
                content="width=device-width, initial-scale=1.0"
            >

            <style>

                body {
                    margin: 0;
                    padding: 0;
                    background-color: #ffffff;
                    font-family: Arial, Helvetica, sans-serif;
                    color: #222222;
                }

                .email-container {
                    width: 100%%;
                    max-width: 760px;
                    margin: 0 auto;
                    background: #ffffff;
                }

                .content {
                    padding: 10px 24px 30px 24px;
                }

                .order-card {
                    border: 1px solid #e5e5e5;
                    border-radius: 14px;
                    overflow: hidden;
                    margin-top: 20px;
                    background: #ffffff;
                }

                .order-card-title {
                    background: #f5f5f5;
                    text-align: center;
                    padding: 18px;
                    font-size: 18px;
                    font-weight: bold;
                    color: #222222;
                }

                .product-row {
                    border-bottom: 1px solid #e5e5e5;
                }

                .product-image {
                    width: 70px;
                    height: 70px;
                    border-radius: 10px;
                    display: block;
                    object-fit: cover;
                }

                .product-name {
                    font-size: 14px;
                    color: #222222;
                    font-weight: 500;
                }

                .product-label {
                    margin-top: 4px;
                    font-size: 12px;
                    color: #777777;
                }

                .product-qty {
                    font-size: 13px;
                    color: #555555;
                    text-align: center;
                }

                .product-price {
                    font-size: 14px;
                    color: #168a2e;
                    font-weight: bold;
                    text-align: right;
                    white-space: nowrap;
                }

                .details-section {
                    background: #f7f7f7;
                    padding: 14px 20px;
                }

                .detail-label {
                    color: #666666;
                    font-size: 13px;
                    vertical-align: top;
                    padding: 12px 0;
                }

                .detail-value {
                    color: #111111;
                    font-size: 13px;
                    font-weight: bold;
                    text-align: right;
                    vertical-align: top;
                    padding: 12px 0;
                }

                .total-section {
                    border-top: 1px solid #dddddd;
                    padding: 18px;
                    text-align: center;
                    font-size: 18px;
                    font-weight: bold;
                    color: #222222;
                }

                .button {
                    display: inline-block;
                    padding: 12px 22px;
                    background: %s;
                    color: #ffffff !important;
                    text-decoration: none;
                    border-radius: 6px;
                    font-size: 14px;
                    font-weight: bold;
                    margin: 6px;
                }

                .invoice-button {
                    display: inline-block;
                    padding: 12px 22px;
                    background: #333333;
                    color: #ffffff !important;
                    text-decoration: none;
                    border-radius: 6px;
                    font-size: 14px;
                    font-weight: bold;
                    margin: 6px;
                }

                .footer {
                    text-align: center;
                    padding: 24px;
                    color: #777777;
                    font-size: 12px;
                    line-height: 1.6;
                }

            </style>

        </head>

        <body>

            <div class="email-container">

                <div class="content">

                    %s

                </div>

                <div class="footer">

                    <strong>Lalitha Surya Sweets</strong>
                    <br>

                    Thank you for choosing us.

                    <br>

                    Please do not reply directly to this email.

                </div>

            </div>

        </body>
        </html>
        """.formatted(
                BRAND_RED,
                bodyHtml
        );
    }


    // =========================================================
    // HTML ESCAPE
    // =========================================================

    private String escapeHtml(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }


    // =========================================================
    // FORMAT ADDRESS
    // =========================================================

    private String formatAddress(String address) {

        if (address == null || address.isBlank()) {
            return "-";
        }

        return escapeHtml(address)
                .replace("\n", "<br>");
    }


    // =========================================================
    // PRODUCT IMAGE PLACEHOLDER
    // =========================================================
    //
    // Your current OrderItem does NOT contain imageUrl.
    //
    // Therefore we intentionally do NOT call:
    //
    // item.getProduct()
    // item.getProduct().getImageUrl()
    //
    // This keeps the code compatible with your current model.
    //
    // =========================================================

    private String productImagePlaceholder() {

        return """
            <div style="
                width:70px;
                height:70px;
                border-radius:10px;
                background:#eeeeee;
                display:flex;
                align-items:center;
                justify-content:center;
                text-align:center;
                color:#777777;
                font-size:11px;
                font-family:Arial,Helvetica,sans-serif;
            ">
                Product
            </div>
        """;
    }


    // =========================================================
    // SEND EMAIL
    // =========================================================

    private void send(
            String to,
            String subject,
            String bodyHtml,
            byte[] pdfAttachment,
            String attachmentName) {

        try {

            MimeMessage message =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            pdfAttachment != null,
                            "UTF-8"
                    );

            helper.setFrom(fromAddress);

            helper.setTo(to);

            helper.setSubject(subject);

            helper.setText(
                    wrapEmail(bodyHtml),
                    true
            );

            if (pdfAttachment != null) {

                helper.addAttachment(
                        attachmentName,
                        new ByteArrayResource(pdfAttachment)
                );
            }

            mailSender.send(message);

            System.out.println(
                    "Order email sent successfully to: " + to
            );

        } catch (Exception e) {

            System.err.println(
                    "Failed to send order email to: " + to
            );

            e.printStackTrace();
        }
    }


    // =========================================================
    // ORDER CONFIRMATION EMAIL
    // =========================================================

    public void sendOrderConfirmation(
            Order order,
            String subject,
            String trackUrl,
            byte[] pdfBytes) {


        // -----------------------------------------------------
        // TRACK URL
        // -----------------------------------------------------

        String resolvedTrackUrl;

        if (trackUrl != null && !trackUrl.isBlank()) {

            resolvedTrackUrl = trackUrl;

        } else {

            resolvedTrackUrl =
                    frontendBaseUrl
                    + "/track/"
                    + order.getId();
        }


        // -----------------------------------------------------
        // CUSTOMER NAME
        // -----------------------------------------------------

        String customerName =
                escapeHtml(
                        order.getCustomerNameSnapshot()
                );


        // -----------------------------------------------------
        // ORDER DATE
        // -----------------------------------------------------

        String orderDate =
                order.getOrderDate() != null
                        ? order.getOrderDate().toString()
                        : "-";


        // -----------------------------------------------------
        // PRODUCT ROWS
        // -----------------------------------------------------

        StringBuilder productRows =
                new StringBuilder();


        List<OrderItem> items =
                order.getItems();


        if (items != null && !items.isEmpty()) {

            for (OrderItem item : items) {


                String productName =
                        escapeHtml(
                                item.getProductName()
                        );


                String priceLabel =
                        item.getPriceLabel();


                int quantity =
                        item.getQuantity();


                /*
                 * Use subtotal for the line total.
                 *
                 * Example:
                 *
                 * price = ₹250
                 * quantity = 2
                 * subtotal = ₹500
                 *
                 * Email shows ₹500.
                 */

                String itemAmount;

                if (item.getSubtotal() != null) {

                    itemAmount =
                            "₹"
                            + item.getSubtotal().toPlainString();

                } else if (item.getPrice() != null) {

                    itemAmount =
                            "₹"
                            + item.getPrice().toPlainString();

                } else {

                    itemAmount =
                            "₹0.00";
                }


                String priceLabelHtml = "";


                if (priceLabel != null
                        && !priceLabel.isBlank()) {

                    priceLabelHtml = """

                        <div class="product-label">
                            %s
                        </div>

                    """.formatted(
                            escapeHtml(priceLabel)
                    );
                }


                productRows.append("""

                    <tr class="product-row">

                        <!-- IMAGE -->

                        <td
                            width="90"
                            style="
                                width:90px;
                                padding:10px;
                                vertical-align:middle;
                            "
                        >

                            %s

                        </td>


                        <!-- PRODUCT NAME -->

                        <td
                            style="
                                padding:10px;
                                vertical-align:middle;
                            "
                        >

                            <div class="product-name">

                                %s

                            </div>

                            %s

                        </td>


                        <!-- QUANTITY -->

                        <td
                            width="70"
                            style="
                                width:70px;
                                padding:10px;
                                vertical-align:middle;
                            "
                        >

                            <div class="product-qty">

                                %d Qty

                            </div>

                        </td>


                        <!-- PRICE -->

                        <td
                            width="100"
                            style="
                                width:100px;
                                padding:10px;
                                vertical-align:middle;
                            "
                        >

                            <div class="product-price">

                                %s

                            </div>

                        </td>

                    </tr>

                """.formatted(

                        productImagePlaceholder(),

                        productName,

                        priceLabelHtml,

                        quantity,

                        itemAmount
				 ) );
            }

        } else {

            productRows.append("""

                <tr>

                    <td
                        colspan="4"
                        style="
                            padding:25px;
                            text-align:center;
                            color:#777777;
                            font-size:14px;
                        "
                    >

                        No items found.

                    </td>

                </tr>

            """);
        }


        // -----------------------------------------------------
        // DELIVERY ADDRESS
        // -----------------------------------------------------

        String deliveryAddress =
                formatAddress(
                        order.getAddress()
                );


        // -----------------------------------------------------
        // BILLING ADDRESS
        // -----------------------------------------------------
        //
        // Your current Order information from the code shown
        // uses order.getAddress().
        //
        // Therefore we use the same address for billing.
        //
        // If your Order.java has a separate billing address,
        // we can change this later.
        //
        // -----------------------------------------------------

        String billingAddress =
                formatAddress(
                        order.getAddress()
                );


        // -----------------------------------------------------
        // ITEM TOTAL
        // -----------------------------------------------------

        String itemTotal =
                order.getTotalAmount() != null
                        ? "₹"
                        + order.getTotalAmount().toPlainString()
                        : "₹0.00";


        // -----------------------------------------------------
        // TOTAL AMOUNT
        // -----------------------------------------------------

        String totalAmount =
                order.getTotalAmount() != null
                        ? "₹"
                        + order.getTotalAmount().toPlainString()
                        : "₹0.00";


        // -----------------------------------------------------
        // INVOICE BUTTON
        // -----------------------------------------------------

        String invoiceButton = "";


        if (pdfBytes != null) {

            String invoiceUrl =
                    backendBaseUrl
                    + "/api/checkout/invoice/"
                    + order.getId();


            invoiceButton = """

                <a
                    href="%s"
                    class="invoice-button"
                >
                    Download Invoice
                </a>

            """.formatted(invoiceUrl);
        }


        // -----------------------------------------------------
        // EMAIL BODY
        // -----------------------------------------------------

        String body = """

        <!-- GREETING -->

        <div
            style="
                font-size:15px;
                line-height:1.7;
            "
        >

            <p>

                Hello <strong>%s</strong>,

            </p>


            <p>

                Thank you for placing your order with
                <strong>Lalitha Surya Sweets</strong>!

                <br>

                We're happy to inform you that your
                order has been successfully Placed.

            </p>

        </div>


        <!-- ORDER DETAILS -->

        <div
            style="
                margin-top:18px;
                font-size:14px;
                line-height:1.8;
            "
        >

            <div>

                <strong>Order Details :</strong>

            </div>


            <div>

                • Order ID :
                <strong>#%d</strong>

            </div>


            <div>

                • Order Date :
                <strong>%s</strong>

            </div>

        </div>


        <!-- PROCESSING MESSAGE -->

        <p
            style="
                margin-top:20px;
                font-size:14px;
                line-height:1.7;
            "
        >

            Our team is now processing your order,
            and you will receive an update once it is shipped.

        </p>


        <!-- BANNER -->

        <div
            style="
                margin-top:20px;
                border:1px solid #999999;
                border-radius:28px;
                padding:22px 15px;
                background:#ffffff;
                text-align:center;
            "
        >

            <div
                style="
                    font-size:28px;
                    font-weight:bold;
                    letter-spacing:2px;
                    color:#555555;
                "
            >

                ORDER PLACED

            </div>


            <div
                style="
                    margin-top:8px;
                    font-size:15px;
                    letter-spacing:1px;
                    color:#333333;
                "
            >

                Thank you for shopping with
                Lalitha Surya Sweets

            </div>

        </div>


        <!-- ORDER SUMMARY -->

        <div class="order-card">


            <!-- TITLE -->

            <div class="order-card-title">

                Order Summary

            </div>


            <!-- PRODUCT TABLE -->

            <table
                width="100%%"
                cellpadding="0"
                cellspacing="0"
                border="0"
                style="
                    width:100%%;
                    border-collapse:collapse;
                "
            >

                %s

            </table>


            <!-- DETAILS -->

            <div class="details-section">

                <table
                    width="100%%"
                    cellpadding="0"
                    cellspacing="0"
                    border="0"
                    style="
                        width:100%%;
                        border-collapse:collapse;
                    "
                >


                    <!-- ORDER DATE -->

                    <tr>

                        <td class="detail-label">

                            Order Date

                        </td>


                        <td class="detail-value">

                            %s

                        </td>

                    </tr>


                    <!-- PAYMENT MODE -->

                    <tr>

                        <td class="detail-label">

                            Payment Mode

                        </td>


                        <td class="detail-value">

                            Razorpay

                        </td>

                    </tr>


                    <!-- DELIVERY ADDRESS -->

                    <tr>

                        <td class="detail-label">

                            Delivery Address

                        </td>


                        <td class="detail-value">

                            %s

                        </td>

                    </tr>


                    <!-- BILLING ADDRESS -->

                    <tr>

                        <td class="detail-label">

                            Billing Address

                        </td>


                        <td class="detail-value">

                            %s

                        </td>

                    </tr>


                    <!-- ITEM TOTAL -->

                    <tr>

                        <td class="detail-label">

                            Item Total

                        </td>


                        <td class="detail-value">

                            %s

                        </td>

                    </tr>


                </table>

            </div>


            <!-- TOTAL -->

            <div class="total-section">

                Total Amount : %s

            </div>


        </div>


        <!-- BUTTONS -->

        <div
            style="
                text-align:center;
                margin-top:24px;
            "
        >

            <a
                href="%s"
                class="button"
            >

                Track Order

            </a>


            %s

        </div>

        """.formatted(

                // 1
                customerName,

                // 2
                order.getId(),

                // 3
                orderDate,

                // 4
                productRows.toString(),

                // 5
                orderDate,

                // 6
                deliveryAddress,

                // 7
                billingAddress,

                // 8
                itemTotal,

                // 9
                totalAmount,

                // 10
                resolvedTrackUrl,

                // 11
                invoiceButton
        );


        // -----------------------------------------------------
        // SEND
        // -----------------------------------------------------

        if (order.getCustomer() != null
                && order.getCustomer().getEmail() != null
                && !order.getCustomer().getEmail().isBlank()) {

            send(
                    order.getCustomer().getEmail(),
                    subject,
                    body,
                    pdfBytes,
                    "invoice-" + order.getId() + ".pdf"
            );

        } else {

            System.err.println(
                    "Customer email is missing for order #"
                    + order.getId()
            );
        }
    }

    // =========================================================
    // ORDER ITEMS SUMMARY
    // =========================================================

	private String buildOrderItemsSummary(Order order) {

    StringBuilder rows = new StringBuilder();

    List<OrderItem> items = order.getItems();

    if (items == null || items.isEmpty()) {

        return """
            <div style="
                padding:20px;
                text-align:center;
                color:#777777;
                font-family:Arial,Helvetica,sans-serif;
                font-size:14px;
            ">
                No order items available.
            </div>
            """;
    }

    for (OrderItem item : items) {

        String productName =
                escapeHtml(item.getProductName());

        String priceLabel = "";

        if (item.getPriceLabel() != null
                && !item.getPriceLabel().isBlank()) {

            priceLabel = """
                <div style="
                    margin-top:4px;
                    font-size:12px;
                    color:#777777;
                ">
                    %s
                </div>
                """.formatted(
                    escapeHtml(item.getPriceLabel())
                );
        }


        String quantity =
                String.valueOf(item.getQuantity());


        String subtotal = "₹0.00";

        if (item.getSubtotal() != null) {

            subtotal =
                    "₹" + item.getSubtotal().toPlainString();

        } else if (item.getPrice() != null) {

            subtotal =
                    "₹" + item.getPrice().toPlainString();
        }


        rows.append("""
            <tr>

                <!-- PRODUCT -->

                <td style="
                    padding:14px;
                    border-bottom:1px solid #e5e5e5;
                    vertical-align:middle;
                ">

                    <div style="
                        font-family:Arial,Helvetica,sans-serif;
                        font-size:14px;
                        font-weight:600;
                        color:#222222;
                    ">

                        %s

                    </div>

                    %s

                </td>


                <!-- QUANTITY -->

                <td
                    width="80"
                    style="
                        width:80px;
                        padding:14px 8px;
                        border-bottom:1px solid #e5e5e5;
                        text-align:center;
                        vertical-align:middle;
                        font-family:Arial,Helvetica,sans-serif;
                        font-size:13px;
                        color:#555555;
                    "
                >

                    %s Qty

                </td>


                <!-- PRICE -->

                <td
                    width="110"
                    style="
                        width:110px;
                        padding:14px;
                        border-bottom:1px solid #e5e5e5;
                        text-align:right;
                        vertical-align:middle;
                        font-family:Arial,Helvetica,sans-serif;
                        font-size:14px;
                        font-weight:bold;
                        color:#168a2e;
                    "
                >

                    %s

                </td>

            </tr>
            """.formatted(
                productName,
                priceLabel,
                quantity,
                subtotal
            ));
    }


    return """
        <table
            width="100%%"
            cellpadding="0"
            cellspacing="0"
            border="0"
            style="
                width:100%%;
                border-collapse:collapse;
                border:1px solid #e5e5e5;
                border-radius:8px;
                overflow:hidden;
            "
        >

            <!-- TABLE HEADER -->

            <tr style="background:#f5f5f5;">

                <th
                    style="
                        padding:12px 14px;
                        text-align:left;
                        font-family:Arial,Helvetica,sans-serif;
                        font-size:12px;
                        color:#666666;
                        font-weight:bold;
                    "
                >

                    ITEM

                </th>


                <th
                    width="80"
                    style="
                        width:80px;
                        padding:12px 8px;
                        text-align:center;
                        font-family:Arial,Helvetica,sans-serif;
                        font-size:12px;
                        color:#666666;
                        font-weight:bold;
                    "
                >

                    QTY

                </th>


                <th
                    width="110"
                    style="
                        width:110px;
                        padding:12px 14px;
                        text-align:right;
                        font-family:Arial,Helvetica,sans-serif;
                        font-size:12px;
                        color:#666666;
                        font-weight:bold;
                    "
                >

                    AMOUNT

                </th>

            </tr>


            %s

        </table>
        """.formatted(rows.toString());
	}




    // =========================================================
    // STATUS UPDATE EMAIL
    // =========================================================
	@Async
    public void sendStatusUpdate(Order order) {

    String name =
            escapeHtml(
                    order.getCustomerNameSnapshot()
            );

    Long id =
            order.getId();

    String trackUrl =
            frontendBaseUrl + "/track/" + id;


    String subject;
    String statusTitle;
    String statusMessage;
    String nextStep;
    String statusColor;


    // =========================================================
    // STATUS-SPECIFIC CONTENT
    // =========================================================

    switch (order.getStatus()) {

        case CONFIRMED -> {

            subject =
                    "Order Confirmed - #" + id;

            statusTitle =
                    "Order Confirmed";

            statusMessage =
                    "Thank you for your order. "
                    + "We have successfully confirmed your order "
                    + "and our team will begin processing it shortly.";

            nextStep =
                    "We will notify you when your order moves to the preparation stage.";

            statusColor =
                    "#2563eb";
        }


        case PREPARING -> {

            subject =
                    "Your Order is Being Prepared - #" + id;

            statusTitle =
                    "Your Order is Being Prepared";

            statusMessage =
                    "Our team is currently preparing your order with care. "
                    + "We are working to have it ready for dispatch as soon as possible.";

            nextStep =
                    "You will receive another update once your order is packed.";

            statusColor =
                    "#d97706";
        }


        case PACKED -> {

            subject =
                    "Your Order Has Been Packed - #" + id;

            statusTitle =
                    "Order Packed";

            statusMessage =
                    "Your order has been carefully packed and is now ready for dispatch.";

            nextStep =
                    "We will notify you as soon as your order has been handed over for delivery.";

            statusColor =
                    "#7c3aed";
        }


        case SHIPPED -> {

            subject =
                    "Your Order Has Been Shipped - #" + id;

            statusTitle =
                    "Order Shipped";

            statusMessage =
                    "Great news! Your order has been dispatched and is now on its way to you.";

            nextStep =
                    "You can use the button below to check your latest order status.";

            statusColor =
                    "#0891b2";
        }


        case OUT_FOR_DELIVERY -> {

            subject =
                    "Your Order is Out for Delivery - #" + id;

            statusTitle =
                    "Out for Delivery";

            statusMessage =
                    "Your order is currently out for delivery and should reach you shortly.";

            nextStep =
                    "Please keep your phone available in case our delivery team needs to contact you.";

            statusColor =
                    "#ea580c";
        }


        case DELIVERED -> {

            subject =
                    "Your Order Has Been Delivered - #" + id;

            statusTitle =
                    "Order Delivered";

            statusMessage =
                    "Your order has been successfully delivered. "
                    + "We hope you enjoy your order from Lalitha Surya Sweets.";

            nextStep =
                    "Thank you for choosing Lalitha Surya Sweets. "
                    + "We look forward to serving you again.";

            statusColor =
                    "#16a34a";
        }


        case CANCELLED -> {

            subject =
                    "Order Cancelled - #" + id;

            statusTitle =
                    "Order Cancelled";

            String reason =
                    order.getCancelReason();

            if (reason == null || reason.isBlank()) {

                reason =
                        "No cancellation reason was provided.";
            }

            statusMessage =
                    "Your order has been cancelled.";

            nextStep =
                    "<strong>Cancellation Reason:</strong><br>"
                    + escapeHtml(reason);

            statusColor =
                    "#dc2626";
        }


        default -> {

            subject =
                    "Order Status Update - #" + id;

            statusTitle =
                    "Order Status Updated";

            statusMessage =
                    "Your order status has been updated.";

            nextStep =
                    "Please use the button below to view the latest information about your order.";

            statusColor =
                    "#7f1d1d";
        }
    }


    // =========================================================
    // ORDER DATE
    // =========================================================

    String orderDate = "-";

    if (order.getOrderDate() != null) {

        orderDate =
                order.getOrderDate().toString();
    }


    // =========================================================
    // TOTAL AMOUNT
    // =========================================================

    String totalAmount =
            "₹0.00";

    if (order.getTotalAmount() != null) {

        totalAmount =
                "₹" + order.getTotalAmount().toPlainString();
    }


    // =========================================================
    // DELIVERY ADDRESS
    // =========================================================

    String deliveryAddress =
            formatAddress(
                    order.getAddress()
            );


    // =========================================================
    // ORDER ITEMS SUMMARY
    // =========================================================

    String orderItemsSummary =
            buildOrderItemsSummary(order);


    // =========================================================
    // EMAIL BODY
    // =========================================================

    String body = """

    <!-- BRAND -->

    <table
        width="100%%"
        cellpadding="0"
        cellspacing="0"
        border="0"
        style="
            width:100%%;
            border-collapse:collapse;
            margin-bottom:25px;
        "
    >

        <tr>

            <td
                style="
                    padding-bottom:14px;
                    border-bottom:2px solid #7f1d1d;
                "
            >

                <span
                    style="
                        font-family:Arial,Helvetica,sans-serif;
                        font-size:20px;
                        font-weight:bold;
                        color:#7f1d1d;
                    "
                >

                    Lalitha Surya Sweets

                </span>

            </td>

        </tr>

    </table>


    <!-- GREETING -->

    <p
        style="
            margin:0 0 18px 0;
            font-family:Arial,Helvetica,sans-serif;
            font-size:15px;
            line-height:1.6;
            color:#222222;
        "
    >

        Hello <strong>%s</strong>,

    </p>


    <!-- STATUS CARD -->

    <table
        width="100%%"
        cellpadding="0"
        cellspacing="0"
        border="0"
        style="
            width:100%%;
            border-collapse:collapse;
            border:1px solid #dddddd;
        "
    >

        <!-- STATUS HEADER -->

        <tr>

            <td
                style="
                    padding:22px;
                    text-align:center;
                    background:%s;
                "
            >

                <div
                    style="
                        font-family:Arial,Helvetica,sans-serif;
                        font-size:22px;
                        line-height:1.3;
                        font-weight:bold;
                        color:#ffffff;
                    "
                >

                    %s

                </div>


                <div
                    style="
                        margin-top:7px;
                        font-family:Arial,Helvetica,sans-serif;
                        font-size:13px;
                        color:#ffffff;
                    "
                >

                    Order #%d

                </div>

            </td>

        </tr>


        <!-- MESSAGE -->

        <tr>

            <td
                style="
                    padding:25px;
                    background:#ffffff;
                "
            >

                <p
                    style="
                        margin:0;
                        font-family:Arial,Helvetica,sans-serif;
                        font-size:15px;
                        line-height:1.7;
                        color:#333333;
                    "
                >

                    %s

                </p>


                <div
                    style="
                        margin-top:18px;
                        padding:13px 15px;
                        background:#f8f8f8;
                        border-left:4px solid %s;
                        font-family:Arial,Helvetica,sans-serif;
                        font-size:14px;
                        line-height:1.6;
                        color:#444444;
                    "
                >

                    %s

                </div>

            </td>

        </tr>

    </table>


    <!-- ORDER SUMMARY TITLE -->

    <div
        style="
            margin-top:28px;
            margin-bottom:10px;
            font-family:Arial,Helvetica,sans-serif;
            font-size:18px;
            font-weight:bold;
            color:#222222;
        "
    >

        Order Summary

    </div>


    <!-- ORDER ITEMS -->

    %s


    <!-- ORDER INFORMATION -->

    <div
        style="
            margin-top:28px;
            margin-bottom:10px;
            font-family:Arial,Helvetica,sans-serif;
            font-size:18px;
            font-weight:bold;
            color:#222222;
        "
    >

        Order Information

    </div>


    <table
        width="100%%"
        cellpadding="0"
        cellspacing="0"
        border="0"
        style="
            width:100%%;
            border-collapse:collapse;
            border:1px solid #e5e5e5;
        "
    >

        <!-- ORDER ID -->

        <tr>

            <td
                width="40%%"
                style="
                    padding:13px 15px;
                    background:#f8f8f8;
                    border-bottom:1px solid #e5e5e5;
                    font-family:Arial,Helvetica,sans-serif;
                    font-size:13px;
                    color:#666666;
                "
            >

                Order ID

            </td>


            <td
                style="
                    padding:13px 15px;
                    border-bottom:1px solid #e5e5e5;
                    font-family:Arial,Helvetica,sans-serif;
                    font-size:13px;
                    font-weight:bold;
                    color:#222222;
                "
            >

                #%d

            </td>

        </tr>


        <!-- ORDER DATE -->

        <tr>

            <td
                style="
                    padding:13px 15px;
                    background:#f8f8f8;
                    border-bottom:1px solid #e5e5e5;
                    font-family:Arial,Helvetica,sans-serif;
                    font-size:13px;
                    color:#666666;
                "
            >

                Order Date

            </td>


            <td
                style="
                    padding:13px 15px;
                    border-bottom:1px solid #e5e5e5;
                    font-family:Arial,Helvetica,sans-serif;
                    font-size:13px;
                    font-weight:bold;
                    color:#222222;
                "
            >

                %s

            </td>

        </tr>


        <!-- PAYMENT -->

        <tr>

            <td
                style="
                    padding:13px 15px;
                    background:#f8f8f8;
                    border-bottom:1px solid #e5e5e5;
                    font-family:Arial,Helvetica,sans-serif;
                    font-size:13px;
                    color:#666666;
                "
            >

                Payment Method

            </td>


            <td
                style="
                    padding:13px 15px;
                    border-bottom:1px solid #e5e5e5;
                    font-family:Arial,Helvetica,sans-serif;
                    font-size:13px;
                    font-weight:bold;
                    color:#222222;
                "
            >

                Razorpay

            </td>

        </tr>


        <!-- TOTAL -->

        <tr>

            <td
                style="
                    padding:13px 15px;
                    background:#f8f8f8;
                    font-family:Arial,Helvetica,sans-serif;
                    font-size:13px;
                    color:#666666;
                "
            >

                Total Amount

            </td>


            <td
                style="
                    padding:13px 15px;
                    font-family:Arial,Helvetica,sans-serif;
                    font-size:15px;
                    font-weight:bold;
                    color:#168a2e;
                "
            >

                %s

            </td>

        </tr>

    </table>


    <!-- DELIVERY ADDRESS -->

    <div
        style="
            margin-top:28px;
            margin-bottom:10px;
            font-family:Arial,Helvetica,sans-serif;
            font-size:18px;
            font-weight:bold;
            color:#222222;
        "
    >

        Delivery Address

    </div>


    <table
        width="100%%"
        cellpadding="0"
        cellspacing="0"
        border="0"
        style="
            width:100%%;
            border-collapse:collapse;
            background:#f8f8f8;
            border:1px solid #e5e5e5;
        "
    >

        <tr>

            <td
                style="
                    padding:16px;
                    font-family:Arial,Helvetica,sans-serif;
                    font-size:14px;
                    line-height:1.6;
                    color:#333333;
                "
            >

                %s

            </td>

        </tr>

    </table>


    <!-- TRACK BUTTON -->

    <div
        style="
            text-align:center;
            margin-top:28px;
        "
    >

        <a
            href="%s"
            style="
                display:inline-block;
                padding:13px 28px;
                background:#7f1d1d;
                color:#ffffff !important;
                text-decoration:none;
                border-radius:6px;
                font-family:Arial,Helvetica,sans-serif;
                font-size:14px;
                font-weight:bold;
            "
        >

            View Order Status

        </a>

    </div>


    <!-- CLOSING -->

    <p
        style="
            margin-top:28px;
            font-family:Arial,Helvetica,sans-serif;
            font-size:14px;
            line-height:1.7;
            color:#555555;
        "
    >

        If you have any questions regarding your order,
        please contact our support team.

        <br><br>

        Thank you for choosing
        <strong>Lalitha Surya Sweets</strong>.

    </p>

    """.formatted(

            // 1
            name,

            // 2
            statusColor,

            // 3
            statusTitle,

            // 4
            id,

            // 5
            statusMessage,

            // 6
            statusColor,

            // 7
            nextStep,

            // 8
            orderItemsSummary,

            // 9
            id,

            // 10
            orderDate,

            // 11
            totalAmount,

            // 12
            deliveryAddress,

            // 13
            trackUrl
    );


    // =========================================================
    // SEND EMAIL
    // =========================================================

    if (order.getCustomer() != null
            && order.getCustomer().getEmail() != null
            && !order.getCustomer().getEmail().isBlank()) {

        send(
                order.getCustomer().getEmail(),
                subject,
                body,
                null,
                null
        );

    } else {

        System.err.println(
                "Customer email is missing for order #"
                + id
        );
    }
}}
