package transmetteurs;

import destinations.DestinationInterface;
import information.Information;
import information.InformationNonConformeException;

/**
 * Composant de décodage de canal (en réception).
 * <p>
 * Ce composant implémente le décodeur associé au code de canal (3, 1) de l'itération 5.
 * Il regroupe les bits reçus par paquets de 3 et applique un décodage à décision
 * dure par recherche du mot de code valide le plus proche au sens de la distance
 * de Hamming (distance minimale $d_{\min} = 3$).
 * </p>
 * <p>
 * Les deux mots de code valides émis étant $C_0 = (0, 1, 0)$ et $C_1 = (1, 0, 1)$,
 * la table de décodage déterministe retenue est :
 * <ul>
 *   <li>{@code 0 0 0} &rarr; {@code 0} (erreur corrigée sur le 2<sup>e</sup> bit)</li>
 *   <li>{@code 0 0 1} &rarr; {@code 1} (erreur corrigée sur le 1<sup>er</sup> bit)</li>
 *   <li>{@code 0 1 0} &rarr; {@code 0} (mot exact, 0 erreur)</li>
 *   <li>{@code 0 1 1} &rarr; {@code 0} (erreur corrigée sur le 3<sup>e</sup> bit)</li>
 *   <li>{@code 1 0 0} &rarr; {@code 1} (erreur corrigée sur le 3<sup>e</sup> bit)</li>
 *   <li>{@code 1 0 1} &rarr; {@code 1} (mot exact, 0 erreur)</li>
 *   <li>{@code 1 1 0} &rarr; {@code 0} (erreur corrigée sur le 1<sup>er</sup> bit)</li>
 *   <li>{@code 1 1 1} &rarr; {@code 1} (erreur corrigée sur le 2<sup>e</sup> bit)</li>
 * </ul>
 * </p>
 * <p>
 * Le décodage est modélisé sous la forme d'un <b>automate fini déterministe</b>
 * à trois états de synchronisation de bloc (attente du premier bit, du deuxième
 * bit, puis du troisième bit avec émission du bit décodé).
 * </p>
 *
 * @author Paul
 * @author Yann
 * @author Enzo
 * @author Damien
 */
public class DecodeurCanal extends Transmetteur<Boolean, Boolean> {

    /**
     * Table de vérité déterministe du décodeur pour les 8 triplets possibles.
     * L'indice dans le tableau correspond à la valeur entière du triplet :
     * {@code (b1 ? 4 : 0) | (b2 ? 2 : 0) | (b3 ? 1 : 0)}.
     */
    private static final boolean[] TABLE_DECODAGE = {
        false, // 0: 000 -> 0
        true,  // 1: 001 -> 1
        false, // 2: 010 -> 0
        false, // 3: 011 -> 0
        true,  // 4: 100 -> 1
        true,  // 5: 101 -> 1
        false, // 6: 110 -> 0
        true   // 7: 111 -> 1
    };

    /**
     * Position (1-indexée) du bit considéré erroné dans le triplet reçu.
     * Vaut 0 si le triplet est un mot de code valide sans erreur.
     */
    private static final int[] POSITION_BIT_ERRONE = {
        2, // 000 -> err sur bit 2 (venait de 010)
        1, // 001 -> err sur bit 1 (venait de 101)
        0, // 010 -> exact
        3, // 011 -> err sur bit 3 (venait de 010)
        3, // 100 -> err sur bit 3 (venait de 101)
        0, // 101 -> exact
        1, // 110 -> err sur bit 1 (venait de 010)
        2  // 111 -> err sur bit 2 (venait de 101)
    };

    /** Nombre total de paquets de 3 bits décodés lors de la dernière exécution */
    private int nbTripletsTraites = 0;

    /** Nombre de paquets de 3 bits pour lesquels une erreur a été détectée et réparée */
    private int nbErreursCorrigees = 0;

    /**
     * Constructeur par défaut du décodeur de canal.
     */
    public DecodeurCanal() {
        super();
        this.nbTripletsTraites = 0;
        this.nbErreursCorrigees = 0;
    }

    /**
     * Reçoit le train binaire démodulé, applique le décodage par automate
     * sur les paquets de 3 bits consécutifs, et émet la séquence d'information
     * décodée vers la destination.
     *
     * @param information la séquence binaire reçue du récepteur
     * @throws InformationNonConformeException si l'information reçue est nulle
     */
    @Override
    public void recevoir(Information<Boolean> information) throws InformationNonConformeException {
        if (information == null) {
            throw new InformationNonConformeException("L'information recue est nulle.");
        }

        this.informationRecue = information;
        this.informationEmise = new Information<Boolean>();
        this.nbTripletsTraites = 0;
        this.nbErreursCorrigees = 0;

        int nbBitsRecus = information.nbElements();
        if (nbBitsRecus == 0) {
            this.emettre();
            return;
        }

        // Automate fini déterministe de décodage par bloc de 3 bits :
        // État 0 : attente du bit 1
        // État 1 : attente du bit 2 (bit 1 en mémoire)
        // État 2 : attente du bit 3 (bits 1 et 2 en mémoire) -> transition vers 0 + émission
        boolean b1 = false;
        boolean b2 = false;
        int etatAutomate = 0;

        for (Boolean b : information) {
            switch (etatAutomate) {
                case 0:
                    b1 = b;
                    etatAutomate = 1;
                    break;
                case 1:
                    b2 = b;
                    etatAutomate = 2;
                    break;
                case 2:
                    boolean b3 = b;
                    boolean bitDecode = decoderTriplet(b1, b2, b3);
                    this.informationEmise.add(bitDecode);

                    this.nbTripletsTraites++;
                    if (estErreurReparee(b1, b2, b3)) {
                        this.nbErreursCorrigees++;
                    }

                    // Réinitialisation de l'état de l'automate pour le bloc suivant
                    etatAutomate = 0;
                    break;
                default:
                    etatAutomate = 0;
                    break;
            }
        }

        // Si la séquence n'était pas un multiple de 3, gestion des bits résiduels
        if (etatAutomate == 1) {
            // Un seul bit résiduel b1 : décision par défaut
            this.informationEmise.add(b1);
        } else if (etatAutomate == 2) {
            // Deux bits résiduels b1, b2 : comparaison avec préfixes de 010 et 101
            // 010 a pour préfixe (0, 1), 101 a pour préfixe (1, 0)
            boolean bitDecode = (!b1 && b2) ? false : true;
            this.informationEmise.add(bitDecode);
        }

        this.emettre();
    }

    /**
     * Émet l'information décodée vers toutes les destinations connectées.
     *
     * @throws InformationNonConformeException si une anomalie survient lors de l'émission
     */
    @Override
    public void emettre() throws InformationNonConformeException {
        for (DestinationInterface<Boolean> destination : this.destinationsConnectees) {
            destination.recevoir(this.informationEmise);
        }
    }

    /**
     * Décode un paquet de 3 bits selon la règle du mot de code le plus proche.
     *
     * @param b1 premier bit reçu du triplet
     * @param b2 deuxième bit reçu du triplet
     * @param b3 troisième bit reçu du triplet
     * @return la valeur booléenne du bit d'information décodé (false pour 0, true pour 1)
     */
    public static boolean decoderTriplet(boolean b1, boolean b2, boolean b3) {
        int index = (b1 ? 4 : 0) | (b2 ? 2 : 0) | (b3 ? 1 : 0);
        return TABLE_DECODAGE[index];
    }

    /**
     * Indique si un triplet reçu a nécessité la correction d'un bit erroné.
     *
     * @param b1 premier bit reçu du triplet
     * @param b2 deuxième bit reçu du triplet
     * @param b3 troisième bit reçu du triplet
     * @return true si une erreur a été détectée et corrigée, false si le mot était déjà exact
     */
    public static boolean estErreurReparee(boolean b1, boolean b2, boolean b3) {
        int index = (b1 ? 4 : 0) | (b2 ? 2 : 0) | (b3 ? 1 : 0);
        return POSITION_BIT_ERRONE[index] != 0;
    }

    /**
     * Renvoie la position (1, 2 ou 3) du bit corrigé dans le triplet.
     *
     * @param b1 premier bit reçu du triplet
     * @param b2 deuxième bit reçu du triplet
     * @param b3 troisième bit reçu du triplet
     * @return 1, 2 ou 3 selon le bit erroné, ou 0 si aucune erreur n'a été détectée
     */
    public static int positionBitErrone(boolean b1, boolean b2, boolean b3) {
        int index = (b1 ? 4 : 0) | (b2 ? 2 : 0) | (b3 ? 1 : 0);
        return POSITION_BIT_ERRONE[index];
    }

    /**
     * Calcule la distance de Hamming entre deux triplets de bits.
     *
     * @param t1 premier triplet de taille 3
     * @param t2 second triplet de taille 3
     * @return le nombre de positions où les deux triplets diffèrent (entre 0 et 3)
     */
    public static int distanceHamming(boolean[] t1, boolean[] t2) {
        int dist = 0;
        int minLen = Math.min(t1.length, t2.length);
        for (int i = 0; i < minLen; i++) {
            if (t1[i] != t2[i]) {
                dist++;
            }
        }
        return dist;
    }

    /**
     * Renvoie le nombre total de paquets de 3 bits traités lors de la dernière réception.
     *
     * @return le nombre de triplets traités
     */
    public int getNbTripletsTraites() {
        return this.nbTripletsTraites;
    }

    /**
     * Renvoie le nombre de paquets de 3 bits pour lesquels une erreur a été corrigée.
     *
     * @return le nombre d'erreurs corrigées
     */
    public int getNbErreursCorrigees() {
        return this.nbErreursCorrigees;
    }
}

