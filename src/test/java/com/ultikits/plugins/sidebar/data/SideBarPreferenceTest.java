package com.ultikits.plugins.sidebar.data;

import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SideBarPreference Tests")
class SideBarPreferenceTest {

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("Should create preference with UUID and enabled state")
        void createWithParameters() {
            String uuid = UUID.randomUUID().toString();
            SideBarPreference pref = new SideBarPreference(uuid, true);

            assertThat(pref.getPlayerUuid()).isEqualTo(uuid);
            assertThat(pref.getEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should create preference with disabled state")
        void createDisabled() {
            String uuid = UUID.randomUUID().toString();
            SideBarPreference pref = new SideBarPreference(uuid, false);

            assertThat(pref.getPlayerUuid()).isEqualTo(uuid);
            assertThat(pref.getEnabled()).isFalse();
        }

        @Test
        @DisplayName("Should create with no-args constructor")
        void noArgsConstructor() {
            SideBarPreference pref = new SideBarPreference();

            assertThat(pref).isNotNull();
            assertThat(pref.getPlayerUuid()).isNull();
            assertThat(pref.getEnabled()).isNull();
        }
    }

    @Nested
    @DisplayName("Getters and Setters")
    class GettersAndSetters {

        @Test
        @DisplayName("Should set and get player UUID")
        void playerUuid() {
            SideBarPreference pref = new SideBarPreference();
            String uuid = UUID.randomUUID().toString();

            pref.setPlayerUuid(uuid);

            assertThat(pref.getPlayerUuid()).isEqualTo(uuid);
        }

        @Test
        @DisplayName("Should set and get enabled state")
        void enabled() {
            SideBarPreference pref = new SideBarPreference();

            pref.setEnabled(true);
            assertThat(pref.getEnabled()).isTrue();

            pref.setEnabled(false);
            assertThat(pref.getEnabled()).isFalse();
        }

        @Test
        @DisplayName("Should handle null enabled state")
        void nullEnabled() {
            SideBarPreference pref = new SideBarPreference();

            pref.setEnabled(null);

            assertThat(pref.getEnabled()).isNull();
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("Should be equal when same UUID and enabled state")
        void equalPreferences() {
            String uuid = UUID.randomUUID().toString();
            SideBarPreference pref1 = new SideBarPreference(uuid, true);
            SideBarPreference pref2 = new SideBarPreference(uuid, true);

            assertThat(pref1).isEqualTo(pref2);
            assertThat(pref1.hashCode()).isEqualTo(pref2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when different UUID")
        void differentUuid() {
            SideBarPreference pref1 = new SideBarPreference(UUID.randomUUID().toString(), true);
            SideBarPreference pref2 = new SideBarPreference(UUID.randomUUID().toString(), true);

            assertThat(pref1).isNotEqualTo(pref2);
        }

        @Test
        @DisplayName("Should not be equal when different enabled state")
        void differentEnabled() {
            String uuid = UUID.randomUUID().toString();
            SideBarPreference pref1 = new SideBarPreference(uuid, true);
            SideBarPreference pref2 = new SideBarPreference(uuid, false);

            assertThat(pref1).isNotEqualTo(pref2);
        }

        @Test
        @DisplayName("Should handle null in equals")
        void equalsNull() {
            SideBarPreference pref = new SideBarPreference(UUID.randomUUID().toString(), true);

            assertThat(pref).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Should be equal to itself")
        void equalsItself() {
            SideBarPreference pref = new SideBarPreference(UUID.randomUUID().toString(), true);

            assertThat(pref).isEqualTo(pref);
        }
    }

    @Nested
    @DisplayName("EqualsHashCodeContract")
    class EqualsHashCodeContract {

        @Test
        @DisplayName("Should not be equal to an instance of an unrelated type")
        void notEqualToUnrelatedType() {
            SideBarPreference pref = new SideBarPreference(UUID.randomUUID().toString(), true);

            assertThat(pref).isNotEqualTo("not a preference");
            assertThat(pref).isNotEqualTo(42);
        }

        @Test
        @DisplayName("Should not be equal to a subclass that rejects canEqual")
        void notEqualWhenCanEqualRejects() {
            String uuid = UUID.randomUUID().toString();
            SideBarPreference pref = new SideBarPreference(uuid, true);

            SideBarPreference notCanEqual = new SideBarPreference(uuid, true) {
                @Override
                public boolean canEqual(Object other) {
                    return false;
                }
            };

            assertThat(pref).isNotEqualTo(notCanEqual);
        }

        @Test
        @DisplayName("Should be equal between two default no-args instances")
        void defaultInstancesAreEqual() {
            SideBarPreference pref1 = new SideBarPreference();
            SideBarPreference pref2 = new SideBarPreference();

            assertThat(pref1).isEqualTo(pref2);
            assertThat(pref1.hashCode()).isEqualTo(pref2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when one playerUuid is null and the other is set")
        void nullPlayerUuidVsValue() {
            SideBarPreference withNullUuid = new SideBarPreference(null, true);
            SideBarPreference withUuid = new SideBarPreference(UUID.randomUUID().toString(), true);

            assertThat(withNullUuid).isNotEqualTo(withUuid);
            assertThat(withUuid).isNotEqualTo(withNullUuid);
        }

        @Test
        @DisplayName("Should not be equal when one enabled is null and the other is set")
        void nullEnabledVsValue() {
            String uuid = UUID.randomUUID().toString();
            SideBarPreference withNullEnabled = new SideBarPreference(uuid, null);
            SideBarPreference withEnabled = new SideBarPreference(uuid, true);

            assertThat(withNullEnabled).isNotEqualTo(withEnabled);
            assertThat(withEnabled).isNotEqualTo(withNullEnabled);
        }

        @Test
        @DisplayName("Should be equal when both playerUuid fields are null")
        void bothPlayerUuidNull() {
            SideBarPreference pref1 = new SideBarPreference(null, true);
            SideBarPreference pref2 = new SideBarPreference(null, true);

            assertThat(pref1).isEqualTo(pref2);
            assertThat(pref1.hashCode()).isEqualTo(pref2.hashCode());
        }

        @Test
        @DisplayName("Should be equal when both enabled fields are null")
        void bothEnabledNull() {
            String uuid = UUID.randomUUID().toString();
            SideBarPreference pref1 = new SideBarPreference(uuid, null);
            SideBarPreference pref2 = new SideBarPreference(uuid, null);

            assertThat(pref1).isEqualTo(pref2);
            assertThat(pref1.hashCode()).isEqualTo(pref2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when the inherited id differs, even with identical own fields")
        void notEqualWhenInheritedIdDiffers() {
            // @EqualsAndHashCode(callSuper = true) means the inherited BaseDataEntity.id must also
            // match; this is a distinct decision from the playerUuid/enabled comparisons above,
            // reached only when the superclass's own equals() check fails first.
            String uuid = UUID.randomUUID().toString();
            SideBarPreference pref1 = new SideBarPreference(uuid, true);
            pref1.setId("row-a");
            SideBarPreference pref2 = new SideBarPreference(uuid, true);
            pref2.setId("row-b");

            assertThat(pref1).isNotEqualTo(pref2);
        }
    }

    @Nested
    @DisplayName("ToString")
    class ToString {

        @Test
        @DisplayName("Should contain UUID and enabled state in toString")
        void toStringContent() {
            String uuid = UUID.randomUUID().toString();
            SideBarPreference pref = new SideBarPreference(uuid, true);

            String str = pref.toString();

            assertThat(str).contains(uuid);
            assertThat(str).contains("true");
        }
    }
}
