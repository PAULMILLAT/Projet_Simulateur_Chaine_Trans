"""
Script d'automatisation des simulations pour le TP5 :
1. Comparaison des 3 formes d'onde (NRZ [-1, 1], NRZT [-1, 1], RZ [0, 1]) avec et sans codage.
2. Comparaison en presence de trajets multiples (-ti 30 0.35) avec et sans codage.
"""

import subprocess
import json
import numpy as np
import matplotlib.pyplot as plt
import os

JAVA_CMD = "java"
CP = "bin"
NB_BITS = 100000
SEED = 100

def executer_simu(args_list):
    cmd = [JAVA_CMD, "-cp", CP, "simulateur.Simulateur", "-mess", str(NB_BITS), "-seed", str(SEED)] + args_list
    try:
        res = subprocess.run(cmd, capture_output=True, text=True, check=True)
        # Parse output : "TEB : 0.01253"
        for line in res.stdout.splitlines():
            if "TEB :" in line:
                val = float(line.split("TEB :")[-1].strip())
                return val
    except Exception as e:
        print(f"Erreur commande {' '.join(cmd)} : {e}")
    return None

def lancer_campagne():
    snrs = [0, 1, 2, 3, 4, 5, 6, 7, 8]
    
    resultats_formes = {
        "NRZ_nc": [], "NRZ_c": [],
        "NRZT_nc": [], "NRZT_c": [],
        "RZ_nc": [], "RZ_c": []
    }

    print("=== 1. Simulation des 3 formes d'onde avec et sans codage ===")
    for snr in snrs:
        print(f"Simulation SNR = {snr} dB...")
        # NRZ
        nrz_nc = executer_simu(["-form", "NRZ", "-ampl", "-1.0", "1.0", "-snrpb", str(snr)])
        nrz_c = executer_simu(["-form", "NRZ", "-ampl", "-1.0", "1.0", "-snrpb", str(snr), "-codeur"])
        resultats_formes["NRZ_nc"].append(nrz_nc)
        resultats_formes["NRZ_c"].append(nrz_c)

        # NRZT
        nrzt_nc = executer_simu(["-form", "NRZT", "-ampl", "-1.0", "1.0", "-snrpb", str(snr)])
        nrzt_c = executer_simu(["-form", "NRZT", "-ampl", "-1.0", "1.0", "-snrpb", str(snr), "-codeur"])
        resultats_formes["NRZT_nc"].append(nrzt_nc)
        resultats_formes["NRZT_c"].append(nrzt_c)

        # RZ
        rz_nc = executer_simu(["-form", "RZ", "-ampl", "0.0", "1.0", "-snrpb", str(snr)])
        rz_c = executer_simu(["-form", "RZ", "-ampl", "0.0", "1.0", "-snrpb", str(snr), "-codeur"])
        resultats_formes["RZ_nc"].append(rz_nc)
        resultats_formes["RZ_c"].append(rz_c)

    print("\n=== 2. Simulation trajets multiples avec et sans codage ===")
    resultats_ti = {
        "AWGN_nc": [],
        "AWGN_c": [],
        "TI_nc": [],
        "TI_c": []
    }
    
    for snr in snrs:
        print(f"Simulation Trajets Multiples SNR = {snr} dB...")
        # AWGN pur (ref NRZ)
        awgn_nc = resultats_formes["NRZ_nc"][snrs.index(snr)]
        awgn_c = resultats_formes["NRZ_c"][snrs.index(snr)]
        resultats_ti["AWGN_nc"].append(awgn_nc)
        resultats_ti["AWGN_c"].append(awgn_c)

        # Trajets multiples : retard dt=30 ech, amplitude ar=0.35
        ti_nc = executer_simu(["-form", "NRZ", "-ampl", "-1.0", "1.0", "-snrpb", str(snr), "-ti", "30", "0.35"])
        ti_c = executer_simu(["-form", "NRZ", "-ampl", "-1.0", "1.0", "-snrpb", str(snr), "-ti", "30", "0.35", "-codeur"])
        resultats_ti["TI_nc"].append(ti_nc)
        resultats_ti["TI_c"].append(ti_c)

    with open("resultats_simus_tp5.json", "w") as f:
        json.dump({"snrs": snrs, "formes": resultats_formes, "trajets": resultats_ti}, f, indent=2)
    print("Simulations terminees et sauvegardees dans resultats_simus_tp5.json")

if __name__ == "__main__":
    lancer_campagne()

