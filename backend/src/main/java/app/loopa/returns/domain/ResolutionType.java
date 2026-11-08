package app.loopa.returns.domain;

/** Ce que le client obtient en échange de son retour. */
public enum ResolutionType {
    /** Remboursement sur le moyen de paiement d'origine (ou virement pour le paiement à la livraison). */
    REFUND,
    /** Échange contre une autre variante ou un autre produit. */
    EXCHANGE,
    /** Avoir / carte cadeau, éventuellement majoré d'un bonus. */
    STORE_CREDIT
}
