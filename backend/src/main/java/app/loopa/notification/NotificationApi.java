package app.loopa.notification;

/** Façade publique du module notification. */
public interface NotificationApi {

    /** Envoie le code de vérification du portail par email (ou SMS quand une passerelle est configurée). */
    void sendPortalCode(String shopName, String contact, String code, String locale);
}
