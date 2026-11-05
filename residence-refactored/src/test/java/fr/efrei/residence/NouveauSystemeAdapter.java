package fr.efrei.residence;

import fr.efrei.residence.contrat.Canal;
import fr.efrei.residence.contrat.DemandeAttribution;
import fr.efrei.residence.contrat.DemandeDevis;
import fr.efrei.residence.contrat.DemandeInscription;
import fr.efrei.residence.contrat.DemandeLogement;
import fr.efrei.residence.contrat.EtatBail;
import fr.efrei.residence.contrat.EtatLogement;
import fr.efrei.residence.contrat.Notification;
import fr.efrei.residence.contrat.Option;
import fr.efrei.residence.contrat.StatutBail;
import fr.efrei.residence.contrat.SystemeResidence;
import fr.efrei.residence.contrat.TypeLogement;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

/**
 * Branche le code historique sur les tests de contrat.
 * <p>
 * Les conversions nécessaires pour brancher le contrat sur le code refactoré sont confinées ici.
 */
public class NouveauSystemeAdapter implements SystemeResidence {

    private GestionResidence gestion;
    private EtudiantRepository etudiants;
    private BailRepository baux;
    private LogementRepository logements;
    private ServiceNotification serviceNotification;
    private CalculateurLoyer calculateur;
    private QuittanceGenerateur quittanceGenerateur;
    private RapportGenerateur rapportGenerateur;

    @Override
    public void reinitialiser() {
        etudiants = new DepotEtudiantMemoire();
        baux = new DepotBailMemoire();
        logements = new DepotLogementMemoire();
        Residence.creerLogementsInitiaux().forEach(logements::sauvegarder);
        serviceNotification = new ServiceNotification(new EmailService(), new SmsService(), new CourrierService());
        calculateur = new CalculateurLoyer(new AssembleurLoyer());
        quittanceGenerateur = new QuittanceGenerateur();
        rapportGenerateur = new RapportGenerateur();
        gestion = new GestionResidence(etudiants, baux, logements, serviceNotification, new ValidateurInscription(),
                new ServiceAttribution(), new CycleVieBailService(baux, serviceNotification), new IdentifiantSequence(),
                calculateur);
    }

    @Override
    public Optional<String> inscrire(DemandeInscription d) {
        Etudiant e = gestion.inscrireEtudiant(d.nom(), d.prenom(), d.email(), d.telephone(), d.adresse(),
                ProfilEtudiant.valueOf(d.profil().name()), d.echelonBourse(), d.ressourcesMensuelles(), d.pmr(),
                CanalNotification.valueOf(d.canalPrefere().name()));
        return Optional.ofNullable(e).map(Etudiant::getId);
    }

    @Override
    public boolean ajouterLogement(DemandeLogement d) {
        int param = d.type() == TypeLogement.COLOCATION ? d.places() : d.surfaceBalcon();
        return gestion.ajouterLogement(fr.efrei.residence.TypeLogement.valueOf(d.type().name()), d.numero(), d.surface(), d.etage(), d.meuble(), param) != null;
    }

    @Override
    public Optional<EtatLogement> logement(String numero) {
        Logement l = logements.trouverParNumero(numero);
        if (l == null) {
            return Optional.empty();
        }
        return Optional.of(new EtatLogement(l.getNumero(), TypeLogement.valueOf(l.getType().name()), l.getNombrePlaces(), l.getOccupants().size(), l.estAccessiblePMR()));
    }

    @Override
    public OptionalDouble devisLoyer(DemandeDevis d) {
        Logement l = logements.trouverParNumero(d.numeroLogement());
        if (l == null) {
            return OptionalDouble.empty();
        }
        Set<Option> o = d.options();
        return OptionalDouble.of(calculateur.calculerLoyer(l, ProfilEtudiant.valueOf(d.profil().name()), d.echelonBourse(),
                convertirOptions(o)));
    }

    @Override
    public Optional<String> attribuer(DemandeAttribution d) {
        Etudiant e = etudiants.trouverParId(d.refEtudiant());
        if (e == null) {
            return Optional.empty();
        }
        Set<Option> o = d.options();
        Bail b = gestion.attribuerLogement(e, fr.efrei.residence.TypeLogement.valueOf(d.type().name()),
                convertirOptions(o), d.dateSignature());
        return Optional.ofNullable(b).map(Bail::getId);
    }

    @Override
    public Optional<EtatBail> consulter(String idBail) {
        Bail b = baux.trouverParId(idBail);
        if (b == null) {
            return Optional.empty();
        }
        return Optional.of(new EtatBail(b.getId(), b.getLogement().getNumero(), StatutBail.valueOf(b.getStatut().name()),
                b.getLoyerHorsCharges(), b.getCharges(), b.getLoyerChargesComprises(), b.getCaution(),
                b.getFraisDossier(), b.getDateFin(), b.soldeDu, b.cautionRestituee));
    }

    @Override
    public boolean entrer(String idBail, LocalDate date) {
        return gestion.entrerDansLesLieux(idBail, date);
    }

    @Override
    public boolean payer(String idBail, YearMonth mois, LocalDate date, double montant) {
        return gestion.payerLoyer(idBail, mois.toString(), date, montant);
    }

    @Override
    public boolean donnerPreavis(String idBail, LocalDate date) {
        return gestion.donnerPreavis(idBail, date);
    }

    @Override
    public boolean sortir(String idBail, LocalDate date, double retenuesDegats) {
        return gestion.sortir(idBail, date, retenuesDegats);
    }

    @Override
    public boolean annuler(String idBail) {
        return gestion.annulerBail(idBail);
    }

    @Override
    public Optional<String> quittance(String idBail, YearMonth mois) {
        return Optional.ofNullable(baux.trouverParId(idBail))
                .map(b -> quittanceGenerateur.generer(b, mois.toString()));
    }

    @Override
    public void relancerImpayes() {
        gestion.relancerImpayes();
    }

    @Override
    public String rapport() {
        return rapportGenerateur.generer(logements.tous(), baux.tous(),
                serviceNotification.getNombreSmsEnvoyes(), serviceNotification.getAffranchissement());
    }

    @Override
    public List<Notification> notifications() {
        List<Notification> notifications = new ArrayList<>();
        for (String[] email : serviceNotification.getEmailJournal()) {
            notifications.add(new Notification(Canal.EMAIL, email[0], email[1]));
        }
        for (String[] sms : serviceNotification.getSmsJournal()) {
            notifications.add(new Notification(Canal.SMS, sms[0], sms[1]));
        }
        for (String[] courrier : serviceNotification.getCourrierJournal()) {
            notifications.add(new Notification(Canal.COURRIER, courrier[0], courrier[1]));
        }
        return notifications;
    }

    private static Set<OptionLoyer> convertirOptions(Set<Option> options) {
        EnumSet<OptionLoyer> converties = EnumSet.noneOf(OptionLoyer.class);
        for (Option option : options) {
            converties.add(OptionLoyer.valueOf(option.name()));
        }
        return converties;
    }
}
