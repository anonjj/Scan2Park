package com.example.parkeasy.api;

import android.os.AsyncTask;
import android.util.Log;
import com.example.parkeasy.model.Booking;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailService {
    // ✅ FIXED: Corrected variable name (removed 'x')
    private static final String SENDER_EMAIL = "parkeasy.dev@gmail.com";
    private static final String SENDER_PASSWORD = "pcte wand baqk xman"; // Your App Password

    public static void sendBookingReceipt(String userEmail, Booking booking, String userName) {
        new SendMailTask(userEmail, booking, userName).execute();
    }

    private static class SendMailTask extends AsyncTask<Void, Void, Void> {
        private final String email;
        private final Booking booking;
        private final String userName;

        public SendMailTask(String email, Booking booking, String userName) {
            this.email = email;
            this.booking = booking;
            this.userName = userName;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.socketFactory.port", "465");
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.port", "465");

                Session session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                    }
                });

                MimeMessage message = new MimeMessage(session);

                // ✅ Updated Sender Name
                message.setFrom(new InternetAddress(SENDER_EMAIL, "Scan2Pay Support"));

                message.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
                message.setSubject("Booking Confirmed: " + booking.getSlotName());

                String htmlBody = getHtmlReceipt(userName, booking);
                message.setContent(htmlBody, "text/html; charset=utf-8");

                Transport.send(message);
                Log.d("EmailService", "Receipt Sent Successfully to " + email);

            } catch (Exception e) {
                Log.e("EmailService", "Failed to send email", e);
            }
            return null;
        }

        private String getHtmlReceipt(String name, Booking b) {
            // Safety check: Use current time if booking time is missing
            Date timeToDisplay = b.getStartTime() != null ? b.getStartTime() : new Date();

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            String dateStr = sdf.format(timeToDisplay);

            // ✅ FIXED: Clean String Concatenation for the Modern Template
            return "<html><body style='background-color:#F4F6F8; color:#1A1D1E; font-family: Helvetica, Arial, sans-serif; padding:20px;'>"
                    + "<div style='background-color:#FFFFFF; padding:30px; border-radius:8px; border:1px solid #E0E0E0; max-width:500px; margin:auto; box-shadow: 0 2px 4px rgba(0,0,0,0.05);'>"

                    // Header
                    + "<h2 style='color:#1A1D1E; text-align:center; margin-bottom:0;'>Booking Confirmed</h2>"
                    + "<p style='text-align:center; color:#6B7280; font-size:14px; margin-top:5px;'>Hi " + name + ", here is your receipt.</p>"

                    // Divider
                    + "<div style='margin: 20px 0; border-top:1px solid #E0E0E0;'></div>"

                    // Table Details
                    + "<table style='width:100%; border-collapse: collapse;'>"
                    + "<tr><td style='padding:8px 0; color:#6B7280; font-size:14px;'>Location</td><td style='text-align:right; font-weight:bold; color:#1A1D1E;'>" + b.getLocationName() + "</td></tr>"
                    + "<tr><td style='padding:8px 0; color:#6B7280; font-size:14px;'>Slot ID</td><td style='text-align:right; font-weight:bold; color:#2962FF;'>" + b.getSlotName() + "</td></tr>"
                    + "<tr><td style='padding:8px 0; color:#6B7280; font-size:14px;'>Date</td><td style='text-align:right; color:#1A1D1E;'>" + dateStr + "</td></tr>"
                    + "<tr><td style='padding:8px 0; color:#6B7280; font-size:14px;'>Duration</td><td style='text-align:right; color:#1A1D1E;'>" + b.getDurationHours() + " Hours</td></tr>"
                    + "<tr><td style='padding:8px 0; color:#6B7280; font-size:14px;'>Vehicle</td><td style='text-align:right; color:#1A1D1E;'>" + b.getVehicleNumber() + "</td></tr>"
                    + "</table>"

                    // Divider
                    + "<div style='margin: 20px 0; border-top:1px dashed #E0E0E0;'></div>"

                    // Total Amount
                    + "<p style='text-align:center; color:#6B7280; font-size:12px; margin:0;'>Total Amount Paid</p>"
                    + "<h1 style='text-align:center; color:#1A1D1E; margin:5px 0 20px 0; font-size:32px;'>₹" + (int)b.getTotalCost() + ".00</h1>"

                    // Transaction ID Footer
                    + "<div style='background-color:#F9FAFB; padding:10px; border-radius:4px; text-align:center;'>"
                    + "<p style='margin:0; font-size:11px; color:#9CA3AF; font-family: monospace;'>Transaction Ref: " + b.getBookingId() + "</p>"
                    + "</div>"

                    + "</div>" // End of Card
                    + "<p style='text-align:center; color:#9CA3AF; font-size:12px; margin-top:20px;'>© Scan2Pay Parking Systems</p>"
                    + "</body></html>";
        }
    }
}