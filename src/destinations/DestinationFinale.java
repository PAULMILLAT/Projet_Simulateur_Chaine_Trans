package destinations;

import information.Information;
/**
 * Destination terminale d'une chaîne de transmission : elle se contente
 * de mémoriser l'information booléenne qu'elle reçoit.
 */
public class DestinationFinale extends Destination<Boolean> {

    /**
     * Reçoit une information et la mémorise.
     * @param information l'information reçue
     */
    @Override
    public void recevoir(Information<Boolean> information) {
        informationRecue = information;
    }
}