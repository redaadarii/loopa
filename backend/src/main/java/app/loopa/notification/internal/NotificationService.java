package app.loopa.notification.internal;

import app.loopa.notification.NotificationApi;
import app.loopa.returns.ReturnEvents.ReturnApproved;
import app.loopa.returns.ReturnEvents.ReturnRejected;
import app.loopa.returns.ReturnEvents.ReturnResolved;
import app.loopa.returns.ReturnEvents.ReturnSubmitted;
import app.loopa.returns.ReturnView;
import app.loopa.returns.ReturnsApi;
import app.loopa.shared.tenant.TenantContext;
import app.loopa.tenancy.ShopView;
import app.loopa.tenancy.TenancyApi;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Emails au client final. Les écouteurs sont asynchrones et s'exécutent après commit ; ils fixent le tenant
 * à partir de l'événement avant toute lecture (Hibernate filtre par tenant à l'ouverture de session).
 */
@Service
class NotificationService implements NotificationApi {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mail;
    private final ReturnsApi returns;
    private final TenancyApi tenancy;
    private final String from;
    private final String portalBaseUrl;
    private final String adminBaseUrl;

    NotificationService(JavaMailSender mail, ReturnsApi returns, TenancyApi tenancy,
                        @Value("${loopa.mail.from:Loopa <no-reply@loopa.app>}") String from,
                        @Value("${loopa.portal.base-url:http://localhost:3001}") String portalBaseUrl,
                        @Value("${loopa.shopify.admin-url:http://localhost:3002}") String adminBaseUrl) {
        this.mail = mail;
        this.returns = returns;
        this.tenancy = tenancy;
        this.from = from;
        this.portalBaseUrl = portalBaseUrl;
        this.adminBaseUrl = adminBaseUrl;
    }

    @Override
    public void sendPortalCode(String shopName, String contact, String code, String locale) {
        if (!contact.contains("@")) {
            // TODO : passerelle SMS (Twilio, Infobip) pour les clients identifiés par téléphone.
            log.info("Code portail pour {} : envoi SMS non configuré", mask(contact));
            return;
        }
        send(contact, MailTemplates.render("CODE", locale, Map.of("shop", shopName, "code", code)));
    }

    @Async
    @TransactionalEventListener
    public void on(ReturnSubmitted e) {
        notifyCustomer(e.tenantId(), e.returnId(), "SUBMITTED");
        notifyTeam(e.tenantId(), e.returnId());
    }

    @Async
    @TransactionalEventListener
    public void on(ReturnApproved e) {
        notifyCustomer(e.tenantId(), e.returnId(), "APPROVED");
    }

    @Async
    @TransactionalEventListener
    public void on(ReturnRejected e) {
        notifyCustomer(e.tenantId(), e.returnId(), "REJECTED");
    }

    @Async
    @TransactionalEventListener
    public void on(ReturnResolved e) {
        notifyCustomer(e.tenantId(), e.returnId(), "RESOLVED");
    }

    private void notifyCustomer(UUID tenantId, UUID returnId, String type) {
        TenantContext.runAs(tenantId, () -> {
            ReturnView r = returns.find(returnId).orElse(null);
            ShopView shop = tenancy.findById(tenantId).orElse(null);
            if (r == null || shop == null || r.customerEmail() == null) {
                return;
            }
            Map<String, String> values = new HashMap<>();
            values.put("shop", shop.name());
            values.put("name", r.customerName() == null ? "" : r.customerName().split(" ")[0]);
            values.put("number", r.number());
            values.put("reason", r.rejectionReason());
            values.put("link", portalBaseUrl + "/" + shop.handle() + "/returns/" + r.id());
            values.put("resolution", resolutionLabel(r.resolutionType(), r.locale()));
            values.put("amount", format(r.creditMinor(), r.currency(), r.locale()));
            send(r.customerEmail(), MailTemplates.render(type, r.locale(), values));
        });
    }

    /** Prévient l'équipe d'une demande qui attend une décision (pas pour les retours approuvés automatiquement). */
    private void notifyTeam(UUID tenantId, UUID returnId) {
        TenantContext.runAs(tenantId, () -> {
            ReturnView r = returns.find(returnId).orElse(null);
            if (r == null || !"SUBMITTED".equals(r.status())) {
                return;
            }
            String items = r.lines().stream().map(l -> l.title() + " ×" + l.quantity())
                    .reduce((a, b) -> a + ", " + b).orElse("");
            Map<String, String> values = new HashMap<>();
            values.put("number", r.number());
            values.put("name", r.customerName() == null ? "" : r.customerName());
            values.put("items", items);
            values.put("amount", format(r.totalMinor(), r.currency(), "fr"));
            values.put("link", adminBaseUrl + "/returns/" + r.id());
            MailTemplates.Mail mail = MailTemplates.render("TEAM_NEW", "fr", values);
            tenancy.teamEmails(tenantId).forEach(email -> send(email, mail));
        });
    }

    private void send(String to, MailTemplates.Mail content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(content.subject());
            message.setText(content.body());
            mail.send(message);
        } catch (MailException ex) {
            // Un email perdu ne doit jamais bloquer un retour : on journalise et on continue.
            log.warn("Envoi d'email impossible à {} : {}", mask(to), ex.getMessage());
        }
    }

    static String resolutionLabel(String type, String locale) {
        return switch (locale) {
            case "en" -> switch (type) { case "REFUND" -> "refund"; case "EXCHANGE" -> "exchange";
                default -> "store credit"; };
            case "ar" -> switch (type) { case "REFUND" -> "استرداد المبلغ"; case "EXCHANGE" -> "الاستبدال";
                default -> "الرصيد"; };
            default -> switch (type) { case "REFUND" -> "remboursement"; case "EXCHANGE" -> "échange";
                default -> "avoir"; };
        };
    }

    static String format(long minor, String currency, String locale) {
        NumberFormat f = NumberFormat.getCurrencyInstance(Locale.forLanguageTag(
                "ar".equals(locale) ? "ar-MA" : "en".equals(locale) ? "en-US" : "fr-MA"));
        f.setCurrency(Currency.getInstance(currency));
        return f.format(minor / 100.0);
    }

    static String mask(String contact) {
        if (contact == null || contact.length() < 4) {
            return "***";
        }
        int at = contact.indexOf('@');
        return at > 1 ? contact.charAt(0) + "***" + contact.substring(at) : "***" + contact.substring(contact.length() - 2);
    }
}
