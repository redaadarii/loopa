#!/usr/bin/env python3
"""Captures d'écran et vidéos de démonstration de Loopa, prises sur l'application qui tourne.

Prérequis : API (simulée ou réelle) sur :8080, vitrine :3000, portail :3001, back-office :3002.
    cd frontend && pnpm mock & pnpm dev
    python3 tools/screenshots/capture.py            # captures → docs/screenshots
    python3 tools/screenshots/capture.py --video    # + vidéos brutes → docs/video/raw

Le parcours est celui d'un vrai utilisateur : le client crée ses retours sur le portail (avec photo),
puis le marchand les traite dans le back-office.
"""
import argparse
import pathlib
import re
import time
import urllib.request

from playwright.sync_api import Page, expect, sync_playwright

ROOT = pathlib.Path(__file__).resolve().parents[2]
SHOTS = ROOT / "docs/screenshots"
RAW = ROOT / "docs/video/raw"
PHOTO = pathlib.Path(__file__).with_name("damaged-shirt.jpg")
WEB, PORTAL, ADMIN = "http://localhost:3000", "http://localhost:3001", "http://localhost:3002"
API = "http://localhost:8080"


def reset_demo_data():
    """API simulée uniquement : remet la commande de démonstration #1042 sans retour."""
    try:
        urllib.request.urlopen(urllib.request.Request(f"{API}/__reset", method="POST"), timeout=5)
    except OSError:
        print("  (pas de /__reset : API réelle, on continue avec ses données)")

MOBILE = dict(viewport={"width": 390, "height": 844}, device_scale_factor=2, is_mobile=True, has_touch=True)
DESKTOP = dict(viewport={"width": 1440, "height": 900}, device_scale_factor=1)


def new_page(ctx):
    page = ctx.new_page()
    page.set_default_timeout(60_000)
    return page


def hydrated(page: Page):
    """Attend que Vue ait pris la main sur le HTML rendu côté serveur (sinon la saisie est perdue)."""
    page.wait_for_function("() => !!document.querySelector('#__nuxt')?.__vue_app__ && !window.__NUXT__?.isHydrating")
    page.wait_for_timeout(300)


def shot(page: Page, name: str, full_page=False):
    page.mouse.move(2, 2)  # aucun bouton en état « survol » sur la capture
    page.wait_for_timeout(600)
    page.screenshot(path=str(SHOTS / f"{name}.png"), full_page=full_page)
    print("  •", name)


def pause(page: Page, ms: int, video: bool):
    if video:
        page.wait_for_timeout(ms)


def portal_flow(page: Page, video=False, capture=True):
    """Parcours client : deux retours sur la commande #1042 (photo obligatoire, puis échange auto-approuvé)."""
    page.goto(f"{PORTAL}/atlas")
    page.get_by_role("heading", name="Retourner ou échanger un article").wait_for()
    hydrated(page)
    pause(page, 1200, video)
    if capture:
        shot(page, "03-portail-accueil")
    page.get_by_label("Numéro de commande").fill("1042") if not video else page.get_by_label("Numéro de commande").type("1042", delay=90)
    page.get_by_label("Email ou téléphone de la commande").type("nadia@exemple.ma", delay=45 if video else 0)
    pause(page, 500, video)
    page.get_by_role("button", name="Retrouver ma commande").click()

    page.get_by_role("heading", name="Entrez le code reçu").wait_for()
    pause(page, 900, video)
    if capture:
        shot(page, "04-portail-code")
    page.get_by_role("button", name=re.compile("Démo : votre code est")).click()
    page.wait_for_url(re.compile(r"/atlas/order"))

    page.get_by_role("heading", name="Quels articles souhaitez-vous retourner ?").wait_for()
    pause(page, 900, video)
    page.get_by_role("button", name=re.compile("Chemise en lin")).click()
    pause(page, 400, video)
    page.get_by_role("button", name=re.compile("Robe midi fleurie")).click()
    pause(page, 700, video)
    if capture:
        shot(page, "05-portail-articles")
    page.get_by_role("button", name="Continuer").click()

    groups = page.get_by_role("radiogroup")
    groups.first.wait_for()
    pause(page, 600, video)
    groups.nth(0).get_by_text("Trop petit", exact=True).click()
    pause(page, 500, video)
    groups.nth(1).get_by_text("Article endommagé", exact=True).click()
    pause(page, 500, video)
    page.locator("input[type=file]").set_input_files(str(PHOTO))
    page.locator("img[src^='blob:']").first.wait_for()
    pause(page, 900, video)
    if capture:
        shot(page, "06-portail-motif-photo", full_page=True)
    page.get_by_role("button", name="Continuer").click()

    page.get_by_role("heading", name="Que souhaitez-vous en échange ?").wait_for()
    pause(page, 1400, video)
    if capture:
        shot(page, "07-portail-solution")
    page.get_by_role("button", name="Continuer").click()

    page.get_by_role("heading", name="Vérifiez votre demande").wait_for()
    page.get_by_label("Un message pour la boutique ? (facultatif)").type(
        "La couture de l'épaule s'est défaite au premier lavage.", delay=25 if video else 0)
    pause(page, 800, video)
    if capture:
        shot(page, "08-portail-recapitulatif")
    page.get_by_role("button", name="Envoyer ma demande").click()
    page.get_by_role("heading", name="Demande envoyée").wait_for()
    pause(page, 1500, video)
    if capture:
        shot(page, "09-portail-demande-envoyee")

    # Deuxième retour : le pantalon, échange de taille → approbation automatique, ajout du suivi.
    page.get_by_role("link", name="Retourner un autre article").click()
    page.get_by_role("heading", name="Quels articles souhaitez-vous retourner ?").wait_for()
    pause(page, 700, video)
    page.get_by_role("button", name=re.compile("Pantalon chino")).click()
    page.get_by_role("button", name="Continuer").click()
    page.get_by_role("radiogroup").first.get_by_text("Trop petit", exact=True).click()
    pause(page, 500, video)
    page.get_by_role("button", name="Continuer").click()
    page.get_by_role("heading", name="Que souhaitez-vous en échange ?").wait_for()
    page.get_by_text("Échange", exact=True).click()
    pause(page, 400, video)
    page.get_by_placeholder("Ex. : taille L").type("Taille 42", delay=60 if video else 0)
    pause(page, 600, video)
    page.get_by_role("button", name="Continuer").click()
    page.get_by_role("button", name="Envoyer ma demande").click()
    page.get_by_role("heading", name="Retour approuvé").wait_for()
    pause(page, 1500, video)
    if capture:
        shot(page, "10-portail-retour-approuve")
    page.get_by_placeholder("AM123456789MA").type("AM482915637MA", delay=60 if video else 0)
    pause(page, 400, video)
    page.get_by_role("button", name="Enregistrer le suivi").click()
    page.get_by_role("heading", name="Colis en route").wait_for()
    pause(page, 1600, video)
    if capture:
        shot(page, "11-portail-suivi")


def portal_arabic(page: Page):
    page.context.add_cookies([{"name": "loopa_lang", "value": "ar", "url": PORTAL}])
    page.goto(f"{PORTAL}/atlas")
    page.get_by_role("heading", name="إرجاع أو استبدال منتج").wait_for()
    shot(page, "12-portail-arabe-rtl")


def admin_flow(page: Page, video=False, capture=True):
    page.goto(f"{ADMIN}/login")
    page.get_by_role("heading", name="Connexion au back-office").wait_for()
    pause(page, 900, video)
    if capture:
        shot(page, "13-admin-connexion")
    page.get_by_role("button", name=re.compile("Compte de démonstration")).click()
    pause(page, 600, video)
    page.get_by_role("button", name="Se connecter").click()
    page.get_by_role("heading", name=re.compile("Bonjour Yasmine")).wait_for()
    page.get_by_text("Retours par jour").wait_for()
    pause(page, 2500, video)
    if capture:
        shot(page, "14-admin-tableau-de-bord", full_page=True)

    page.get_by_role("link", name=re.compile(r"^Retours")).first.click()
    page.get_by_placeholder("N° de retour, client, commande…").wait_for()
    page.get_by_role("row").nth(3).wait_for()
    pause(page, 1500, video)
    if capture:
        shot(page, "15-admin-liste-retours")
    if capture:
        # Approbation groupée : onglet « À traiter », deux retours cochés.
        page.get_by_role("tab", name=re.compile("^À traiter")).click()
        page.wait_for_timeout(1200)
        boxes = page.get_by_role("checkbox", name=re.compile(r"^RMA-"))
        boxes.nth(1).click()
        boxes.nth(2).click()
        page.get_by_text(re.compile(r"retour\(s\) sélectionné")).wait_for()
        shot(page, "23-admin-approbation-groupee")
        page.get_by_role("button", name="Annuler").click()
        page.get_by_role("tab", name=re.compile("^Tous")).click()
        page.wait_for_timeout(800)
    page.get_by_placeholder("N° de retour, client, commande…").type("Nadia", delay=80 if video else 0)
    page.wait_for_timeout(900)
    pause(page, 800, video)
    # Le retour avec photo (à traiter) est le plus récent, en tête de liste.
    page.get_by_role("row").filter(has_text="À traiter").first.click()
    page.get_by_text("Photos du client").wait_for()
    page.locator("img[src^='blob:']").first.wait_for()
    pause(page, 2200, video)
    if capture:
        shot(page, "16-admin-detail-retour", full_page=True)

    if video:
        page.get_by_placeholder("Ajouter une note interne…").type("Défaut de couture confirmé sur les photos.", delay=35)
        page.keyboard.press("Enter")
        page.wait_for_timeout(1000)
        page.get_by_role("button", name="Approuver").click()
        page.get_by_text("Retour approuvé").first.wait_for()
        page.wait_for_timeout(1500)
        page.get_by_role("button", name="Marquer reçu").click()
        page.wait_for_timeout(2600)
        page.reload()
        page.get_by_text("Résolution exécutée").first.wait_for()
        page.wait_for_timeout(2000)

    if video:
        # Navigation par la barre latérale (navigation côté client, sans rechargement).
        page.get_by_role("button", name="Réglages").first.click()
        page.wait_for_timeout(500)
    for path, title, name in [
        ("/settings/policy", "Politique de retour", "17-admin-politique"),
        ("/settings/branding", "Portail client", "18-admin-personnalisation-portail"),
        ("/settings/team", "Équipe", "19-admin-equipe"),
        ("/settings/billing", "Abonnement", "20-admin-abonnement"),
        ("/settings/integrations", "Intégrations", "21-admin-integrations"),
    ]:
        if video:
            page.get_by_role("link", name=title, exact=True).first.click()
        else:
            page.goto(f"{ADMIN}{path}")
        page.get_by_role("heading", name=title).first.wait_for()
        page.wait_for_load_state("networkidle")
        pause(page, 1800, video)
        if capture:
            shot(page, name, full_page=path == "/settings/policy")
        if video and path == "/settings/branding":
            page.get_by_role("button", name="#7C3AED").click()
            page.wait_for_timeout(1500)
            page.get_by_role("button", name="#0E7C6B").click()
            page.wait_for_timeout(800)

    if capture:
        page.goto(f"{ADMIN}/account")
        page.get_by_role("heading", name="Mon compte").first.wait_for()
        page.get_by_label("Mot de passe actuel").fill("Loopa2026!")
        page.get_by_label("Nouveau mot de passe", exact=True).fill("Atlas-2026-retours")
        page.get_by_label("Confirmer le nouveau mot de passe").fill("Atlas-2026-retours")
        shot(page, "24-admin-mon-compte")


def admin_mobile(page: Page):
    page.goto(f"{ADMIN}/login")
    page.get_by_role("button", name=re.compile("Compte de démonstration")).click()
    page.get_by_role("button", name="Se connecter").click()
    page.get_by_role("heading", name=re.compile("Bonjour Yasmine")).wait_for()
    page.get_by_text("Retours par jour").wait_for()
    shot(page, "22-admin-mobile")


def website(page: Page):
    page.goto(WEB)
    page.get_by_role("heading", name=re.compile("Les retours qui font")).wait_for()
    shot(page, "01-vitrine-accueil")
    page.goto(f"{WEB}/pricing")
    page.get_by_role("heading", name="Des tarifs qui grandissent avec vous").wait_for()
    shot(page, "02-vitrine-tarifs")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--video", action="store_true", help="enregistre aussi les parcours en vidéo")
    parser.add_argument("--video-only", action="store_true", help="vidéos seulement, sans recapturer les écrans")
    args = parser.parse_args()
    SHOTS.mkdir(parents=True, exist_ok=True)
    with sync_playwright() as p:
        browser = p.chromium.launch(channel="chrome", headless=True)
        # Les serveurs de dev compilent chaque page à sa première visite : délais généreux.
        expect.set_options(timeout=60_000)
        common = dict(locale="fr-FR", timezone_id="Africa/Casablanca", color_scheme="light")

        if args.video_only:
            args.video = True
        print("Vitrine") if not args.video_only else None
        if not args.video_only:
            ctx = browser.new_context(**DESKTOP, **common)
            website(new_page(ctx))
            ctx.close()

            reset_demo_data()
            print("Portail client (mobile)")
            ctx = browser.new_context(**MOBILE, **common)
            portal_flow(new_page(ctx))
            portal_arabic(new_page(ctx))
            ctx.close()

            print("Back-office")
            ctx = browser.new_context(**DESKTOP, **common)
            admin_flow(new_page(ctx))
            ctx.close()
            ctx = browser.new_context(**MOBILE, **common)
            admin_mobile(new_page(ctx))
            ctx.close()

        if args.video:
            RAW.mkdir(parents=True, exist_ok=True)
            reset_demo_data()
            print("Vidéo : parcours client")
            ctx = browser.new_context(**MOBILE, **common, record_video_dir=str(RAW / "portal"),
                                      record_video_size={"width": 390, "height": 844})
            portal_flow(new_page(ctx), video=True, capture=False)
            ctx.close()
            print("Vidéo : back-office")
            ctx = browser.new_context(**DESKTOP, **common, record_video_dir=str(RAW / "admin"),
                                      record_video_size={"width": 1440, "height": 900})
            admin_flow(new_page(ctx), video=True, capture=False)
            ctx.close()
        browser.close()


if __name__ == "__main__":
    main()
