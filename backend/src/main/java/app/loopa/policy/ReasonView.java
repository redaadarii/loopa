package app.loopa.policy;

/** Motif de retour. {@code requiresPhoto} impose une photo et une validation manuelle. */
public record ReasonView(String code, String labelFr, String labelEn, String labelAr, boolean requiresPhoto,
                         boolean active, int position) {

    public String label(String locale) {
        return switch (locale == null ? "fr" : locale) {
            case "en" -> labelEn;
            case "ar" -> labelAr;
            default -> labelFr;
        };
    }
}
