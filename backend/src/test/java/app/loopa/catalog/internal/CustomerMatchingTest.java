package app.loopa.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CustomerMatchingTest {

    @Test
    void orderNumberFormatsAreEquivalent() {
        assertThat(CatalogService.normalizeName("#1042")).isEqualTo("1042");
        assertThat(CatalogService.normalizeName(" # 1042 ")).isEqualTo("1042");
        assertThat(CatalogService.normalizeName("1042")).isEqualTo("1042");
    }

    @Test
    void phoneMatchesWithOrWithoutCountryCode() {
        OrderEntity order = order("nadia@exemple.ma", "212612345678");

        assertThat(CatalogService.contactMatches(order, "0612345678")).isTrue();
        assertThat(CatalogService.contactMatches(order, "+212 6 12 34 56 78")).isTrue();
        assertThat(CatalogService.contactMatches(order, "0612345679")).isFalse();
        assertThat(CatalogService.contactMatches(order, "5678")).isFalse();
    }

    @Test
    void emailIsCaseInsensitiveButExact() {
        OrderEntity order = order("nadia@exemple.ma", null);

        assertThat(CatalogService.contactMatches(order, "Nadia@Exemple.MA")).isTrue();
        assertThat(CatalogService.contactMatches(order, "nadia@exemple.com")).isFalse();
    }

    private static OrderEntity order(String email, String phone) {
        OrderEntity o = new OrderEntity();
        o.customerEmail = email;
        o.customerPhone = phone;
        return o;
    }
}
