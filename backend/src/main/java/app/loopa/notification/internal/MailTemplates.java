package app.loopa.notification.internal;

import java.util.Map;

/**
 * Textes des emails, par langue. Volontairement simples (texte brut) : un client lit ces messages
 * sur son téléphone et doit comprendre en une ligne ce qui se passe.
 */
final class MailTemplates {

    private MailTemplates() {
    }

    record Mail(String subject, String body) {
    }

    private static final Map<String, Map<String, String[]>> TEXTS = Map.of(
            "fr", Map.of(
                    "TEAM_NEW", new String[]{"Nouveau retour à traiter : {number}",
                            "{name} demande un retour : {items} ({amount}).\n\nTraiter la demande : {link}"},
                    "CODE", new String[]{"Votre code de vérification {shop}",
                            "Bonjour,\n\nVotre code pour accéder à votre retour chez {shop} est : {code}\n"
                                    + "Il est valable 10 minutes.\n\nSi vous n'êtes pas à l'origine de cette demande, "
                                    + "ignorez ce message."},
                    "SUBMITTED", new String[]{"Demande de retour {number} reçue",
                            "Bonjour {name},\n\nNous avons bien reçu votre demande de retour {number}. "
                                    + "{shop} vous répond rapidement.\n\nSuivi : {link}"},
                    "APPROVED", new String[]{"Votre retour {number} est approuvé",
                            "Bonjour {name},\n\nBonne nouvelle : votre retour {number} est approuvé.\n"
                                    + "Emballez le ou les articles avec le numéro {number} et déposez le colis, "
                                    + "puis indiquez le numéro de suivi ici : {link}"},
                    "REJECTED", new String[]{"Votre retour {number} n'a pas pu être accepté",
                            "Bonjour {name},\n\n{shop} n'a pas pu accepter votre retour {number}.\n"
                                    + "Motif : {reason}\n\nPour toute question, répondez à cet email."},
                    "RESOLVED", new String[]{"Votre retour {number} est terminé",
                            "Bonjour {name},\n\nNous avons reçu vos articles et votre {resolution} de {amount} "
                                    + "a été traité(e). Merci pour votre confiance !"}),
            "en", Map.of(
                    "CODE", new String[]{"Your {shop} verification code",
                            "Hello,\n\nYour code to access your return at {shop} is: {code}\n"
                                    + "It is valid for 10 minutes.\n\nIf you did not request it, ignore this email."},
                    "SUBMITTED", new String[]{"Return request {number} received",
                            "Hello {name},\n\nWe received your return request {number}. "
                                    + "{shop} will get back to you shortly.\n\nTrack it: {link}"},
                    "APPROVED", new String[]{"Your return {number} is approved",
                            "Hello {name},\n\nGood news: your return {number} is approved.\n"
                                    + "Pack the item(s) with the number {number}, drop off the parcel, "
                                    + "then add the tracking number here: {link}"},
                    "REJECTED", new String[]{"Your return {number} could not be accepted",
                            "Hello {name},\n\n{shop} could not accept your return {number}.\n"
                                    + "Reason: {reason}\n\nReply to this email if you have any question."},
                    "RESOLVED", new String[]{"Your return {number} is complete",
                            "Hello {name},\n\nWe received your items and your {resolution} of {amount} "
                                    + "has been processed. Thank you!"}),
            "ar", Map.of(
                    "CODE", new String[]{"رمز التحقق الخاص بك - {shop}",
                            "مرحبًا،\n\nرمز الوصول إلى طلب الإرجاع لدى {shop} هو: {code}\n"
                                    + "صالح لمدة 10 دقائق.\n\nإذا لم تطلب هذا الرمز، تجاهل هذه الرسالة."},
                    "SUBMITTED", new String[]{"تم استلام طلب الإرجاع {number}",
                            "مرحبًا {name}،\n\nتم استلام طلب الإرجاع {number}. سيرد عليك {shop} قريبًا.\n\n"
                                    + "التتبع: {link}"},
                    "APPROVED", new String[]{"تمت الموافقة على الإرجاع {number}",
                            "مرحبًا {name}،\n\nتمت الموافقة على الإرجاع {number}.\n"
                                    + "ضع المنتجات في الطرد مع الرقم {number} ثم أضف رقم التتبع هنا: {link}"},
                    "REJECTED", new String[]{"تعذر قبول الإرجاع {number}",
                            "مرحبًا {name}،\n\nتعذر على {shop} قبول الإرجاع {number}.\nالسبب: {reason}"},
                    "RESOLVED", new String[]{"اكتمل الإرجاع {number}",
                            "مرحبًا {name}،\n\nاستلمنا المنتجات وتمت معالجة {resolution} بقيمة {amount}. شكرًا لك!"}));

    static Mail render(String type, String locale, Map<String, String> values) {
        Map<String, String[]> byType = TEXTS.getOrDefault(locale, TEXTS.get("fr"));
        String[] t = byType.get(type);
        return new Mail(fill(t[0], values), fill(t[1], values));
    }

    private static String fill(String template, Map<String, String> values) {
        String result = template;
        for (Map.Entry<String, String> e : values.entrySet()) {
            result = result.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
        }
        return result;
    }
}
