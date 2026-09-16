package tests;

import destinations.DestinationFinale;
import information.Information;

/**
 * Classe de test unitaire simple pour le composant DestinationFinale.
 *
 * @author Paul
 * @author Yann
 * @author Enzo
 * @author Damien
 */
public class TestDestination {

    /**
     * Point d'entrée pour exécuter les tests de DestinationFinale.
     *
     * @param args non utilisé
     */
    public static void main(String[] args) {
        System.out.println("--- Test de DestinationFinale ---");
        int nbSucces = 0;
        int nbTests = 0;

        // Test 1 : Réception d'une information booléenne valide
        nbTests++;
        try {
            DestinationFinale destination = new DestinationFinale();
            Information<Boolean> info = new Information<Boolean>();
            info.add(true);
            info.add(false);
            info.add(true);

            destination.recevoir(info);
            Information<Boolean> recue = destination.getInformationRecue();

            if (recue != null && recue.nbElements() == 3 && recue.equals(info)) {
                System.out.println("[OK] Test reception et memorisation de l'information");
                nbSucces++;
            } else {
                System.out.println("[ECHEC] L'information recue ne correspond pas a l'information attendue");
            }
        } catch (Exception e) {
            System.out.println("[ECHEC] Exception inattendue : " + e.getMessage());
        }

        System.out.println("Resultat TestDestination : " + nbSucces + "/" + nbTests + " tests reussis.\n");
        if (nbSucces != nbTests) {
            System.exit(1);
        }
    }
}
