package sources;

import information.Information;
import java.util.Random;

/**
 * Classe d'un composant source émettant un message booléen généré
 * de manière aléatoire.
 *
 * @author Yann
 */
public class SourceAleatoire extends Source<Boolean> {

    /** Longueur par défaut du message aléatoire en nombre de bits */
    private static final int LONGUEUR_PAR_DEFAUT = 100;

    /**
     * Construit une source émettant un message aléatoire de longueur donnée,
     * avec un germe optionnel pour initialiser le générateur aléatoire.
     *
     * @param nbBits le nombre de bits du message à générer
     * @param seed la valeur du germe d'initialisation (si null, aucun germe n'est utilisé)
     */
    public SourceAleatoire(int nbBits, Integer seed) {
        super();
        Random random;
        
        if (seed != null) {
            random = new Random(seed);
        } else {
            random = new Random();
        }

        Boolean[] message = new Boolean[nbBits];

        for (int i = 0; i < nbBits; i++) {
            message[i] = random.nextBoolean();
        }
        
        this.informationGeneree = new Information<Boolean>(message);
    }

    /**
     * Construit une source émettant un message aléatoire de longueur donnée
     * sans germe fixé.
     *
     * @param nbBits le nombre de bits du message à générer
     */
    public SourceAleatoire(int nbBits) {
        this(nbBits, null);
    }

    /**
     * Construit une source émettant un message aléatoire de longueur par défaut (100 bits)
     * sans germe fixé.
     */
    public SourceAleatoire() {
        this(LONGUEUR_PAR_DEFAUT, null);
    }
}
