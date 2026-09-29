package visualisations;

import java.awt.*;
import java.awt.geom.*;

/**
 * Vue pour l'affichage d'un diagramme de l'oeil : plusieurs tranches
 * temporelles ("traces") d'un signal analogique, chacune large de deux
 * périodes bit, sont superposées sur les mêmes axes (pas d'effacement
 * entre deux traces). L'ouverture visuelle de l'"oeil" ainsi formé
 * dégradée par le bruit et/ou les trajets multiples, ce qui la rend
 * plus étroite et plus floue.
 *
 * Construction très proche de {@link VueCourbe} (même style d'axes et
 * d'échelle), adaptée pour tracer plusieurs traces superposées au lieu
 * d'une seule courbe continue.
 *
 * @author Damien
 */
public class VueOeil extends Vue {

    private static final long serialVersionUID = 1L;

    /** Chaque trace correspond à une fenêtre temporelle (2 bits) du signal reçu */
    private Point2D.Float[][] traces;
    private float yMax = 0;
    private float yMin = 0;

    /**
     * Construit la vue et affiche immédiatement le diagramme de l'oeil.
     *
     * @param valeurs les traces à superposer, toutes de même longueur
     * @param nom le nom de la fenêtre d'affichage
     */
    public VueOeil(float[][] valeurs, String nom) {
        super(nom);

        int xPosition = Vue.getXPosition();
        int yPosition = Vue.getYPosition();
        setLocation(xPosition, yPosition);

        construireTraces(valeurs);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        int largeurTrace = (valeurs.length > 0 && valeurs[0].length > 0) ? valeurs[0].length : 30;
        int largeur = (largeurTrace * 10) + 10;
        if (largeur > 1000) {
            largeur = 1000;
        }
        setSize(largeur, 200);
        setVisible(true);
        repaint();
    }

    /**
     * Remplace les traces affichées et redessine la vue.
     *
     * @param valeurs les nouvelles traces à superposer
     */
    public void changer(float[][] valeurs) {
        construireTraces(valeurs);
        paint();
    }

    private void construireTraces(float[][] valeurs) {
        int nbTraces = valeurs.length;
        this.traces = new Point2D.Float[nbTraces][];
        yMax = 0;
        yMin = 0;

        for (int t = 0; t < nbTraces; t++) {
            float[] trace = valeurs[t];
            this.traces[t] = new Point2D.Float[trace.length];
            for (int i = 0; i < trace.length; i++) {
                if (trace[i] > yMax) {
                    yMax = trace[i];
                }
                if (trace[i] < yMin) {
                    yMin = trace[i];
                }
                this.traces[t][i] = new Point2D.Float(i, trace[i]);
            }
        }
    }

    public void paint() {
        paint(getGraphics());
    }

    public void paint(Graphics g) {
        if (g == null) {
            return;
        }
        // effacement total
        g.setColor(Color.white);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.black);

        int x0Axe = 10;
        float deltaX = getContentPane().getWidth() - (2 * x0Axe);

        int y0Axe = 10;
        float deltaY = getContentPane().getHeight() - (2 * y0Axe);

        if ((yMax > 0) && (yMin <= 0)) {
            y0Axe += (int) (deltaY * (yMax / (yMax - yMin)));
        } else if ((yMax > 0) && (yMin > 0)) {
            y0Axe += deltaY;
        } else if (yMax <= 0) {
            y0Axe += 0;
        }
        getContentPane().getGraphics().drawLine(x0Axe, y0Axe, x0Axe + (int) deltaX + x0Axe, y0Axe);

        int longueurTrace = (traces.length > 0) ? traces[0].length - 1 : 1;
        float dx = (longueurTrace > 0) ? deltaX / (float) longueurTrace : 1.0f;
        float dy = 0.0f;
        if ((yMax >= 0) && (yMin <= 0)) {
            dy = deltaY / (yMax - yMin);
        } else if (yMin > 0) {
            dy = deltaY / yMax;
        } else if (yMax < 0) {
            dy = -(deltaY / yMin);
        }

        // Chaque trace est dessinee avec la meme echelle, superposee aux
        // autres (aucun effacement entre deux traces) : c'est cette
        // superposition qui forme le diagramme de l'oeil.
        for (Point2D.Float[] trace : traces) {
            for (int i = 1; i < trace.length; i++) {
                int x1 = (int) (trace[i - 1].getX() * dx);
                int x2 = (int) (trace[i].getX() * dx);
                int y1 = (int) (trace[i - 1].getY() * dy);
                int y2 = (int) (trace[i].getY() * dy);
                getContentPane().getGraphics().drawLine(x0Axe + x1, y0Axe - y1, x0Axe + x2, y0Axe - y2);
            }
        }
    }
}
