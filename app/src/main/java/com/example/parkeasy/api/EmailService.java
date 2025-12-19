package com.example.parkeasy.api;

import android.os.AsyncTask;
import android.util.Log;
import com.example.parkeasy.model.Booking;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailService {

    // 🔒 CREDENTIALS (Use App Password)
    private static final String SENDER_EMAIL = "your-email@gmail.com";
    private static final String SENDER_PASSWORD = "your-app-password";

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
                message.setFrom(new InternetAddress(SENDER_EMAIL, "Scan2Pay Admin"));
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
                message.setSubject("Booking Confirmed: " + booking.getSlotName());

                String htmlBody = getHtmlReceipt(userName, booking);
                message.setContent(htmlBody, "text/html; charset=utf-8");

                Transport.send(message);
                Log.d("EmailService", "Receipt Sent Successfully!");

            } catch (Exception e) {
                Log.e("EmailService", "Failed to send email", e);
            }
            return null;
        }

        private String getHtmlReceipt(String name, Booking b) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            String dateStr = sdf.format(b.getStartTime());

            return "<html><body style='background-color:#0F1628; color:#ffffff; font-family: sans-serif; padding:20px;'>"
                    + "<div style='background-color:#151932; padding:20px; border-radius:10px; border:1px solid #00F0FF; max-width:500px; margin:auto;'>"
                    + "<h2 style='color:#00F0FF; text-align:center;'>BOOKING CONFIRMED</h2>"
                    + "<p style='text-align:center; color:#7A8BA0;'>Hi " + name + ", your spot is secured.</p>"
                    + "<hr style='border-color:#333;'>"
                    + "<table style='width:100%; color:#fff;'>"
                    + "<tr><td style='padding:8px; color:#7A8BA0;'>Location</td><td style='text-align:right; font-weight:bold;'>" + b.getLocationName() + "</td></tr>"
                    + "<tr><td style='padding:8px; color:#7A8BA0;'>Slot ID</td><td style='text-align:right; font-weight:bold; color:#FF00FF;'>" + b.getSlotName() + "</td></tr>"
                    + "<tr><td style='padding:8px; color:#7A8BA0;'>Date</td><td style='text-align:right;'>" + dateStr + "</td></tr>"
                    + "<tr><td style='padding:8px; color:#7A8BA0;'>Duration</td><td style='text-align:right;'>" + b.getDurationHours() + " Hours</td></tr>"
                    + "<tr><td style='padding:8px; color:#7A8BA0;'>Vehicle</td><td style='text-align:right;'>" + b.getVehicleNumber() + "</td></tr>"
                    + "</table>"
                    + "<hr style='border-color:#333;'>"
                    // 👇 UPDATED TO USE getTotalCost()
                    + "<h1 style='text-align:center; color:#00FF88;'>₹" + (int)b.getTotalCost() + ".00</h1>"
                    + "<p style='text-align:center; font-size:12px; color:#505050;'>TxID: " + b.getBookingId() + "</p>"
                    + "</div></body></html>";
        }
    }
}