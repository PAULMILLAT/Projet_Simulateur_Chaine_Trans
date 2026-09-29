package transmetteurs;

import destinations.DestinationInterface;
import information.Information;
import information.InformationNonConformeException;

/**
 * Classe représentant le composant Récepteur de la chaîne de transmission.
 *
 * Le récepteur convertit un signal analogique échantillonné
 * (Information<Float>) en une séquence de bits logiques
 * (Information<Boolean>).
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
     * Reçoit le signal échantillonné.
     *
     * Si un en-tête PN est détecté, le canal est estimé à partir de
     * celui-ci et les trajets multiples sont compensés avant la
     * démodulation.
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

        // Conversion en tableau pour faciliter les calculs
        float[] echantillons = new float[nbEchantillonsTotal];

        int idx = 0;
        for (Float f : information) {
            echantillons[idx++] = f;
        }

        recevoirSansSondage(echantillons);

        this.emettre();
    }

    /**
     * Traitement normal lorsqu'aucun en-tête de sondage n'est présent.
     */
    private void recevoirSansSondage(float[] echantillons) {

        int nbEchantillonsTotal = echantillons.length;

        int nbBits = nbEchantillonsTotal / this.nbEch;

        float seuil = (this.amplMin + this.amplMax) / 2.0f;

        int tiers1 = this.nbEch / 3;
        int tiers2 = 2 * this.nbEch / 3;

        for (int k = 0; k < nbBits; k++) {

            int debutBit = k * this.nbEch;

            float somme = 0.0f;
            int nbEchPris = 0;

            if ("NRZ".equalsIgnoreCase(this.formeOnde)) {

                // NRZ : moyenne sur tout le bit
                for (int i = 0; i < this.nbEch; i++) {
                    somme += echantillons[debutBit + i];
                    nbEchPris++;
                }

            } else {

                // RZ / NRZT : moyenne sur le plateau central
                for (int i = tiers1; i < tiers2; i++) {
                    somme += echantillons[debutBit + i];
                    nbEchPris++;
                }
            }

            float moyenne =
                    (nbEchPris > 0)
                    ? somme / nbEchPris
                    : 0.0f;

            boolean bitDecide = moyenne > seuil;

            this.informationEmise.add(bitDecide);
        }
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