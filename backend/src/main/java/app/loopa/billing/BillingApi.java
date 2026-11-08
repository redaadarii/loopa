package app.loopa.billing;

/** Façade publique du module billing (tenant courant). */
public interface BillingApi {

    /** Lève une erreur 402 si le quota mensuel de retours du plan est atteint. */
    void assertCanCreateReturn();
}
