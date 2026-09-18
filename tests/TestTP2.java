/* package tests; */

import information.Information;
import simulateur.ArgumentsException;
import simulateur.Simulateur;
import transmetteurs.Emetteur;
import transmetteurs.Recepteur;

/**
 * Suite de tests unitaires et d'intégration pour le TP2 (Étape 2).
 * Vérifie les formes d'onde (NRZ, NRZT, RZ), la démodulation par seuil,
 * la chaîne analogique complète et le respect des contraintes d'amplitude.
 *
 * @author Paul
 * @author Yann
 * @author Enzo
 * @author Damien
 */
public class TestTP2 {

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
     * Exécute l'ensemble des tests du TP2.
     *
     * @return true si tous les tests ont réussi, false sinon
     */
    public static boolean executerTests() {
        System.out.println("=== Lancement des tests Java - TP2 (Etape 2) ===");
        nbTests = 0;
        nbSucces = 0;

        // 1. Tests unitaires de l'Émetteur
        System.out.println("\n1. Tests des formes d'onde de l'Emetteur :");
        try {
            // Test forme NRZ
            Emetteur emetteurNRZ = new Emetteur("NRZ", 10, -2.0f, 3.0f);
            Information<Boolean> bitsNRZ = new Information<Boolean>();
            bitsNRZ.add(true);
            bitsNRZ.add(false);
            emetteurNRZ.recevoir(bitsNRZ);

            Information<Float> sigNRZ = emetteurNRZ.getInformationEmise();
            tester(sigNRZ.nbElements() == 20, "Emetteur NRZ : nombre d'echantillons (2 bits x 10 ech = 20)");
            tester(sigNRZ.iemeElement(0) == 3.0f && sigNRZ.iemeElement(9) == 3.0f,
                    "Emetteur NRZ : echantillons a Amax (3.0f) pour le bit 1");
            tester(sigNRZ.iemeElement(10) == -2.0f && sigNRZ.iemeElement(19) == -2.0f,
                    "Emetteur NRZ : echantillons a Amin (-2.0f) pour le bit 0");

            // Test forme RZ
            Emetteur emetteurRZ = new Emetteur("RZ", 30, 0.0f, 5.0f);
            Information<Boolean> bitsRZ = new Information<Boolean>();
            bitsRZ.add(true);
            bitsRZ.add(false);
            emetteurRZ.recevoir(bitsRZ);

            Information<Float> sigRZ = emetteurRZ.getInformationEmise();
            tester(sigRZ.nbElements() == 60, "Emetteur RZ : nombre d'echantillons (2 bits x 30 ech = 60)");
            tester(sigRZ.iemeElement(0) == 0.0f && sigRZ.iemeElement(9) == 0.0f,
                    "Emetteur RZ : 1er tiers a 0.0 pour bit 1");
            tester(sigRZ.iemeElement(10) == 5.0f && sigRZ.iemeElement(19) == 5.0f,
                    "Emetteur RZ : 2eme tiers a Amax (5.0) pour bit 1");
            tester(sigRZ.iemeElement(20) == 0.0f && sigRZ.iemeElement(29) == 0.0f,
                    "Emetteur RZ : 3eme tiers a 0.0 pour bit 1");
            tester(sigRZ.iemeElement(30) == 0.0f && sigRZ.iemeElement(45) == 0.0f && sigRZ.iemeElement(59) == 0.0f,
                    "Emetteur RZ : echantillons a 0.0 pour bit 0");

            // Test forme NRZT
            Emetteur emetteurNRZT = new Emetteur("NRZT", 30, -5.0f, 5.0f);
            Information<Boolean> bitsNRZT = new Information<Boolean>();
            bitsNRZT.add(true);
            bitsNRZT.add(true);
            bitsNRZT.add(false);
            emetteurNRZT.recevoir(bitsNRZT);

            Information<Float> sigNRZT = emetteurNRZT.getInformationEmise();
            tester(sigNRZT.nbElements() == 90, "Emetteur NRZT : nombre d'echantillons (3 bits x 30 ech = 90)");
            tester(sigNRZT.iemeElement(15) == 5.0f, "Emetteur NRZT : plateau bit 0 a Amax (5.0)");
            tester(sigNRZT.iemeElement(45) == 5.0f, "Emetteur NRZT : plateau bit 1 a Amax (5.0)");
            tester(sigNRZT.iemeElement(75) == -5.0f, "Emetteur NRZT : plateau bit 2 a Amin (-5.0)");
        } catch (Exception e) {
            tester(false, "Erreur lors des tests de l'Emetteur : " + e.getMessage());
        }

        // 2. Tests unitaires du Récepteur
        System.out.println("\n2. Tests de la demodulation du Recepteur :");
        try {
            Information<Boolean> bitsOrigine = new Information<Boolean>();
            bitsOrigine.add(true);
            bitsOrigine.add(false);
            bitsOrigine.add(true);
            bitsOrigine.add(true);
            bitsOrigine.add(false);

            // Test chaine Emetteur NRZ -> Recepteur NRZ
            Emetteur emNRZ = new Emetteur("NRZ", 30, -1.0f, 1.0f);
            Recepteur recNRZ = new Recepteur("NRZ", 30, -1.0f, 1.0f);
            emNRZ.connecter(recNRZ);
            emNRZ.recevoir(bitsOrigine);
            tester(recNRZ.getInformationEmise().equals(bitsOrigine),
                    "Recepteur NRZ demodule fidelement la sequence");

            // Test chaine Emetteur RZ -> Recepteur RZ
            Emetteur emRZ = new Emetteur("RZ", 30, 0.0f, 1.0f);
            Recepteur recRZ = new Recepteur("RZ", 30, 0.0f, 1.0f);
            emRZ.connecter(recRZ);
            emRZ.recevoir(bitsOrigine);
            tester(recRZ.getInformationEmise().equals(bitsOrigine),
                    "Recepteur RZ demodule fidelement la sequence");

            // Test chaine Emetteur NRZT -> Recepteur NRZT
            Emetteur emNRZT = new Emetteur("NRZT", 30, -5.0f, 5.0f);
            Recepteur recNRZT = new Recepteur("NRZT", 30, -5.0f, 5.0f);
            emNRZT.connecter(recNRZT);
            emNRZT.recevoir(bitsOrigine);
            tester(recNRZT.getInformationEmise().equals(bitsOrigine),
                    "Recepteur NRZT demodule fidelement la sequence");
        } catch (Exception e) {
            tester(false, "Erreur lors des tests du Recepteur : " + e.getMessage());
        }

        // 3. Tests d'intégration avec Simulateur (chaîne analogique complète)
        System.out.println("\n3. Tests d'integration Simulateur (chaine analogique TP2) :");
        try {
            // Simulation NRZ
            Simulateur simNRZ = new Simulateur(new String[]{"-form", "NRZ", "-mess", "100"});
            simNRZ.execute();
            tester(simNRZ.calculTauxErreurBinaire() == 0.0f, "Simulation complete NRZ -> TEB = 0.0");

            // Simulation RZ (forme par defaut si transmission analogique demandee)
            Simulateur simRZ = new Simulateur(new String[]{"-form", "RZ", "-nbEch", "30"});
            simRZ.execute();
            tester(simRZ.calculTauxErreurBinaire() == 0.0f, "Simulation complete RZ -> TEB = 0.0");

            // Simulation NRZT avec amplitudes negatives et positives
            Simulateur simNRZT = new Simulateur(new String[]{"-form", "NRZT", "-ampl", "-5.0", "5.0", "-mess", "150"});
            simNRZT.execute();
            tester(simNRZT.calculTauxErreurBinaire() == 0.0f, "Simulation complete NRZT (ampl -5/5) -> TEB = 0.0");

            // Simulation avec nbEch different (ex: 60)
            Simulateur simNbEch = new Simulateur(new String[]{"-form", "NRZ", "-nbEch", "60"});
            simNbEch.execute();
            tester(simNbEch.calculTauxErreurBinaire() == 0.0f, "Simulation avec nbEch=60 -> TEB = 0.0");
        } catch (Exception e) {
            tester(false, "Erreur simulation analogique : " + e.getMessage());
        }

        // 4. Tests de robustesse et vérification des contraintes du sujet TP2
        System.out.println("\n4. Tests de verification des contraintes du TP2 :");
        testerRejet(new String[]{"-form", "RZ", "-ampl", "-5.0", "5.0"},
                "Rejet RZ avec Amin != 0 (-ampl -5.0 5.0)");
        testerRejet(new String[]{"-form", "NRZ", "-ampl", "2.0", "5.0"},
                "Rejet NRZ avec Amin > 0 (-ampl 2.0 5.0)");
        testerRejet(new String[]{"-form", "NRZT", "-ampl", "5.0", "2.0"},
                "Rejet avec Amin >= Amax (-ampl 5.0 2.0)");
        testerRejet(new String[]{"-nbEch", "0"},
                "Rejet nbEch <= 0 (-nbEch 0)");
        testerRejet(new String[]{"-nbEch", "-5"},
                "Rejet nbEch negatif (-nbEch -5)");
        testerRejet(new String[]{"-form", "INCONNU"},
                "Rejet forme d'onde invalide (-form INCONNU)");

        System.out.println("\nBilan TP2 : " + nbSucces + "/" + nbTests + " tests reussis.");
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
     * Point d'entrée pour exécuter les tests du TP2 seuls.
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

