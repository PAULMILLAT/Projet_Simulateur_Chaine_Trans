package transmetteurs;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import destinations.DestinationFinale;
import information.Information;
import sources.SourceAleatoire;
import sources.SourceFixe;

/**
 * Tests d'intégration de la chaîne avec sondage :
 * Source -> InsertionEntete -> Emetteur -> TransmetteurAnalogiqueTrajetsMultiples
 * -> RecepteurSonde -> DestinationFinale.
 *
 * Vérifie que le message utile est retrouvé à l'identique (ou quasi, en
 * présence de bruit) après détection et compensation des trajets
 * multiples, quel que soit le nombre de trajets (0 à 5), et que le
 * garde-fou de stabilité ne fait pas planter le récepteur sur un canal
 * non minimum de phase.
 */
class RecepteurSondeTest {

    private static final int NB_ECH = 30;
    private static final String FORME_ONDE = "RZ";

    /**
     * Fait transiter un message fixe donné à travers la chaîne complète
     * avec sondage, pour un canal à trajets multiples donné, et renvoie
     * le message effectivement reçu (en-tête déjà retirée par RecepteurSonde).
     */
    private Information<Boolean> transmettreAvecSondage(
            String messageBits, int nbTrajets, int[] decalages, float[] amplitudes, Float snrpb) throws Exception {

        SourceFixe source = new SourceFixe(messageBits);
        InsertionEntete inserteur = new InsertionEntete();
        Emetteur emetteur = new Emetteur(FORME_ONDE, NB_ECH, 0.0f, 1.0f);
        TransmetteurAnalogiqueTrajetsMultiples canal =
            new TransmetteurAnalogiqueTrajetsMultiples(snrpb, NB_ECH, nbTrajets, decalages, amplitudes, 42);
        RecepteurSonde recepteur = new RecepteurSonde(FORME_ONDE, NB_ECH, 0.0f, 1.0f);
        DestinationFinale destination = new DestinationFinale();

        source.connecter(inserteur);
        inserteur.connecter(emetteur);
        emetteur.connecter(canal);
        canal.connecter(recepteur);
        recepteur.connecter(destination);

        source.emettre();

        return destination.getInformationRecue();
    }

    private String versChaine(Information<Boolean> info) {
        StringBuilder sb = new StringBuilder();
        for (Boolean b : info) {
            sb.append(b ? '1' : '0');
        }
        return sb.toString();
    }

    @Test
    void canalParfaitSansTrajetIndirectEstDecodeParfaitement() throws Exception {
        String message = "1101001011010010110100101101";
        Information<Boolean> recu = transmettreAvecSondage(message, 0, new int[0], new float[0], null);
        assertEquals(message, versChaine(recu));
    }

    @Test
    void unTrajetIndirectEstDetecteEtCompense() throws Exception {
        String message = "1101001011010010110100101101"; // 29 bits
        Information<Boolean> recu =
            transmettreAvecSondage(message, 1, new int[]{60}, new float[]{0.5f}, null);
        assertEquals(message, versChaine(recu));
    }

    @Test
    void deuxTrajetsIndirectsSimultanesSontCompenses() throws Exception {
        String message = "110100101101001011010010110100101101";
        Information<Boolean> recu =
            transmettreAvecSondage(message, 2, new int[]{90, 60}, new float[]{0.3f, -0.2f}, null);
        assertEquals(message, versChaine(recu));
    }

    @Test
    void cinqTrajetsIndirectsMaximumSontCompenses() throws Exception {
        String message = "11010010110100101101001011010010110100101101";
        Information<Boolean> recu = transmettreAvecSondage(
            message, 5,
            new int[]{60, 90, 120, 150, 180},
            new float[]{0.3f, -0.25f, 0.2f, -0.15f, 0.1f},
            null);
        assertEquals(message, versChaine(recu));
    }

    @Test
    void gardeFouStabiliteNeFaitPasPlanterLeRecepteur() {
        // |ar| = 0.98 >= SEUIL_STABILITE (0.95) : l'egalisation doit se
        // desactiver proprement plutot que de diverger ou lever une exception.
        String message = "1101001011010010110100101101";
        assertDoesNotThrow(() ->
            transmettreAvecSondage(message, 1, new int[]{60}, new float[]{0.98f}, null));
    }

    @Test
    void canalBruiteAvecUnTrajetGardeUnTebRaisonnable() throws Exception {
        // Message aleatoire suffisamment long pour moyenner le TEB
        int nbBits = 2000;
        SourceAleatoire source = new SourceAleatoire(nbBits, 7);
        InsertionEntete inserteur = new InsertionEntete();
        Emetteur emetteur = new Emetteur(FORME_ONDE, NB_ECH, 0.0f, 1.0f);
        TransmetteurAnalogiqueTrajetsMultiples canal = new TransmetteurAnalogiqueTrajetsMultiples(
            10.0f, NB_ECH, 1, new int[]{90}, new float[]{0.4f}, 7);
        RecepteurSonde recepteur = new RecepteurSonde(FORME_ONDE, NB_ECH, 0.0f, 1.0f);
        DestinationFinale destination = new DestinationFinale();

        source.connecter(inserteur);
        inserteur.connecter(emetteur);
        emetteur.connecter(canal);
        canal.connecter(recepteur);
        recepteur.connecter(destination);
        source.emettre();

        Information<Boolean> emis = source.getInformationEmise();
        Information<Boolean> recu = destination.getInformationRecue();
        int nbCommun = Math.min(emis.nbElements(), recu.nbElements());
        int erreurs = 0;
        for (int i = 0; i < nbCommun; i++) {
            if (!emis.iemeElement(i).equals(recu.iemeElement(i))) {
                erreurs++;
            }
        }
        float teb = (float) erreurs / nbCommun;

        // A Eb/N0 = 10 dB avec sondage/egalisation, le TEB doit rester
        // tres inferieur a une transmission non egalisee (~0.08, cf. tests
        // manuels) : seuil large pour ne pas etre fragile au bruit genere.
        assertTrue(teb < 0.05f, "TEB trop eleve avec sondage sur canal bruite : " + teb);
    }

    @Test
    void decalagesEtAmplitudesEstimesSontExposesApresReception() throws Exception {
        RecepteurSonde recepteur = new RecepteurSonde(FORME_ONDE, NB_ECH, 0.0f, 1.0f);
        SourceFixe source = new SourceFixe("1101001011010010110100101101");
        InsertionEntete inserteur = new InsertionEntete();
        Emetteur emetteur = new Emetteur(FORME_ONDE, NB_ECH, 0.0f, 1.0f);
        TransmetteurAnalogiqueTrajetsMultiples canal =
            new TransmetteurAnalogiqueTrajetsMultiples(null, NB_ECH, 1, new int[]{60}, new float[]{0.5f});

        source.connecter(inserteur);
        inserteur.connecter(emetteur);
        emetteur.connecter(canal);
        canal.connecter(recepteur);
        recepteur.connecter(new DestinationFinale());
        source.emettre();

        assertEquals(1, recepteur.getDecalagesEstimes().length);
        assertEquals(60, recepteur.getDecalagesEstimes()[0]);
        assertEquals(0.5f, recepteur.getAmplitudesEstimees()[0], 0.05f);
    }
}
