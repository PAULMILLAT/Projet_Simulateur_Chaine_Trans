package visualisations;

import information.Information;

/**
 * Sonde affichant un diagramme de l'oeil : le signal reçu est découpé en
 * fenêtres temporelles de 2*nbEch échantillons (deux périodes bit),
 * glissées d'une période bit à chaque fois, et toutes superposées sur la
 * même fenêtre graphique ({@link VueOeil}).
 *
 * À utiliser typiquement connectée en sortie du transmetteur analogique
 * (avant démodulation par le récepteur), pour visualiser l'effet du bruit
 * et/ou des trajets multiples sur la marge de décision.
 *
 * @author Damien
 */
public class SondeOeil extends Sonde<Float> {

    /** Nombre d'échantillons par bit */
    private int nbEch;

    /** Taille (en échantillons) de chaque tranche affichée : 2 périodes bit */
    private int tailleFenetre;

    /**
     * Construit une sonde "diagramme de l'oeil".
     *
     * @param nom le nom de la fenêtre d'affichage
     * @param nbEch le nombre d'échantillons par bit du signal observé
     */
    public SondeOeil(String nom, int nbEch) {
        super(nom);
        this.nbEch = nbEch;
        this.tailleFenetre = 2 * nbEch;
    }

    /**
     * Reçoit le signal analogique, le découpe en fenêtres de deux
     * périodes bit glissées d'une période bit, et les affiche superposées.
     *
     * @param information le signal analogique reçu
     */
    @Override
    public void recevoir(Information<Float> information) {
        informationRecue = information;
        int nbElements = information.nbElements();

        float[] echantillons = new float[nbElements];
        int idx = 0;
        for (float f : information) {
            echantillons[idx++] = f;
        }

        if (nbElements < tailleFenetre) {
            // signal trop court pour former au moins une fenetre complete
            return;
        }
        int nbFenetres = (nbElements - tailleFenetre) / nbEch + 1;

        float[][] traces = new float[nbFenetres][tailleFenetre];
        for (int f = 0; f < nbFenetres; f++) {
            int debut = f * nbEch;
            System.arraycopy(echantillons, debut, traces[f], 0, tailleFenetre);
        }

        new VueOeil(traces, nom);
    }
}
