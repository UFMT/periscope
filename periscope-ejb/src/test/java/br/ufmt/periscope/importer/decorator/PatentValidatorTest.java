package br.ufmt.periscope.importer.decorator;

import br.ufmt.periscope.enumerated.ClassificationType;
import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.ApplicantType;
import br.ufmt.periscope.model.Classification;
import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.Inventor;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.State;
import br.ufmt.periscope.repository.ApplicantTypeRepository;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PatentValidatorTest {

    @Mock
    private ApplicantTypeRepository typeRepository;

    @InjectMocks
    private PatentValidator validator;

    @Test
    void validatesCompletePatentWithoutMutatingOtherPatents() {
        Patent complete = new Patent();
        complete.setTitleSelect("Solar Panel");
        complete.setApplicationNumber("BR102020001");
        complete.setApplicationDate(new Date());
        Classification ic = new Classification("H01L 31/04", ClassificationType.IC);
        Classification icai = new Classification("H02S 40/00", ClassificationType.ICAI);
        complete.setClassifications(List.of(ic, icai));
        Applicant app = new Applicant("ACME");
        Country br = new Country();
        br.setAcronym("BR");
        br.setStates(List.of(new State()));
        app.setCountry(br);
        app.setType(new ApplicantType("private.company"));
        complete.setApplicants(List.of(app));
        complete.setInventors(List.of(new Inventor("Ada Lovelace")));

        Patent incomplete = new Patent();
        incomplete.setTitleSelect("Draft");
        incomplete.setApplicants(null);
        incomplete.setInventors(null);
        incomplete.setPriorities(null);
        incomplete.setClassifications(null);

        validator.validate(complete);
        validator.validate(incomplete);

        assertThat(complete.getCompleted()).isTrue();
        assertThat(complete.getMainClassification().getValue()).isEqualTo("H02S 40/00");
        assertThat(app.getCountry().getStates()).isNull();
        verify(typeRepository).createIfNotExists(any(ApplicantType.class));
        assertThat(incomplete.getCompleted()).isFalse();
    }
}
