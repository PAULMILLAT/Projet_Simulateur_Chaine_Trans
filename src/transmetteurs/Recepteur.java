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
 * Lorsque le signal contient l'en-tête de sondage EntetePN, celui-ci
 * est utilisé pour estimer automatiquement les trajets multiples
 * du canal puis pour les compenser avant la décision sur les bits.
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

    /*
     * Nombre maximal d'échantillons sur lesquels on recherche
     * un trajet retardé.
     *
     * On prend quelques temps-bit pour rester simple tout en
     * permettant de trouver des trajets assez éloignés.
     */
    private static final int NB_BITS_RECHERCHE_CANAL = 10;

    /*
     * Un trajet dont l'amplitude estimée est inférieure à cette valeur
     * est considéré comme négligeable.
     */
    private static final float SEUIL_TRAJET = 0.05f;

    /*
     * Corrélation minimale pour considérer que l'en-tête est présent.
     */
    private static final float SEUIL_DETECTION_ENTETE = 0.30f;

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

        /*
         * ------------------------------------------------------------
         * 1. Tentative de détection de l'en-tête
         * ------------------------------------------------------------
         */

        int longueurEnteteEchantillons = EntetePN.LONGUEUR * this.nbEch;

        if (nbEchantillonsTotal >= longueurEnteteEchantillons) {

            float correlationEntete =
                    calculerCorrelationEntete(
                            echantillons,
                            0,
                            longueurEnteteEchantillons);

            /*
             * L'en-tête est une séquence pseudo-aléatoire. Une forte
             * corrélation signifie que les premiers échantillons
             * correspondent bien à EntetePN.SEQUENCE.
             */
            if (Math.abs(correlationEntete) >= SEUIL_DETECTION_ENTETE) {

                recevoirAvecSondage(echantillons);
                this.emettre();
                return;
            }
        }

        /*
         * ------------------------------------------------------------
         * 2. Pas d'en-tête détecté :
         *    comportement classique de l'ancien récepteur
         * ------------------------------------------------------------
         */

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
     * Traitement avec estimation du canal à partir de l'en-tête PN.
     */
    private void recevoirAvecSondage(float[] signalRecu) {

        /*
         * ------------------------------------------------------------
         * 1. Génération de la forme d'onde connue de l'en-tête
         * ------------------------------------------------------------
         *
         * C'est exactement le signal que l'émetteur aurait produit
         * sans le canal.
         */

        float[] signalEntete =
                genererSignalEntete();

        int longueurEntete = signalEntete.length;

        /*
         * ------------------------------------------------------------
         * 2. Estimation de la réponse impulsionnelle du canal
         * ------------------------------------------------------------
         *
         * On recherche les trajets retardés jusqu'à quelques temps-bit.
         *
         * Le signal est centré autour de zéro avant corrélation :
         *
         *     0 -> -1
         *     1 -> +1
         *
         * Cela évite que la composante continue des bits 0/1
         * perturbe fortement la corrélation.
         */

        int maxDecalage =
                Math.min(
                        NB_BITS_RECHERCHE_CANAL * this.nbEch,
                        signalRecu.length - 1);

        float[] canal =
                estimerCanal(
                        signalRecu,
                        signalEntete,
                        maxDecalage);

        /*
         * ------------------------------------------------------------
         * 3. Recherche du dernier trajet significatif
         * ------------------------------------------------------------
         */

        int dernierTrajet = trouverDernierTrajet(canal);

        /*
         * ------------------------------------------------------------
         * 4. Compensation du canal
         * ------------------------------------------------------------
         *
         * Le canal est de la forme :
         *
         * y[n] = h[0]x[n] + h[1]x[n-1] + ...
         *
         * On reconstruit x[n] récursivement :
         *
         * x[n] = (y[n] - h[1]x[n-1] - ...)
         *        / h[0]
         */

        float[] signalCorrige =
                egaliser(signalRecu, canal);

        /*
         * ------------------------------------------------------------
         * 5. Suppression de la queue du canal
         * ------------------------------------------------------------
         *
         * Le transmetteur à trajets multiples ajoute
         * "dernier trajet" échantillons à la fin du signal.
         *
         * On ne doit donc pas transformer cette queue en bits
         * supplémentaires.
         */

        int longueurUtile =
                signalCorrige.length - dernierTrajet;

        if (longueurUtile < longueurEntete) {
            return;
        }

        /*
         * ------------------------------------------------------------
         * 6. Suppression de l'en-tête
         * ------------------------------------------------------------
         */

        int debutMessage = longueurEntete;

        int nbBitsMessage =
                (longueurUtile - debutMessage) / this.nbEch;

        /*
         * ------------------------------------------------------------
         * 7. Décision sur les bits du message
         * ------------------------------------------------------------
         */

        float seuil =
                (this.amplMin + this.amplMax) / 2.0f;

        int tiers1 = this.nbEch / 3;
        int tiers2 = 2 * this.nbEch / 3;

        for (int k = 0; k < nbBitsMessage; k++) {

            int debutBit =
                    debutMessage + k * this.nbEch;

            float somme = 0.0f;
            int nbEchPris = 0;

            if ("NRZ".equalsIgnoreCase(this.formeOnde)) {

                for (int i = 0; i < this.nbEch; i++) {
                    somme += signalCorrige[debutBit + i];
                    nbEchPris++;
                }

            } else {

                for (int i = tiers1; i < tiers2; i++) {
                    somme += signalCorrige[debutBit + i];
                    nbEchPris++;
                }
            }

            float moyenne =
                    (nbEchPris > 0)
                    ? somme / nbEchPris
                    : 0.0f;

            boolean bitDecide =
                    moyenne > seuil;

            this.informationEmise.add(bitDecide);
        }
    }

    /**
     * Génère le signal échantillonné correspondant aux 63 bits
     * connus de l'en-tête.
     */
    private float[] genererSignalEntete() {

        float[] signal =
                new float[EntetePN.LONGUEUR * this.nbEch];

        int index = 0;

        int tiers1 = this.nbEch / 3;
        int tiers2 = 2 * this.nbEch / 3;
        int d3 = this.nbEch - tiers2;

        for (int k = 0; k < EntetePN.LONGUEUR; k++) {

            boolean bit =
                    EntetePN.SEQUENCE[k];

            float valeurCourante =
                    bit ? this.amplMax : this.amplMin;

            for (int i = 0; i < this.nbEch; i++) {

                float valeur;

                if ("NRZ".equalsIgnoreCase(this.formeOnde)) {

                    valeur = valeurCourante;

                } else if ("RZ".equalsIgnoreCase(this.formeOnde)) {

                    if (i >= tiers1 && i < tiers2 && bit) {
                        valeur = this.amplMax;
                    } else {
                        valeur = this.amplMin;
                    }

                } else {

                    /*
                     * Reproduction du NRZT utilisé par Emetteur.
                     */

                    if (i < tiers1) {

                        if (k == 0) {

                            valeur =
                                    (tiers1 > 0)
                                    ? valeurCourante
                                            * ((float) i / tiers1)
                                    : valeurCourante;

                        } else {

                            float valeurPrecedente =
                                    EntetePN.SEQUENCE[k - 1]
                                    ? this.amplMax
                                    : this.amplMin;

                            if (valeurPrecedente == valeurCourante) {

                                valeur = valeurCourante;

                            } else {

                                float valeurMilieu =
                                        (valeurPrecedente
                                                + valeurCourante)
                                                / 2.0f;

                                valeur =
                                        (tiers1 > 0)
                                        ? valeurMilieu
                                            + (valeurCourante
                                                - valeurMilieu)
                                            * ((float) i / tiers1)
                                        : valeurCourante;
                            }
                        }

                    } else if (i < tiers2) {

                        valeur = valeurCourante;

                    } else {

                        int j = i - tiers2;

                        if (k == EntetePN.LONGUEUR - 1) {

                            valeur =
                                    (d3 > 0)
                                    ? valeurCourante
                                        + (0.0f - valeurCourante)
                                        * ((float) j / d3)
                                    : valeurCourante;

                        } else {

                            float valeurSuivante =
                                    EntetePN.SEQUENCE[k + 1]
                                    ? this.amplMax
                                    : this.amplMin;

                            if (valeurSuivante == valeurCourante) {

                                valeur = valeurCourante;

                            } else {

                                float valeurMilieu =
                                        (valeurCourante
                                                + valeurSuivante)
                                                / 2.0f;

                                valeur =
                                        (d3 > 0)
                                        ? valeurCourante
                                            + (valeurMilieu
                                                - valeurCourante)
                                            * ((float) j / d3)
                                        : valeurCourante;
                            }
                        }
                    }
                }

                signal[index++] = valeur;
            }
        }

        return signal;
    }

    /**
     * Calcule la corrélation entre le signal reçu et l'en-tête connu.
     *
     * La valeur retournée est normalisée entre environ -1 et +1.
     */
    private float calculerCorrelationEntete(
            float[] recu,
            int debut,
            int longueur) {

        if (debut + longueur > recu.length) {
            return 0.0f;
        }

        float moyenneRecu = 0.0f;

        for (int i = 0; i < longueur; i++) {
            moyenneRecu += recu[debut + i];
        }

        moyenneRecu /= longueur;

        float moyenneEntete = 0.0f;

        float[] entete = genererSignalEntete();

        for (float v : entete) {
            moyenneEntete += v;
        }

        moyenneEntete /= entete.length;

        float numerateur = 0.0f;
        float energieRecu = 0.0f;
        float energieEntete = 0.0f;

        for (int i = 0; i < longueur; i++) {

            float a =
                    recu[debut + i] - moyenneRecu;

            float b =
                    entete[i] - moyenneEntete;

            numerateur += a * b;
            energieRecu += a * a;
            energieEntete += b * b;
        }

        if (energieRecu == 0.0f || energieEntete == 0.0f) {
            return 0.0f;
        }

        return (float) (
                numerateur
                / Math.sqrt(
                        energieRecu * energieEntete));
    }

    /**
     * Estime les coefficients h[d] du canal par corrélation.
     *
     * h[0] correspond au trajet direct.
     * h[d] correspond à un trajet retardé de d échantillons.
     */
    private float[] estimerCanal(
            float[] recu,
            float[] entete,
            int maxDecalage) {

        float[] canal =
                new float[maxDecalage + 1];

        /*
         * Passage en signal bipolaire :
         *
         *   amplitude basse -> -1
         *   amplitude haute -> +1
         *
         * Le canal agissant linéairement, cela permet d'estimer
         * directement les coefficients des différents trajets.
         */

        float moyenneRecu = 0.0f;

        for (int i = 0; i < entete.length; i++) {
            moyenneRecu += recu[i];
        }

        moyenneRecu /= entete.length;

        float[] enteteBipolaire =
                new float[entete.length];

        float moyenneEntete = 0.0f;

        for (float v : entete) {
            moyenneEntete += v;
        }

        moyenneEntete /= entete.length;

        for (int i = 0; i < entete.length; i++) {
            enteteBipolaire[i] =
                    entete[i] - moyenneEntete;
        }

        /*
         * Autocorrélation de la séquence connue.
         *
         * Pour une séquence PN de longueur 63, la corrélation
         * hors zéro est très faible, ce qui permet de distinguer
         * les différents trajets.
         */

        float energieEntete = 0.0f;

        for (float v : enteteBipolaire) {
            energieEntete += v * v;
        }

        if (energieEntete == 0.0f) {
            return canal;
        }

        /*
         * Pour chaque retard, on corrèle le signal reçu avec
         * l'en-tête décalé.
         */
        for (int decalage = 0;
             decalage <= maxDecalage;
             decalage++) {

            int nombreEchantillons =
                    entete.length - decalage;

            if (nombreEchantillons <= 0) {
                break;
            }

            float numerateur = 0.0f;

            float energieSignal = 0.0f;

            for (int n = decalage;
                 n < entete.length;
                 n++) {

                float r =
                        recu[n] - moyenneRecu;

                float s =
                        enteteBipolaire[n - decalage];

                numerateur += r * s;
                energieSignal += s * s;
            }

            if (energieSignal > 0.0f) {

                /*
                 * Facteur 2 car :
                 *
                 * x = (s + 1) / 2
                 *
                 * et le signal centré contient donc la moitié
                 * de la contribution du canal.
                 */
                canal[decalage] =
                        2.0f
                        * numerateur
                        / energieSignal;
            }
        }

        /*
         * Le calcul précédent peut légèrement sous-estimer le
         * trajet direct à cause des bords de l'en-tête.
         *
         * On force néanmoins une valeur raisonnable si celui-ci
         * est détecté.
         */

        if (Math.abs(canal[0]) < 0.1f) {
            canal[0] = 1.0f;
        }

        return canal;
    }

    /**
     * Détermine le dernier trajet significatif.
     */
    private int trouverDernierTrajet(float[] canal) {

        int dernier = 0;

        for (int d = 1; d < canal.length; d++) {

            if (Math.abs(canal[d]) >= SEUIL_TRAJET) {
                dernier = d;
            }
        }

        return dernier;
    }

    /**
     * Compense le canal par annulation récursive des trajets retardés.
     *
     * On utilise les échantillons déjà reconstruits pour supprimer
     * les contributions des trajets précédents.
     */
    private float[] egaliser(
            float[] recu,
            float[] canal) {

        float[] corrige =
                new float[recu.length];

        float h0 = canal[0];

        /*
         * Protection contre une estimation aberrante.
         */
        if (Math.abs(h0) < 0.1f) {
            h0 = 1.0f;
        }

        for (int n = 0; n < recu.length; n++) {

            float valeur =
                    recu[n];

            /*
             * Suppression des trajets retardés.
             */
            for (int d = 1;
                 d < canal.length && d <= n;
                 d++) {

                if (Math.abs(canal[d]) >= SEUIL_TRAJET) {

                    valeur -=
                            canal[d] * corrige[n - d];
                }
            }

            /*
             * Suppression du trajet direct pondéré.
             */
            corrige[n] =
                    valeur / h0;
        }

        return corrige;
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