package transmetteurs;

import destinations.DestinationInterface;
import information.Information;
import information.InformationNonConformeException;

/**
 * Récepteur : convertit un signal analogique échantillonné
 * (Information<Float>) en une séquence de bits (Information<Boolean>).
 *
 * Ce récepteur suppose qu'aucun en-tête n'est présent : tous les
 * échantillons utiles sont traités comme des données.
 *
 * La décision se fait en deux temps :
 *  1. une métrique (moyenne sur la fenêtre de décision) est calculée
 *     pour chaque bit ;
 *  2. un seuil adaptatif (2-means) est estimé à partir de ces métriques,
 *     ce qui compense le décalage de niveaux dû aux trajets multiples.
 *
 * Les derniers échantillons (queue du canal, de longueur retardMax)
 * sont ignorés : ils ne contiennent que des échos et pas de nouveau bit.
 *
 * @author Paul
 * @author Yann
 * @author Enzo
 * @author Damien
 */
public class Recepteur extends Transmetteur<Float, Boolean> {

    /** Forme d'onde utilisée ("NRZ", "NRZT" ou "RZ") */
    private String formeOnde;

    /** Nombre d'échantillons par bit */
    private int nbEch;

    /** Amplitude minimale */
    private float amplMin;

    /** Amplitude maximale */
    private float amplMax;

    /** Retard maximal du canal en échantillons (queue à ignorer) */
    private int retardMax = 0;

    /** Nombre maximal d'itérations de l'algorithme 2-means */
    private static final int NB_ITERATIONS = 10;

    /**
     * Écart minimal entre les deux nuages de métriques, en proportion de
     * (amplMax - amplMin), en dessous duquel on considère qu'il n'y a
     * qu'un seul niveau présent et on revient au seuil nominal.
     */
    private static final float ECART_MIN_RELATIF = 0.25f;

    /**
     * Constructeur par défaut : forme RZ, 30 échantillons par bit,
     * amplMin = 0.0f, amplMax = 1.0f.
     */
    public Recepteur() {
        this("RZ", 30, 0.0f, 1.0f);
    }

    /**
     * Constructeur paramétré du récepteur.
     *
     * @param formeOnde la forme d'onde ("NRZ", "NRZT" ou "RZ")
     * @param nbEch le nombre d'échantillons par bit
     * @param amplMin l'amplitude minimale
     * @param amplMax l'amplitude maximale
     */
    public Recepteur(String formeOnde, int nbEch, float amplMin, float amplMax) {
        super();
        this.formeOnde = formeOnde;
        this.nbEch = nbEch;
        this.amplMin = amplMin;
        this.amplMax = amplMax;
    }

    /**
     * Définit le retard maximal du canal (en échantillons). Les
     * retardMax derniers échantillons reçus sont ignorés.
     *
     * @param retardMax le retard maximal (0 si canal sans trajets multiples)
     */
    public void setRetardMax(int retardMax) {
        this.retardMax = Math.max(0, retardMax);
    }

    /**
     * Reçoit le signal échantillonné, le démodule et l'émet.
     *
     * @param information le signal analogique reçu
     * @throws InformationNonConformeException si l'information est nulle
     */
    @Override
    public void recevoir(Information<Float> information)
            throws InformationNonConformeException {

        if (information == null) {
            throw new InformationNonConformeException(
                    "L'information recue est nulle.");
        }

        this.informationRecue = information;
        this.informationEmise = new Information<Boolean>();

        int nbEchantillonsTotal = information.nbElements();

        if (nbEchantillonsTotal == 0 || this.nbEch <= 0) {
            this.emettre();
            return;
        }

        float[] echantillons = new float[nbEchantillonsTotal];
        int idx = 0;
        for (Float f : information) {
            echantillons[idx++] = f;
        }

        // On retire la queue du canal : il ne reste que les échantillons utiles
        int nbUtiles = Math.max(0, echantillons.length - this.retardMax);

        float[] metriques = calculerMetriques(echantillons, nbUtiles);
        float seuil = calculerSeuil(metriques);

        for (float m : metriques) {
            this.informationEmise.add(m > seuil);
        }

        this.emettre();
    }

    /**
     * Calcule, pour chaque bit, la moyenne des échantillons sur la
     * fenêtre de décision : bit entier pour le NRZ, tiers central
     * pour le RZ et le NRZT.
     *
     * @param echantillons le signal reçu
     * @param nbUtiles le nombre d'échantillons utiles (hors queue du canal)
     */
    private float[] calculerMetriques(float[] echantillons, int nbUtiles) {

        int nbBits = nbUtiles / this.nbEch;
        float[] metriques = new float[nbBits];

        int debutFenetre;
        int finFenetre;

        if ("NRZ".equalsIgnoreCase(this.formeOnde)) {
            debutFenetre = 0;
            finFenetre = this.nbEch;
        } else {
            debutFenetre = this.nbEch / 3;
            finFenetre = 2 * this.nbEch / 3;
            if (finFenetre <= debutFenetre) { // nbEch très petit
                debutFenetre = 0;
                finFenetre = this.nbEch;
            }
        }

        int tailleFenetre = finFenetre - debutFenetre;

        for (int k = 0; k < nbBits; k++) {
            int debutBit = k * this.nbEch;
            float somme = 0.0f;
            for (int i = debutFenetre; i < finFenetre; i++) {
                somme += echantillons[debutBit + i];
            }
            metriques[k] = somme / tailleFenetre;
        }

        return metriques;
    }

    /**
     * Estime le seuil de décision par un 2-means à une dimension.
     * Repli sur le seuil nominal si les métriques ne forment pas deux
     * nuages distincts (par exemple message ne contenant qu'un seul
     * type de bit).
     */
    private float calculerSeuil(float[] metriques) {

        float seuilNominal = (this.amplMin + this.amplMax) / 2.0f;

        if (metriques.length == 0) {
            return seuilNominal;
        }

        float min = metriques[0];
        float max = metriques[0];
        for (float m : metriques) {
            if (m < min) min = m;
            if (m > max) max = m;
        }

        if (max - min < ECART_MIN_RELATIF * (this.amplMax - this.amplMin)) {
            return seuilNominal;
        }

        float seuil = (min + max) / 2.0f;

        for (int it = 0; it < NB_ITERATIONS; it++) {
            float somme0 = 0.0f, somme1 = 0.0f;
            int n0 = 0, n1 = 0;

            for (float m : metriques) {
                if (m > seuil) {
                    somme1 += m;
                    n1++;
                } else {
                    somme0 += m;
                    n0++;
                }
            }

            if (n0 == 0 || n1 == 0) {
                break;
            }

            float nouveauSeuil = (somme0 / n0 + somme1 / n1) / 2.0f;
            if (Math.abs(nouveauSeuil - seuil) < 1e-6f) {
                seuil = nouveauSeuil;
                break;
            }
            seuil = nouveauSeuil;
        }

        return seuil;
    }

    /**
     * Émet l'information binaire démodulée vers les destinations.
     *
     * @throws InformationNonConformeException si une anomalie survient
     */
    @Override
    public void emettre()
            throws InformationNonConformeException {

        for (DestinationInterface<Boolean> destination
                : this.destinationsConnectees) {
            destination.recevoir(this.informationEmise);
        }
    }
}