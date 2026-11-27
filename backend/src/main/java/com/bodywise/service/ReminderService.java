package com.bodywise.service;

import com.bodywise.domain.MealType;
import com.bodywise.domain.User;
import com.bodywise.repo.UserRepo;
import com.bodywise.web.Dtos.*;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Every evening (7 pm by default) mails each user a checklist of what to buy for tomorrow's plan.
 * The email is pixel-styled HTML (tables + inline styles, which Gmail and phone mail apps render)
 * with a plain-text alternative.
 */
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
                log.info("Grocery reminder for user {}: {}", u.id, sendFor(u));
            } catch (Exception e) {
                log.warn("Grocery reminder for user {} failed: {}", u.id, e.getMessage());
            }
        }
    }

    /** Sends tomorrow's checklist now. Returns a human-readable result. */
    public String sendFor(User u) throws Exception {
        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
        GroceryView view = grocery.view(u.id, tomorrow);
        String to = view.reminderEmail();
        String subject = "🛒 Your Bodywise checklist for " + tomorrow.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH));

        if (!grocery.mailConfigured()) {
            log.info("Mail not configured (MAIL_HOST empty). Would send to {}:\n{}", to, plain(u, view));
            return "Email is not configured on the server yet (MAIL_HOST / MAIL_USERNAME / MAIL_PASSWORD). The list was written to the server log.";
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) return "No mail sender available.";

        MimeMessage msg = sender.createMimeMessage();
        MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
        if (from != null && !from.isBlank()) h.setFrom(from, "Bodywise");
        h.setTo(to);
        h.setSubject(subject);
        h.setText(plain(u, view), html(u, view));
        sender.send(msg);
        return "Sent to " + to;
    }

    // ------------------------------------------------------------------ plain text

    public String plain(User u, GroceryView view) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hi ").append(firstName(u)).append(",\n\n");
        if (view.toBuy().isEmpty()) {
            sb.append("Nothing left to buy for ").append(view.forDate()).append(". You're all set!\n");
        } else {
            sb.append("Here's your checklist for ").append(view.forDate())
              .append(view.planTitle() != null ? " (" + view.planTitle() + ")" : "").append(":\n");
            for (GroceryDish d : view.dishes()) {
                if (d.ingredients().stream().allMatch(GroceryIngredient::purchased)) continue;
                sb.append("\n").append(d.dish()).append("\n");
                for (GroceryIngredient i : d.ingredients()) if (!i.purchased()) sb.append("  [ ] ").append(i.name()).append("\n");
            }
            for (ListDto l : view.lists()) {
                List<GItemDto> left = l.items().stream().filter(i -> !i.purchased()).toList();
                if (left.isEmpty()) continue;
                sb.append("\n").append(l.name()).append("\n");
                for (GItemDto i : left) {
                    sb.append("  [ ] ").append(i.name());
                    if (i.quantity() != null && !i.quantity().isBlank()) sb.append(" (").append(i.quantity()).append(")");
                    sb.append("\n");
                }
            }
        }
        sb.append("\nTick items off at ").append(frontendUrl).append("/grocery\n");
        return sb.toString();
    }

    // ------------------------------------------------------------------ pixel HTML

    private static final String BG = "#0b0d10", PANEL = "#12151a", LINE = "#313843", INK = "#e8ecf1",
            MUTED = "#8b949e", PINK = "#ff8fb8", MUSTARD = "#f5c542";

    private static final Map<MealType, String[]> MEAL_STYLE = Map.of(
            MealType.EARLY_MORNING, new String[]{"Early morning", "#f5c542"},
            MealType.BREAKFAST, new String[]{"Breakfast", "#ffa66b"},
            MealType.MID_MORNING, new String[]{"Mid morning", "#ff8fb8"},
            MealType.LUNCH, new String[]{"Lunch", "#5fe0b4"},
            MealType.TEA_TIME, new String[]{"Tea time", "#ffa66b"},
            MealType.DINNER, new String[]{"Dinner", "#b8a1ff"},
            MealType.POST_DINNER, new String[]{"Post dinner", "#79c4ff"},
            MealType.SWEETS, new String[]{"Sweets", "#ff8fb8"});

    /** The bodywise logo (pink leaf slanting like "/", same as the favicon), drawn as a table of coloured cells. */
    private static final String[] LOGO = {
        "................", "...........oooo.", "........ooommmo.", "......oommmmpmo.",
        ".....ommmmmpmmo.", "....ommmmmpmmmo.", "...ommmmmpmmmo..", "...ommmmpmmmmo..",
        "..ommmmpmmmmo...", "..ommmpmmmmo....", "..ommpmmmmo.....", "..ompmmmoo......",
        "..opmooo........", ".opo............", "op..............",
    };
    private static final Map<Character, String> LOGO_COLORS = Map.of('o', "#f6eedc", 'm', "#ff4f9a", 'p', "#ff9ec7");

    private static String pixelLogo(int px) {
        StringBuilder sb = new StringBuilder("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse\">");
        for (String row : LOGO) {
            sb.append("<tr>");
            for (char c : row.toCharArray()) {
                String color = LOGO_COLORS.get(c);
                sb.append("<td width=\"").append(px).append("\" height=\"").append(px).append("\" style=\"width:").append(px)
                  .append("px;height:").append(px).append("px;padding:0;font-size:0;line-height:0;")
                  .append(color != null ? "background:" + color + ";" : "").append("\"></td>");
            }
            sb.append("</tr>");
        }
        return sb.append("</table>").toString();
    }

    private static String box(String color) {
        return "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td style=\"width:12px;height:12px;border:2px solid " + color
                + ";background:" + PANEL + ";font-size:0;line-height:0\">&nbsp;</td></tr></table>";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String firstName(User u) {
        if (u.name != null && !u.name.isBlank()) return u.name.trim().split("\\s+")[0];
        return "there";
    }

    public String html(User u, GroceryView view) {
        String font = "font-family:'Courier New',Courier,monospace;";
        String date = view.forDate().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH));
        StringBuilder b = new StringBuilder();
        b.append("<!doctype html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width\">")
         .append("<title>Bodywise checklist</title></head>")
         .append("<body style=\"margin:0;padding:0;background:").append(BG).append(";\">")
         .append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background:").append(BG).append(";\"><tr><td align=\"center\" style=\"padding:24px 10px;\">")
         .append("<table width=\"560\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:100%;max-width:560px;background:").append(PANEL)
         .append(";border:3px solid ").append(LINE).append(";border-radius:14px;").append(font).append("color:").append(INK).append(";\">");

        // header
        b.append("<tr><td style=\"padding:22px 24px 8px;\"><table cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr>")
         .append("<td valign=\"middle\" style=\"padding-right:14px;\">").append(pixelLogo(3)).append("</td>")
         .append("<td valign=\"middle\"><div style=\"").append(font).append("font-size:28px;font-weight:bold;color:").append(PINK).append(";letter-spacing:1px;\">bodywise</div>")
         .append("<div style=\"").append(font).append("font-size:13px;color:").append(MUTED).append(";\">$ grocery --for tomorrow</div></td>")
         .append("</tr></table></td></tr>");

        // greeting
        b.append("<tr><td style=\"padding:8px 24px 4px;font-size:16px;line-height:1.5;\">hi ").append(esc(firstName(u)))
         .append("! here's your shopping checklist for <b style=\"color:").append(MUSTARD).append(";\">").append(esc(date)).append("</b>")
         .append(view.planTitle() != null ? " &middot; card <b style=\"color:" + PINK + ";\">" + esc(view.planTitle()) + "</b>" : "")
         .append("</td></tr>");

        if (view.toBuy().isEmpty()) {
            b.append("<tr><td style=\"padding:18px 24px;\"><div style=\"border:2px solid #3fb950;background:#12241a;color:#3fb950;padding:14px;text-align:center;font-size:16px;border-radius:8px;\">")
             .append("&#10003; nothing left to buy. you're all set!</div></td></tr>");
        }

        // dishes grouped by meal
        MealType current = null;
        for (GroceryDish d : view.dishes()) {
            List<GroceryIngredient> left = d.ingredients().stream().filter(i -> !i.purchased()).toList();
            if (left.isEmpty()) continue;
            if (d.mealType() != current) {
                current = d.mealType();
                String[] st = MEAL_STYLE.get(current);
                b.append("<tr><td style=\"padding:18px 24px 4px;\"><div style=\"").append(font).append("font-size:15px;font-weight:bold;color:").append(st[1])
                 .append(";border-bottom:2px dashed ").append(LINE).append(";padding-bottom:6px;letter-spacing:1px;\">&#9632; ").append(st[0].toUpperCase()).append("</div></td></tr>");
            }
            b.append("<tr><td style=\"padding:8px 24px 2px 30px;\"><div style=\"font-size:15px;font-weight:bold;color:").append(INK).append(";\">").append(esc(d.dish()))
             .append(d.amount() != null ? " <span style=\"font-weight:normal;color:" + MUTED + ";font-size:13px;\">&middot; " + esc(d.amount()) + "</span>" : "")
             .append("</div></td></tr>");
            for (GroceryIngredient i : left) {
                b.append("<tr><td style=\"padding:3px 24px 3px 38px;\"><table cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td valign=\"middle\" style=\"padding-right:10px;\">")
                 .append(box(MUSTARD)).append("</td><td valign=\"middle\" style=\"font-size:15px;color:").append(INK).append(";\">").append(esc(i.name())).append("</td></tr></table></td></tr>");
            }
        }

        // own lists
        for (ListDto l : view.lists()) {
            List<GItemDto> left = l.items().stream().filter(i -> !i.purchased()).toList();
            if (left.isEmpty()) continue;
            b.append("<tr><td style=\"padding:18px 24px 4px;\"><div style=\"").append(font).append("font-size:15px;font-weight:bold;color:#5fe0b4;border-bottom:2px dashed ")
             .append(LINE).append(";padding-bottom:6px;letter-spacing:1px;\">&#9632; ").append(esc(l.name()).toUpperCase()).append("</div></td></tr>");
            for (GItemDto i : left) {
                b.append("<tr><td style=\"padding:3px 24px 3px 38px;\"><table cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td valign=\"middle\" style=\"padding-right:10px;\">")
                 .append(box("#5fe0b4")).append("</td><td valign=\"middle\" style=\"font-size:15px;color:").append(INK).append(";\">").append(esc(i.name()))
                 .append(i.quantity() != null && !i.quantity().isBlank() ? " <span style=\"color:" + MUTED + ";\">&middot; " + esc(i.quantity()) + "</span>" : "")
                 .append("</td></tr></table></td></tr>");
            }
        }

        // footer
        b.append("<tr><td align=\"center\" style=\"padding:26px 24px 10px;\">")
         .append("<a href=\"").append(esc(frontendUrl)).append("/grocery\" style=\"display:inline-block;background:").append(PINK)
         .append(";color:#15171c;text-decoration:none;font-weight:bold;font-size:15px;padding:12px 22px;border:3px solid #15171c;border-radius:8px;box-shadow:4px 4px 0 #000;").append(font).append("\">")
         .append("&#9654; tick them off</a></td></tr>")
         .append("<tr><td align=\"center\" style=\"padding:6px 24px 22px;font-size:12px;color:").append(MUTED).append(";\">")
         .append("sent by bodywise every evening at 7 pm &middot; turn it off in profile</td></tr>")
         .append("</table></td></tr></table></body></html>");
        return b.toString();
    }
}
