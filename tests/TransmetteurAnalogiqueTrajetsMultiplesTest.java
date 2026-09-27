package transmetteurs;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import information.Information;

/**
 * Tests unitaires pour {@link TransmetteurAnalogiqueTrajetsMultiples}.
 *
 * Couvre : validation des arguments du constructeur, superposition
 * correcte des trajets indirects (décalage + amplitude relative),
 * longueur du signal composite, et transmission non bruitée par défaut.
 */
class TransmetteurAnalogiqueTrajetsMultiplesTest {

    private static final int NB_ECH = 30;

    /** Construit une Information<Float> à partir d'un tableau de floats. */
    private Information<Float> versInformation(float[] valeurs) {
        Information<Float> info = new Information<Float>();
        for (float v : valeurs) {
            info.add(v);
        }
        return info;
    }

    /** Convertit une Information<Float> en tableau pour les assertions. */
    private float[] versTableau(Information<Float> info) {
        float[] resultat = new float[info.nbElements()];
        int i = 0;
        for (Float f : info) {
            resultat[i++] = f;
        }
        return resultat;
    }

    @Test
    void constructeurRejettePlusDeCinqTrajets() {
        int[] decalages = {10, 20, 30, 40, 50, 60};
        float[] amplitudes = {0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f};
        assertThrows(IllegalArgumentException.class, () ->
            new TransmetteurAnalogiqueTrajetsMultiples(null, NB_ECH, 6, decalages, amplitudes));
    }

    @Test
    void constructeurAccepteExactementCinqTrajets() {
        int[] decalages = {10, 20, 30, 40, 50};
        float[] amplitudes = {0.1f, 0.1f, 0.1f, 0.1f, 0.1f};
        assertDoesNotThrow(() ->
            new TransmetteurAnalogiqueTrajetsMultiples(null, NB_ECH, 5, decalages, amplitudes));
    }

    @Test
    void constructeurRejetteTableauxIncoherentsAvecNbTrajets() {
        int[] decalages = {10, 20};
        float[] amplitudes = {0.1f}; // taille différente de decalages et de nbTrajets
        assertThrows(IllegalArgumentException.class, () ->
            new TransmetteurAnalogiqueTrajetsMultiples(null, NB_ECH, 2, decalages, amplitudes));
    }

    @Test
    void sansTrajetIndirectLeSignalEstInchangeEtNonBruite() throws Exception {
        TransmetteurAnalogiqueTrajetsMultiples canal =
            new TransmetteurAnalogiqueTrajetsMultiples(null, NB_ECH, 0, new int[0], new float[0]);

        float[] direct = {0.0f, 1.0f, 1.0f, 0.0f, 0.5f};
        canal.recevoir(versInformation(direct));

        assertFalse(canal.isBruite(), "sans -snrpb, le canal ne doit pas etre bruite");
        assertArrayEquals(direct, versTableau(canal.getInformationEmise()), 1e-6f);
    }

    @Test
    void unTrajetIndirectEstCorrectementSuperpose() throws Exception {
        // Un seul trajet indirect : decalage de 3 echantillons, amplitude relative 0.5
        TransmetteurAnalogiqueTrajetsMultiples canal =
            new TransmetteurAnalogiqueTrajetsMultiples(null, NB_ECH, 1, new int[]{3}, new float[]{0.5f});

        float[] direct = {1.0f, 0.0f, 1.0f, 1.0f, 0.0f};
        canal.recevoir(versInformation(direct));
        float[] composite = versTableau(canal.getInformationEmise());

        // longueur attendue : nbEchantillons + decalMax
        assertEquals(direct.length + 3, composite.length);

        // attendu[n] = direct[n] + 0.5*direct[n-3] (0 si hors bornes)
        float[] attendu = new float[direct.length + 3];
        for (int n = 0; n < direct.length; n++) {
            attendu[n] += direct[n];
        }
        for (int n = 0; n < direct.length; n++) {
            attendu[n + 3] += 0.5f * direct[n];
        }
        assertArrayEquals(attendu, composite, 1e-6f);
    }

    @Test
    void deuxTrajetsIndirectsSeSuperposentCorrectement() throws Exception {
        TransmetteurAnalogiqueTrajetsMultiples canal =
            new TransmetteurAnalogiqueTrajetsMultiples(
                null, NB_ECH, 2, new int[]{2, 4}, new float[]{0.5f, -0.25f});

        float[] direct = {1.0f, 1.0f, 1.0f, 1.0f};
        canal.recevoir(versInformation(direct));
        float[] composite = versTableau(canal.getInformationEmise());

        float[] attendu = new float[direct.length + 4];
        for (int n = 0; n < direct.length; n++) {
            attendu[n] += direct[n];
            attendu[n + 2] += 0.5f * direct[n];
            attendu[n + 4] += -0.25f * direct[n];
        }
        assertArrayEquals(attendu, composite, 1e-6f);
    }

    @Test
    void laMemeGraineDonneLeMemeBruit() throws Exception {
        float[] direct = new float[300];
        for (int i = 0; i < direct.length; i++) {
            direct[i] = (i % 2 == 0) ? 1.0f : 0.0f;
        }

        TransmetteurAnalogiqueTrajetsMultiples canal1 =
            new TransmetteurAnalogiqueTrajetsMultiples(10.0f, NB_ECH, 1, new int[]{30}, new float[]{0.3f}, 123);
        TransmetteurAnalogiqueTrajetsMultiples canal2 =
            new TransmetteurAnalogiqueTrajetsMultiples(10.0f, NB_ECH, 1, new int[]{30}, new float[]{0.3f}, 123);

        canal1.recevoir(versInformation(direct));
        canal2.recevoir(versInformation(direct));

        assertArrayEquals(
            versTableau(canal1.getInformationEmise()),
            versTableau(canal2.getInformationEmise()),
            1e-6f,
            "deux canaux construits avec la meme graine doivent produire le meme bruit");
    }

    @Test
    void informationVideNeLeveAucuneExceptionEtRestVide() throws Exception {
        TransmetteurAnalogiqueTrajetsMultiples canal =
            new TransmetteurAnalogiqueTrajetsMultiples(null, NB_ECH, 1, new int[]{5}, new float[]{0.5f});

        canal.recevoir(new Information<Float>());

        assertEquals(0, canal.getInformationEmise().nbElements());
    }

    @Test
    void informationNulleLeveInformationNonConformeException() {
        TransmetteurAnalogiqueTrajetsMultiples canal =
            new TransmetteurAnalogiqueTrajetsMultiples(null, NB_ECH, 0, new int[0], new float[0]);

        assertThrows(information.InformationNonConformeException.class, () -> canal.recevoir(null));
    }
}
