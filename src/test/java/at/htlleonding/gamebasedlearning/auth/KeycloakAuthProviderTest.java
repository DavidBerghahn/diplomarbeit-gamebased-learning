package at.htlleonding.gamebasedlearning.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeycloakAuthProviderTest {
    @Test
    void recognizesTeacherOrganizationalUnit() {
        String distinguishedName = "CN=teacher,OU=Teachers,OU=HTL,DC=EDU,DC=HTL-LEONDING,DC=AC,DC=AT";

        assertTrue(KeycloakAuthProvider.isTeacherDistinguishedName(distinguishedName));
    }

    @Test
    void doesNotTreatStudentAsTeacher() {
        String distinguishedName = "CN=student,OU=5BHITM,OU=Students,OU=HTL,DC=EDU,DC=HTL-LEONDING,DC=AC,DC=AT";

        assertFalse(KeycloakAuthProvider.isTeacherDistinguishedName(distinguishedName));
    }

    @Test
    void rejectsMalformedDistinguishedName() {
        assertFalse(KeycloakAuthProvider.isTeacherDistinguishedName("not-an-ldap-name,"));
    }
}
