package transmetteurs;

import information.Information;
import information.InformationNonConformeException;


import java.util.Random;
public class TransmetteurBruite <T> extends Transmetteur<T, T> {

     /** rapport signal sur bruit par bit Eb/N0, exprime en dB */
    private final float snrpbDb;

    /** nombre d'echantillons par bit, necessaire au calcul de la variance */
    private final int nbEch;

    /** generateur pseudo-aleatoire utilise pour tirer les echantillons de bruit */
    private final Random generateurBruit;

    /** le bruit seul, conserve pour pouvoir le visualiser avec une sonde */
    private Information<Float> informationBruit;

        public TransmetteurBruite(float snrpbDb, int nbEch) {
        super();
        this.snrpbDb = snrpbDb;
        this.nbEch = nbEch;
        this.generateurBruit = new Random();
        this.informationBruit = null;
    }

    public TransmetteurBruite(float snrpbDb, int nbEch, int seed) {
        super();
        this.snrpbDb = snrpbDb;
        this.nbEch = nbEch;
        this.generateurBruit = new Random(seed);
        this.informationBruit = null;
    }

     @Override 
     public void recevoir(Information<T> information) throws InformationNonConformeException {
        if (information == null) {
            throw new InformationNonConformeException("L'information recue est nulle.");
        }
        
        
    }
    @Override 
     public void emettre() throws InformationNonConformeException {


     }

}
