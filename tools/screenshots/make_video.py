#!/usr/bin/env python3
"""Monte la vidéo de démonstration (loopa-presentation.mp4, à la racine du dépôt) à partir des parcours filmés par capture.py.

Structure : titre → parcours client (portail mobile) → parcours marchand (back-office) → récapitulatif.
Chaque séquence filmée est posée sur un fond aux couleurs de Loopa, avec une légende à gauche.
Prérequis : ffmpeg, Pillow, polices Inter (paquet fonts-inter).

    python3 tools/screenshots/capture.py --video
    python3 tools/screenshots/make_video.py
"""
import pathlib
import subprocess
import tempfile

from PIL import Image, ImageDraw, ImageFont

ROOT = pathlib.Path(__file__).resolve().parents[2]
RAW = ROOT / "docs/video/raw"
OUT = ROOT / "loopa-presentation.mp4"
W, H = 1920, 1080
GREEN, INK, CANVAS, CORAL = (14, 124, 107), (11, 31, 26), (245, 247, 244), (255, 107, 74)
FONT_DIR = pathlib.Path("/usr/share/fonts/opentype/inter")


def font(weight: str, size: int):
    return ImageFont.truetype(str(FONT_DIR / f"Inter-{weight}.otf"), size)


def logo(draw: ImageDraw.ImageDraw, x: int, y: int, size: int, text=True):
    draw.rounded_rectangle([x, y, x + size, y + size], radius=size // 3.5, fill=GREEN)
    s = size / 32
    draw.arc([x + 10 * s, y + 10 * s, x + 22 * s, y + 22 * s], start=180, end=90, fill="white", width=max(2, int(3 * s)))
    draw.line([(x + 15 * s, y + 22 * s), (x + 13 * s, y + 22 * s)], fill="white", width=max(2, int(3 * s)))
    if text:
        draw.text((x + size + size * 0.35, y + size * 0.5), "Loopa", font=font("ExtraBold", int(size * 0.8)), fill=INK, anchor="lm")


def card(path: pathlib.Path, eyebrow: str, title: str, subtitle: str, bullets=()):
    img = Image.new("RGB", (W, H), CANVAS)
    d = ImageDraw.Draw(img)
    d.rectangle([0, H - 14, W, H], fill=GREEN)
    logo(d, 160, 150, 72)
    d.text((160, 380), eyebrow.upper(), font=font("Bold", 30), fill=GREEN)
    y = 430
    for line in title.split("\n"):
        d.text((160, y), line, font=font("ExtraBold", 92), fill=INK)
        y += 110
    d.text((160, y + 20), subtitle, font=font("Medium", 38), fill=(91, 107, 102))
    y += 110
    for b in bullets:
        d.ellipse([162, y + 14, 178, y + 30], fill=CORAL)
        d.text((200, y), b, font=font("Medium", 34), fill=INK)
        y += 58
    img.save(path)


def stage(path: pathlib.Path, step: str, title: str, lines: list[str], phone: bool):
    """Fond d'une séquence filmée : légende à gauche, emplacement de la vidéo à droite (ou en bas)."""
    img = Image.new("RGB", (W, H), CANVAS)
    d = ImageDraw.Draw(img)
    if phone:
        d.rectangle([1060, 0, W, H], fill=(221, 236, 231))
        logo(d, 120, 110, 54)
        d.text((120, 300), step.upper(), font=font("Bold", 28), fill=GREEN)
        y = 345
        for line in title.split("\n"):
            d.text((120, y), line, font=font("ExtraBold", 64), fill=INK)
            y += 78
        y += 30
        for line in lines:
            d.ellipse([122, y + 12, 136, y + 26], fill=CORAL)
            d.text((158, y), line, font=font("Medium", 32), fill=INK)
            y += 56
    else:
        d.text((60, 36), f"{step.upper()} · {title.replace(chr(10), ' ')}", font=font("Bold", 30), fill=INK)
    img.save(path)


def run(*args):
    subprocess.run(args, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def main():
    portal = next((RAW / "portal").glob("*.webm"))
    admin = next((RAW / "admin").glob("*.webm"))
    with tempfile.TemporaryDirectory() as tmp:
        t = pathlib.Path(tmp)
        card(t / "intro.png", "Démonstration", "Les retours qui font\nrevenir vos clients.",
             "Loopa · retours et échanges pour boutiques Shopify",
             ["Portail client à vos couleurs, sans compte (fr · en · ar)",
              "Avoir majoré, échange ou remboursement",
              "Back-office : approbation, suivi, statistiques"])
        card(t / "outro.png", "Loopa", "Moins de remboursements,\nplus de ventes.",
             "Spring Boot · Nuxt · PostgreSQL — monolithe modulaire",
             ["Approbation automatique selon vos règles",
              "Bonus shop-now pour orienter vers l'avoir",
              "Résolution exécutée dans Shopify à la réception"])
        stage(t / "portal-bg.png", "1 · Côté client", "Le client fait\nson retour en\n2 minutes",
              ["N° de commande + code reçu", "Choix des articles et motifs", "Photo si l'article est abîmé",
               "Avoir +50 MAD mis en avant", "Échange : approbation auto", "Suivi du colis"], phone=True)
        stage(t / "admin-bg.png", "2 · Côté marchand", "Le marchand traite le retour", [], phone=False)

        clips = []
        def still(png, seconds, name):
            out = t / f"{name}.mp4"
            run("ffmpeg", "-y", "-loop", "1", "-t", str(seconds), "-i", str(png), "-f", "lavfi", "-t", str(seconds),
                "-i", "anullsrc=r=48000:cl=stereo", "-vf", "fps=30,format=yuv420p,fade=t=in:st=0:d=0.4,fade=t=out:st="
                f"{seconds - 0.4}:d=0.4", "-c:v", "libx264", "-preset", "veryfast", "-crf", "20", "-c:a", "aac",
                "-shortest", str(out))
            clips.append(out)

        still(t / "intro.png", 5, "c0")
        # Portail : téléphone centré dans la partie droite (hauteur 1000 px).
        out = t / "c1.mp4"
        run("ffmpeg", "-y", "-loop", "1", "-i", str(t / "portal-bg.png"), "-i", str(portal), "-f", "lavfi", "-i",
            "anullsrc=r=48000:cl=stereo", "-filter_complex",
            "[1:v]scale=-2:1000:flags=lanczos,fps=30[p];[0:v][p]overlay=x=1060+(860-overlay_w)/2:y=40:shortest=1,format=yuv420p",
            "-c:v", "libx264", "-preset", "veryfast", "-crf", "20", "-c:a", "aac", "-shortest", str(out))
        clips.append(out)
        # Back-office : vidéo 1440×900 agrandie à 1600×1000 sous le bandeau de titre, accélérée ×1,25.
        out = t / "c2.mp4"
        run("ffmpeg", "-y", "-loop", "1", "-i", str(t / "admin-bg.png"), "-i", str(admin), "-f", "lavfi", "-i",
            "anullsrc=r=48000:cl=stereo", "-filter_complex",
            "[1:v]setpts=PTS/1.25,scale=1600:1000,fps=30[a];[0:v][a]overlay=x=(1920-overlay_w)/2:y=80:shortest=1,"
            "format=yuv420p", "-c:v", "libx264", "-preset", "veryfast", "-crf", "20", "-c:a", "aac", "-shortest",
            str(out))
        clips.append(out)
        still(t / "outro.png", 5, "c3")

        listing = t / "list.txt"
        listing.write_text("".join(f"file '{c}'\n" for c in clips))
        OUT.parent.mkdir(parents=True, exist_ok=True)
        run("ffmpeg", "-y", "-f", "concat", "-safe", "0", "-i", str(listing), "-c:v", "libx264", "-preset",
            "veryfast", "-crf", "21", "-pix_fmt", "yuv420p", "-c:a", "aac", "-movflags", "+faststart", str(OUT))
    print(OUT)


if __name__ == "__main__":
    main()
