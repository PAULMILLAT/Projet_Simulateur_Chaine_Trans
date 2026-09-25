package simulateur;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Locale;

/**
 * Campagne de mesure du TEB en fonction du SNR par bit (Eb/N0).
 * <p>
 * Pour chaque forme d'onde et chaque valeur de Eb/N0, la classe lance le
 * {@link Simulateur} sur un nombre fixe de blocs de bits (1 million de bits
 * par point) et cumule les erreurs. Les résultats sont
 * écrits dans un fichier CSV, qui sert ensuite à tracer la courbe
 * (script Python ou tableur).
 * <p>
 * Lancement : java -cp bin simulateur.CourbeTEB
 *
 * @author Paul
 * @author Yann
 * @author Damien
 * @author Enzo
 */
public class CourbeTEB {

    // ------------------------------------------------------------------
    // Paramètres de la campagne
    // ------------------------------------------------------------------

    /** Nom du fichier CSV produit. */
    private static final String FICHIER_CSV = "teb.csv";

    /** Première valeur de Eb/N0 mesurée (dB). */
    private static final float SNR_MIN = 0f;

    /** Dernière valeur de Eb/N0 mesurée (dB). */
    private static final float SNR_MAX = 14f;

    /** Pas entre deux valeurs de Eb/N0 (dB). */
    private static final float SNR_PAS = 1f;

    /** Nombre de bits envoyés à chaque appel du simulateur (max 999999 à cause de -mess). */
    private static final int TAILLE_BLOC = 100000;

    /** Nombre de blocs envoyés par point : 10 x 100 000 = 1 million de bits par point. */
    private static final int NB_BLOCS = 10;

    /** Nombre d'échantillons par bit. */
    private static final int NB_ECH = 30;

    /** Constructeur privé : classe utilitaire, on n'en crée pas d'instance. */
    private CourbeTEB() {
    }

    // ------------------------------------------------------------------
    // Étape 1 : construire la "ligne de commande" du simulateur
    // ------------------------------------------------------------------

    /**
     * Construit le tableau d'arguments à passer au constructeur du Simulateur,
     * exactement comme si on tapait la commande dans le terminal.
     *
     * @param forme   forme d'onde ("NRZ", "NRZT" ou "RZ")
     * @param amplMin amplitude minimale (texte, ex : "-1")
     * @param amplMax amplitude maximale (texte, ex : "1")
     * @param snr     Eb/N0 en dB
     * @return le tableau d'arguments
     */
    private static String[] construireArguments(String forme, String amplMin,
                                                 String amplMax, float snr) {
        return new String[] {
            "-mess", String.valueOf(TAILLE_BLOC),
            "-form", forme,
            "-nbEch", String.valueOf(NB_ECH),
            "-ampl", amplMin, amplMax,
            "-snrpb", String.valueOf(snr)
        };
    }

    // ------------------------------------------------------------------
    // Étape 2 : mesurer un point de la courbe
    // ------------------------------------------------------------------

    /**
     * Mesure le TEB pour une forme d'onde et une valeur de Eb/N0, en envoyant
     * toujours le même nombre de bits : NB_BLOCS blocs de TAILLE_BLOC bits.
     *
     * @param forme   forme d'onde
     * @param amplMin amplitude minimale
     * @param amplMax amplitude maximale
     * @param snr     Eb/N0 en dB
     * @return un tableau {nombre de bits envoyés, nombre d'erreurs}
     * @throws Exception si le simulateur rencontre une erreur
     */
    private static long[] mesurerPoint(String forme, String amplMin,
                                       String amplMax, float snr) throws Exception {
        long bits = 0;
        long erreurs = 0;

        for (int b = 0; b < NB_BLOCS; b++) {
            // Un appel du simulateur = un bloc de TAILLE_BLOC bits
            Simulateur sim = new Simulateur(construireArguments(forme, amplMin, amplMax, snr));
            sim.execute();

            // TEB du bloc = erreurs du bloc / TAILLE_BLOC, donc erreurs du bloc = TEB * TAILLE_BLOC.
            // Math.round corrige les arrondis du float (ex : 5.9999995 -> 6).
            float tebBloc = sim.calculTauxErreurBinaire();
            erreurs += Math.round(tebBloc * TAILLE_BLOC);
            bits += TAILLE_BLOC;
        }

        return new long[] {bits, erreurs};
    }

    // ------------------------------------------------------------------
    // Étape 3 : écrire une ligne de résultat
    // ------------------------------------------------------------------

    /**
     * Mesure un point, puis écrit le résultat dans le CSV et dans le terminal.
     *
     * @param csv     le fichier CSV ouvert en écriture
     * @param forme   forme d'onde
     * @param amplMin amplitude minimale
     * @param amplMax amplitude maximale
     * @param snr     Eb/N0 en dB
     * @throws Exception si le simulateur rencontre une erreur
     */
    private static void mesurerEtEcrire(PrintWriter csv, String forme, String amplMin,
                                        String amplMax, float snr) throws Exception {
        long[] resultat = mesurerPoint(forme, amplMin, amplMax, snr);
        long bits = resultat[0];
        long erreurs = resultat[1];

        // Le cast en double évite la division entière entre deux long (qui donnerait 0)
        double teb = (double) erreurs / bits;

        // Locale.ROOT : nombres avec un point (0.0061), lisibles par Python.
        // Pour un Excel en français, remplacer par Locale.FRANCE (0,0061).
        String ligne = String.format(Locale.ROOT, "%s;%s;%s;%.1f;%d;%d;%.4e",
                forme, amplMin, amplMax, snr, bits, erreurs, teb);
        csv.println(ligne);
        csv.flush(); // écrit tout de suite sur le disque, même si on coupe en cours de route
        System.out.println(ligne);
    }

    // ------------------------------------------------------------------
    // Étape 4 : la campagne, une boucle par forme d'onde
    // ------------------------------------------------------------------

    /**
     * Point d'entrée : lance toute la campagne et écrit le fichier CSV.
     *
     * @param args non utilisés
     * @throws Exception si une simulation ou l'écriture du fichier échoue
     */
    public static void main(String[] args) throws Exception {
        try (PrintWriter csv = new PrintWriter(new FileWriter(FICHIER_CSV))) {

            // En-tête du CSV : une colonne par information, séparées par des ';'
            csv.println("forme;amplMin;amplMax;snrpb_dB;nbBits;nbErreurs;TEB");

            // ---- Boucle 1 : NRZ, amplitudes -1 et 1 ----
            System.out.println("=== NRZ [-1, 1] ===");
            for (float snr = SNR_MIN; snr <= SNR_MAX + 1e-6f; snr += SNR_PAS) {
                mesurerEtEcrire(csv, "NRZ", "-1", "1", snr);
            }

            // ---- Boucle 2 : NRZT, amplitudes -1 et 1 ----
            System.out.println("=== NRZT [-1, 1] ===");
            for (float snr = SNR_MIN; snr <= SNR_MAX + 1e-6f; snr += SNR_PAS) {
                mesurerEtEcrire(csv, "NRZT", "-1", "1", snr);
            }

            // ---- Boucle 3 : RZ, amplitudes 0 et 1 (le RZ impose amplMin = 0) ----
            System.out.println("=== RZ [0, 1] ===");
            for (float snr = SNR_MIN; snr <= SNR_MAX + 1e-6f; snr += SNR_PAS) {
                mesurerEtEcrire(csv, "RZ", "0", "1", snr);
            }
        }
        System.out.println("Resultats ecrits dans " + FICHIER_CSV);
    }
}