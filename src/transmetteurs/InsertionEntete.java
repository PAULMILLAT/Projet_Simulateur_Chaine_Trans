package transmetteurs;

import destinations.DestinationInterface;
import information.Information;
import information.InformationNonConformeException;

/**
 * Composant transparent inséré entre la {@code Source} et l'{@code Emetteur}
 * lorsque l'option -sondage est active : il préfixe la séquence d'en-tête
 * de sondage ({@link EntetePN#SEQUENCE}) au message binaire reçu, avant
 * modulation. Le récepteur adapté (RecepteurSonde) connaît la même
 * séquence et pourra donc estimer le canal à partir de sa réception, puis
 * retirer l'en-tête décodé avant de transmettre le message utile.
 *
 * @author Damien
 */
public class InsertionEntete extends Transmetteur<Boolean, Boolean> {

    /**
     * Reçoit le message binaire d'origine, lui préfixe la séquence
     * d'en-tête de sondage, puis émet le tout.
     *
     * @param information le message binaire d'origine (sans en-tête)
     * @throws InformationNonConformeException si l'information est nulle
     */
    @Override
    public void recevoir(Information<Boolean> information) throws InformationNonConformeException {
        if (information == null) {
            throw new InformationNonConformeException("L'information recue est nulle.");
        }

        this.informationRecue = information;
        this.informationEmise = new Information<Boolean>();

        for (boolean b : EntetePN.SEQUENCE) {
            this.informationEmise.add(b);
        }
        for (Boolean b : information) {
            this.informationEmise.add(b);
        }

        this.emettre();
    }

    /**
     * Émet le message préfixé de son en-tête vers les destinations connectées.
     *
     * @throws InformationNonConformeException si une anomalie survient
     */
    @Override
    public void emettre() throws InformationNonConformeException {
        for (DestinationInterface<Boolean> destination : this.destinationsConnectees) {
            destination.recevoir(this.informationEmise);
        }
    }
}
