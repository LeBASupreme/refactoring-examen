package fr.efrei.residence;

import java.util.List;

/**
 * Ce que tout logement de la résidence sait faire, quel que soit son type.
 */
public interface ILogement {

    String getNumero();

    TypeLogement getType();

    double getSurface();

    int getEtage();

    boolean isMeuble();

    int getNombrePlaces();

    List<Etudiant> getOccupants();

    boolean estLibre();

    boolean estAccessiblePMR();

    void loger(Etudiant e);

    void liberer(Etudiant e);
}