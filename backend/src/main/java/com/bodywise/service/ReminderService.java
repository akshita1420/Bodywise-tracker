package com.bodywise.service;

import com.bodywise.domain.User;
import com.bodywise.repo.UserRepo;
import com.bodywise.web.Dtos.GroceryView;
import com.bodywise.web.Dtos.ToBuy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Every evening (7 pm by default) mails each user what to buy for tomorrow's plan. */
@Service
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    private final UserRepo users;
    private final GroceryService grocery;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final Clock clock;
    private final String from;
    private final String frontendUrl;

    public ReminderService(UserRepo users, GroceryService grocery, ObjectProvider<JavaMailSender> mailSender,
                           Clock clock, @Value("${app.mail.from:}") String from,
                           @Value("${app.frontend-url}") String frontendUrl) {
        this.users = users;
        this.grocery = grocery;
        this.mailSender = mailSender;
        this.clock = clock;
        this.from = from;
        this.frontendUrl = frontendUrl;
    }

    @Scheduled(cron = "${app.reminder.cron}", zone = "${app.timezone}")
    public void sendDailyReminders() {
        for (User u : users.findByRemindersEnabledTrue()) {
            try {
                String result = sendFor(u);
                log.info("Grocery reminder for user {}: {}", u.id, result);
            } catch (Exception e) {
                log.warn("Grocery reminder for user {} failed: {}", u.id, e.getMessage());
            }
        }
    }

    /** Sends tomorrow's list now. Returns a human-readable result. */
    public String sendFor(User u) {
        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
        GroceryView view = grocery.view(u.id, tomorrow);
        String to = view.reminderEmail();
        String subject = "Bodywise: buy these for " + tomorrow.format(DateTimeFormatter.ofPattern("EEE d MMM"));
        String body = body(u, view);

        if (!grocery.mailConfigured()) {
            log.info("Mail not configured (MAIL_HOST empty). Would send to {}:\n{}", to, body);
            return "Email is not configured on the server (set MAIL_HOST etc.). The list was written to the server log.";
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) return "No mail sender available.";

        SimpleMailMessage msg = new SimpleMailMessage();
        if (from != null && !from.isBlank()) msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        sender.send(msg);
        return "Sent to " + to;
    }

    private String body(User u, GroceryView view) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hi ").append(u.name != null && !u.name.isBlank() ? u.name : "there").append(",\n\n");
        if (view.toBuy().isEmpty()) {
            sb.append("Nothing left to buy for ").append(view.forDate()).append(". You're all set!\n");
        } else {
            sb.append("Here's what to pick up so you're ready for ").append(view.forDate()).append(":\n");
            Map<String, List<ToBuy>> bySource = new LinkedHashMap<>();
            for (ToBuy t : view.toBuy()) bySource.computeIfAbsent(t.source(), k -> new java.util.ArrayList<>()).add(t);
            bySource.forEach((source, list) -> {
                sb.append("\n").append(source).append("\n");
                for (ToBuy t : list) {
                    sb.append("  [ ] ").append(t.name());
                    if (t.quantity() != null && !t.quantity().isBlank()) sb.append(" (").append(t.quantity()).append(")");
                    sb.append("\n");
                }
            });
        }
        sb.append("\nTick items off at ").append(frontendUrl).append("/grocery\n");
        return sb.toString();
    }
}
