package fr.efrei.residence;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

public class CycleVieBailService {

    public static final int JOUR_ECHEANCE_LOYER = 5;

    private final BailRepository baux;
    private final NotificationService notificationService;

    public CycleVieBailService(BailRepository baux, NotificationService notificationService) {
        this.baux = Objects.requireNonNull(baux, "baux");
        this.notificationService = Objects.requireNonNull(notificationService, "notificationService");
    }

    public boolean entrerDansLesLieux(String idBail, LocalDate date) {
        Bail bail = baux.trouverParId(idBail);
        if (bail == null || bail.getStatut() != StatutBail.SIGNE) {
            System.out.println("Entrée impossible pour " + idBail);
            return false;
        }
        if (date.isBefore(bail.getDateSignature())) {
            System.out.println("Entrée impossible avant la signature du bail");
            return false;
        }
        bail.setDateEntree(date);
        bail.setStatut(StatutBail.ACTIF);
        baux.mettreAJour(bail);
        System.out.println("Entrée dans les lieux : " + idBail + " le " + Utils.date(date));
        notificationService.notifier(bail.getEtudiant(), "État des lieux d'entrée",
                "Votre état des lieux d'entrée du logement " + bail.getLogement().getNumero() + " est fait le "
                        + Utils.date(date) + ". Remise des clés effectuée. Pensez à souscrire une assurance habitation si ce n'est pas déjà fait.");
        return true;
    }

    public boolean payerLoyer(String idBail, String mois, LocalDate datePaiement, double montant) {
        Bail bail = baux.trouverParId(idBail);
        if (bail == null || !(bail.getStatut() == StatutBail.ACTIF || bail.getStatut() == StatutBail.PREAVIS)) {
            System.out.println("Paiement refusé pour " + idBail);
            return false;
        }
        LocalDate echeance = YearMonth.parse(mois).atDay(JOUR_ECHEANCE_LOYER);
        double penalite = 0;
        if (datePaiement.isAfter(echeance)) {
            PolitiqueProfil politique = PolitiqueProfil.pour(bail.getEtudiant().getProfil());
            penalite = Utils.arrondir(politique.penaliteSiRetard(bail.getLoyerChargesComprises()));
        }
        double du = Utils.arrondir(bail.getLoyerChargesComprises() + penalite);
        bail.paiements.add(new Paiement(mois, datePaiement, montant, penalite, du));
        bail.soldeDu = Utils.arrondir(bail.soldeDu + du - montant);
        baux.mettreAJour(bail);

        System.out.println("Paiement " + idBail + " " + mois + " : " + Utils.euros(montant) + " / " + Utils.euros(du)
                + (penalite > 0 ? " (dont pénalité " + Utils.euros(penalite) + ")" : ""));
        if (montant < du) {
            notificationService.notifier(bail.getEtudiant(), "Paiement incomplet " + mois,
                    "Nous avons reçu " + Utils.euros(montant) + " sur " + Utils.euros(du) + ". Reste dû : "
                            + Utils.euros(Utils.arrondir(du - montant)) + ".");
        } else {
            notificationService.notifier(bail.getEtudiant(), "Quittance " + mois,
                    "Merci, votre loyer de " + mois + " est réglé.");
        }
        return true;
    }

    public boolean donnerPreavis(String idBail, LocalDate date) {
        Bail bail = baux.trouverParId(idBail);
        if (bail == null || bail.getStatut() != StatutBail.ACTIF) {
            System.out.println("Préavis impossible pour " + idBail);
            return false;
        }
        int jours = PolitiqueProfil.pour(bail.getEtudiant().getProfil()).dureePreavisJours();
        bail.setDateFin(date.plusDays(jours));
        bail.setStatut(StatutBail.PREAVIS);
        baux.mettreAJour(bail);
        System.out.println("Préavis " + idBail + " : fin du bail le " + Utils.date(bail.getDateFin()));
        notificationService.notifier(bail.getEtudiant(), "Préavis de départ",
                "Votre préavis de " + jours + " jours est enregistré. Fin du bail le "
                        + Utils.date(bail.getDateFin()) + ".");
        return true;
    }

    public boolean sortir(String idBail, LocalDate date, double retenuesDegats) {
        Bail bail = baux.trouverParId(idBail);
        if (bail == null || bail.getStatut() != StatutBail.PREAVIS) {
            System.out.println("Sortie impossible pour " + idBail);
            return false;
        }
        if (date.isBefore(bail.getDateFin())) {
            System.out.println("Sortie impossible avant la fin du préavis (" + Utils.date(bail.getDateFin()) + ")");
            return false;
        }
        Logement logement = bail.getLogement();
        logement.liberer(bail.getEtudiant());
        logement.apresSortie();

        double impayes = bail.soldeDu > 0 ? bail.soldeDu : 0;
        double restitution = bail.getCaution() - retenuesDegats - impayes;
        double resteAPayer = 0;
        if (restitution < 0) {
            resteAPayer = -restitution;
            restitution = 0;
        }
        bail.retenues = Utils.arrondir(retenuesDegats);
        bail.cautionRestituee = Utils.arrondir(restitution);
        bail.soldeDu = Utils.arrondir(resteAPayer);
        bail.setStatut(StatutBail.TERMINE);
        baux.mettreAJour(bail);

        System.out.println("Sortie " + idBail + " le " + Utils.date(date) + " : caution " + Utils.euros(bail.getCaution())
                + ", retenues " + Utils.euros(bail.retenues) + ", impayés " + Utils.euros(impayes)
                + " -> restitué " + Utils.euros(bail.cautionRestituee));
        String message = "Votre dépôt de garantie : " + Utils.euros(bail.cautionRestituee)
                + " vous sera restitué sous 30 jours.";
        if (resteAPayer > 0) {
            message = message + " Il reste " + Utils.euros(bail.soldeDu) + " à régler.";
        }
        notificationService.notifier(bail.getEtudiant(), "Sortie du logement " + logement.getNumero(), message);
        return true;
    }

    public boolean annulerBail(String idBail) {
        Bail bail = baux.trouverParId(idBail);
        if (bail == null || bail.getStatut() != StatutBail.SIGNE) {
            System.out.println("Annulation impossible pour " + idBail);
            return false;
        }
        Logement logement = bail.getLogement();
        logement.liberer(bail.getEtudiant());
        bail.setStatut(StatutBail.ANNULE);
        baux.mettreAJour(bail);
        System.out.println("Bail " + idBail + " annulé, frais de dossier conservés : "
                + Utils.euros(bail.getFraisDossier()));
        notificationService.notifier(bail.getEtudiant(), "Annulation du bail " + idBail,
                "Votre bail est annulé. Les frais de dossier (" + Utils.euros(bail.getFraisDossier()) + ") restent acquis.");
        return true;
    }

    public void relancerImpayes() {
        System.out.println("--- Relance des impayés");
        for (Bail bail : baux.tous()) {
            if (bail.soldeDu <= 0) continue;
            if (bail.getStatut() == StatutBail.ANNULE) continue;
            notificationService.relancerImpayes(bail);
        }
    }
}
