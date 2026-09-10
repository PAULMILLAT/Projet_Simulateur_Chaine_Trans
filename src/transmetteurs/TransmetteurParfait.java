package transmetteurs;

import destinations.DestinationInterface;
import information.Information;
import information.InformationNonConformeException;

/**
 * Classe représentant un transmetteur parfait (sans bruit ni distorsion).
 * L'information reçue en entrée est directement retransmise en sortie
 * vers toutes les destinations connectés.
 *
 * @param <T> le type d'information manipulée (ex: Boolean pour un canal logique)
 * @author Yann
 */
public class TransmetteurParfait<T> extends Transmetteur<T, T> {

    /**
     * Construit un transmetteur parfait avec une liste vide de destinations connectées.
     */
    public TransmetteurParfait() {
        super();
    }

    /**
     * Reçoit une information, la mémorise, l'assigne à l'information émise,
     * et appelle immédiatement la méthode émettre().
     *
     * @param information l'information reçue en entrée du transmetteur
     * @throws InformationNonConformeException si l'information reçue est nulle ou non conforme
     */
    @Override
    public void recevoir(Information<T> information) throws InformationNonConformeException {
        if (information == null) {
            throw new InformationNonConformeException("L'information recue est nulle.");
        }
        this.informationRecue = information;
        this.informationEmise = this.informationRecue;
        this.emettre();
    }

    /**
     * Émet l'information construite par le transmetteur vers l'ensemble
     * des composants destinations connectés.
     *
     * @throws InformationNonConformeException si l'information à émettre comporte une anomalie
     */
    @Override
    public void emettre() throws InformationNonConformeException {
        for (DestinationInterface<T> destinationConnectee : destinationsConnectees) {
            destinationConnectee.recevoir(this.informationEmise);
        }
    }
}
