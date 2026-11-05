package fr.efrei.residence;

import java.util.List;
import java.util.Set;

public class ServiceAttribution {

    public ResultatAttribution attribuer(Etudiant etudiant, TypeLogement typeSouhaite, Set<OptionLoyer> options,
                                         List<Logement> logements, CalculateurLoyer calculateur) {
        System.out.println("--- Attribution d'un " + typeSouhaite + " à " + etudiant.getNomComplet());
        for (Logement logement : logements) {
            if (logement.getType() != typeSouhaite) continue;
            if (!logement.estLibre()) continue;
            if (etudiant.isPmr() && !logement.estAccessiblePMR()) continue;

            double loyer = calculateur.calculerLoyer(logement, etudiant.getProfil(), etudiant.getEchelonBourse(), options);
            double charges = calculateur.calculerCharges(logement);
            double total = loyer + charges;
            boolean solvable = PolitiqueProfil.pour(etudiant.getProfil())
                    .estSolvable(etudiant.getRessourcesMensuelles(), total);
            if (!solvable) {
                System.out.println("  " + logement.getNumero() + " : ressources insuffisantes ("
                        + Utils.euros(total) + "/mois)");
                continue;
            }

            logement.loger(etudiant);
            return new ResultatAttribution(logement, loyer, charges);
        }

        System.out.println("  Aucun logement disponible pour " + etudiant.getNomComplet());
        return null;
    }
}
