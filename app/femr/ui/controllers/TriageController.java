package femr.ui.controllers;

import com.google.inject.Inject;
import com.google.inject.Provider;
import femr.business.dtos.CurrentUser;
import femr.business.dtos.ServiceResponse;
import femr.business.services.ISearchService;
import femr.business.services.ISessionService;
import femr.business.services.ITriageService;
import femr.common.models.IPatient;
import femr.common.models.IPatientEncounter;
import femr.common.models.IPatientEncounterVital;
import femr.common.models.IVital;
import femr.ui.models.triage.CreateViewModel;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TriageController extends Controller {
    private final Form<CreateViewModel> createViewModelForm = Form.form(CreateViewModel.class);
    private ITriageService triageService;
    private ISessionService sessionService;
    private Provider<IPatient> patientProvider;
    private ISearchService searchService;
    private Provider<IPatientEncounter> patientEncounterProvider;
    private Provider<IPatientEncounterVital> patientEncounterVitalProvider;

    @Inject
    public TriageController(ITriageService triageService,
                            ISessionService sessionService,
                            ISearchService searchService,
                            Provider<IPatient> patientProvider,
                            Provider<IPatientEncounter> patientEncounterProvider,
                            Provider<IPatientEncounterVital> patientEncounterVitalProvider) {
        this.triageService = triageService;
        this.sessionService = sessionService;
        this.searchService = searchService;
        this.patientProvider = patientProvider;
        this.patientEncounterProvider = patientEncounterProvider;
        this.patientEncounterVitalProvider = patientEncounterVitalProvider;
    }

    public Result createGet() {
        List<? extends IVital> vitalNames = triageService.findAllVitals();

        CurrentUser currentUser = sessionService.getCurrentUserSession();

        return ok(femr.ui.views.html.triage.create.render(currentUser, vitalNames));
    }

    public Result createPost() {
        CreateViewModel viewModel = createViewModelForm.bindFromRequest().get();

        CurrentUser currentUser = sessionService.getCurrentUserSession();

        IPatient patient = populatePatient(viewModel, currentUser);
        ServiceResponse<IPatient> patientServiceResponse = triageService.createPatient(patient);

        IPatientEncounter patientEncounter = populatePatientEncounter(viewModel, patientServiceResponse, currentUser);
        ServiceResponse<IPatientEncounter> patientEncounterServiceResponse =
                triageService.createPatientEncounter(patientEncounter);

        List<IPatientEncounterVital> patientEncounterVitals =
                populatePatientEncounterVitals(viewModel, patientEncounterServiceResponse, currentUser);
        for (int i = 0; i < patientEncounterVitals.size(); i++) {
            triageService.createPatientEncounterVital(patientEncounterVitals.get(i));
        }

        return redirect("/show/" + patientServiceResponse.getResponseObject().getId());
    }

    /*
    Used when user is creating an encounter for an existing patient.
     */
    public Result createNewEncounterGet(int id) {
        List<? extends IVital> vitalNames = triageService.findAllVitals();

        CurrentUser currentUser = sessionService.getCurrentUserSession();

        ServiceResponse<IPatient> patientServiceResponse = searchService.findPatientById(id);
        IPatient patient = patientServiceResponse.getResponseObject();

        return ok(femr.ui.views.html.triage.createEncounter.render(currentUser, vitalNames, patient));
    }

    public Result createNewEncounterPost(int id) {
        CreateViewModel viewModel = createViewModelForm.bindFromRequest().get();

        CurrentUser currentUser = sessionService.getCurrentUserSession();

        ServiceResponse<IPatient> patientServiceResponse = searchService.findPatientById(id);

        IPatientEncounter patientEncounter = populatePatientEncounter(viewModel, patientServiceResponse, currentUser);
        ServiceResponse<IPatientEncounter> patientEncounterServiceResponse =
                triageService.createPatientEncounter(patientEncounter);

        List<IPatientEncounterVital> patientEncounterVitals =
                populatePatientEncounterVitals(viewModel, patientEncounterServiceResponse, currentUser);
        for (int i = 0; i < patientEncounterVitals.size(); i++) {
            triageService.createPatientEncounterVital(patientEncounterVitals.get(i));
        }

        return redirect("/show/" + patientServiceResponse.getResponseObject().getId());
    }

    private IPatient populatePatient(CreateViewModel viewModel, CurrentUser currentUser) {
        IPatient patient = patientProvider.get();
        patient.setUserId(currentUser.getId());
        patient.setFirstName(viewModel.getFirstName());
        patient.setLastName(viewModel.getLastName());
        patient.setAge(viewModel.getAge());
        patient.setSex(viewModel.getSex());
        patient.setAddress(viewModel.getAddress());
        patient.setCity(viewModel.getCity());
        return patient;
    }

    private IPatientEncounter populatePatientEncounter(CreateViewModel viewModel,
                                                       ServiceResponse<IPatient> patientServiceResponse,
                                                       CurrentUser currentUser) {
        IPatientEncounter patientEncounter = patientEncounterProvider.get();
        patientEncounter.setPatientId(patientServiceResponse.getResponseObject().getId());
        patientEncounter.setUserId(currentUser.getId());
        patientEncounter.setDateOfVisit(triageService.getCurrentDateTime());
        patientEncounter.setChiefComplaint(viewModel.getChiefComplaint());

        return patientEncounter;
    }

    private List<IPatientEncounterVital> populatePatientEncounterVitals(CreateViewModel viewModel,
                                                                        ServiceResponse<IPatientEncounter> patientEncounterServiceResponse,
                                                                        CurrentUser currentUser) {

        List<IPatientEncounterVital> patientEncounterVitals = new ArrayList<>();
        IPatientEncounterVital[] patientEncounterVital = new IPatientEncounterVital[9];

        patientEncounterVital[0] = patientEncounterVitalProvider.get();
        patientEncounterVital[0].setDateTaken((triageService.getCurrentDateTime()));
        patientEncounterVital[0].setUserId(currentUser.getId());
        patientEncounterVital[0].setPatientEncounterId(patientEncounterServiceResponse.getResponseObject().getId());
        patientEncounterVital[0].setVitalId(1);
        patientEncounterVital[0].setVitalValue(viewModel.getRespiratoryRate().floatValue());

        patientEncounterVital[1] = patientEncounterVitalProvider.get();
        patientEncounterVital[1].setDateTaken((triageService.getCurrentDateTime()));
        patientEncounterVital[1].setUserId(currentUser.getId());
        patientEncounterVital[1].setPatientEncounterId(patientEncounterServiceResponse.getResponseObject().getId());
        patientEncounterVital[1].setVitalId(2);
        patientEncounterVital[1].setVitalValue(viewModel.getHeartRate().floatValue());

        patientEncounterVital[2] = patientEncounterVitalProvider.get();
        patientEncounterVital[2].setDateTaken((triageService.getCurrentDateTime()));
        patientEncounterVital[2].setUserId(currentUser.getId());
        patientEncounterVital[2].setPatientEncounterId(patientEncounterServiceResponse.getResponseObject().getId());
        patientEncounterVital[2].setVitalId(3);
        patientEncounterVital[2].setVitalValue(viewModel.getTemperature().floatValue());

        patientEncounterVital[3] = patientEncounterVitalProvider.get();
        patientEncounterVital[3].setDateTaken((triageService.getCurrentDateTime()));
        patientEncounterVital[3].setUserId(currentUser.getId());
        patientEncounterVital[3].setPatientEncounterId(patientEncounterServiceResponse.getResponseObject().getId());
        patientEncounterVital[3].setVitalId(4);
        patientEncounterVital[3].setVitalValue(viewModel.getOxygenSaturation().floatValue());

        patientEncounterVital[4] = patientEncounterVitalProvider.get();
        patientEncounterVital[4].setDateTaken((triageService.getCurrentDateTime()));
        patientEncounterVital[4].setUserId(currentUser.getId());
        patientEncounterVital[4].setPatientEncounterId(patientEncounterServiceResponse.getResponseObject().getId());
        patientEncounterVital[4].setVitalId(5);
        patientEncounterVital[4].setVitalValue(viewModel.getHeightFeet().floatValue());

        patientEncounterVital[5] = patientEncounterVitalProvider.get();
        patientEncounterVital[5].setDateTaken((triageService.getCurrentDateTime()));
        patientEncounterVital[5].setUserId(currentUser.getId());
        patientEncounterVital[5].setPatientEncounterId(patientEncounterServiceResponse.getResponseObject().getId());
        patientEncounterVital[5].setVitalId(6);
        patientEncounterVital[5].setVitalValue(viewModel.getHeightInches().floatValue());

        patientEncounterVital[6] = patientEncounterVitalProvider.get();
        patientEncounterVital[6].setDateTaken((triageService.getCurrentDateTime()));
        patientEncounterVital[6].setUserId(currentUser.getId());
        patientEncounterVital[6].setPatientEncounterId(patientEncounterServiceResponse.getResponseObject().getId());
        patientEncounterVital[6].setVitalId(7);
        patientEncounterVital[6].setVitalValue(viewModel.getWeight().floatValue());

        patientEncounterVital[7] = patientEncounterVitalProvider.get();
        patientEncounterVital[7].setDateTaken((triageService.getCurrentDateTime()));
        patientEncounterVital[7].setUserId(currentUser.getId());
        patientEncounterVital[7].setPatientEncounterId(patientEncounterServiceResponse.getResponseObject().getId());
        patientEncounterVital[7].setVitalId(8);
        patientEncounterVital[7].setVitalValue(viewModel.getBloodPressureSystolic().floatValue());

        patientEncounterVital[8] = patientEncounterVitalProvider.get();
        patientEncounterVital[8].setDateTaken((triageService.getCurrentDateTime()));
        patientEncounterVital[8].setUserId(currentUser.getId());
        patientEncounterVital[8].setPatientEncounterId(patientEncounterServiceResponse.getResponseObject().getId());
        patientEncounterVital[8].setVitalId(9);
        patientEncounterVital[8].setVitalValue(viewModel.getBloodPressureDiastolic().floatValue());

        patientEncounterVitals.addAll(Arrays.asList(patientEncounterVital));
        return patientEncounterVitals;
    }

}
