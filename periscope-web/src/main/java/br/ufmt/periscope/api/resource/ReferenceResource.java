package br.ufmt.periscope.api.resource;

import br.ufmt.periscope.api.dto.CountryDTO;
import br.ufmt.periscope.api.dto.StateDTO;
import br.ufmt.periscope.api.dto.UserSummaryDTO;
import br.ufmt.periscope.model.ApplicantType;
import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.State;
import br.ufmt.periscope.model.User;
import br.ufmt.periscope.repository.ApplicantTypeRepository;
import br.ufmt.periscope.repository.CountryRepository;
import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Sort;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/reference")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
@RolesAllowed({"ADMIN", "USER"})
@Tag(name = "Reference")
@SecurityRequirement(name = "bearerAuth")
public class ReferenceResource {

    @Inject
    private CountryRepository countryRepository;

    @Inject
    private ApplicantTypeRepository applicantTypeRepository;

    @Inject
    private Datastore ds;

    @GET
    @Path("/countries")
    @Operation(summary = "Listar países e estados cadastrados")
    public List<CountryDTO> listCountries() {
        return countryRepository.getAll().stream()
                .map(ReferenceResource::toCountryDto)
                .toList();
    }

    @GET
    @Path("/applicant-types")
    @Operation(summary = "Listar naturezas de depositantes")
    public List<String> listApplicantTypes() {
        return applicantTypeRepository.getAll().stream()
                .map(ApplicantType::getName)
                .filter(n -> n != null && !n.isBlank())
                .toList();
    }

    @GET
    @Path("/users")
    @Operation(summary = "Listar resumos de usuários para compartilhamento de projetos")
    public List<UserSummaryDTO> listUsers() {
        return ds.find(User.class)
                .iterator(new FindOptions().sort(Sort.ascending("username")))
                .toList()
                .stream()
                .map(UserSummaryDTO::from)
                .toList();
    }

    private static CountryDTO toCountryDto(Country country) {
        List<StateDTO> states = country.getStates() == null
                ? List.of()
                : country.getStates().stream()
                .filter(s -> s != null)
                .map(ReferenceResource::toStateDto)
                .toList();
        return new CountryDTO(country.getAcronym(), country.getName(), states);
    }

    private static StateDTO toStateDto(State state) {
        return new StateDTO(state.getAcronym(), state.getName(), state.getRegion());
    }
}
