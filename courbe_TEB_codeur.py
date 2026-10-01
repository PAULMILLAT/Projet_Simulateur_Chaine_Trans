"""
Trace la comparaison des courbes TEB = f(Eb/N0) avec et sans codage de canal (-codeur).
Pour la forme d'onde NRZ antipodale [-1, 1] (conforme au TP3).

Usage :
    python courbe_TEB_codeur.py
"""

import math
import numpy as np
import matplotlib.pyplot as plt

def Q(x):
    """Fonction Q : probabilite qu'une gaussienne centree reduite depasse x."""
    return 0.5 * math.erfc(x / math.sqrt(2.0))

def teb_theorique_non_code(snr_db):
    """TEB theorique pour NRZ antipodal [-1, 1] non code : Q(sqrt(2 * gamma))."""
    gamma = 10.0 ** (snr_db / 10.0)
    return Q(math.sqrt(2.0 * gamma))

def teb_theorique_code(snr_db):
    """TEB theorique pour code (3,1) avec decision par mot le plus proche : 3p^2 - 2p^3."""
    p = teb_theorique_non_code(snr_db)
    return 3.0 * (p ** 2) - 2.0 * (p ** 3)

def tracer_courbes(fichiers_png=["courbe_teb_codeur.png", "B3.Cecotti.Jacq.Millat.Plougonven--Lastennet.rapport.etape4.pdf/figures/courbe_teb_codeur.png"]):
    # Courbes continues theoriques
    snr_theorie = np.linspace(0.0, 8.5, 300)
    teb_th_nc = [teb_theorique_non_code(s) for s in snr_theorie]
    teb_th_c = [teb_theorique_code(s) for s in snr_theorie]

    # Points mesures par le Simulateur SIT213 (100 000 bits par point, NRZ [-1, 1], seed 100)
    snr_mesures_nc = [0, 1, 2, 3, 4, 5, 6, 7, 8]
    teb_mesures_nc = [0.07856, 0.05590, 0.03743, 0.02295, 0.01253, 0.00601, 0.00247, 0.00085, 0.00020]

    # Pour le codage, a 7 et 8 dB sur 100 000 bits, 0 erreur detectee (TEB=0 non tracable en log)
    snr_mesures_c = [0, 1, 2, 3, 4, 5, 6]
    teb_mesures_c = [0.01704, 0.00864, 0.00396, 0.00165, 0.00055, 0.00017, 0.00002]

    plt.figure(figsize=(9, 6.5), dpi=200)

    # 1. Non code
    plt.semilogy(snr_theorie, teb_th_nc, "b-", label=r"Théorie : Non codé $Q(\sqrt{2\gamma})$", linewidth=2)
    plt.semilogy(snr_mesures_nc, teb_mesures_nc, "bo", label="Simulation : Non codé (NRZ [-1, 1])",
                 markersize=7, markerfacecolor="none", markeredgewidth=1.8)

    # 2. Avec codeur
    plt.semilogy(snr_theorie, teb_th_c, "r-", label=r"Théorie : Avec codage $(3,1)$ ($3p^2 - 2p^3$)", linewidth=2)
    plt.semilogy(snr_mesures_c, teb_mesures_c, "rs", label="Simulation : Avec codage (-codeur)",
                 markersize=7, markerfacecolor="none", markeredgewidth=1.8)

    plt.grid(True, which="both", linestyle=":", alpha=0.6)
    plt.xlim(0, 8.5)
    plt.ylim(1e-5, 0.15)
    plt.xlabel(r"$E_b/N_0$ en dB (par symbole de canal)", fontsize=12)
    plt.ylabel("Taux d'Erreur Binaire (TEB)", fontsize=12)
    plt.title("Comparaison du TEB théorique et simulé avec et sans codage (3, 1)", fontsize=13, fontweight="bold")
    plt.legend(loc="lower left", fontsize=11)
    plt.tight_layout()

    for f in fichiers_png:
        plt.savefig(f, dpi=200)
        print(f"Graphique enregistre dans : {f}")
    plt.close()

if __name__ == "__main__":
    tracer_courbes()

