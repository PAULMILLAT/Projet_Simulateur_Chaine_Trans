import sources.SourceFixe;
import destinations.DestinationFinale;

public class Essai {
    public static void main(String[] args) throws Exception {
        Boolean[] message = {true, false, true, true, false};

        SourceFixe source = new SourceFixe(message);
        DestinationFinale destination = new DestinationFinale();

        source.connecter(destination);

        System.out.println("Avant emettre() : " + destination.getInformationRecue());
        source.emettre();
        System.out.println("Apres emettre() : " + destination.getInformationRecue());
    }
}