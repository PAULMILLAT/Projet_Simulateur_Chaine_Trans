/* package tests; */

import information.Information;
import simulateur.ArgumentsException;
import simulateur.Simulateur;
import transmetteurs.TransmetteurAnalogiqueBruite;

/**
 * Suite de tests unitaires et d'intégration pour le TP3 (Étape 3).
 * Vérifie le canal à bruit blanc additif gaussien (AWGN),
 * la méthode de Box-Muller, les statistiques du bruit (loi gaussienne),
 * la reproductibilité avec semence, et le comportement du TEB selon le SNR par bit.
 *
 * @author Paul
 * @author Yann
 * @author Enzo
 * @author Damien
 */
public class TestTP3 {

    private static int nbTests = 0;
    private static int nbSucces = 0;

    /**
     * Méthode utilitaire pour valider un test.
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
     * Exécute l'ensemble des tests du TP3.
     *
     * @return true si tous les tests ont réussi, false sinon
     */
    public static boolean executerTests() {
        System.out.println("=== Lancement des tests Java - TP3 (Etape 3) ===");
        nbTests = 0;
        nbSucces = 0;

        // 1. Tests unitaires du TransmetteurAnalogiqueBruite
        System.out.println("\n1. Tests unitaires du TransmetteurAnalogiqueBruite :");
        try {
            // Test exception si information nulle
            boolean exceptionNulle = false;
            try {
                TransmetteurAnalogiqueBruite transmetteur = new TransmetteurAnalogiqueBruite(10.0f, 30);
                transmetteur.recevoir(null);
            } catch (Exception e) {
                exceptionNulle = true;
            }
            tester(exceptionNulle, "Exception levee si information recue est nulle");

            // Test information vide
            TransmetteurAnalogiqueBruite transVide = new TransmetteurAnalogiqueBruite(10.0f, 30);
            Information<Float> infoVide = new Information<Float>();
            transVide.recevoir(infoVide);
            tester(transVide.getInformationEmise() != null && transVide.getInformationEmise().nbElements() == 0,
                    "Information vide geree sans erreur");

            // Test calcul de la puissance du signal Ps et de sigma_b
            // Signal test : 50 echantillons a 1.0f et 50 a 0.0f => Ps = 0.5
            TransmetteurAnalogiqueBruite trans = new TransmetteurAnalogiqueBruite(10.0f, 30, 42);
            Information<Float> sigTest = new Information<Float>();
            for (int i = 0; i < 50; i++) {
                sigTest.add(1.0f);
            }
            for (int i = 0; i < 50; i++) {
                sigTest.add(0.0f);
            }
            trans.recevoir(sigTest);

            float psAttendu = 0.5f;
            tester(Math.abs(trans.getPuissanceSignal() - psAttendu) < 1e-4,
                    "Calcul de la puissance du signal Ps (0.5 attendu, obtenu : " + trans.getPuissanceSignal() + ")");

            // Formule : sigma = sqrt( (Ps * N) / (2 * 10^(snrpb/10)) )
            // Pour Ps = 0.5, N = 30, snrpb = 10 dB :
            // 10^(10/10) = 10, sigma = sqrt( (0.5 * 30) / (2 * 10) ) = sqrt(15 / 20) = sqrt(0.75) = 0.866025
            float sigmaAttendu = (float) Math.sqrt((0.5 * 30.0) / (2.0 * 10.0));
            tester(Math.abs(trans.getSigmaBruit() - sigmaAttendu) < 1e-4,
                    "Calcul de sigma_b (attendu : " + sigmaAttendu + ", obtenu : " + trans.getSigmaBruit() + ")");

        } catch (Exception e) {
            tester(false, "Erreur lors des tests unitaires : " + e.getMessage());
        }

        // 2. Tests statistiques du bruit gaussien (Box-Muller & histogramme)
        System.out.println("\n2. Tests statistiques du bruit gaussien (moyenne, ecart-type, loi normale) :");
        try {
            // On genere un grand nombre d'echantillons pour verifier la loi gaussienne
            int nbEchTotal = 10000;
            TransmetteurAnalogiqueBruite transStat = new TransmetteurAnalogiqueBruite(5.0f, 30, 12345);
            Information<Float> sigZero = new Information<Float>();
            for (int i = 0; i < nbEchTotal; i++) {
                sigZero.add(1.0f); // Ps = 1.0f
            }
            transStat.recevoir(sigZero);

            Information<Float> bruit = transStat.getBruitGenere();
            tester(bruit != null && bruit.nbElements() == nbEchTotal,
                    "Nombre d'echantillons de bruit generes conforme (" + nbEchTotal + ")");

            // Calcul de la moyenne et de la variance empirique du bruit
            double somme = 0.0;
            for (Float b : bruit) {
                somme += b;
            }
            double moyenne = somme / nbEchTotal;

            double sommeCarresDiff = 0.0;
            for (Float b : bruit) {
                sommeCarresDiff += (b - moyenne) * (b - moyenne);
            }
            double varianceEmpirique = sommeCarresDiff / nbEchTotal;
            double sigmaEmpirique = Math.sqrt(varianceEmpirique);
            float sigmaTheorique = transStat.getSigmaBruit();

            // La moyenne du bruit blanc gaussien doit etre tres proche de 0
            tester(Math.abs(moyenne) < 0.05 * sigmaTheorique,
                    "Moyenne du bruit proche de 0 (obtenu : " + String.format("%.4f", moyenne) + ")");

            // L'ecart-type empirique doit etre proche de sigma theorique (marge < 5%)
            tester(Math.abs(sigmaEmpirique - sigmaTheorique) / sigmaTheorique < 0.05,
                    "Ecart-type empirique conforme au sigma theorique (theorie : "
                            + String.format("%.4f", sigmaTheorique) + ", empirique : "
                            + String.format("%.4f", sigmaEmpirique) + ")");

            // Verification de la loi normale (remarque du sujet TP3 slide 3) :
            // Dans une loi gaussienne : ~68.3% des valeurs sont dans [-sigma, +sigma]
            // et ~95.4% sont dans [-2*sigma, +2*sigma]
            int nbDans1Sigma = 0;
            int nbDans2Sigma = 0;
            for (Float b : bruit) {
                if (Math.abs(b) <= sigmaTheorique) {
                    nbDans1Sigma++;
                }
                if (Math.abs(b) <= 2.0 * sigmaTheorique) {
                    nbDans2Sigma++;
                }
            }
            double prop1Sigma = (double) nbDans1Sigma / nbEchTotal;
            double prop2Sigma = (double) nbDans2Sigma / nbEchTotal;

            tester(prop1Sigma >= 0.65 && prop1Sigma <= 0.72,
                    "Distribution gaussienne a 1*sigma : " + String.format("%.1f", prop1Sigma * 100) + "% (attendu ~68%)");
            tester(prop2Sigma >= 0.93 && prop2Sigma <= 0.97,
                    "Distribution gaussienne a 2*sigma : " + String.format("%.1f", prop2Sigma * 100) + "% (attendu ~95%)");

            // Reproductibilite avec meme graine
            TransmetteurAnalogiqueBruite transGraine1 = new TransmetteurAnalogiqueBruite(5.0f, 30, 42);
            TransmetteurAnalogiqueBruite transGraine2 = new TransmetteurAnalogiqueBruite(5.0f, 30, 42);
            transGraine1.recevoir(sigZero);
            transGraine2.recevoir(sigZero);
            tester(transGraine1.getBruitGenere().equals(transGraine2.getBruitGenere()),
                    "Reproductibilite du bruit avec la meme semence (-seed 42)");

            // Bruits differents avec deux graines distinctes
            TransmetteurAnalogiqueBruite transGraine3 = new TransmetteurAnalogiqueBruite(5.0f, 30, 999);
            transGraine3.recevoir(sigZero);
            tester(!transGraine1.getBruitGenere().equals(transGraine3.getBruitGenere()),
                    "Bruits distincts avec deux semences differentes");

        } catch (Exception e) {
            tester(false, "Erreur tests statistiques du bruit : " + e.getMessage());
        }

        // 3. Tests d'intégration Simulateur (chaîne complète avec bruit)
        System.out.println("\n3. Tests d'integration Simulateur avec canal bruite :");
        try {
            // Fort SNR (30 dB) => TEB attendu = 0.0
            Simulateur simFortSNR = new Simulateur(new String[]{"-form", "NRZ", "-snrpb", "30", "-mess", "1000", "-seed", "42"});
            simFortSNR.execute();
            tester(simFortSNR.calculTauxErreurBinaire() == 0.0f,
                    "Transmission NRZ fort SNR (30 dB) -> TEB = 0.0");

            // Faible SNR (-10 dB) => TEB attendu eleve (> 0)
            Simulateur simFaibleSNR = new Simulateur(new String[]{"-form", "NRZ", "-snrpb", "-10", "-mess", "1000", "-seed", "42"});
            simFaibleSNR.execute();
            float tebFaible = simFaibleSNR.calculTauxErreurBinaire();
            tester(tebFaible > 0.2f && tebFaible < 0.6f,
                    "Transmission NRZ faible SNR (-10 dB) -> TEB eleve (" + tebFaible + ")");

            // Decroissance du TEB avec l'augmentation du SNR
            Simulateur simSNR0 = new Simulateur(new String[]{"-form", "NRZ", "-snrpb", "0", "-mess", "2000", "-seed", "123"});
            simSNR0.execute();
            float teb0 = simSNR0.calculTauxErreurBinaire();

            Simulateur simSNR5 = new Simulateur(new String[]{"-form", "NRZ", "-snrpb", "5", "-mess", "2000", "-seed", "123"});
            simSNR5.execute();
            float teb5 = simSNR5.calculTauxErreurBinaire();

            Simulateur simSNR10 = new Simulateur(new String[]{"-form", "NRZ", "-snrpb", "10", "-mess", "2000", "-seed", "123"});
            simSNR10.execute();
            float teb10 = simSNR10.calculTauxErreurBinaire();

            tester(teb0 > teb5 && teb5 >= teb10,
                    "Decroissance du TEB avec le SNR (TEB(0dB)=" + teb0 + " > TEB(5dB)=" + teb5 + " >= TEB(10dB)=" + teb10 + ")");

            // Test avec les autres formes d'onde
            Simulateur simRZ = new Simulateur(new String[]{"-form", "RZ", "-snrpb", "30", "-mess", "500", "-seed", "42"});
            simRZ.execute();
            tester(simRZ.calculTauxErreurBinaire() == 0.0f,
                    "Transmission RZ fort SNR (30 dB) -> TEB = 0.0");

            Simulateur simNRZT = new Simulateur(new String[]{"-form", "NRZT", "-ampl", "-5.0", "5.0", "-snrpb", "30", "-mess", "500", "-seed", "42"});
            simNRZT.execute();
            tester(simNRZT.calculTauxErreurBinaire() == 0.0f,
                    "Transmission NRZT fort SNR (30 dB) -> TEB = 0.0");

            // Reproductibilite de la simulation avec -seed
            Simulateur simRep1 = new Simulateur(new String[]{"-form", "NRZ", "-snrpb", "3", "-mess", "1000", "-seed", "777"});
            simRep1.execute();
            Simulateur simRep2 = new Simulateur(new String[]{"-form", "NRZ", "-snrpb", "3", "-mess", "1000", "-seed", "777"});
            simRep2.execute();
            tester(simRep1.calculTauxErreurBinaire() == simRep2.calculTauxErreurBinaire(),
                    "Reproductibilite exacte de la simulation avec -seed 777 (TEB=" + simRep1.calculTauxErreurBinaire() + ")");

            // Verification de l'alias -snr
            Simulateur simAliasSNR = new Simulateur(new String[]{"-form", "NRZ", "-snr", "30", "-mess", "500", "-seed", "42"});
            simAliasSNR.execute();
            tester(simAliasSNR.calculTauxErreurBinaire() == 0.0f,
                    "Alias -snr pris en compte comme -snrpb");

        } catch (Exception e) {
            tester(false, "Erreur simulation complete TP3 : " + e.getMessage());
        }

        // 4. Tests de robustesse et gestion des erreurs d'arguments
        System.out.println("\n4. Tests de robustesse & rejets d'arguments (-snrpb) :");
        testerRejet(new String[]{"-snrpb"}, "Rejet -snrpb sans valeur");
        testerRejet(new String[]{"-snrpb", "abc"}, "Rejet -snrpb avec valeur non flottante");
        testerRejet(new String[]{"-snr"}, "Rejet -snr sans valeur");
        testerRejet(new String[]{"-snr", "xyz"}, "Rejet -snr avec valeur non flottante");

        System.out.println("\nBilan TP3 : " + nbSucces + "/" + nbTests + " tests reussis.");
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
     * Point d'entrée pour exécuter les tests du TP3 seuls.
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

