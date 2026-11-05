package fr.efrei.residence;

import java.util.List;

public class RapportGenerateur {

    public void afficher(List<Logement> logements, List<Bail> baux, int nombreSms, double affranchissement) {
        System.out.println();
        System.out.println(generer(logements, baux, nombreSms, affranchissement));
    }

    public String generer(List<Logement> logements, List<Bail> baux, int nombreSms, double affranchissement) {
        String r = "";
        r += "=============== RAPPORT " + Residence.NOM + " ===============" + "\n";
        int nbStudios = 0, nbStudiosOccupes = 0;
        int nbT1 = 0, nbT1Occupes = 0;
        int nbT2 = 0, nbT2Occupes = 0;
        int nbPlacesColoc = 0, nbPlacesColocOccupees = 0;
        double surfaceBalcons = 0;
        for (Logement l : logements) {
            if (l.getType() == TypeLogement.STUDIO) {
                nbStudios++;
                if (!l.estLibre()) nbStudiosOccupes++;
            } else if (l.getType() == TypeLogement.T1) {
                nbT1++;
                if (!l.estLibre()) nbT1Occupes++;
            } else if (l.getType() == TypeLogement.T2) {
                nbT2++;
                if (!l.estLibre()) nbT2Occupes++;
            } else if (l.getType() == TypeLogement.COLOCATION) {
                nbPlacesColoc += l.getNombrePlaces();
                nbPlacesColocOccupees += l.getOccupants().size();
            }
            surfaceBalcons += l.getSurfaceBalcon();
        }
        r += "Studios      : " + nbStudiosOccupes + "/" + nbStudios + "\n";
        r += "T1           : " + nbT1Occupes + "/" + nbT1 + "\n";
        r += "T2           : " + nbT2Occupes + "/" + nbT2 + "\n";
        r += "Colocation   : " + nbPlacesColocOccupees + "/" + nbPlacesColoc + " places" + "\n";
        int total = nbStudios + nbT1 + nbT2 + nbPlacesColoc;
        int occupes = nbStudiosOccupes + nbT1Occupes + nbT2Occupes + nbPlacesColocOccupees;
        r += "Occupation   : " + Math.round(occupes * 100.0 / total) + " %" + "\n";
        r += "Balcons      : " + surfaceBalcons + " m²" + "\n";

        double loyersMensuels = 0;
        double impayes = 0;
        double cautions = 0;
        for (Bail b : baux) {
            if (b.getStatut() == StatutBail.ACTIF || b.getStatut() == StatutBail.PREAVIS) {
                loyersMensuels += b.getLoyerChargesComprises();
                cautions += b.getCaution();
            }
            if (b.soldeDu > 0) impayes += b.soldeDu;
        }
        r += "Loyers CC    : " + Utils.euros(Utils.arrondir(loyersMensuels)) + " / mois" + "\n";
        r += "Cautions     : " + Utils.euros(Utils.arrondir(cautions)) + "\n";
        r += "Impayés      : " + Utils.euros(Utils.arrondir(impayes)) + "\n";
        r += "SMS envoyés  : " + nombreSms + "\n";
        r += "Timbres      : " + Utils.euros(Utils.arrondir(affranchissement)) + "\n";
        r += "============================================================";
        return r;
    }
}
