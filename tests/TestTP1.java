package tests;

import destinations.DestinationFinale;
import information.Information;
import simulateur.ArgumentsException;
import simulateur.Simulateur;
import sources.SourceAleatoire;
import sources.SourceFixe;
import transmetteurs.TransmetteurParfait;

/**
 * Suite de tests unitaires et d'intégration pour le TP1 (Étape 1).
 * Vérifie le bon fonctionnement de la chaîne logique élémentaire.
 *
 * @author Paul
 * @author Yann
 * @author Enzo
 * @author Damien
 */
public class TestTP1 {

    private static int nbTests = 0;
    private static int nbSucces = 0;

    /**
     * Méthode utilitaire simple pour valider un test.
     *
     * @param condition vrai si le test réussit
     * @param description descriptif du test
     */
    private static void tester(boolean condition, String description) {
        nbTests++;
        if (condition) {
            nbSucces++;
            System.out.println("  [OK] " + description);
        } else {
            System.out.println("  [ECHEC] " + description);
        }
    }

    /**
     * Exécute l'ensemble des tests du TP1.
     *
     * @return true si tous les tests ont réussi, false sinon
     */
    public static boolean executerTests() {
        System.out.println("=== Lancement des tests Java - TP1 (Etape 1) ===");
        nbTests = 0;
        nbSucces = 0;

        // 1. Tests de la SourceFixe
        System.out.println("\n1. Tests de SourceFixe :");
        try {
            SourceFixe sf = new SourceFixe("01010101");
            sf.emettre();
            Information<Boolean> info = sf.getInformationEmise();
            tester(info != null && info.nbElements() == 8, "Longueur du message fixe (8 bits)");
            tester(!info.iemeElement(0) && info.iemeElement(1), "Valeurs des bits du message fixe (0 puis 1)");

            // Test de SourceFixe avec tableau de Boolean
            Boolean[] bitsTab = new Boolean[]{true, false, true};
            SourceFixe sfTab = new SourceFixe(bitsTab);
            sfTab.emettre();
            tester(sfTab.getInformationEmise().nbElements() == 3, "Creation SourceFixe avec tableau Boolean[]");
        } catch (Exception e) {
            tester(false, "Creation SourceFixe valide : " + e.getMessage());
        }

        // 2. Tests de la SourceAleatoire
        System.out.println("\n2. Tests de SourceAleatoire :");
        try {
            SourceAleatoire sa50 = new SourceAleatoire(50);
            sa50.emettre();
            tester(sa50.getInformationEmise().nbElements() == 50, "Longueur message aleatoire (50 bits)");

            // Reproductibilite avec meme seed
            SourceAleatoire saSeed1 = new SourceAleatoire(100, 42);
            saSeed1.emettre();
            SourceAleatoire saSeed2 = new SourceAleatoire(100, 42);
            saSeed2.emettre();
            tester(saSeed1.getInformationEmise().equals(saSeed2.getInformationEmise()),
                    "Reproductibilite du message avec une meme semence (-seed 42)");

            // Non-identite avec deux seeds differentes
            SourceAleatoire saSeed3 = new SourceAleatoire(100, 999);
            saSeed3.emettre();
            tester(!saSeed1.getInformationEmise().equals(saSeed3.getInformationEmise()),
                    "Messages differents avec deux semences distinctes");
        } catch (Exception e) {
            tester(false, "Erreur lors des tests de SourceAleatoire : " + e.getMessage());
        }

        // 3. Tests de TransmetteurParfait et DestinationFinale
        System.out.println("\n3. Tests TransmetteurParfait & DestinationFinale :");
        try {
            TransmetteurParfait<Boolean> tp = new TransmetteurParfait<Boolean>();
            DestinationFinale dest = new DestinationFinale();
            tp.connecter(dest);

            Information<Boolean> messageTest = new Information<Boolean>();
            messageTest.add(true);
            messageTest.add(false);
            messageTest.add(true);

            tp.recevoir(messageTest);
            tester(dest.getInformationRecue().equals(messageTest),
                    "TransmetteurParfait transmet fidelement a DestinationFinale");
        } catch (Exception e) {
            tester(false, "Erreur transmission par defaut : " + e.getMessage());
        }

        // 4. Tests d'integration avec Simulateur (chaîne logique)
        System.out.println("\n4. Tests d'integration Simulateur (chaine logique TP1) :");
        try {
            // Execution par defaut (100 bits aleatoires)
            Simulateur sim1 = new Simulateur(new String[]{});
            sim1.execute();
            tester(sim1.calculTauxErreurBinaire() == 0.0f, "Simulation par defaut -> TEB = 0.0");

            // Execution avec message fixe
            Simulateur sim2 = new Simulateur(new String[]{"-mess", "1100101011110000"});
            sim2.execute();
            tester(sim2.calculTauxErreurBinaire() == 0.0f, "Simulation message fixe 16 bits -> TEB = 0.0");

            // Execution avec seed
            Simulateur sim3 = new Simulateur(new String[]{"-mess", "200", "-seed", "12345"});
            sim3.execute();
            tester(sim3.calculTauxErreurBinaire() == 0.0f, "Simulation message aleatoire 200 bits avec germe -> TEB = 0.0");
        } catch (Exception e) {
            tester(false, "Erreur simulation logique : " + e.getMessage());
        }

        // 5. Tests de robustesse des arguments invalides
        System.out.println("\n5. Tests de robustesse (arguments invalides) :");
        testerRejet(new String[]{"-invalide"}, "Option inconnue (-invalide)");
        testerRejet(new String[]{"-mess", "0"}, "Longueur de message nulle (-mess 0)");
        testerRejet(new String[]{"-mess", "abc"}, "Format message invalide (-mess abc)");
        testerRejet(new String[]{"-mess", "01010102"}, "Message fixe contenant un caractere non binaire (-mess 01010102)");
        testerRejet(new String[]{"-seed", "abc"}, "Germe non numerique (-seed abc)");

        System.out.println("\nBilan TP1 : " + nbSucces + "/" + nbTests + " tests reussis.");
        return (nbSucces == nbTests);
    }

    /**
     * Verifie qu'un jeu d'arguments incorrect leve bien ArgumentsException.
     */
    private static void testerRejet(String[] args, String description) {
        boolean exceptionLevee = false;
        try {
            new Simulateur(args);
        } catch (ArgumentsException e) {
            exceptionLevee = true;
        } catch (Exception e) {
            exceptionLevee = false;
        }
        tester(exceptionLevee, "Rejet attendu : " + description);
    }

    /**
     * Point d'entrée pour exécuter les tests du TP1 seuls.
     *
     * @param args non utilisé
     */
    public static void main(String[] args) {
        boolean succes = executerTests();
        if (!succes) {
            System.exit(1);
        }
    }
}
