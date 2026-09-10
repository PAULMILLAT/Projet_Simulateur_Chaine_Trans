package sources;

import information.Information;

/**
 * Source émettant un message booléen fixé à la construction.
 * @author Paul
 */
public class SourceFixe extends Source<Boolean> {

    /**
     * Construit une source émettant le message fourni.
     * @param message les bits à émettre
     */
    public SourceFixe(Boolean[] message) {
        informationGeneree = new Information<Boolean>(message);
    }

    /**
     * Construit une source émettant le message sous forme de chaîne binaire de '0' et '1'.
     * @param messageString la suite de caractères '0' et '1'
     * 
     * @author Yann
     */
    public SourceFixe(String messageString) {
        Boolean[] message = new Boolean[messageString.length()];
        for (int i = 0; i < messageString.length(); i++) {
            message[i] = (messageString.charAt(i) == '1');
        }
        informationGeneree = new Information<Boolean>(message);
    }
}