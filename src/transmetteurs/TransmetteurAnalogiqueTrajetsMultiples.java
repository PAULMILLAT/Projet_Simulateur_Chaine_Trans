package transmetteurs;

import destinations.DestinationInterface;
import information.Information;
import information.InformationNonConformeException;

/**
 * Classe représentant un transmetteur analogique à trajets indirects
 * (canal à évanouissement / multitrajet), suivi, si demandé, d'un
 * bruit blanc additif gaussien (BBAG) appliqué sur le signal recombiné.
 *
 * Conforme à l'option "-ti dt ar [dt ar ...]" de la commande unique :
 * le trajet direct est toujours implicite (décalage nul, amplitude
 * relative 1), et chaque trajet indirect k (k = 0..nbTrajets-1, au
 * maximum 5) est caractérisé par :
 * <ul>
 *   <li>un décalage temporel decalages[k], exprimé directement en
 *       nombre d'échantillons entre le trajet indirect et le trajet
 *       direct ;</li>
 *   <li>une amplitude relative amplitudesRelatives[k] (un flottant,
 *       pas de dB) appliquée au signal du trajet direct pour obtenir
 *       la contribution de ce trajet indirect.</li>
 * </ul>
 * Par défaut (aucun trajet indirect), ce composant se comporte comme
 * le trajet direct seul.
 *
 * Le rapport signal sur bruit par bit snrpb (Eb/N0, en dB) est
 * optionnel (null = transmission non bruitée, comportement par
 * défaut, cohérent avec -snrpb absent). Quand il est fourni, le
 * bruit n'est ajouté qu'une seule fois, sur le signal composite déjà
 * recombiné (délégué à {@link TransmetteurAnalogiqueBruite}) : cela
 * correspond physiquement à un bruit thermique capté au niveau du
 * récepteur, et non à un bruit indépendant par trajet.
 *
 * @author Damien
 */
public class TransmetteurAnalogiqueTrajetsMultiples extends Transmetteur<Float, Float> {

    /** Rapport signal sur bruit par bit (Eb/N0) en dB ; null si transmission non bruitée */
    private Float snrpb;

    /** Nombre d'échantillons par bit */
    private int nbEch;

    /** Germe optionnel pour le générateur aléatoire du bruiteur interne */
    private Integer seed;

    /** Nombre de trajets indirects (en plus du trajet direct implicite), 5 au maximum */
    private int nbTrajets;

    /** Décalages de chaque trajet indirect, en nombre d'échantillons (taille nbTrajets) */
    private int[] decalages;

    /** Amplitudes relatives (par rapport au trajet direct) de chaque trajet indirect, taille nbTrajets */
    private float[] amplitudesRelatives;

    /** Bruiteur interne appliqué une seule fois, au signal composite recombiné (null si snrpb == null) */
    private TransmetteurAnalogiqueBruite bruiteur;

    /**
     * Construit un transmetteur analogique à trajets indirects.
     *
     * @param snrpb le rapport signal sur bruit par bit (Eb/N0) en dB, ou null pour une transmission non bruitée
     * @param nbEch le nombre d'échantillons par bit
     * @param nbTrajets le nombre de trajets indirects (0 = trajet direct seul, 5 au maximum)
     * @param decalages les décalages (en nombre d'échantillons) de chaque trajet indirect, taille nbTrajets
     * @param amplitudesRelatives les amplitudes relatives (par rapport au trajet direct) de chaque trajet indirect, taille nbTrajets
     * @param seed la graine pour le générateur aléatoire du bruit (null pour pas de graine)
     * @throws IllegalArgumentException si nbTrajets &gt; 5 ou si les tableaux ne sont pas de taille nbTrajets
     */
    public TransmetteurAnalogiqueTrajetsMultiples(Float snrpb, int nbEch, int nbTrajets,
            int[] decalages, float[] amplitudesRelatives, Integer seed) {
        super();
        if (nbTrajets > 5) {
            throw new IllegalArgumentException("Au maximum 5 trajets indirects sont autorises (recu : " + nbTrajets + ")");
        }
        if (decalages.length != nbTrajets || amplitudesRelatives.length != nbTrajets) {
            throw new IllegalArgumentException("Les tableaux decalages/amplitudesRelatives ne correspondent pas a nbTrajets");
        }
        this.snrpb = snrpb;
        this.nbEch = nbEch;
        this.nbTrajets = nbTrajets;
        this.decalages = decalages;
        this.amplitudesRelatives = amplitudesRelatives;
        this.seed = seed;
        this.bruiteur = (snrpb != null) ? new TransmetteurAnalogiqueBruite(snrpb, nbEch, seed) : null;
    }

    /**
     * Construit un transmetteur à trajets indirects sans graine aléatoire.
     */
    public TransmetteurAnalogiqueTrajetsMultiples(Float snrpb, int nbEch, int nbTrajets,
            int[] decalages, float[] amplitudesRelatives) {
        this(snrpb, nbEch, nbTrajets, decalages, amplitudesRelatives, null);
    }

    /**
     * Construit un transmetteur sans trajet indirect (trajet direct seul,
     * bruité ou non selon snrpb).
     */
    public TransmetteurAnalogiqueTrajetsMultiples(Float snrpb, int nbEch, Integer seed) {
        this(snrpb, nbEch, 0, new int[0], new float[0], seed);
    }

    /**
     * Reçoit le signal analogique du trajet direct, lui superpose les
     * trajets indirects retardés et pondérés par leur amplitude
     * relative, puis, si snrpb n'est pas null, transmet le signal
     * composite au bruiteur interne pour ajout du BBAG avant émission.
     *
     * @param information le signal analogique du trajet direct (non bruité, non retardé)
     * @throws InformationNonConformeException si l'information est nulle
     */
    @Override
    public void recevoir(Information<Float> information) throws InformationNonConformeException {
        if (information == null) {
            throw new InformationNonConformeException("L'information recue est nulle.");
        }

        this.informationRecue = information;

        int nbEchantillons = information.nbElements();
        if (nbEchantillons == 0) {
            if (this.bruiteur != null) {
                this.bruiteur.recevoir(new Information<Float>());
                this.informationEmise = this.bruiteur.getInformationEmise();
            } else {
                this.informationEmise = new Information<Float>();
            }
            this.emettre();
            return;
        }

        // Recopie du trajet direct dans un tableau pour un accès direct par indice
        float[] direct = new float[nbEchantillons];
        int idx = 0;
        for (Float s : information) {
            direct[idx++] = s;
        }

        // Le trajet indirect le plus retarde determine l'allongement du signal composite
        int decalMax = 0;
        for (int k = 0; k < nbTrajets; k++) {
            if (decalages[k] > decalMax) {
                decalMax = decalages[k];
            }
        }
        int longueurComposite = nbEchantillons + decalMax;
        // Tableau initialise a 0 par Java : sert deja de "padding" par defaut
        float[] composite = new float[longueurComposite];

        // 1. Superposition du trajet direct : amplitude relative 1, decalage nul, implicite
        for (int n = 0; n < nbEchantillons; n++) {
            composite[n] += direct[n];
        }

        // 2. Superposition de chaque trajet indirect : signal du trajet direct
        // multiplie par son amplitude relative (flottant, pas de dB), decale de
        // decalages[k] echantillons. Le padding de zeros avant le debut du
        // trajet retarde est deja assure par l'initialisation du tableau
        // composite ; le padding apres la fin du signal l'est par le
        // dimensionnement de composite sur decalMax.
        for (int k = 0; k < nbTrajets; k++) {
            int offset = decalages[k];
            float ar = amplitudesRelatives[k];
            for (int n = 0; n < nbEchantillons; n++) {
                composite[n + offset] += ar * direct[n];
            }
        }

        Information<Float> infoComposite = new Information<Float>();
        for (float v : composite) {
            infoComposite.add(v);
        }

        // 3. Ajout eventuel du bruit blanc gaussien, une seule fois, sur le
        // signal composite deja recombine (cf. TransmetteurAnalogiqueBruite).
        // Transmission non bruitee si snrpb == null (comportement par defaut).
        if (this.bruiteur != null) {
            this.bruiteur.recevoir(infoComposite);
            this.informationEmise = this.bruiteur.getInformationEmise();
        } else {
            this.informationEmise = infoComposite;
        }

        // 4. Emission du signal composite vers les destinations connectees
        this.emettre();
    }

    /**
     * Émet le signal composite vers toutes les destinations connectées.
     *
     * @throws InformationNonConformeException si une anomalie survient
     */
    @Override
    public void emettre() throws InformationNonConformeException {
        for (DestinationInterface<Float> destination : this.destinationsConnectees) {
            destination.recevoir(this.informationEmise);
        }
    }

    // --- Accesseurs ---

    /** @return snrpb (Eb/N0 en dB), ou null si transmission non bruitee */
    public Float getSnrpb() { return this.snrpb; }

    public int getNbEch() { return this.nbEch; }

    public Integer getSeed() { return this.seed; }

    public int getNbTrajets() { return this.nbTrajets; }

    /** @return les decalages (en nombre d'echantillons) de chaque trajet indirect */
    public int[] getDecalages() { return this.decalages; }

    /** @return les amplitudes relatives de chaque trajet indirect (par rapport au trajet direct) */
    public float[] getAmplitudesRelatives() { return this.amplitudesRelatives; }

    /** @return true si la transmission est bruitee (snrpb != null) */
    public boolean isBruite() { return this.bruiteur != null; }

    /** Renvoie la puissance moyenne du signal composite, calculee par le bruiteur interne (0 si non bruite). */
    public float getPuissanceSignal() { return (this.bruiteur != null) ? this.bruiteur.getPuissanceSignal() : 0.0f; }

    /** Renvoie l'ecart-type du bruit gaussien, calcule par le bruiteur interne (0 si non bruite). */
    public float getSigmaBruit() { return (this.bruiteur != null) ? this.bruiteur.getSigmaBruit() : 0.0f; }

    /** Renvoie la sequence de bruit generee par le bruiteur interne (null si non bruite). */
    public Information<Float> getBruitGenere() { return (this.bruiteur != null) ? this.bruiteur.getBruitGenere() : null; }
}