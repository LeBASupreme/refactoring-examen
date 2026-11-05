package fr.efrei.residence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Logement implements ILogement {

    public static final int ETAGE_REZ_DE_CHAUSSEE = 0;

    protected String numero;
    protected TypeLogement type;
    protected double surface;
    protected int etage;
    protected boolean meuble;
    private final List<Etudiant> occupants = new ArrayList<Etudiant>();

    public Logement(String numero, TypeLogement type, double surface, int etage, boolean meuble) {
        this.numero = numero;
        this.type = type;
        this.surface = surface;
        this.etage = etage;
        this.meuble = meuble;
    }

    public String getNumero() { return numero; }
    public TypeLogement getType() { return type; }
    public double getSurface() { return surface; }
    public int getEtage() { return etage; }
    public boolean isMeuble() { return meuble; }

    public int getNombrePlaces() {
        return 1;
    }

    public List<Etudiant> getOccupants() {
        return Collections.unmodifiableList(occupants);
    }

    public boolean estLibre() {
        return occupants.size() < getNombrePlaces();
    }

    public boolean estAccessiblePMR() {
        // seul le bâtiment A a un ascenseur
        return etage == ETAGE_REZ_DE_CHAUSSEE || numero.startsWith("A");
    }

    public void loger(Etudiant e) {
        if (!estLibre()) {
            throw new IllegalStateException("Logement " + numero + " complet");
        }
        occupants.add(e);
    }

    public void liberer(Etudiant e) {
        occupants.remove(e);
    }

    public double getSurfaceBalcon() {
        return 0;
    }

    public void apresSortie() {
    }
}