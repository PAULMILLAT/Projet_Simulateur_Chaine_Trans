package transmetteurs;

import destinations.DestinationInterface;
import information.Information;
import information.InformationNonConformeException;

/**
 * Composant de codage de canal (en émission).
 * <p>
 * Ce composant implémente le code correcteur de canal retenu pour l'itération 5.
 * Pour chaque bit d'information reçu en entrée, il produit en sortie une séquence
 * de trois bits selon la règle suivante :
 * <ul>
 *   <li>{@code 0} &rarr; {@code 0 1 0} (false, true, false)</li>
 *   <li>{@code 1} &rarr; {@code 1 0 1} (true, false, true)</li>
 * </ul>
 * </p>
 * <p>
 * Propriétés remarquables de ce codage :
 * <ul>
 *   <li><b>Synchronisation :</b> Dans le train de bits transmis, il n'y a jamais
 *       plus de deux bits consécutifs de même valeur, garantissant des transitions
 *       fréquentes indispensables à la récupération de rythme en télécommunications.</li>
 *   <li><b>Détection et correction :</b> Les deux mots de code valides ({@code 010} et {@code 101})
 *       sont à distance de Hamming $d = 3$. Cela permet de corriger toute erreur simple
 *       survenue sur un paquet de 3 bits.</li>
 * </ul>
 * </p>
 *
 * @author Paul
 * @author Yann
 * @author Enzo
 * @author Damien
 */
public class CodeurCanal extends Transmetteur<Boolean, Boolean> {

    /**
     * Constructeur par défaut du codeur de canal.
     */
    public CodeurCanal() {
        super();
    }

    /**
     * Reçoit une séquence de bits d'information, applique le codage de canal
     * en transformant chaque bit en un paquet de trois bits, puis émet la séquence codée.
     *
     * @param information la séquence binaire issue de la source
     * @throws InformationNonConformeException si l'information reçue est nulle
     */
    @Override
    public void recevoir(Information<Boolean> information) throws InformationNonConformeException {
        if (information == null) {
            throw new InformationNonConformeException("L'information recue est nulle.");
        }

        this.informationRecue = information;
        this.informationEmise = new Information<Boolean>();

        // Si l'information est vide, on émet immédiatement une information vide
        if (information.nbElements() == 0) {
            this.emettre();
            return;
        }

        // Pour chaque bit d'information, on génère 3 bits codés
        for (Boolean bit : information) {
            if (bit) {
                // 1 -> 1 0 1
                this.informationEmise.add(true);
                this.informationEmise.add(false);
                this.informationEmise.add(true);
            } else {
                // 0 -> 0 1 0
                this.informationEmise.add(false);
                this.informationEmise.add(true);
                this.informationEmise.add(false);
            }
        }

        this.emettre();
    }

    /**
     * Émet la séquence codée vers toutes les destinations connectées en sortie.
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
     * Méthode utilitaire vérifiant qu'une séquence de bits ne comporte jamais
     * plus de deux bits consécutifs identiques.
     *
     * @param sequence la séquence binaire à analyser
     * @return true si la contrainte de synchronisation est respectée, false sinon
     */
    public static boolean verifierProprieteTransitions(Information<Boolean> sequence) {
        if (sequence == null || sequence.nbElements() <= 2) {
            return true;
        }

        int count = 1;
        Boolean precedent = null;

        for (Boolean b : sequence) {
            if (precedent != null && b.equals(precedent)) {
                count++;
                if (count > 2) {
                    return false;
                }
            } else {
                count = 1;
                precedent = b;
            }
        }
        return true;
    }
}

