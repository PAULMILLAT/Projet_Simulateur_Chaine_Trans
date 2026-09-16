package transmetteurs;

import destinations.DestinationInterface;
import information.Information;
import information.InformationNonConformeException;

/**
 * Classe représentant le composant Émetteur de la chaîne de transmission.
 * L'émetteur convertit une séquence de symboles logiques (Information&lt;Boolean&gt;)
 * en un signal analogique échantillonné (Information&lt;Float&gt;) selon une forme
 * d'onde choisie (NRZ, NRZT ou RZ).
 *
 * @author Paul
 * @author Yann
 * @author Enzo
 * @author Damien
 */
public class Emetteur extends Transmetteur<Boolean, Float> {

    /** Forme d'onde du signal ("NRZ", "NRZT" ou "RZ") */
    private String formeOnde;

    /** Nombre d'échantillons par bit */
    private int nbEch;

    /** Amplitude minimale du signal */
    private float amplMin;

    /** Amplitude maximale du signal */
    private float amplMax;

    /**
     * Constructeur par défaut : forme RZ, 30 échantillons par bit,
     * amplMin = 0.0f, amplMax = 1.0f.
     */
    public Emetteur() {
        this("RZ", 30, 0.0f, 1.0f);
    }

    /**
     * Constructeur paramétré de l'émetteur.
     *
     * @param formeOnde la forme d'onde ("NRZ", "NRZT" ou "RZ")
     * @param nbEch le nombre d'échantillons par bit
     * @param amplMin l'amplitude minimale du signal
     * @param amplMax l'amplitude maximale du signal
     */
    public Emetteur(String formeOnde, int nbEch, float amplMin, float amplMax) {
        super();
        this.formeOnde = formeOnde;
        this.nbEch = nbEch;
        this.amplMin = amplMin;
        this.amplMax = amplMax;
    }

    /**
     * Reçoit une séquence binaire, effectue la conversion en signal
     * analogique échantillonné selon la forme d'onde, puis émet le signal.
     *
     * @param information l'information binaire reçue
     * @throws InformationNonConformeException si l'information est nulle
     */
    @Override
    public void recevoir(Information<Boolean> information) throws InformationNonConformeException {
        if (information == null) {
            throw new InformationNonConformeException("L'information recue est nulle.");
        }

        this.informationRecue = information;
        this.informationEmise = new Information<Float>();

        // Extraction des bits dans un tableau pour simplifier les parcours
        int nbBits = information.nbElements();
        Boolean[] bits = new Boolean[nbBits];
        int idx = 0;
        for (Boolean b : information) {
            bits[idx++] = b;
        }

        // Génération des échantillons selon la forme d'onde
        if ("NRZ".equalsIgnoreCase(this.formeOnde)) {
            genererNRZ(bits);
        } else if ("NRZT".equalsIgnoreCase(this.formeOnde)) {
            genererNRZT(bits);
        } else {
            // Par défaut ou si RZ
            genererRZ(bits);
        }

        // Émission du signal vers les destinations connectées
        this.emettre();
    }

    /**
     * Génère un signal analogique selon le codage NRZ (Non-Return-to-Zero).
     * Chaque bit 1 est représenté par amplMax pendant tout le temps bit.
     * Chaque bit 0 est représenté par amplMin pendant tout le temps bit.
     *
     * @param bits le tableau des bits à émettre
     */
    private void genererNRZ(Boolean[] bits) {
        for (Boolean b : bits) {
            float valeur = b ? this.amplMax : this.amplMin;
            for (int i = 0; i < this.nbEch; i++) {
                this.informationEmise.add(valeur);
            }
        }
    }

    /**
     * Génère un signal analogique selon le codage RZ (Return-to-Zero).
     * Le temps bit est divisé en trois tiers :
     * - 1er tiers : amplitude minimale (0)
     * - 2ème tiers : amplitude maximale si bit 1, amplitude minimale si bit 0
     * - 3ème tiers : amplitude minimale (0)
     *
     * @param bits le tableau des bits à émettre
     */
    private void genererRZ(Boolean[] bits) {
        int tiers1 = this.nbEch / 3;
        int tiers2 = 2 * this.nbEch / 3;

        for (Boolean b : bits) {
            for (int i = 0; i < this.nbEch; i++) {
                if (i >= tiers1 && i < tiers2 && b) {
                    // Impulsion au tiers central pour un bit 1
                    this.informationEmise.add(this.amplMax);
                } else {
                    // Amplitude min (0.0f) pour le reste
                    this.informationEmise.add(this.amplMin);
                }
            }
        }
    }

    /**
     * Génère un signal analogique selon le codage NRZT (Non-Return-to-Zero Trapézoïdal).
     * Les montées et descentes s'effectuent sur un tiers du temps bit.
     * Le tiers central est toujours un plateau à la valeur du bit.
     *
     * @param bits le tableau des bits à émettre
     */
    private void genererNRZT(Boolean[] bits) {
        int nbBits = bits.length;
        int tiers1 = this.nbEch / 3;
        int tiers2 = 2 * this.nbEch / 3;
        int d3 = this.nbEch - tiers2; // durée du dernier tiers

        for (int k = 0; k < nbBits; k++) {
            float vCourant = bits[k] ? this.amplMax : this.amplMin;

            for (int i = 0; i < this.nbEch; i++) {
                if (i < tiers1) {
                    // Premier tiers : transition montante ou descendante vers vCourant
                    if (k == 0) {
                        // Pour le premier bit, on démarre de 0.0f vers vCourant
                        float v = (tiers1 > 0) ? (vCourant * ((float) i / tiers1)) : vCourant;
                        this.informationEmise.add(v);
                    } else {
                        float vPrec = bits[k - 1] ? this.amplMax : this.amplMin;
                        if (vPrec == vCourant) {
                            this.informationEmise.add(vCourant);
                        } else {
                            // Transition démarrée dans le bit précédent, valeur médiane à i=0
                            float vMid = (vPrec + vCourant) / 2.0f;
                            float v = (tiers1 > 0) ? (vMid + (vCourant - vMid) * ((float) i / tiers1)) : vCourant;
                            this.informationEmise.add(v);
                        }
                    }
                } else if (i < tiers2) {
                    // Deuxième tiers : plateau constant à la valeur nominale du bit
                    this.informationEmise.add(vCourant);
                } else {
                    // Dernier tiers : transition vers le bit suivant (ou vers 0 si fin)
                    int j = i - tiers2;
                    if (k == nbBits - 1) {
                        // Dernier bit : retour progressif vers 0.0f
                        float v = (d3 > 0) ? (vCourant + (0.0f - vCourant) * ((float) j / d3)) : vCourant;
                        this.informationEmise.add(v);
                    } else {
                        float vSuiv = bits[k + 1] ? this.amplMax : this.amplMin;
                        if (vSuiv == vCourant) {
                            this.informationEmise.add(vCourant);
                        } else {
                            // Transition vers la valeur médiane
                            float vMid = (vCourant + vSuiv) / 2.0f;
                            float v = (d3 > 0) ? (vCourant + (vMid - vCourant) * ((float) j / d3)) : vCourant;
                            this.informationEmise.add(v);
                        }
                    }
                }
            }
        }
    }

    /**
     * Émet l'information construite vers toutes les destinations connectées.
     *
     * @throws InformationNonConformeException si une anomalie survient
     */
    @Override
    public void emettre() throws InformationNonConformeException {
        for (DestinationInterface<Float> destination : this.destinationsConnectees) {
            destination.recevoir(this.informationEmise);
        }
    }
}

