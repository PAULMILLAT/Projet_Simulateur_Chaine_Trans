"""
Script de trace des courbes de comparaison pour le TP5 :
1. courbe_comparaison_formes_onde.png : Comparaison NRZ [-1, 1], NRZT [-1, 1] et RZ [0, 1] avec et sans codage.
2. courbe_comparaison_trajets_multiples.png : Comparaison canal direct AWGN vs canal multi-trajets avec et sans codage.
3. courbe_comparaison_tp5_complete.png : Figure combinant les deux analyses pour le rapport.
"""

import json
import os
import matplotlib.pyplot as plt
import numpy as np

def charger_donnees(fichier="resultats_simus_tp5.json"):
    if not os.path.exists(fichier):
        print(f"Fichier {fichier} introuvable.")
        return None
    with open(fichier, "r", encoding="utf-8") as f:
        return json.load(f)

def filtrer_zeros(x, y):
    """Filtre les points ou TEB == 0.0 pour affichage semi-log."""
    x_filtre, y_filtre = [], []
    for xi, yi in zip(x, y):
        if yi is not None and yi > 0.0:
            x_filtre.append(xi)
            y_filtre.append(yi)
    return x_filtre, y_filtre

def tracer_formes_onde(data, dossiers_sortie=["."]):
    snrs = data["snrs"]
    f = data["formes"]

    plt.figure(figsize=(9, 6.5), dpi=200)

    # NRZ [-1, 1]
    x_nrz_nc, y_nrz_nc = filtrer_zeros(snrs, f["NRZ_nc"])
    x_nrz_c, y_nrz_c = filtrer_zeros(snrs, f["NRZ_c"])
    plt.semilogy(x_nrz_nc, y_nrz_nc, "b-o", linewidth=2, markersize=7, label="NRZ [-1, 1] : Sans codage")
    plt.semilogy(x_nrz_c, y_nrz_c, "b--d", linewidth=2, markersize=7, markerfacecolor="white", markeredgewidth=2, label="NRZ [-1, 1] : Avec codage (-codeur)")

    # NRZT [-1, 1]
    x_nrzt_nc, y_nrzt_nc = filtrer_zeros(snrs, f["NRZT_nc"])
    x_nrzt_c, y_nrzt_c = filtrer_zeros(snrs, f["NRZT_c"])
    plt.semilogy(x_nrzt_nc, y_nrzt_nc, "g-s", linewidth=2, markersize=7, label="NRZT [-1, 1] : Sans codage")
    plt.semilogy(x_nrzt_c, y_nrzt_c, "g--^", linewidth=2, markersize=7, markerfacecolor="white", markeredgewidth=2, label="NRZT [-1, 1] : Avec codage (-codeur)")

    # RZ [0, 1]
    x_rz_nc, y_rz_nc = filtrer_zeros(snrs, f["RZ_nc"])
    x_rz_c, y_rz_c = filtrer_zeros(snrs, f["RZ_c"])
    plt.semilogy(x_rz_nc, y_rz_nc, "r-v", linewidth=2, markersize=7, label="RZ [0, 1] : Sans codage")
    plt.semilogy(x_rz_c, y_rz_c, "r--x", linewidth=2, markersize=8, markeredgewidth=2, label="RZ [0, 1] : Avec codage (-codeur)")

    plt.grid(True, which="both", linestyle=":", alpha=0.6)
    plt.xlim(-0.2, 8.5)
    plt.ylim(1e-5, 0.25)
    plt.xlabel(r"$E_b/N_0$ en dB", fontsize=12)
    plt.ylabel("Taux d'Erreur Binaire (TEB)", fontsize=12)
    plt.title("Comparaison du TEB selon la forme d'onde avec et sans codage de canal", fontsize=13, fontweight="bold")
    plt.legend(loc="lower left", fontsize=10.5, framealpha=0.95)
    plt.tight_layout()

    for d in dossiers_sortie:
        chemin = os.path.join(d, "courbe_comparaison_formes_onde.png")
        os.makedirs(d, exist_ok=True)
        plt.savefig(chemin, dpi=200)
        print(f"Figure sauvegardee : {chemin}")
    plt.close()

def tracer_trajets_multiples(data, dossiers_sortie=["."]):
    snrs = data["snrs"]
    t = data["trajets"]

    plt.figure(figsize=(9, 6.5), dpi=200)

    # AWGN pur (NRZ)
    x_awgn_nc, y_awgn_nc = filtrer_zeros(snrs, t["AWGN_nc"])
    x_awgn_c, y_awgn_c = filtrer_zeros(snrs, t["AWGN_c"])
    plt.semilogy(x_awgn_nc, y_awgn_nc, "b-o", linewidth=2, markersize=7, label="Canal AWGN sans écho : Sans codage")
    plt.semilogy(x_awgn_c, y_awgn_c, "b--d", linewidth=2, markersize=7, markerfacecolor="white", markeredgewidth=2, label="Canal AWGN sans écho : Avec codage (-codeur)")

    # Trajets multiples
    x_ti_nc, y_ti_nc = filtrer_zeros(snrs, t["TI_nc"])
    x_ti_c, y_ti_c = filtrer_zeros(snrs, t["TI_c"])
    plt.semilogy(x_ti_nc, y_ti_nc, "m-s", linewidth=2, markersize=7, label=r"Canal Multi-trajets ($\tau=30, \alpha=0.35$) : Sans codage")
    plt.semilogy(x_ti_c, y_ti_c, "m--^", linewidth=2, markersize=7, markerfacecolor="white", markeredgewidth=2, label=r"Canal Multi-trajets ($\tau=30, \alpha=0.35$) : Avec codage (-codeur)")

    plt.grid(True, which="both", linestyle=":", alpha=0.6)
    plt.xlim(-0.2, 8.5)
    plt.ylim(1e-5, 0.25)
    plt.xlabel(r"$E_b/N_0$ en dB", fontsize=12)
    plt.ylabel("Taux d'Erreur Binaire (TEB)", fontsize=12)
    plt.title("Impact du codage de canal en présence de trajets multiples", fontsize=13, fontweight="bold")
    plt.legend(loc="lower left", fontsize=10.5, framealpha=0.95)
    plt.tight_layout()

    for d in dossiers_sortie:
        chemin = os.path.join(d, "courbe_comparaison_trajets_multiples.png")
        os.makedirs(d, exist_ok=True)
        plt.savefig(chemin, dpi=200)
        print(f"Figure sauvegardee : {chemin}")
    plt.close()

def tracer_combinaison(data, dossiers_sortie=["."]):
    snrs = data["snrs"]
    f = data["formes"]
    t = data["trajets"]

    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(16, 6.5), dpi=200)

    # Graphe 1 : Formes d'onde
    x_nrz_nc, y_nrz_nc = filtrer_zeros(snrs, f["NRZ_nc"])
    x_nrz_c, y_nrz_c = filtrer_zeros(snrs, f["NRZ_c"])
    ax1.semilogy(x_nrz_nc, y_nrz_nc, "b-o", linewidth=2, markersize=6, label="NRZ : Sans codage")
    ax1.semilogy(x_nrz_c, y_nrz_c, "b--d", linewidth=2, markersize=6, markerfacecolor="white", markeredgewidth=1.8, label="NRZ : Avec codage")

    x_nrzt_nc, y_nrzt_nc = filtrer_zeros(snrs, f["NRZT_nc"])
    x_nrzt_c, y_nrzt_c = filtrer_zeros(snrs, f["NRZT_c"])
    ax1.semilogy(x_nrzt_nc, y_nrzt_nc, "g-s", linewidth=2, markersize=6, label="NRZT : Sans codage")
    ax1.semilogy(x_nrzt_c, y_nrzt_c, "g--^", linewidth=2, markersize=6, markerfacecolor="white", markeredgewidth=1.8, label="NRZT : Avec codage")

    x_rz_nc, y_rz_nc = filtrer_zeros(snrs, f["RZ_nc"])
    x_rz_c, y_rz_c = filtrer_zeros(snrs, f["RZ_c"])
    ax1.semilogy(x_rz_nc, y_rz_nc, "r-v", linewidth=2, markersize=6, label="RZ : Sans codage")
    ax1.semilogy(x_rz_c, y_rz_c, "r--x", linewidth=2, markersize=7, markeredgewidth=1.8, label="RZ : Avec codage")

    ax1.grid(True, which="both", linestyle=":", alpha=0.6)
    ax1.set_xlim(-0.2, 8.5)
    ax1.set_ylim(1e-5, 0.25)
    ax1.set_xlabel(r"$E_b/N_0$ en dB", fontsize=11)
    ax1.set_ylabel("Taux d'Erreur Binaire (TEB)", fontsize=11)
    ax1.set_title("(a) Influence de la forme d'onde et du codage", fontsize=12, fontweight="bold")
    ax1.legend(loc="lower left", fontsize=9.5, framealpha=0.95)

    # Graphe 2 : Trajets multiples
    x_awgn_nc, y_awgn_nc = filtrer_zeros(snrs, t["AWGN_nc"])
    x_awgn_c, y_awgn_c = filtrer_zeros(snrs, t["AWGN_c"])
    ax2.semilogy(x_awgn_nc, y_awgn_nc, "b-o", linewidth=2, markersize=6, label="Canal AWGN sans écho : Sans codage")
    ax2.semilogy(x_awgn_c, y_awgn_c, "b--d", linewidth=2, markersize=6, markerfacecolor="white", markeredgewidth=1.8, label="Canal AWGN sans écho : Avec codage")

    x_ti_nc, y_ti_nc = filtrer_zeros(snrs, t["TI_nc"])
    x_ti_c, y_ti_c = filtrer_zeros(snrs, t["TI_c"])
    ax2.semilogy(x_ti_nc, y_ti_nc, "m-s", linewidth=2, markersize=6, label=r"Multi-trajets ($\tau=30, \alpha=0.35$) : Sans codage")
    ax2.semilogy(x_ti_c, y_ti_c, "m--^", linewidth=2, markersize=6, markerfacecolor="white", markeredgewidth=1.8, label=r"Multi-trajets ($\tau=30, \alpha=0.35$) : Avec codage")

    ax2.grid(True, which="both", linestyle=":", alpha=0.6)
    ax2.set_xlim(-0.2, 8.5)
    ax2.set_ylim(1e-5, 0.25)
    ax2.set_xlabel(r"$E_b/N_0$ en dB", fontsize=11)
    ax2.set_ylabel("Taux d'Erreur Binaire (TEB)", fontsize=11)
    ax2.set_title("(b) Influence des trajets multiples et du codage", fontsize=12, fontweight="bold")
    ax2.legend(loc="lower left", fontsize=9.5, framealpha=0.95)

    plt.tight_layout()
    for d in dossiers_sortie:
        chemin = os.path.join(d, "courbe_comparaison_tp5_complete.png")
        os.makedirs(d, exist_ok=True)
        plt.savefig(chemin, dpi=200)
        print(f"Figure sauvegardee : {chemin}")
    plt.close()

if __name__ == "__main__":
    dossiers = [".", "B3.Cecotti.Jacq.Millat.Plougonven--Lastennet.rapport.etape4.pdf/figures"]
    data = charger_donnees()
    if data:
        tracer_formes_onde(data, dossiers)
        tracer_trajets_multiples(data, dossiers)
        tracer_combinaison(data, dossiers)
        print("Toutes les figures ont ete generees avec succes !")
