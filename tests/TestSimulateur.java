/* package tests; */

/**
 * Lanceur principal de l'ensemble des tests du simulateur.
 * Exécute successivement les tests du TP1 et du TP2.
 *
 * @author Paul
 * @author Yann
 * @author Enzo
 * @author Damien
 */
public class TestSimulateur {
    

    /**
     * Point d'entrée principal des tests.
     *
     * @param args non utilisé
     */
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   EXECUTION DES TESTS DU SIMULATEUR SIT213     ");
        System.out.println("=================================================\n");

        boolean succesTP1 = TestTP1.executerTests();
        System.out.println();
        boolean succesTP2 = TestTP2.executerTests();

        System.out.println("\n=================================================");
        if (succesTP1 && succesTP2) {
            System.out.println("   BILAN GLOBAL : TOUS LES TESTS ONT REUSSI !    ");
            System.out.println("=================================================");
            System.exit(0);
        } else {
            System.out.println("   BILAN GLOBAL : DES TESTS ONT ECHOUE !         ");
            System.out.println("=================================================");
            System.exit(1);
        }
    }
}
