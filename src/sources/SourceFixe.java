package sources;

import information.Information;

/**
 * Source émettant un message booléen fixé à la construction.
 */
public class SourceFixe extends Source<Boolean> {

    /**
     * Construit une source émettant le message fourni.
     * @param message les bits à émettre
     */
    public SourceFixe(Boolean[] message) {
        informationGeneree = new Information<Boolean>(message);
    }
}