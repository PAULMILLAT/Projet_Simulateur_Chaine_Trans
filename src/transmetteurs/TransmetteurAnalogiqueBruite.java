package transmetteurs;

import destinations.DestinationInterface;
import information.Information;
import information.InformationNonConformeException;
import java.util.Random;

/**
 * Classe représentant un transmetteur analogique bruité (canal de transmission non idéal).
 * Ce composant modélise un canal à bruit blanc additif gaussien (BBAG / AWGN)
 * selon la méthode de Box-Muller, en fonction du rapport signal sur bruit par bit (Eb/N0 en dB).
 *
 * @author Paul
 * @author Yann
 * @author Enzo
 * @author Damien
 */
public class TransmetteurAnalogiqueBruite extends Transmetteur<Float, Float> {

    /** Rapport signal sur bruit par bit (Eb/N0) en décibels (dB) */
    private float snrpb;

    /** Nombre d'échantillons par bit */
    private int nbEch;

    /** Germe optionnel pour le générateur aléatoire */
    private Integer seed;

    /** Générateur de nombres pseudo-aléatoires */
    private Random random;

    /** Puissance moyenne calculée du signal émis Ps */
    private float puissanceSignal;

    /** Écart-type du bruit gaussien sigma_b */
    private float sigmaBruit;

    /** Séquence des échantillons de bruit générés (utile pour les tests et sondes) */
    private Information<Float> bruitGenere;

    /**
     * Constructeur complet du transmetteur analogique bruité.
     *
     * @param snrpb le rapport signal sur bruit par bit (Eb/N0) en dB
     * @param nbEch le nombre d'échantillons par bit
     * @param seed la graine pour le générateur aléatoire (null pour pas de graine)
     */
    public TransmetteurAnalogiqueBruite(float snrpb, int nbEch, Integer seed) {
        super();
        this.snrpb = snrpb;
        this.nbEch = nbEch;
        this.seed = seed;
        if (seed != null) {
            this.random = new Random(seed);
        } else {
            this.random = new Random();
        }
        this.puissanceSignal = 0.0f;
        this.sigmaBruit = 0.0f;
        this.bruitGenere = new Information<Float>();
    }

    /**
     * Constructeur sans graine aléatoire.
     *
     * @param snrpb le rapport signal sur bruit par bit (Eb/N0) en dB
     * @param nbEch le nombre d'échantillons par bit
     */
    public TransmetteurAnalogiqueBruite(float snrpb, int nbEch) {
        this(snrpb, nbEch, null);
    }

    /**
     * Constructeur avec nbEch par défaut (30) et sans graine.
     *
     * @param snrpb le rapport signal sur bruit par bit (Eb/N0) en dB
     */
    public TransmetteurAnalogiqueBruite(float snrpb) {
        this(snrpb, 30, null);
    }

    /**
     * Constructeur par défaut (snrpb = 0.0 dB, nbEch = 30, sans graine).
     */
    public TransmetteurAnalogiqueBruite() {
        this(0.0f, 30, null);
    }

    /**
     * Reçoit le signal analogique en entrée, calcule sa puissance,
     * génère et ajoute le bruit gaussien, puis émet le signal bruité.
     *
     * @param information le signal analogique non bruité
     * @throws InformationNonConformeException si l'information est nulle
     */
    @Override
    public void recevoir(Information<Float> information) throws InformationNonConformeException {
        if (information == null) {
            throw new InformationNonConformeException("L'information recue est nulle.");
        }

        this.informationRecue = information;
        this.informationEmise = new Information<Float>();
        this.bruitGenere = new Information<Float>();

        int nbEchantillons = information.nbElements();
        if (nbEchantillons == 0) {
            this.emettre();
            return;
        }

        // 1. Calcul de la puissance moyenne du signal émis : Ps = (1/K) * sum( s(n)^2 )
        double sommeCarres = 0.0;
        for (Float s : information) {
            sommeCarres += s * s;
        }
        this.puissanceSignal = (float) (sommeCarres / nbEchantillons);

        // 2. Calcul de sigma_b à partir du SNR par bit :
        // (Eb/N0) = (Ps * N) / (2 * sigma_b^2)
        // d'où sigma_b = sqrt( (Ps * N) / (2 * 10^(snrpb/10)) )
        double snrLin = Math.pow(10.0, this.snrpb / 10.0);
        if (this.puissanceSignal > 0.0f && snrLin > 0.0) {
            this.sigmaBruit = (float) Math.sqrt((this.puissanceSignal * this.nbEch) / (2.0 * snrLin));
        } else {
            this.sigmaBruit = 0.0f;
        }

        // 3. Génération du bruit blanc gaussien via Box-Muller et addition au signal :
        // b(n) = sigma_b * sqrt(-2 * ln(1 - a1)) * cos(2 * pi * a2)
        // r(n) = s(n) + b(n)
        for (Float s : information) {
            double a1 = this.random.nextDouble(); // dans [0, 1[
            double a2 = this.random.nextDouble(); // dans [0, 1[

            double b = this.sigmaBruit * Math.sqrt(-2.0 * Math.log(1.0 - a1)) * Math.cos(2.0 * Math.PI * a2);
            float bruit = (float) b;

            this.bruitGenere.add(bruit);
            this.informationEmise.add(s + bruit);
        }

        // 4. Émission du signal bruité vers les composants connectés
        this.emettre();
    }

    /**
     * Émet le signal bruité vers toutes les destinations connectées.
     *
     * @throws InformationNonConformeException si une anomalie survient
     */
    @Override
    public void emettre() throws InformationNonConformeException {
        for (DestinationInterface<Float> destination : this.destinationsConnectees) {
            destination.recevoir(this.informationEmise);
        }
    }

    /**
     * Renvoie la valeur de snrpb configurée.
     * @return snrpb en dB
     */
    public float getSnrpb() {
        return this.snrpb;
    }

    /**
     * Renvoie le nombre d'échantillons par bit.
     * @return nbEch
     */
    public int getNbEch() {
        return this.nbEch;
    }

    /**
     * Renvoie la graine utilisée ou null si aucune.
     * @return seed
     */
    public Integer getSeed() {
        return this.seed;
    }

    /**
     * Renvoie la puissance moyenne du signal calculée.
     * @return puissanceSignal (Ps)
     */
    public float getPuissanceSignal() {
        return this.puissanceSignal;
    }

    /**
     * Renvoie l'écart-type du bruit gaussien calculé.
     * @return sigmaBruit (sigma_b)
     */
    public float getSigmaBruit() {
        return this.sigmaBruit;
    }

    /**
     * Renvoie la séquence d'échantillons de bruit générés.
     * @return bruitGenere
     */
    public Information<Float> getBruitGenere() {
        return this.bruitGenere;
    }
}
