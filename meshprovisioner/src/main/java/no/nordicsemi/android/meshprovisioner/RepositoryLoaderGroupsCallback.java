package no.nordicsemi.android.meshprovisioner;

import java.util.List;

public interface RepositoryLoaderGroupsCallback {
    void onGroupsLoaded(List<Group> groupsList);
}
