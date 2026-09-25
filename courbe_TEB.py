"""
Trace la courbe TEB = f(Eb/N0) a partir du fichier CSV produit par simulateur.CourbeTEB.

Usage : python3 trace_teb.py [teb.csv] [courbe_teb.png]
Necessite : matplotlib et numpy  (pip install matplotlib numpy)
"""
import sys
import csv
import math

import numpy as np
import matplotlib.pyplot as plt

# ----------------------------------------------------------------------
# 1. Parametres
# ----------------------------------------------------------------------
FICHIER_CSV = sys.argv[1] if len(sys.argv) > 1 else "teb.csv"
FICHIER_PNG = sys.argv[2] if len(sys.argv) > 2 else "courbe_teb.png"

# En dessous de ce nombre d'erreurs, le TEB mesure n'est pas fiable : on ecarte le point
ERREURS_MIN = 10

# ----------------------------------------------------------------------
# 2. Lecture du CSV : on range les points par forme d'onde
# ----------------------------------------------------------------------
snr = {"NRZ": [], "NRZT": [], "RZ": []}
teb = {"NRZ": [], "NRZT": [], "RZ": []}

with open(FICHIER_CSV) as f:
    lecteur = csv.DictReader(f, delimiter=";")
    for ligne in lecteur:
        forme = ligne["forme"]
        if int(ligne["nbErreurs"]) < ERREURS_MIN:
            continue  # point pas assez fiable
        snr[forme].append(float(ligne["snrpb_dB"]))
        teb[forme].append(float(ligne["TEB"]))

# ----------------------------------------------------------------------
# 3. Courbes theoriques (pour valider le simulateur)
# ----------------------------------------------------------------------
def Q(x):
    """Fonction Q : probabilite qu'une gaussienne centree reduite depasse x."""
    return 0.5 * math.erfc(x / math.sqrt(2))

def teb_theorique(forme, snr_db):
    g = 10 ** (snr_db / 10)          # dB -> valeur lineaire
    if forme == "NRZ":               # antipodal, moyenne sur tout le bit
        return Q(math.sqrt(2 * g)   )
    if forme == "NRZT":              # Ps = 7A^2/9, moyenne sur le tiers central
        return Q(math.sqrt(6 * g / 7))
    if forme == "RZ":                # Ps = A^2/6, moyenne sur le tiers central
        return Q(math.sqrt(g))

# ----------------------------------------------------------------------
# 4. Trace
# ----------------------------------------------------------------------
couleur = {"NRZ": "blue", "NRZT": "green", "RZ": "red"}
marqueur = {"NRZ": "o", "NRZT": "s", "RZ": "d"}
legende = {"NRZ": "NRZ [-1, 1]", "NRZT": "NRZT [-1, 1]", "RZ": "RZ [0, 1]"}

plt.figure(figsize=(8, 6))
x_theorie = np.linspace(0, 14, 300)

for forme in ["NRZ", "NRZT", "RZ"]:
    # Points mesures par le simulateur
    plt.semilogy(snr[forme], teb[forme], marker=marqueur[forme], color=couleur[forme],
                 markerfacecolor="none", label=legende[forme] + " (simulé)")
    # Courbe theorique en pointilles
    plt.semilogy(x_theorie, [teb_theorique(forme, x) for x in x_theorie], "--",
                 color=couleur[forme], alpha=0.5, label=legende[forme] + " (théorique)")

plt.xlim(0, 14)
plt.ylim(1e-6, 1)
plt.xlabel("Eb/N0 (dB)")
plt.ylabel("TEB")
plt.title("TEB en fonction de Eb/N0 (10⁶ bits par point, 30 éch./bit)")
plt.grid(True, which="both", linestyle=":", alpha=0.6)
plt.legend(loc="lower left")
plt.tight_layout()
plt.savefig(FICHIER_PNG, dpi=150)
print("Courbe enregistree dans", FICHIER_PNG)
plt.show()