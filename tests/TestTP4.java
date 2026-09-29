/* package tests; */

import destinations.DestinationFinale;
import information.Information;
import simulateur.ArgumentsException;
import simulateur.Simulateur;
import sources.SourceFixe;
import transmetteurs.Emetteur;
import transmetteurs.Recepteur;
import transmetteurs.TransmetteurAnalogiqueTrajetsMultiples;

/**
 * Suite de tests unitaires et d'integration pour le TP4 (Etape 4).
 * Verifie le canal a trajets indirects multiples (canal multi-trajets),
 * la superposition temporelle des echos, la gestion conjointe du bruit,
 * l'analyse de ligne de commande (-ti), et la robustesse des arguments.
 *
 * @author Paul
 * @author Yann
 * @author Enzo
 * @author Damien
 */
public class TestTP4 {

    private static int nbTests = 0;
    private static int nbSucces = 0;

    /**
     * Methode utilitaire pour valider un test.
     *
     * @param condition vrai si le test reussit
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
     * Convertit une Information de flottants en tableau de float primitif.
     */
    private static float[] versTableau(Information<Float> info) {
        if (info == null) {
            return new float[0];
        }
        float[] t = new float[info.nbElements()];
        int idx = 0;
        for (Float f : info) {
            t[idx++] = f;
        }
        return t;
    }

    /**
     * Execute l'ensemble des tests du TP4.
     *
     * @return true si tous les tests ont reussi, false sinon
     */
    public static boolean executerTests() {
        System.out.println("=== Lancement des tests Java - TP4 (Etape 4) ===");
        nbTests = 0;
        nbSucces = 0;

        // 1. Tests unitaires du TransmetteurAnalogiqueTrajetsMultiples
        System.out.println("\n1. Tests unitaires du TransmetteurAnalogiqueTrajetsMultiples :");
        try {
            // Test exception si information nulle
            boolean exceptionNulle = false;
            try {
                TransmetteurAnalogiqueTrajetsMultiples canal =
                    new TransmetteurAnalogiqueTrajetsMultiples(null, 30, 0, new int[0], new float[0]);
                canal.recevoir(null);
            } catch (Exception e) {
                exceptionNulle = true;
            }
            tester(exceptionNulle, "Exception levee si information recue est nulle");

            // Test information vide
            TransmetteurAnalogiqueTrajetsMultiples canalVide =
                new TransmetteurAnalogiqueTrajetsMultiples(null, 30, 1, new int[]{5}, new float[]{0.5f});
            Information<Float> infoVide = new Information<Float>();
            canalVide.recevoir(infoVide);
            tester(canalVide.getInformationEmise() != null && canalVide.getInformationEmise().nbElements() == 0,
                    "Information vide traitee sans lever d'exception");

            // Test trajet direct seul (0 trajet indirect) : signal inchange et non bruite
            TransmetteurAnalogiqueTrajetsMultiples canalDirect =
                new TransmetteurAnalogiqueTrajetsMultiples(null, 30, 0, new int[0], new float[0]);
            Information<Float> sigDirect = new Information<Float>();
            sigDirect.add(0.0f);
            sigDirect.add(1.0f);
            sigDirect.add(0.5f);
            sigDirect.add(1.0f);
            canalDirect.recevoir(sigDirect);

            float[] recuDirect = versTableau(canalDirect.getInformationEmise());
            boolean signalIdentique = (recuDirect.length == 4)
                && (Math.abs(recuDirect[0] - 0.0f) < 1e-5)
                && (Math.abs(recuDirect[1] - 1.0f) < 1e-5)
                && (Math.abs(recuDirect[2] - 0.5f) < 1e-5)
                && (Math.abs(recuDirect[3] - 1.0f) < 1e-5);
            tester(signalIdentique && !canalDirect.isBruite(),
                    "Trajet direct seul (0 trajet indirect) : signal inchange et non bruite");

            // Test 1 trajet indirect simple : superposition temporelle et allongement
            // direct: [1.0, 0.0, 1.0, 0.0], decalage dt=2, ar=0.5
            // n=0: 1.0
            // n=1: 0.0
            // n=2: 1.0 + 0.5*1.0 = 1.5
            // n=3: 0.0 + 0.5*0.0 = 0.0
            // n=4: 0.0 + 0.5*1.0 = 0.5
            // n=5: 0.0 + 0.5*0.0 = 0.0
            TransmetteurAnalogiqueTrajetsMultiples canal1Trajet =
                new TransmetteurAnalogiqueTrajetsMultiples(null, 30, 1, new int[]{2}, new float[]{0.5f});
            Information<Float> sig1 = new Information<Float>();
            sig1.add(1.0f);
            sig1.add(0.0f);
            sig1.add(1.0f);
            sig1.add(0.0f);
            canal1Trajet.recevoir(sig1);

            float[] composite1 = versTableau(canal1Trajet.getInformationEmise());
            boolean superposition1OK = (composite1.length == 6)
                && (Math.abs(composite1[0] - 1.0f) < 1e-5)
                && (Math.abs(composite1[1] - 0.0f) < 1e-5)
                && (Math.abs(composite1[2] - 1.5f) < 1e-5)
                && (Math.abs(composite1[3] - 0.0f) < 1e-5)
                && (Math.abs(composite1[4] - 0.5f) < 1e-5)
                && (Math.abs(composite1[5] - 0.0f) < 1e-5);
            tester(superposition1OK,
                    "Superposition exacte d'un trajet indirect (dt=2, ar=0.5)");

            // Test 2 trajets indirects simultanes avec amplitude negative
            // direct: [1.0, 1.0], dt1=1 ar1=0.5, dt2=2 ar2=-0.25
            // n=0: 1.0
            // n=1: 1.0 + 0.5*1.0 = 1.5
            // n=2: 0.0 + 0.5*1.0 - 0.25*1.0 = 0.25
            // n=3: 0.0 + 0.0 - 0.25*1.0 = -0.25
            TransmetteurAnalogiqueTrajetsMultiples canal2Trajets =
                new TransmetteurAnalogiqueTrajetsMultiples(null, 30, 2, new int[]{1, 2}, new float[]{0.5f, -0.25f});
            Information<Float> sig2 = new Information<Float>();
            sig2.add(1.0f);
            sig2.add(1.0f);
            canal2Trajets.recevoir(sig2);

            float[] composite2 = versTableau(canal2Trajets.getInformationEmise());
            boolean superposition2OK = (composite2.length == 4)
                && (Math.abs(composite2[0] - 1.0f) < 1e-5)
                && (Math.abs(composite2[1] - 1.5f) < 1e-5)
                && (Math.abs(composite2[2] - 0.25f) < 1e-5)
                && (Math.abs(composite2[3] - (-0.25f)) < 1e-5);
            tester(superposition2OK,
                    "Superposition exacte de deux trajets indirects avec attenuation negative");

            // Test limite haute : 5 trajets indirects autorises
            boolean cinqTrajetsOK = true;
            try {
                int[] dec5 = {5, 10, 15, 20, 25};
                float[] amp5 = {0.1f, 0.2f, 0.15f, 0.05f, -0.1f};
                TransmetteurAnalogiqueTrajetsMultiples canal5 =
                    new TransmetteurAnalogiqueTrajetsMultiples(null, 30, 5, dec5, amp5);
                canal5.recevoir(sig1);
                cinqTrajetsOK = (canal5.getNbTrajets() == 5);
            } catch (Exception e) {
                cinqTrajetsOK = false;
            }
            tester(cinqTrajetsOK, "Acceptation de la configuration maximale autorisee (5 trajets indirects)");

            // Test calcul de puissance Ps sur signal composite et bruitage
            TransmetteurAnalogiqueTrajetsMultiples canalBruite =
                new TransmetteurAnalogiqueTrajetsMultiples(10.0f, 30, 1, new int[]{2}, new float[]{0.5f}, 42);
            canalBruite.recevoir(sig1);
            tester(canalBruite.isBruite() && canalBruite.getPuissanceSignal() > 0.0f && canalBruite.getSigmaBruit() > 0.0f,
                    "Calcul de Ps et activation du bruitage sur le signal composite");

            // Test reproductibilite avec graine sur canal multi-trajets bruite
            TransmetteurAnalogiqueTrajetsMultiples canalSeed1 =
                new TransmetteurAnalogiqueTrajetsMultiples(10.0f, 30, 1, new int[]{2}, new float[]{0.5f}, 1234);
            TransmetteurAnalogiqueTrajetsMultiples canalSeed2 =
                new TransmetteurAnalogiqueTrajetsMultiples(10.0f, 30, 1, new int[]{2}, new float[]{0.5f}, 1234);
            canalSeed1.recevoir(sig1);
            canalSeed2.recevoir(sig1);
            float[] s1 = versTableau(canalSeed1.getInformationEmise());
            float[] s2 = versTableau(canalSeed2.getInformationEmise());
            boolean memeBruit = true;
            for (int i = 0; i < s1.length; i++) {
                if (Math.abs(s1[i] - s2[i]) > 1e-6) {
                    memeBruit = false;
                    break;
                }
            }
            tester(memeBruit, "Reproductibilite du canal bruite et multi-trajets avec la meme semence (-seed 1234)");

            // Validation constructeur : rejet > 5 trajets et tableaux non conformes
            boolean rejet6Trajets = false;
            try {
                new TransmetteurAnalogiqueTrajetsMultiples(null, 30, 6, new int[6], new float[6]);
            } catch (IllegalArgumentException e) {
                rejet6Trajets = true;
            }
            tester(rejet6Trajets, "Rejet dans le constructeur si nbTrajets > 5");

            boolean rejetTableauxIncoherents = false;
            try {
                new TransmetteurAnalogiqueTrajetsMultiples(null, 30, 2, new int[]{10}, new float[]{0.5f, 0.2f});
            } catch (IllegalArgumentException e) {
                rejetTableauxIncoherents = true;
            }
            tester(rejetTableauxIncoherents, "Rejet si la taille des tableaux ne correspond pas a nbTrajets");

            boolean rejetDecalageNegatif = false;
            try {
                new TransmetteurAnalogiqueTrajetsMultiples(null, 30, 1, new int[]{-5}, new float[]{0.5f});
            } catch (IllegalArgumentException e) {
                rejetDecalageNegatif = true;
            }
            tester(rejetDecalageNegatif, "Rejet dans le constructeur d'un retard temporel negatif");

        } catch (Exception e) {
            tester(false, "Erreur inattendue tests unitaires canal : " + e.getMessage());
        }

        // 2. Tests d'integration Simulateur (chaine complete avec -ti)
        System.out.println("\n2. Tests d'integration Simulateur avec l'option -ti :");
        try {
            // Configuration nominale 1 trajet indirect
            Simulateur sim1 = new Simulateur(new String[]{"-mess", "1000", "-form", "RZ", "-ti", "60", "0.5"});
            sim1.execute();
            tester(sim1.isTransmissionAnalogique() && sim1.isTrajetsMultiples() && sim1.getNbTrajets() == 1,
                    "Simulation avec 1 trajet indirect (-ti 60 0.5) correctement configuree");

            // Configuration nominale 2 trajets indirects
            Simulateur sim2 = new Simulateur(new String[]{"-mess", "1000", "-form", "NRZ", "-ti", "30", "0.3", "60", "-0.2"});
            sim2.execute();
            tester(sim2.getNbTrajets() == 2,
                    "Simulation avec 2 trajets indirects (-ti 30 0.3 60 -0.2) correctement configuree");

            // -ti seul impose la transmission analogique par defaut (conforme commande_unique)
            Simulateur simTiSeul = new Simulateur(new String[]{"-mess", "500", "-ti", "40", "0.2"});
            tester(simTiSeul.isTransmissionAnalogique(),
                    "La presence de -ti active automatiquement la transmission analogique");

            // Multi-trajets combine avec le bruit (-ti et -snrpb)
            Simulateur simTiBruit = new Simulateur(new String[]{
                "-mess", "1000", "-form", "NRZ", "-ti", "30", "0.3", "-snrpb", "20", "-seed", "42"
            });
            simTiBruit.execute();
            tester(simTiBruit.isCanalBruite() && simTiBruit.isTrajetsMultiples(),
                    "Combinaison fonctionnelle de -ti et -snrpb");

            // Reproductibilite de la simulation complete avec -seed
            Simulateur simRep1 = new Simulateur(new String[]{
                "-mess", "1000", "-form", "RZ", "-ti", "45", "0.4", "-snrpb", "5", "-seed", "99"
            });
            simRep1.execute();
            Simulateur simRep2 = new Simulateur(new String[]{
                "-mess", "1000", "-form", "RZ", "-ti", "45", "0.4", "-snrpb", "5", "-seed", "99"
            });
            simRep2.execute();
            tester(simRep1.calculTauxErreurBinaire() == simRep2.calculTauxErreurBinaire(),
                    "Reproductibilite exacte de la chaine multi-trajets bruitee avec -seed 99");

            // Compatibilite avec les 3 formes d'onde (RZ, NRZ, NRZT)
            Simulateur simNRZT = new Simulateur(new String[]{
                "-mess", "500", "-form", "NRZT", "-ampl", "-3.0", "3.0", "-ti", "30", "0.2"
            });
            simNRZT.execute();
            tester(simNRZT.calculTauxErreurBinaire() >= 0.0f,
                    "Compatibilite multi-trajets avec la forme d'onde NRZT");

        } catch (Exception e) {
            tester(false, "Erreur simulation complete TP4 : " + e.getMessage());
        }

        // 3. Tests de la fonctionnalite sondage et egalisation (-sondage)
        System.out.println("\n3. Tests du sondage et de l'egalisation de canal (-sondage) :");
        try {
            // Canal multi-trajets avec sondage : l'egaliseur compense le retard et restitue TEB = 0
            Simulateur simSondage1 = new Simulateur(new String[]{
                "-mess", "3000", "-form", "RZ", "-nbEch", "30", "-ti", "60", "0.5", "-sondage"
            });
            simSondage1.execute();
            tester(simSondage1.calculTauxErreurBinaire() == 0.0f,
                    "Egalisation de canal avec -sondage (1 trajet dt=60, ar=0.5) -> TEB = 0.0");

            // Non-regression : canal parfait avec sondage reste a TEB = 0.0
            Simulateur simSondageParfait = new Simulateur(new String[]{
                "-mess", "2000", "-form", "RZ", "-nbEch", "30", "-sondage"
            });
            simSondageParfait.execute();
            tester(simSondageParfait.calculTauxErreurBinaire() == 0.0f,
                    "Non-regression avec -sondage sur canal sans trajet indirect -> TEB = 0.0");

        } catch (Exception e) {
            tester(false, "Erreur tests sondage/egalisation : " + e.getMessage());
        }

        // 4. Tests de robustesse et rejets d'arguments (-ti)
        System.out.println("\n4. Tests de robustesse & rejets d'arguments (-ti) :");
        testerRejet(new String[]{"-ti"}, "Rejet -ti sans aucun parametre");
        testerRejet(new String[]{"-ti", "30"}, "Rejet -ti avec couple incomplet (dt sans ar)");
        testerRejet(new String[]{"-ti", "abc", "0.5"}, "Rejet -ti avec dt non entier");
        testerRejet(new String[]{"-ti", "30", "xyz"}, "Rejet -ti avec ar non flottant");
        testerRejet(new String[]{
            "-ti", "10", "0.1", "20", "0.1", "30", "0.1", "40", "0.1", "50", "0.1", "60", "0.1"
        }, "Rejet -ti avec plus de 5 trajets indirects");
        testerRejet(new String[]{"-ti", "-10", "0.5"}, "Rejet -ti avec dt negatif");

        System.out.println("\nBilan TP4 : " + nbSucces + "/" + nbTests + " tests reussis.");
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
     * Point d'entree pour executer les tests du TP4 seuls.
     *
     * @param args non utilise
     */
    public static void main(String[] args) {
        boolean succes = executerTests();
        if (!succes) {
            System.exit(1);
        }
    }
}
