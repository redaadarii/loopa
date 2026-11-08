#!/usr/bin/env python3
"""Génère le jeu de données de démonstration de Loopa (boutique « Atlas Store »).

Sortie : backend/src/main/resources/demo/seed.json, lu à la fois par le back-end (profil dev)
et par l'API simulée du front (frontend/tools/mock-api). Les dates sont relatives (« il y a N minutes »)
pour que la démo reste fraîche. Déterministe : relancer le script donne le même fichier.

Usage : python3 tools/demo-seed/generate.py
"""
import json, random, uuid, pathlib

rnd = random.Random(2026)
NS = uuid.UUID("6f1c2a0e-5b1d-4c3e-9a77-1f0e2d3c4b5a")
def uid(*parts): return str(uuid.uuid5(NS, "/".join(map(str, parts))))

SHOP_ID = "00000000-0000-0000-0000-000000000001"
DAY = 24 * 60

products = [
    ("Chemise en lin", ["S", "M", "L", "XL"], 29900, "Chemises"),
    ("Pantalon chino", ["38", "40", "42", "44"], 45000, "Pantalons"),
    ("Robe midi fleurie", ["XS", "S", "M", "L"], 54900, "Robes"),
    ("Baskets cuir blanc", ["39", "40", "41", "42", "43"], 79000, "Chaussures"),
    ("Veste en jean", ["S", "M", "L"], 69900, "Vestes"),
    ("T-shirt coton bio", ["S", "M", "L", "XL"], 14900, "T-shirts"),
    ("Djellaba brodée", ["M", "L", "XL"], 89000, "Traditionnel"),
    ("Sac cabas cuir", ["Unique"], 64900, "Accessoires"),
    ("Babouches artisanales", ["38", "39", "40", "41"], 24900, "Chaussures"),
    ("Pull maille fine", ["S", "M", "L"], 39900, "Pulls"),
]
first = ["Nadia", "Omar", "Salma", "Youssef", "Imane", "Mehdi", "Kenza", "Hamza", "Sara", "Anas", "Leila", "Rachid",
         "Ghita", "Ayoub", "Meryem", "Ilyas", "Hiba", "Adil", "Zineb", "Karim", "Nora", "Amine", "Houda", "Saad"]
last = ["Benali", "El Amrani", "Kabbaj", "Tazi", "Chraibi", "Bennani", "Alaoui", "Idrissi", "Berrada", "Fassi",
        "Lahlou", "Sqalli", "Ouazzani", "Tahiri", "Mansouri", "Bakkali", "Naciri", "Skalli", "Zniber", "Hajji"]
gateways = ["Paiement à la livraison"] * 6 + ["CMI"] * 3 + ["Shopify Payments"]

def phone(): return "+2126" + "".join(str(rnd.randint(0, 9)) for _ in range(8))
def slug(s): return s.lower().replace(" ", ".").replace("é", "e")

orders = []
# Commande de démonstration du portail : récente, sans retour, contact connu.
demo_lines = [(0, "M", 1), (1, "40", 1), (2, "S", 1)]
order_no = 1001
def make_order(name, cust_name, email, ph, days_ago, picks):
    oid = uid("order", name)
    lines = []
    for i, (pi, variant, qty) in enumerate(picks):
        title, _, price, ptype = products[pi]
        ext = str(14500000000 + len(orders) * 10 + i)
        lines.append({"externalLineId": ext, "productId": str(8100000000 + pi), "variantId": str(45000000000 + pi * 10 + i),
                      "title": title, "variantTitle": variant, "sku": f"ATL-{pi:02d}-{variant}", "productType": ptype,
                      "quantity": qty, "unitPriceMinor": price, "imageUrl": None})
    total = sum(l["unitPriceMinor"] * l["quantity"] for l in lines) + 3000
    return {"id": oid, "externalId": str(6100000000 + len(orders)), "name": name, "customerName": cust_name,
            "customerEmail": email, "customerPhone": ph, "currency": "MAD", "totalMinor": total,
            "paymentGateway": rnd.choice(gateways), "orderedMinutesAgo": days_ago * DAY + rnd.randint(0, 600),
            "fulfilledMinutesAgo": max(0, (days_ago - 2) * DAY), "lines": lines}

for n in range(100):
    days_ago = rnd.randint(3, 9) if n % 7 == 0 else (rnd.randint(4, 38) if rnd.random() < 0.8 else rnd.randint(39, 80))
    fn, ln = rnd.choice(first), rnd.choice(last)
    picks = []
    for _ in range(rnd.choice([1, 1, 2, 2, 3])):
        pi = rnd.randrange(len(products))
        if any(p[0] == pi for p in picks):
            continue
        picks.append((pi, rnd.choice(products[pi][1]), rnd.choice([1, 1, 1, 2])))
    orders.append(make_order(f"#{order_no}", f"{fn} {ln}", f"{slug(fn)}.{slug(ln)}@exemple.ma", phone(), days_ago, picks))
    order_no += 1
demo = make_order("#1042", "Nadia Benali", "nadia@exemple.ma", "+212612345678", 4, demo_lines)
demo["paymentGateway"] = "Paiement à la livraison"
orders = [o for o in orders if o["name"] != "#1042"] + [demo]
orders.sort(key=lambda o: o["orderedMinutesAgo"], reverse=True)

reason_weights = [("TOO_SMALL", 34), ("TOO_BIG", 14), ("DAMAGED", 20), ("NOT_AS_DESCRIBED", 16), ("CHANGED_MIND", 16)]
res_weights = [("STORE_CREDIT", 46), ("EXCHANGE", 31), ("REFUND", 23)]
def pick(weights):
    return rnd.choices([w[0] for w in weights], [w[1] for w in weights])[0]

agents = ["Yasmine Alaoui", "Karim Benjelloun"]
returns = []
number = 1
candidates = [o for o in orders if o["name"] != "#1042"]
rnd.shuffle(candidates)
for o in candidates[:84]:
    created = o["fulfilledMinutesAgo"] - rnd.randint(1 * DAY, 9 * DAY)
    if created < 30:
        created = rnd.randint(30, 2 * DAY)
    resolution = pick(res_weights)
    lines = []
    for l in o["lines"][: rnd.choice([1, 1, 2])]:
        reason = pick(reason_weights)
        alt = [v for v in next(p for p in products if p[0] == l["title"])[1] if v != l["variantTitle"]]
        lines.append({"orderLineId": l["externalLineId"], "title": l["title"], "variantTitle": l["variantTitle"],
                      "quantity": 1, "unitPriceMinor": l["unitPriceMinor"], "currency": "MAD", "reasonCode": reason,
                      "exchangeVariantId": (rnd.choice(alt) if resolution == "EXCHANGE" and alt else None)})
    photo = any(l["reasonCode"] in ("DAMAGED", "NOT_AS_DESCRIBED") for l in lines)
    total = sum(l["unitPriceMinor"] * l["quantity"] for l in lines)
    auto = (not photo) and total <= 100000
    age_days = created / DAY
    # Plus le retour est ancien, plus il est avancé dans son cycle de vie.
    if age_days > 12:
        status = rnd.choices(["RESOLVED", "REJECTED", "CANCELLED"], [84, 10, 6])[0]
    elif age_days > 6:
        status = rnd.choices(["RESOLVED", "RECEIVED", "IN_TRANSIT", "REJECTED"], [55, 15, 22, 8])[0]
    elif age_days > 2:
        status = rnd.choices(["IN_TRANSIT", "APPROVED", "SUBMITTED", "RECEIVED"], [25, 25, 35, 15])[0]
    else:
        status = rnd.choices(["SUBMITTED", "APPROVED"], [60, 40])[0]
    if status == "SUBMITTED":
        auto = False
    hist = [{"type": "SUBMITTED", "actor": "CUSTOMER", "message": None, "minutesAgo": created}]
    approved_after = None
    t = created
    rejection = None
    carrier = tracking = ref = None
    def step(kind, actor, msg, delay):
        global t
        t = max(1, t - delay)
        hist.append({"type": kind, "actor": actor, "message": msg, "minutesAgo": t})
    if status == "REJECTED":
        rejection = rnd.choice(["Article porté et lavé : non éligible au retour.",
                                "Photos ne montrant aucun défaut.", "Article hors délai de retour."])
        step("REJECTED", rnd.choice(agents), rejection, rnd.randint(40, 600))
    elif status == "CANCELLED":
        step("CANCELLED", "CUSTOMER", None, rnd.randint(60, 1500))
    elif status != "SUBMITTED":
        delay = 1 if auto else rnd.randint(25, 520)
        approved_after = delay
        step("APPROVED", "SYSTEM" if auto else rnd.choice(agents), "Approbation automatique" if auto else None, delay)
        if status in ("IN_TRANSIT", "RECEIVED", "RESOLVED"):
            carrier = rnd.choice(["Amana", "Sendit", "CTM Messagerie", "Cathedis"])
            tracking = f"{carrier[:2].upper()}{rnd.randint(100000000, 999999999)}MA"
            step("SHIPPED", "CUSTOMER", f"{carrier} · {tracking}", rnd.randint(300, 2 * DAY))
        if status in ("RECEIVED", "RESOLVED"):
            step("RECEIVED", rnd.choice(agents), None, rnd.randint(DAY, 3 * DAY))
        if status == "RESOLVED":
            prefix = {"REFUND": "REFUND", "EXCHANGE": "EXCHANGE", "STORE_CREDIT": "GIFTCARD"}[resolution]
            ref = f"{prefix}-{number:06d}"
            step("RESOLVED", "SYSTEM", ref, rnd.randint(1, 5))
    fn = o["customerName"]
    returns.append({
        "id": uid("return", number), "number": f"RMA-2026-{number:06d}", "orderId": o["id"], "orderName": o["name"],
        "customerName": fn, "customerEmail": o["customerEmail"], "locale": rnd.choices(["fr", "ar", "en"], [80, 15, 5])[0],
        "status": status, "resolutionType": resolution, "bonusMinor": 5000 if resolution == "STORE_CREDIT" else 0,
        "customerNote": rnd.choice([None, None, None, "La couture de l'épaule s'est défaite au premier lavage.",
                                    "Taille trop juste, je préfère la taille au-dessus.", "Couleur différente de la photo."]),
        "rejectionReason": rejection, "carrier": carrier, "trackingNumber": tracking, "resolutionRef": ref,
        "createdMinutesAgo": created, "approvedAfterMinutes": approved_after, "updatedMinutesAgo": t,
        "lines": lines, "history": hist,
    })
    number += 1
returns.sort(key=lambda r: r["createdMinutesAgo"])
# Renumérote du plus ancien au plus récent pour des numéros croissants dans le temps.
for i, r in enumerate(sorted(returns, key=lambda r: -r["createdMinutesAgo"]), start=1):
    r["number"] = f"RMA-2026-{i:06d}"
    if r["resolutionRef"]:
        r["resolutionRef"] = r["resolutionRef"].rsplit("-", 1)[0] + f"-{i:06d}"
        for h in r["history"]:
            if h["type"] == "RESOLVED":
                h["message"] = r["resolutionRef"]

seed = {
    "password": "Loopa2026!",
    "shop": {"id": SHOP_ID, "handle": "atlas", "name": "Atlas Store", "platform": "DEMO", "currency": "MAD",
             "defaultLocale": "fr", "plan": "GROWTH", "primaryColor": "#0E7C6B", "logoUrl": None,
             "welcomeMessage": "Retours gratuits sous 30 jours. Échangez ou recevez un avoir majoré de 50 MAD."},
    "users": [
        {"id": uid("user", 1), "email": "yasmine@atlas-store.ma", "name": "Yasmine Alaoui", "role": "OWNER"},
        {"id": uid("user", 2), "email": "karim@atlas-store.ma", "name": "Karim Benjelloun", "role": "AGENT"},
        {"id": uid("user", 3), "email": "salma@atlas-store.ma", "name": "Salma Kettani", "role": "ADMIN"},
    ],
    "policy": {"windowDays": 30, "autoApprove": True, "autoApproveMaxAmountMinor": 100000, "allowRefund": True,
               "allowExchange": True, "allowStoreCredit": True, "storeCreditBonusMinor": 5000,
               "excludedProductTypes": ["Lingerie"]},
    "reasons": [
        {"code": "TOO_SMALL", "labelFr": "Trop petit", "labelEn": "Too small", "labelAr": "صغير جدًا", "requiresPhoto": False},
        {"code": "TOO_BIG", "labelFr": "Trop grand", "labelEn": "Too big", "labelAr": "كبير جدًا", "requiresPhoto": False},
        {"code": "DAMAGED", "labelFr": "Article endommagé", "labelEn": "Damaged item", "labelAr": "منتج تالف", "requiresPhoto": True},
        {"code": "NOT_AS_DESCRIBED", "labelFr": "Différent de la description", "labelEn": "Not as described", "labelAr": "مختلف عن الوصف", "requiresPhoto": True},
        {"code": "CHANGED_MIND", "labelFr": "J'ai changé d'avis", "labelEn": "Changed my mind", "labelAr": "غيرت رأيي", "requiresPhoto": False},
    ],
    "orders": orders,
    "returns": returns,
}
out = pathlib.Path(__file__).resolve().parents[2] / "backend/src/main/resources/demo/seed.json"
out.write_text(json.dumps(seed, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
print(f"{out} : {len(orders)} commandes, {len(returns)} retours")
