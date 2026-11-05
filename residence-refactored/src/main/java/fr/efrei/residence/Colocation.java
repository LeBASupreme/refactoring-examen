package fr.efrei.residence;

public class Colocation extends Logement {

    public static final int PLACES_MINIMUM = 2;

    private int places;

    public Colocation(String numero, double surface, int etage, boolean meuble, int places) {
        super(numero, TypeLogement.COLOCATION, surface, etage, meuble);
        this.places = places;
    }

    @Override
    public int getNombrePlaces() {
        return places;
    }

    @Override
    public void apresSortie() {
        System.out.println("[ENTRETIEN] Nettoyage des parties communes de la colocation " + numero);
    }
}