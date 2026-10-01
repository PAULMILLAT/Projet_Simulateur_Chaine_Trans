/* package tests; */

import destinations.DestinationFinale;
import information.Information;
import information.InformationNonConformeException;
import simulateur.ArgumentsException;
import simulateur.Simulateur;
import sources.SourceFixe;
import transmetteurs.Codeur;
import transmetteurs.CodeurCanal;
import transmetteurs.Decodeur;
import transmetteurs.DecodeurCanal;
import transmetteurs.TransmetteurParfait;

/**
 * Suite de tests unitaires et d'intégration pour le TP5 (Étape 5).
 * Vérifie le bon fonctionnement du codage et du décodage de canal :
 * <ul>
 *   <li>Composant CodeurCanal (expansion 1 vers 3, contrainte de transition, absence de plus de 2 bits consécutifs identiques)</li>
 *   <li>Composant DecodeurCanal (automate fini déterministe, table de décodage des 8 triplets, détection et correction d'erreur)</li>
 *   <li>Capacité de correction d'erreurs simples (distance minimale d=3, correction d'un bit erroné par triplet)</li>
 *   <li>Intégration dans le Simulateur avec l'option -codeur (en logique, en analogique NRZ/RZ/NRZT, avec bruit et sondage)</li>
 *   <li>Vérification de l'amélioration du TEB sous canal bruité grâce au codage</li>
 * </ul>
 *
 * @author Paul
 * @author Yann
 * @author Enzo
 * @author Damien
 */
public class TestTP5 {

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
     * Exécute l'ensemble des tests du TP5.
     *
     * @return true si tous les tests ont réussi, false sinon
     */
    public static boolean executerTests() {
        System.out.println("=== Lancement des tests Java - TP5 (Etape 5) ===");
        nbTests = 0;
        nbSucces = 0;

        // =========================================================================
        // 1. Tests unitaires du CodeurCanal
        // =========================================================================
        System.out.println("\n1. Tests unitaires du CodeurCanal :");
        try {
            // Test exception si information reçue nulle
            boolean exceptionNulle = false;
            try {
                CodeurCanal codeur = new CodeurCanal();
                codeur.recevoir(null);
            } catch (InformationNonConformeException e) {
                exceptionNulle = true;
            }
            tester(exceptionNulle, "Exception levee si information recue est nulle");

            // Test information vide
            CodeurCanal codeurVide = new CodeurCanal();
            DestinationFinale destVide = new DestinationFinale();
            codeurVide.connecter(destVide);
            codeurVide.recevoir(new Information<Boolean>());
            tester(codeurVide.getInformationEmise() != null && codeurVide.getInformationEmise().nbElements() == 0,
                    "Information vide traitee sans exception");

            // Test codage du bit 0 -> 0 1 0
            CodeurCanal codeur0 = new CodeurCanal();
            Information<Boolean> info0 = new Information<Boolean>();
            info0.add(false);
            codeur0.recevoir(info0);
            Information<Boolean> res0 = codeur0.getInformationEmise();
            tester(res0 != null && res0.nbElements() == 3
                    && !res0.iemeElement(0) && res0.iemeElement(1) && !res0.iemeElement(2),
                    "Codage du bit 0 -> 0 1 0");

            // Test codage du bit 1 -> 1 0 1
            CodeurCanal codeur1 = new CodeurCanal();
            Information<Boolean> info1 = new Information<Boolean>();
            info1.add(true);
            codeur1.recevoir(info1);
            Information<Boolean> res1 = codeur1.getInformationEmise();
            tester(res1 != null && res1.nbElements() == 3
                    && res1.iemeElement(0) && !res1.iemeElement(1) && res1.iemeElement(2),
                    "Codage du bit 1 -> 1 0 1");

            // Test séquence connue "010011" -> "010 101 010 010 101 101" (18 bits)
            CodeurCanal codeurSeq = new CodeurCanal();
            Information<Boolean> infoSeq = new Information<Boolean>();
            boolean[] bitsInit = {false, true, false, false, true, true};
            for (boolean b : bitsInit) {
                infoSeq.add(b);
            }
            codeurSeq.recevoir(infoSeq);
            Information<Boolean> resSeq = codeurSeq.getInformationEmise();
            boolean[] attenduSeq = {
                false, true, false,  // 0 -> 010
                true, false, true,   // 1 -> 101
                false, true, false,  // 0 -> 010
                false, true, false,  // 0 -> 010
                true, false, true,   // 1 -> 101
                true, false, true    // 1 -> 101
            };
            boolean conformeSeq = (resSeq != null && resSeq.nbElements() == 18);
            if (conformeSeq) {
                for (int i = 0; i < 18; i++) {
                    if (resSeq.iemeElement(i) != attenduSeq[i]) {
                        conformeSeq = false;
                        break;
                    }
                }
            }
            tester(conformeSeq, "Codage d'une sequence composite (010011 -> 18 bits)");

            // Test de la propriété de transition (synchronisation horloge) :
            // Jamais plus de 2 bits consécutifs de même valeur
            Information<Boolean> milleZeros = new Information<Boolean>();
            for (int i = 0; i < 500; i++) {
                milleZeros.add(false);
            }
            codeurSeq.recevoir(milleZeros);
            tester(CodeurCanal.verifierProprieteTransitions(codeurSeq.getInformationEmise()),
                    "Propriete transitions : au plus 2 bits identiques sur suite de 500 zeros");

            Information<Boolean> milleUns = new Information<Boolean>();
            for (int i = 0; i < 500; i++) {
                milleUns.add(true);
            }
            codeurSeq.recevoir(milleUns);
            tester(CodeurCanal.verifierProprieteTransitions(codeurSeq.getInformationEmise()),
                    "Propriete transitions : au plus 2 bits identiques sur suite de 500 uns");

            // Test alias Codeur
            Codeur aliasCodeur = new Codeur();
            tester(aliasCodeur instanceof CodeurCanal, "L'alias Codeur herite bien de CodeurCanal");

        } catch (Exception e) {
            tester(false, "Exception inattendue dans les tests de CodeurCanal : " + e.getMessage());
        }

        // =========================================================================
        // 2. Tests unitaires du DecodeurCanal (Automate & Table de décodage)
        // =========================================================================
        System.out.println("\n2. Tests unitaires du DecodeurCanal :");
        try {
            // Exception si nulle
            boolean exceptionNulleDec = false;
            try {
                DecodeurCanal decodeur = new DecodeurCanal();
                decodeur.recevoir(null);
            } catch (InformationNonConformeException e) {
                exceptionNulleDec = true;
            }
            tester(exceptionNulleDec, "Exception levee si information recue est nulle au decodeur");

            // Information vide
            DecodeurCanal decodeurVide = new DecodeurCanal();
            decodeurVide.recevoir(new Information<Boolean>());
            tester(decodeurVide.getInformationEmise() != null && decodeurVide.getInformationEmise().nbElements() == 0,
                    "Information vide traitee sans exception au decodeur");

            // Test exhaustif des 8 combinaisons de 3 bits
            // 000 -> 0 (bit 2 erroné)
            tester(!DecodeurCanal.decoderTriplet(false, false, false)
                    && DecodeurCanal.estErreurReparee(false, false, false)
                    && DecodeurCanal.positionBitErrone(false, false, false) == 2,
                    "Decodage 000 -> 0 (erreur reparee sur bit 2)");

            // 001 -> 1 (bit 1 erroné)
            tester(DecodeurCanal.decoderTriplet(false, false, true)
                    && DecodeurCanal.estErreurReparee(false, false, true)
                    && DecodeurCanal.positionBitErrone(false, false, true) == 1,
                    "Decodage 001 -> 1 (erreur reparee sur bit 1)");

            // 010 -> 0 (mot exact, 0 erreur)
            tester(!DecodeurCanal.decoderTriplet(false, true, false)
                    && !DecodeurCanal.estErreurReparee(false, true, false)
                    && DecodeurCanal.positionBitErrone(false, true, false) == 0,
                    "Decodage 010 -> 0 (mot exact, 0 erreur)");

            // 011 -> 0 (bit 3 erroné)
            tester(!DecodeurCanal.decoderTriplet(false, true, true)
                    && DecodeurCanal.estErreurReparee(false, true, true)
                    && DecodeurCanal.positionBitErrone(false, true, true) == 3,
                    "Decodage 011 -> 0 (erreur reparee sur bit 3)");

            // 100 -> 1 (bit 3 erroné)
            tester(DecodeurCanal.decoderTriplet(true, false, false)
                    && DecodeurCanal.estErreurReparee(true, false, false)
                    && DecodeurCanal.positionBitErrone(true, false, false) == 3,
                    "Decodage 100 -> 1 (erreur reparee sur bit 3)");

            // 101 -> 1 (mot exact, 0 erreur)
            tester(DecodeurCanal.decoderTriplet(true, false, true)
                    && !DecodeurCanal.estErreurReparee(true, false, true)
                    && DecodeurCanal.positionBitErrone(true, false, true) == 0,
                    "Decodage 101 -> 1 (mot exact, 0 erreur)");

            // 110 -> 0 (bit 1 erroné)
            tester(!DecodeurCanal.decoderTriplet(true, true, false)
                    && DecodeurCanal.estErreurReparee(true, true, false)
                    && DecodeurCanal.positionBitErrone(true, true, false) == 1,
                    "Decodage 110 -> 0 (erreur reparee sur bit 1)");

            // 111 -> 1 (bit 2 erroné)
            tester(DecodeurCanal.decoderTriplet(true, true, true)
                    && DecodeurCanal.estErreurReparee(true, true, true)
                    && DecodeurCanal.positionBitErrone(true, true, true) == 2,
                    "Decodage 111 -> 1 (erreur reparee sur bit 2)");

            // Test de l'automate sur un flux de bits continu
            // On envoie 010 (mot 0), 000 (0 bruite bit 2), 101 (mot 1), 111 (1 bruite bit 2)
            DecodeurCanal decodeurFlux = new DecodeurCanal();
            Information<Boolean> fluxBits = new Information<Boolean>();
            boolean[] bitsRecus = {
                false, true, false,   // 010 -> 0
                false, false, false,  // 000 -> 0 (corrigé)
                true, false, true,    // 101 -> 1
                true, true, true      // 111 -> 1 (corrigé)
            };
            for (boolean b : bitsRecus) {
                fluxBits.add(b);
            }
            decodeurFlux.recevoir(fluxBits);
            Information<Boolean> fluxDec = decodeurFlux.getInformationEmise();
            tester(fluxDec != null && fluxDec.nbElements() == 4
                    && !fluxDec.iemeElement(0) && !fluxDec.iemeElement(1)
                    && fluxDec.iemeElement(2) && fluxDec.iemeElement(3),
                    "Decodage par flux d'automate : 4 triplets correctement decodes (0, 0, 1, 1)");

            tester(decodeurFlux.getNbTripletsTraites() == 4,
                    "Compteur de triplets traites egal a 4");
            tester(decodeurFlux.getNbErreursCorrigees() == 2,
                    "Compteur d'erreurs corrigees egal a 2");

            // Test alias Decodeur
            Decodeur aliasDecodeur = new Decodeur();
            tester(aliasDecodeur instanceof DecodeurCanal, "L'alias Decodeur herite bien de DecodeurCanal");

        } catch (Exception e) {
            tester(false, "Exception inattendue dans les tests de DecodeurCanal : " + e.getMessage());
        }

        // =========================================================================
        // 3. Tests de bout-en-bout et capacite de correction d'erreurs
        // =========================================================================
        System.out.println("\n3. Tests bout-en-bout et capacite de correction :");
        try {
            // Chaine parfaite : SourceFixe -> CodeurCanal -> TransmetteurParfait -> DecodeurCanal -> Destination
            SourceFixe sf = new SourceFixe("0110100110");
            CodeurCanal cod = new CodeurCanal();
            TransmetteurParfait<Boolean> tp = new TransmetteurParfait<Boolean>();
            DecodeurCanal dec = new DecodeurCanal();
            DestinationFinale dest = new DestinationFinale();

            sf.connecter(cod);
            cod.connecter(tp);
            tp.connecter(dec);
            dec.connecter(dest);

            sf.emettre();

            Information<Boolean> emise = sf.getInformationEmise();
            Information<Boolean> recue = dest.getInformationRecue();

            boolean identiq = (emise.nbElements() == recue.nbElements());
            if (identiq) {
                for (int i = 0; i < emise.nbElements(); i++) {
                    if (!emise.iemeElement(i).equals(recue.iemeElement(i))) {
                        identiq = false;
                        break;
                    }
                }
            }
            tester(identiq, "Transmission bout-en-bout parfaite sans bruit (10 bits)");
            tester(dec.getNbErreursCorrigees() == 0, "Aucune correction requise sur canal parfait");

            // Test d'injection d'une erreur par mot de code :
            // Source : "0101" -> Codé : [010] [101] [010] [101] (12 bits)
            // Canal bruité artificiel avec 1 inversion par mot :
            // [000] [111] [011] [100]
            // Le décodeur doit retrouver "0101" sans aucune erreur résiduelle !
            Information<Boolean> codedBruite = new Information<Boolean>();
            boolean[] bitsBruites = {
                false, false, false,  // au lieu de 010
                true, true, true,     // au lieu de 101
                false, true, true,    // au lieu de 010
                true, false, false    // au lieu de 101
            };
            for (boolean b : bitsBruites) {
                codedBruite.add(b);
            }
            DecodeurCanal decCorrection = new DecodeurCanal();
            decCorrection.recevoir(codedBruite);
            Information<Boolean> resCorr = decCorrection.getInformationEmise();
            boolean succesCorrection = (resCorr.nbElements() == 4
                    && !resCorr.iemeElement(0) && resCorr.iemeElement(1)
                    && !resCorr.iemeElement(2) && resCorr.iemeElement(3));
            tester(succesCorrection, "Correction a 100% de 1 erreur par mot de code sur sequence test");
            tester(decCorrection.getNbErreursCorrigees() == 4,
                    "Le decodeur a bien detecte et repare 4 erreurs sur les 4 triplets");

        } catch (Exception e) {
            tester(false, "Exception inattendue dans les tests de bout-en-bout : " + e.getMessage());
        }

        // =========================================================================
        // 4. Tests d'intégration Simulateur avec -codeur
        // =========================================================================
        System.out.println("\n4. Tests d'integration Simulateur avec l'option -codeur :");
        try {
            // 4.1 Transmission logique avec -codeur
            Simulateur simLogique = new Simulateur(new String[]{"-codeur"});
            simLogique.execute();
            tester(simLogique.isCodeur(), "Option -codeur active le drapeau isCodeur()");
            tester(simLogique.getCodeur() != null, "Composant Codeur instancie");
            tester(simLogique.getDecodeur() != null, "Composant Decodeur instancie");
            tester(simLogique.calculTauxErreurBinaire() == 0.0f, "Simulation logique avec -codeur -> TEB = 0.0");

            // 4.2 Transmission analogique NRZ avec -codeur
            Simulateur simNRZ = new Simulateur(new String[]{"-mess", "500", "-form", "NRZ", "-codeur"});
            simNRZ.execute();
            tester(simNRZ.calculTauxErreurBinaire() == 0.0f, "Simulation analogique NRZ avec -codeur -> TEB = 0.0");

            // 4.3 Transmission analogique RZ avec -codeur
            Simulateur simRZ = new Simulateur(new String[]{"-mess", "500", "-form", "RZ", "-codeur"});
            simRZ.execute();
            tester(simRZ.calculTauxErreurBinaire() == 0.0f, "Simulation analogique RZ avec -codeur -> TEB = 0.0");

            // 4.4 Transmission analogique NRZT avec -codeur
            Simulateur simNRZT = new Simulateur(new String[]{"-mess", "500", "-form", "NRZT", "-ampl", "-5.0", "5.0", "-codeur"});
            simNRZT.execute();
            tester(simNRZT.calculTauxErreurBinaire() == 0.0f, "Simulation analogique NRZT avec -codeur -> TEB = 0.0");

            // 4.5 Amélioration effective du TEB sous canal bruité (SNR = 3 dB)
            // On compare sans codeur vs avec codeur sur un même message aléatoire fixé par semence
            Simulateur simSansCodeur = new Simulateur(new String[]{"-mess", "5000", "-form", "NRZ", "-snrpb", "3.0", "-seed", "42"});
            simSansCodeur.execute();
            float tebSansCodeur = simSansCodeur.calculTauxErreurBinaire();

            Simulateur simAvecCodeur = new Simulateur(new String[]{"-mess", "5000", "-form", "NRZ", "-snrpb", "3.0", "-seed", "42", "-codeur"});
            simAvecCodeur.execute();
            float tebAvecCodeur = simAvecCodeur.calculTauxErreurBinaire();

            tester(tebAvecCodeur < tebSansCodeur,
                    "Amelioration du TEB avec -codeur a SNR=3dB (sans : " + tebSansCodeur + ", avec : " + tebAvecCodeur + ")");

            // 4.6 Reproductibilité avec -seed
            Simulateur simRejoue1 = new Simulateur(new String[]{"-mess", "2000", "-form", "NRZ", "-snrpb", "2.0", "-seed", "999", "-codeur"});
            simRejoue1.execute();
            Simulateur simRejoue2 = new Simulateur(new String[]{"-mess", "2000", "-form", "NRZ", "-snrpb", "2.0", "-seed", "999", "-codeur"});
            simRejoue2.execute();
            tester(simRejoue1.calculTauxErreurBinaire() == simRejoue2.calculTauxErreurBinaire(),
                    "Reproductibilite exacte de la simulation avec -seed");

            // 4.7 Combinaison avec trajets multiples et sondage (-ti + -sondage + -codeur)
            Simulateur simMulti = new Simulateur(new String[]{
                "-mess", "2000", "-form", "RZ", "-nbEch", "30",
                "-ti", "60", "0.4", "-sondage", "-codeur"
            });
            simMulti.execute();
            tester(simMulti.calculTauxErreurBinaire() == 0.0f,
                    "Combinaison fonctionnelle -ti + -sondage + -codeur -> TEB = 0.0");

        } catch (Exception e) {
            tester(false, "Exception inattendue lors de l'integration Simulateur : " + e.getMessage());
        }

        // =========================================================================
        // 5. Tests de robustesse
        // =========================================================================
        System.out.println("\n5. Tests de robustesse CLI avec -codeur :");
        try {
            boolean rejetInconnu = false;
            try {
                new Simulateur(new String[]{"-codeur", "-optionInconnue"});
            } catch (ArgumentsException e) {
                rejetInconnu = true;
            }
            tester(rejetInconnu, "Rejet attendu : Option inconnue avec -codeur");

            boolean rejetMessInvalide = false;
            try {
                new Simulateur(new String[]{"-codeur", "-mess", "0"});
            } catch (ArgumentsException e) {
                rejetMessInvalide = true;
            }
            tester(rejetMessInvalide, "Rejet attendu : Longueur de message nulle avec -codeur");

        } catch (Exception e) {
            tester(false, "Exception inattendue dans les tests de robustesse : " + e.getMessage());
        }

        System.out.println("\nBilan TP5 : " + nbSucces + "/" + nbTests + " tests reussis.");
        return (nbSucces == nbTests);
    }
}

