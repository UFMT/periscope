package br.ufmt.periscope.bean;

import br.ufmt.periscope.model.User;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SeedBeanConfigTest {

    @Test
    void appliesEnvironmentOverridesToInitialAdmin() {
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword("123456");
        admin.setEmail("admin@gmail.com");

        Map<String, String> env = Map.of(
                "PERISCOPE_ADMIN_USERNAME", "rootadmin",
                "PERISCOPE_ADMIN_PASSWORD", "StrongPass#2026",
                "PERISCOPE_ADMIN_EMAIL", "sec@ufmt.br",
                "PERISCOPE_FORCE_ADMIN_PASSWORD_CHANGE", "true");

        SeedBean.applyAdminEnvOverrides(List.of(admin), env::get);

        assertThat(admin.getUsername()).isEqualTo("rootadmin");
        assertThat(admin.getPassword()).isEqualTo("StrongPass#2026");
        assertThat(admin.getEmail()).isEqualTo("sec@ufmt.br");
        assertThat(admin.getMustChangePassword()).isTrue();
    }

    @Test
    void leavesDefaultsIntactWhenEnvVarsUnset() {
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword("123456");
        admin.setEmail("admin@gmail.com");
        admin.setMustChangePassword(false);

        SeedBean.applyAdminEnvOverrides(List.of(admin), k -> null);

        assertThat(admin.getUsername()).isEqualTo("admin");
        assertThat(admin.getPassword()).isEqualTo("123456");
        assertThat(admin.getMustChangePassword()).isFalse();
    }
}
